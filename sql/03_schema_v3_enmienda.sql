-- ============================================================
-- DENTRIX DENTAL · Esquema v3 · ENMIENDA DEL PLAN ACEPTADO
-- Archivo ADITIVO: se ejecuta DESPUÉS de 02_schema_v3_plan_presupuesto.sql
--   psql -U postgres -d dentrix -f sql/03_schema_v3_enmienda.sql
--
-- QUÉ RESUELVE (P3 §16 punto 8)
--   Un presupuesto ACEPTADO es inmutable (R22), pero el plan puede cambiar: el paciente
--   amplía (agrega un tratamiento) o reduce (renuncia a uno). No se edita el presupuesto
--   ni se hace uno nuevo: se firma una ENMIENDA con el delta.
--
-- REGLAS QUE IMPONE LA BASE
--   1. Nada se agrega ni se anula sin una enmienda FIRMADA.
--   2. No se anula un ítem con dinero imputado (P5 §2.1: sin anticipos ni saldo a favor).
--   3. No se anula un ítem ya ejecutado (lo hecho, hecho está).
--   4. El ítem agregado entra con precio congelado: la suba del tarifario no lo toca (R21).
--   5. El encabezado (subtotal/descuento) solo cambia por la función firmar_enmienda().
--   6. Nada se borra: enmienda y detalle no admiten DELETE; se anulan con estado.
--
-- COMPATIBILIDAD
--   No toca ninguna tabla del v2 ni las 5 del paso 3. Solo agrega dos columnas a
--   presupuesto_item (enmienda_id y motivo_anulacion) y redifine presupuesto_inmutable()
--   para que el encabezado pueda actualizarse únicamente desde firmar_enmienda().
-- ============================================================
SET search_path TO dentrix;

-- ------------------------------------------------------------
-- Tipos
-- ------------------------------------------------------------
CREATE TYPE tipo_enmienda   AS ENUM ('AMPLIACION','REDUCCION','MIXTA');
CREATE TYPE estado_enmienda AS ENUM ('BORRADOR','FIRMADA','RECHAZADA','ANULADA');
CREATE TYPE accion_enmienda AS ENUM ('AGREGAR','ANULAR');

-- ------------------------------------------------------------
-- 1. Enmienda (documento firmado con el delta)
-- ------------------------------------------------------------
CREATE TABLE enmienda (
    id                        BIGSERIAL PRIMARY KEY,
    presupuesto_id            BIGINT NOT NULL REFERENCES presupuesto(id),
    numero                    INT NOT NULL CHECK (numero > 0),
    tipo                      tipo_enmienda,                       -- se fija al firmar
    motivo                    VARCHAR(200) NOT NULL,
    estado                    estado_enmienda NOT NULL DEFAULT 'BORRADOR',
    descuento_adicional       NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (descuento_adicional >= 0),
    descuento_motivo          VARCHAR(200),
    descuento_autorizado_por  INT REFERENCES usuario(id),           -- R20
    firmante_nombre           VARCHAR(120),
    firmante_documento        VARCHAR(20),
    firmante_rol              VARCHAR(10) CHECK (firmante_rol IN ('PACIENTE','TUTOR','PAGADOR')),
    fecha_firma               TIMESTAMP,
    firma_hash                CHAR(64),
    firmado_por               INT REFERENCES usuario(id),           -- usuario que registró la firma
    creado_por                INT NOT NULL REFERENCES usuario(id),
    fecha_creacion            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT enmienda_numero_unico UNIQUE (presupuesto_id, numero),
    CONSTRAINT enmienda_descuento_autorizado CHECK (
        descuento_adicional = 0 OR descuento_autorizado_por IS NOT NULL
    ),
    CONSTRAINT enmienda_firmada_completa CHECK (
        estado <> 'FIRMADA'
        OR (tipo IS NOT NULL AND firmante_nombre IS NOT NULL AND firmante_documento IS NOT NULL
            AND firmante_rol IS NOT NULL AND fecha_firma IS NOT NULL AND firma_hash IS NOT NULL)
    )
);
CREATE INDEX ix_enmienda_presupuesto ON enmienda (presupuesto_id, estado);

