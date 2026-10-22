-- ============================================================
-- DENTRIX DENTAL · Esquema v3 · COMPROBANTE Y AUDITORÍA (paso 1 y 6)
-- Archivo ADITIVO: se ejecuta DESPUÉS de 16a_estado_documento_borrador.sql
--   psql -U postgres -d dentrix -f sql/16b_comprobante_auditoria.sql
--
-- QUÉ RESUELVE (P5 §6–§7)
--   1. El documento ya no se numera al nacer: `pago` puede estar BORRADOR, se edita,
--      y recién al CONFIRMAR recibe serie y correlativo y queda EMITIDO.
--   2. El detalle del comprobante vive en `comprobante_item` (concepto, cantidad,
--      precio, afectación IGV), no en la línea clínica del plan.
--   3. Anular exige motivo y no se puede si hay dinero imputado (eso pide nota de
--      crédito, declarada fuera de alcance).
--   4. Auditoría append-only de las tablas sensibles (paso 6): quién, cuándo y qué cambió.
--
-- QUÉ NO ENTRA
--   - Nota de crédito, CDR, XML UBL: fuera de alcance v3 (emisión simulada).
-- ============================================================
SET search_path TO dentrix;

-- ------------------------------------------------------------
-- 1. El documento puede nacer en borrador y sin número
-- ------------------------------------------------------------
ALTER TABLE pago DROP CONSTRAINT IF EXISTS pago_documento_o_abono;
ALTER TABLE pago ADD CONSTRAINT pago_documento_o_abono CHECK (
    (pago_origen_id IS NULL AND (numero IS NOT NULL OR estado = 'BORRADOR'))
    OR (pago_origen_id IS NOT NULL)
);

ALTER TABLE pago ADD COLUMN IF NOT EXISTS motivo_anulacion VARCHAR(200);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS anulada_por INT REFERENCES usuario(id);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS fecha_anulacion TIMESTAMP;
ALTER TABLE pago ADD COLUMN IF NOT EXISTS descuento NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (descuento >= 0);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS descuento_motivo VARCHAR(200);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS descuento_autorizado_por INT REFERENCES usuario(id);