-- ------------------------------------------------------------
-- 2. Columnas nuevas en el ítem
-- ------------------------------------------------------------
ALTER TABLE presupuesto_item
    ADD COLUMN enmienda_id      BIGINT REFERENCES enmienda(id),   -- NULL = ítem original
    ADD COLUMN motivo_anulacion VARCHAR(200);

ALTER TABLE presupuesto_item
    ADD CONSTRAINT item_anulado_con_motivo CHECK (
        estado <> 'ANULADO' OR motivo_anulacion IS NOT NULL
    );

-- ------------------------------------------------------------
-- 3. Detalle de la enmienda
-- ------------------------------------------------------------
CREATE TABLE enmienda_item (
    id                  BIGSERIAL PRIMARY KEY,
    enmienda_id         BIGINT NOT NULL REFERENCES enmienda(id),
    presupuesto_item_id BIGINT NOT NULL REFERENCES presupuesto_item(id),
    accion              accion_enmienda NOT NULL,
    motivo              VARCHAR(200),
    CONSTRAINT enmienda_item_unico UNIQUE (enmienda_id, presupuesto_item_id)
);
CREATE INDEX ix_enmienda_item ON enmienda_item (presupuesto_item_id);

-- ------------------------------------------------------------
-- Reglas en la base
-- ------------------------------------------------------------

-- Nada se borra: enmienda y detalle se anulan con estado.
CREATE OR REPLACE FUNCTION bloquea_delete_enmienda() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'R22: % no admite DELETE; use el estado de anulación', TG_TABLE_NAME;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_enmienda_no_delete      BEFORE DELETE ON enmienda      FOR EACH ROW EXECUTE FUNCTION bloquea_delete_enmienda();
CREATE TRIGGER tr_enmienda_item_no_delete BEFORE DELETE ON enmienda_item FOR EACH ROW EXECUTE FUNCTION bloquea_delete_enmienda();

-- Numeración correlativa por presupuesto.
CREATE OR REPLACE FUNCTION numerar_enmienda() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.numero IS NULL THEN
        SELECT COALESCE(MAX(numero), 0) + 1 INTO NEW.numero
          FROM enmienda WHERE presupuesto_id = NEW.presupuesto_id;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_enmienda_numera BEFORE INSERT ON enmienda
FOR EACH ROW EXECUTE FUNCTION numerar_enmienda();

-- Firmada la enmienda, su contenido no cambia (solo el estado puede anularse).
CREATE OR REPLACE FUNCTION enmienda_inmutable() RETURNS TRIGGER AS $$
BEGIN
    IF OLD.estado <> 'BORRADOR'
       AND (NEW.presupuesto_id, NEW.motivo, NEW.descuento_adicional, NEW.descuento_motivo,
            NEW.firmante_nombre, NEW.firmante_documento, NEW.firmante_rol, NEW.firma_hash)
           IS DISTINCT FROM
           (OLD.presupuesto_id, OLD.motivo, OLD.descuento_adicional, OLD.descuento_motivo,
            OLD.firmante_nombre, OLD.firmante_documento, OLD.firmante_rol, OLD.firma_hash)
    THEN
        RAISE EXCEPTION 'R22: la enmienda % está % y no se edita; abra otra', OLD.id, OLD.estado;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_enmienda_inmutable BEFORE UPDATE ON enmienda
FOR EACH ROW EXECUTE FUNCTION enmienda_inmutable();

-- El detalle solo se toca mientras la enmienda está en borrador.
CREATE OR REPLACE FUNCTION enmienda_item_control() RETURNS TRIGGER AS $$
DECLARE v_estado estado_enmienda;
BEGIN
    SELECT estado INTO v_estado FROM enmienda
     WHERE id = COALESCE(NEW.enmienda_id, OLD.enmienda_id);
    IF v_estado <> 'BORRADOR' THEN
        RAISE EXCEPTION 'R22: la enmienda % está % y su detalle no se edita', 
            COALESCE(NEW.enmienda_id, OLD.enmienda_id), v_estado;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_enmienda_item_control BEFORE INSERT OR UPDATE ON enmienda_item
FOR EACH ROW EXECUTE FUNCTION enmienda_item_control();

-- Un ítem solo puede ANULARSE dentro de una enmienda que se está firmando.
CREATE OR REPLACE FUNCTION item_anulacion_control() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.estado = 'ANULADO' AND OLD.estado <> 'ANULADO' THEN
        IF NEW.motivo_anulacion IS NULL OR btrim(NEW.motivo_anulacion) = '' THEN
            RAISE EXCEPTION 'R22: el ítem % necesita motivo de anulación', NEW.id;
        END IF;
        IF coalesce(current_setting('dentrix.enmienda', true), '') = '' THEN
            RAISE EXCEPTION 'R22: el ítem % solo se anula con una enmienda firmada', NEW.id;
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_item_anulacion BEFORE UPDATE OF estado ON presupuesto_item
FOR EACH ROW EXECUTE FUNCTION item_anulacion_control();

-- Un ítem agregado solo pasa a ACEPTADO cuando su enmienda está FIRMADA.
CREATE OR REPLACE FUNCTION item_agregado_control() RETURNS TRIGGER AS $$
DECLARE v_estado estado_enmienda;
BEGIN
    IF NEW.estado = 'ACEPTADO' AND OLD.estado = 'PROPUESTO' AND NEW.enmienda_id IS NOT NULL THEN
        SELECT estado INTO v_estado FROM enmienda WHERE id = NEW.enmienda_id;
        IF v_estado <> 'FIRMADA' THEN
            RAISE EXCEPTION 'R7: el ítem % se agregó por la enmienda %, que está %',
                NEW.id, NEW.enmienda_id, v_estado;
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_item_agregado BEFORE UPDATE OF estado ON presupuesto_item
FOR EACH ROW EXECUTE FUNCTION item_agregado_control();

-- Un ítem nuevo en un presupuesto ya aceptado solo puede venir de una enmienda.
CREATE OR REPLACE FUNCTION item_nuevo_control() RETURNS TRIGGER AS $$
DECLARE
    v_estado_pres estado_presupuesto;
    v_estado_enm  estado_enmienda;
    v_pres_enm    BIGINT;
BEGIN
    SELECT estado INTO v_estado_pres FROM presupuesto WHERE id = NEW.presupuesto_id;
    IF v_estado_pres = 'BORRADOR' THEN
        RETURN NEW;
    END IF;
    IF NEW.enmienda_id IS NULL THEN
        RAISE EXCEPTION 'R22: el presupuesto % está %; un ítem nuevo debe venir de una enmienda',
            NEW.presupuesto_id, v_estado_pres;
    END IF;
    SELECT estado, presupuesto_id INTO v_estado_enm, v_pres_enm
      FROM enmienda WHERE id = NEW.enmienda_id;
    IF v_estado_enm <> 'BORRADOR' OR v_pres_enm <> NEW.presupuesto_id THEN
        RAISE EXCEPTION 'R22: el ítem no puede nacer de la enmienda % (estado %) para el presupuesto %',
            NEW.enmienda_id, v_estado_enm, v_pres_enm;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_item_nuevo BEFORE INSERT ON presupuesto_item
FOR EACH ROW EXECUTE FUNCTION item_nuevo_control();

-- R22 · el encabezado solo cambia por firmar_enmienda() y solo para reflejar los ítems.
CREATE OR REPLACE FUNCTION presupuesto_inmutable() RETURNS TRIGGER AS $$
DECLARE
    v_enmienda BIGINT;
    v_suma     NUMERIC(10,2);
    v_adic     NUMERIC(10,2);