-- ------------------------------------------------------------
-- 2. Detalle del comprobante
-- ------------------------------------------------------------
CREATE TABLE comprobante_item (
    id                   BIGSERIAL PRIMARY KEY,
    pago_id              BIGINT NOT NULL REFERENCES pago(id) ON DELETE CASCADE,
    presupuesto_item_id  BIGINT REFERENCES presupuesto_item(id),
    descripcion          VARCHAR(160) NOT NULL,
    cantidad             NUMERIC(6,2) NOT NULL CHECK (cantidad > 0),
    precio_unitario      NUMERIC(10,2) NOT NULL CHECK (precio_unitario >= 0),
    subtotal             NUMERIC(10,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED,
    afectacion_igv       CHAR(2) NOT NULL DEFAULT '10'
);
CREATE INDEX ix_comprobante_item ON comprobante_item (pago_id);
CREATE UNIQUE INDEX ux_comprobante_item_presupuesto ON comprobante_item (pago_id, presupuesto_item_id)
    WHERE presupuesto_item_id IS NOT NULL;

-- ------------------------------------------------------------
-- 3. Auditoría (paso 6)
-- ------------------------------------------------------------
CREATE TABLE auditoria (
    id             BIGSERIAL PRIMARY KEY,
    tabla          VARCHAR(40) NOT NULL,
    operacion      CHAR(1) NOT NULL CHECK (operacion IN ('I','U','D')),
    registro_id    BIGINT,
    usuario_id     INT,
    fecha          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    datos_antes    JSONB,
    datos_despues  JSONB
);
CREATE INDEX ix_auditoria_tabla ON auditoria (tabla, registro_id, fecha);

-- ------------------------------------------------------------
-- Reglas
-- ------------------------------------------------------------

-- Solo se edita el detalle de un borrador.
CREATE OR REPLACE FUNCTION comprobante_item_control() RETURNS TRIGGER AS $$
DECLARE v_estado estado_documento;
BEGIN
    SELECT estado INTO v_estado FROM pago
     WHERE id = CASE WHEN TG_OP = 'DELETE' THEN OLD.pago_id ELSE NEW.pago_id END;
    IF FOUND AND v_estado <> 'BORRADOR' THEN
        RAISE EXCEPTION 'P5: el comprobante % está % y su detalle no se edita',
            CASE WHEN TG_OP = 'DELETE' THEN OLD.pago_id ELSE NEW.pago_id END, v_estado;
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_comprobante_item_control BEFORE INSERT OR UPDATE OR DELETE ON comprobante_item
FOR EACH ROW EXECUTE FUNCTION comprobante_item_control();

-- Totales del documento a partir de su detalle (y su descuento).
CREATE OR REPLACE FUNCTION comprobante_item_total() RETURNS TRIGGER AS $$
DECLARE
    v_pago     BIGINT;
    v_bruto    NUMERIC(10,2);
    v_desc     NUMERIC(10,2);
    v_abonos   NUMERIC(10,2);
    v_neto     NUMERIC(10,2);
    v_igv_pct  NUMERIC(4,2);
    v_sub      NUMERIC(10,2);
BEGIN
    v_pago := COALESCE(NEW.pago_id, OLD.pago_id);
    SELECT COALESCE(SUM(subtotal), 0) INTO v_bruto FROM comprobante_item WHERE pago_id = v_pago;
    SELECT descuento INTO v_desc FROM pago WHERE id = v_pago;
    SELECT COALESCE(SUM(monto), 0) INTO v_abonos FROM pago WHERE pago_origen_id = v_pago;
    SELECT igv_porcentaje INTO v_igv_pct FROM configuracion WHERE id = 1;

    v_neto := GREATEST(v_bruto - COALESCE(v_desc, 0), 0);
    v_sub  := ROUND(v_neto / (1 + COALESCE(v_igv_pct, 0) / 100), 2);

    UPDATE pago
       SET monto = v_neto,
           subtotal = v_sub,
           igv = v_neto - v_sub,
           saldo_deudor = GREATEST(v_neto - v_abonos, 0),
           estado_pago = CASE WHEN v_abonos = 0 THEN 'PENDIENTE'::estado_pago
                              WHEN v_abonos >= v_neto THEN 'PAGADO'::estado_pago
                              ELSE 'PARCIAL'::estado_pago END
     WHERE id = v_pago AND estado <> 'ANULADO';
    RETURN NULL;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_comprobante_item_total AFTER INSERT OR UPDATE OR DELETE ON comprobante_item
FOR EACH ROW EXECUTE FUNCTION comprobante_item_total();

-- El documento emitido no cambia de identidad ni de cliente.
CREATE OR REPLACE FUNCTION pago_inmutable() RETURNS TRIGGER AS $$
BEGIN
    IF OLD.estado <> 'BORRADOR' THEN
        IF (NEW.numero, NEW.tipo, NEW.paciente_id, NEW.ficha_id, NEW.pago_origen_id, NEW.descuento)
           IS DISTINCT FROM
           (OLD.numero, OLD.tipo, OLD.paciente_id, OLD.ficha_id, OLD.pago_origen_id, OLD.descuento)
        THEN
            RAISE EXCEPTION 'P5: el comprobante % está % y no se edita; anule y emita otro',
                OLD.id, OLD.estado;
        END IF;
    END IF;

    IF NEW.estado IS DISTINCT FROM OLD.estado THEN
        IF NOT (
            (OLD.estado = 'BORRADOR' AND NEW.estado IN ('EMITIDO','ANULADO'))
            OR (OLD.estado = 'EMITIDO' AND NEW.estado = 'ANULADO')
        ) THEN
            RAISE EXCEPTION 'P5: el documento % no puede pasar de % a %', OLD.id, OLD.estado, NEW.estado;
        END IF;
    END IF;

    IF NEW.estado = 'ANULADO' AND OLD.estado <> 'ANULADO' THEN
        IF NEW.motivo_anulacion IS NULL OR btrim(NEW.motivo_anulacion) = '' THEN
            RAISE EXCEPTION 'P5: anular el documento % exige motivo', OLD.id;
        END IF;
        IF EXISTS (SELECT 1 FROM pago WHERE pago_origen_id = OLD.id AND estado <> 'ANULADO') THEN
            RAISE EXCEPTION 'P5: el documento % tiene pagos registrados; requiere nota de crédito (fuera de alcance v3)',
                OLD.id;
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_pago_inmutable BEFORE UPDATE ON pago
FOR EACH ROW EXECUTE FUNCTION pago_inmutable();

-- Borrador descartable; emitido o anulado no.
CREATE OR REPLACE FUNCTION bloquea_delete_pago() RETURNS TRIGGER AS $$
BEGIN
    IF OLD.estado = 'BORRADOR' AND OLD.pago_origen_id IS NULL THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'P5: el documento % está % y no se borra; se anula con motivo', OLD.id, OLD.estado;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_pago_no_delete BEFORE DELETE ON pago
FOR EACH ROW EXECUTE FUNCTION bloquea_delete_pago();

-- Al emitir: hace falta número y al menos un ítem.
CREATE OR REPLACE FUNCTION pago_al_emitir() RETURNS TRIGGER AS $$
DECLARE v_items INT;
BEGIN
    IF NEW.estado = 'EMITIDO' AND OLD.estado = 'BORRADOR' THEN
        IF NEW.numero IS NULL THEN
            RAISE EXCEPTION 'P5: el documento % no puede emitirse sin serie y correlativo', OLD.id;
        END IF;
        SELECT COUNT(*) INTO v_items FROM comprobante_item WHERE pago_id = NEW.id;
        IF v_items = 0 THEN
            RAISE EXCEPTION 'P5: el documento % no tiene ítems; no se emite', OLD.id;
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_pago_al_emitir BEFORE UPDATE ON pago
FOR EACH ROW EXECUTE FUNCTION pago_al_emitir();

-- Auditoría append-only.
CREATE OR REPLACE FUNCTION audita() RETURNS TRIGGER AS $$
DECLARE v_usuario INT;
BEGIN
    v_usuario := NULLIF(coalesce(current_setting('dentrix.usuario', true), ''), '')::INT;
    INSERT INTO auditoria (tabla, operacion, registro_id, usuario_id, datos_antes, datos_despues)
    VALUES (TG_TABLE_NAME,
            CASE TG_OP WHEN 'INSERT' THEN 'I' WHEN 'UPDATE' THEN 'U' ELSE 'D' END,
            CASE WHEN TG_OP = 'DELETE' THEN (to_jsonb(OLD)->>'id')::BIGINT
                 ELSE (to_jsonb(NEW)->>'id')::BIGINT END,
            v_usuario,
            CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN to_jsonb(OLD) END,
            CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN to_jsonb(NEW) END);
    RETURN NULL;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_audita_presupuesto      AFTER INSERT OR UPDATE OR DELETE ON presupuesto      FOR EACH ROW EXECUTE FUNCTION audita();
CREATE TRIGGER tr_audita_presupuesto_item AFTER INSERT OR UPDATE OR DELETE ON presupuesto_item FOR EACH ROW EXECUTE FUNCTION audita();
CREATE TRIGGER tr_audita_enmienda         AFTER INSERT OR UPDATE OR DELETE ON enmienda         FOR EACH ROW EXECUTE FUNCTION audita();
CREATE TRIGGER tr_audita_pago             AFTER INSERT OR UPDATE OR DELETE ON pago             FOR EACH ROW EXECUTE FUNCTION audita();
CREATE TRIGGER tr_audita_ejecucion        AFTER INSERT OR UPDATE OR DELETE ON ejecucion_item   FOR EACH ROW EXECUTE FUNCTION audita();
CREATE TRIGGER tr_audita_orden_lab        AFTER INSERT OR UPDATE OR DELETE ON orden_laboratorio FOR EACH ROW EXECUTE FUNCTION audita();

CREATE OR REPLACE FUNCTION bloquea_delete_auditoria() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Paso 6: la auditoría es append-only';
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_auditoria_no_delete BEFORE DELETE ON auditoria
FOR EACH ROW EXECUTE FUNCTION bloquea_delete_auditoria();

-- Ranking de tratamientos: suma lo viejo (plan_tratamiento) y lo nuevo (comprobante_item).
CREATE OR REPLACE VIEW v_ranking_tratamientos AS
SELECT t.id AS tratamiento_id, t.codigo, t.nombre, t.especialidad,
       SUM(x.cantidad)::BIGINT AS unidades_cobradas, SUM(x.subtotal) AS monto_cobrado
  FROM (
        SELECT pl.tratamiento_id, pl.cantidad, pl.subtotal
          FROM plan_tratamiento pl
         WHERE pl.estado = 'COBRADO'
        UNION ALL
        SELECT i.tratamiento_id, ci.cantidad, ci.subtotal
          FROM comprobante_item ci
          JOIN presupuesto_item i ON i.id = ci.presupuesto_item_id
          JOIN pago p ON p.id = ci.pago_id
         WHERE p.estado = 'EMITIDO'
       ) x
  JOIN tratamiento t ON t.id = x.tratamiento_id
 GROUP BY t.id, t.codigo, t.nombre, t.especialidad;

COMMENT ON TABLE comprobante_item IS 'P5 · detalle del comprobante: concepto, cantidad, precio y afectación IGV';
COMMENT ON TABLE auditoria        IS 'Paso 6 · bitácora append-only de cambios en las tablas sensibles';
COMMENT ON COLUMN pago.descuento  IS 'P5 · descuento del documento (R20); se resta del bruto del detalle';