BEGIN
    IF OLD.estado = 'BORRADOR' THEN
        RETURN NEW;
    END IF;

    v_enmienda := NULLIF(coalesce(current_setting('dentrix.enmienda', true), ''), '')::BIGINT;

    IF v_enmienda IS NOT NULL THEN
        SELECT descuento_adicional INTO v_adic
          FROM enmienda
         WHERE id = v_enmienda AND presupuesto_id = OLD.id AND estado = 'FIRMADA';
        SELECT COALESCE(SUM(subtotal), 0) INTO v_suma
          FROM presupuesto_item
         WHERE presupuesto_id = OLD.id AND estado <> 'ANULADO';

        IF v_adic IS NULL THEN
            RAISE EXCEPTION 'R22: la enmienda % no pertenece al presupuesto % o no está firmada',
                v_enmienda, OLD.id;
        END IF;
        IF NEW.subtotal <> v_suma THEN
            RAISE EXCEPTION 'R22: el subtotal del presupuesto % debe ser % (suma de ítems vigentes), no %',
                OLD.id, v_suma, NEW.subtotal;
        END IF;
        IF NEW.descuento <> OLD.descuento + v_adic THEN
            RAISE EXCEPTION 'R22: el descuento solo puede crecer en el de la enmienda (%): % -> %',
                v_adic, OLD.descuento, NEW.descuento;
        END IF;
        IF NEW.vigencia_dias <> OLD.vigencia_dias OR NEW.moneda <> OLD.moneda THEN
            RAISE EXCEPTION 'R22: vigencia y moneda del presupuesto % no cambian por enmienda', OLD.id;
        END IF;
        RETURN NEW;
    END IF;

    IF (NEW.subtotal, NEW.descuento, NEW.vigencia_dias, NEW.moneda)
       IS DISTINCT FROM (OLD.subtotal, OLD.descuento, OLD.vigencia_dias, OLD.moneda)
    THEN
        RAISE EXCEPTION 'R22: el presupuesto % está en % y no se edita; cree una versión nueva o una enmienda',
            OLD.id, OLD.estado;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

-- R7 · no se ejecuta un ítem que no esté ACEPTADO (cierra el salto PROPUESTO → EN_EJECUCION).
CREATE OR REPLACE FUNCTION item_requiere_aceptacion() RETURNS TRIGGER AS $$
DECLARE v_pres estado_presupuesto; v_cons INT;
BEGIN
    IF NEW.estado IN ('EN_EJECUCION','EJECUTADO','CONTROLADO')
       AND OLD.estado NOT IN ('EN_EJECUCION','EJECUTADO','CONTROLADO')
    THEN
        IF OLD.estado <> 'ACEPTADO' THEN
            RAISE EXCEPTION 'R7: el ítem % está en % y no se puede ejecutar sin aceptación', NEW.id, OLD.estado;
        END IF;
        SELECT estado INTO v_pres FROM presupuesto WHERE id = NEW.presupuesto_id;
        IF v_pres <> 'ACEPTADO' THEN
            RAISE EXCEPTION 'R7: el ítem % no se puede ejecutar; su presupuesto está en %',
                NEW.id, v_pres;
        END IF;
        IF NEW.requiere_consentimiento THEN
            SELECT COUNT(*) INTO v_cons FROM consentimiento
             WHERE presupuesto_item_id = NEW.id AND estado = 'FIRMADO';
            IF v_cons = 0 THEN
                RAISE EXCEPTION 'R7: el ítem % exige consentimiento firmado', NEW.id;
            END IF;
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

-- ------------------------------------------------------------
-- firmar_enmienda: el único camino para aplicar el delta
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION firmar_enmienda(p_enmienda BIGINT, p_usuario INT)
RETURNS VOID AS $$
DECLARE
    v_presupuesto BIGINT;
    v_estado      estado_enmienda;
    v_est_pres    estado_presupuesto;
    v_descuento   NUMERIC(10,2);
    v_motivo      VARCHAR(200);
    v_agrega      INT;
    v_anula       INT;
    v_item        RECORD;
    v_suma        NUMERIC(10,2);
BEGIN
    SELECT presupuesto_id, estado, descuento_adicional, motivo
      INTO v_presupuesto, v_estado, v_descuento, v_motivo
      FROM enmienda WHERE id = p_enmienda FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La enmienda % no existe', p_enmienda;
    END IF;
    IF v_estado <> 'BORRADOR' THEN
        RAISE EXCEPTION 'La enmienda % está en % y no se puede firmar', p_enmienda, v_estado;
    END IF;

    SELECT estado INTO v_est_pres FROM presupuesto WHERE id = v_presupuesto FOR UPDATE;
    IF v_est_pres <> 'ACEPTADO' THEN
        RAISE EXCEPTION 'Solo se enmienda un presupuesto ACEPTADO; el % está en %',
            v_presupuesto, v_est_pres;
    END IF;

    SELECT COUNT(*) FILTER (WHERE accion = 'AGREGAR'),
           COUNT(*) FILTER (WHERE accion = 'ANULAR')
      INTO v_agrega, v_anula
      FROM enmienda_item WHERE enmienda_id = p_enmienda;
    IF v_agrega + v_anula = 0 THEN
        RAISE EXCEPTION 'La enmienda % no tiene ítems: no hay nada que firmar', p_enmienda;
    END IF;

    -- Validación de lo que se anula: no pagado, no ejecutado, estado permitido.
    FOR v_item IN
        SELECT i.id, i.estado, COALESCE(ei.motivo, v_motivo) AS motivo,
               (SELECT COUNT(*) FROM imputacion m WHERE m.presupuesto_item_id = i.id) AS pagos
          FROM enmienda_item ei
          JOIN presupuesto_item i ON i.id = ei.presupuesto_item_id
         WHERE ei.enmienda_id = p_enmienda AND ei.accion = 'ANULAR'
    LOOP
        IF v_item.estado IN ('EN_EJECUCION','EJECUTADO','CONTROLADO') THEN
            RAISE EXCEPTION 'No se anula el ítem %: ya está %', v_item.id, v_item.estado;
        END IF;
        IF v_item.estado NOT IN ('PROPUESTO','ACEPTADO','DIFERIDO') THEN
            RAISE EXCEPTION 'No se anula el ítem %: está %', v_item.id, v_item.estado;
        END IF;
        IF v_item.pagos > 0 THEN
            RAISE EXCEPTION 'No se anula el ítem %: tiene % imputación(es); resuélvalo en caja (fuera de alcance v3)',
                v_item.id, v_item.pagos;
        END IF;
    END LOOP;

    -- Validación de lo que se agrega: debe haber nacido con esta enmienda.
    FOR v_item IN
        SELECT i.id, i.estado, i.enmienda_id
          FROM enmienda_item ei
          JOIN presupuesto_item i ON i.id = ei.presupuesto_item_id
         WHERE ei.enmienda_id = p_enmienda AND ei.accion = 'AGREGAR'
    LOOP
        IF v_item.enmienda_id IS DISTINCT FROM p_enmienda THEN
            RAISE EXCEPTION 'El ítem % no fue creado por la enmienda %', v_item.id, p_enmienda;
        END IF;
        IF v_item.estado <> 'PROPUESTO' THEN
            RAISE EXCEPTION 'El ítem agregado % debe estar PROPUESTO; está %', v_item.id, v_item.estado;
        END IF;
    END LOOP;

    -- 1) La enmienda queda firmada (el encabezado la leerá así).
    UPDATE enmienda
       SET estado = 'FIRMADA',
           tipo = CASE WHEN v_agrega > 0 AND v_anula > 0 THEN 'MIXTA'::tipo_enmienda
                       WHEN v_agrega > 0 THEN 'AMPLIACION'::tipo_enmienda
                       ELSE 'REDUCCION'::tipo_enmienda END,
           fecha_firma = COALESCE(fecha_firma, CURRENT_TIMESTAMP),
           firmado_por = p_usuario
     WHERE id = p_enmienda;

    -- 2) Ítems anulados (el trigger exige el contexto de enmienda).
    PERFORM set_config('dentrix.enmienda', p_enmienda::text, true);
    FOR v_item IN
        SELECT i.id, COALESCE(ei.motivo, v_motivo) AS motivo
          FROM enmienda_item ei
          JOIN presupuesto_item i ON i.id = ei.presupuesto_item_id
         WHERE ei.enmienda_id = p_enmienda AND ei.accion = 'ANULAR'
    LOOP
        UPDATE presupuesto_item
           SET estado = 'ANULADO', motivo_anulacion = v_item.motivo
         WHERE id = v_item.id;
    END LOOP;

    -- 3) Ítems agregados: precio congelado desde la firma (R21).
    UPDATE presupuesto_item i
       SET estado = 'ACEPTADO', precio_congelado = TRUE
      FROM enmienda_item ei
     WHERE ei.enmienda_id = p_enmienda AND ei.accion = 'AGREGAR'
       AND ei.presupuesto_item_id = i.id;

    -- 4) Encabezado = plan vigente (solo permitido dentro de esta enmienda).
    SELECT COALESCE(SUM(subtotal), 0) INTO v_suma
      FROM presupuesto_item
     WHERE presupuesto_id = v_presupuesto AND estado <> 'ANULADO';
    UPDATE presupuesto
       SET subtotal = v_suma,
           descuento = descuento + v_descuento
     WHERE id = v_presupuesto;
    PERFORM set_config('dentrix.enmienda', '', true);
END $$ LANGUAGE plpgsql SET search_path = dentrix;

-- ------------------------------------------------------------
-- Vistas
-- ------------------------------------------------------------

-- Plan vigente: los ítems que siguen en pie, con su saldo.
CREATE VIEW v_plan_vigente AS
SELECT i.presupuesto_id,
       i.id                                  AS item_id,
       i.tratamiento_id,
       i.pieza_fdi,
       i.fase,
       i.prioridad,
       i.secuencia,
       i.cantidad,
       i.precio_unitario,
       i.subtotal,
       i.estado,
       COALESCE(s.imputado, 0)               AS imputado,
       i.subtotal - COALESCE(s.imputado, 0)  AS saldo,
       i.enmienda_id
  FROM presupuesto_item i
  LEFT JOIN v_saldo_item s ON s.item_id = i.id
 WHERE i.estado <> 'ANULADO';

-- Enmiendas con su delta resumido.
CREATE VIEW v_enmienda_resumen AS
SELECT e.id,
       e.presupuesto_id,
       e.numero,
       e.tipo,
       e.estado,
       e.motivo,
       e.descuento_adicional,
       e.fecha_creacion,
       e.fecha_firma,
       COUNT(*) FILTER (WHERE ei.accion = 'AGREGAR') AS items_agregados,
       COUNT(*) FILTER (WHERE ei.accion = 'ANULAR')  AS items_anulados
  FROM enmienda e
  LEFT JOIN enmienda_item ei ON ei.enmienda_id = e.id
 GROUP BY e.id;

COMMENT ON TABLE enmienda        IS 'P3 §16.8 · enmienda firmada de un presupuesto ACEPTADO (amplía o reduce)';
COMMENT ON TABLE enmienda_item   IS 'P3 · detalle de la enmienda: qué ítem se agrega o se anula y por qué';
COMMENT ON FUNCTION firmar_enmienda(BIGINT, INT) IS 'Único camino para aplicar una enmienda; valida dinero, ejecución y precio congelado';
