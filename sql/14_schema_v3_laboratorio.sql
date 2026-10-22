-- ============================================================
-- DENTRIX DENTAL · Esquema v3 · P4-B: LABORATORIO Y LOTE
-- Archivo ADITIVO: se ejecuta DESPUÉS de 13_schema_v3_sesion_borrador.sql
--   psql -U postgres -d dentrix -f sql/14_schema_v3_laboratorio.sql
--
-- QUÉ RESUELVE (P4 §7 pasos 6–9 · R18 y R19)
--   1. No existía orden de laboratorio: ahora `orden_laboratorio` nace SOLICITADA,
--      se recibe, se controla (ajuste/color/contactos) y solo si es CONFORME se instala.
--   2. El control de calidad es un documento (`control_laboratorio`); un rechazo exige
--      observación y da lugar a un reproceso que referencia la orden anterior.
--   3. No había trazabilidad de insumos: `insumo`, `lote_insumo` y `consumo_lote`
--      vinculan lote, paciente, pieza, fecha y profesional (R19). Sin inventario.
--   4. Un ítem cuyo tratamiento exige laboratorio no pasa a EJECUTADO sin orden CONFORME.
--
-- QUÉ NO ENTRA (declarado)
--   - Inventario, compras, stock mínimo, esterilización: fuera de los seis mapas.
--   - Anticipo mínimo para laboratorio (R28): los anticipos están fuera de alcance v3.
-- ============================================================
SET search_path TO dentrix;

CREATE TYPE estado_orden_lab AS ENUM ('SOLICITADA','RECIBIDA','CONFORME','RECHAZADA','INSTALADA','ANULADA');

-- El catálogo dice qué prestaciones pasan por laboratorio.
ALTER TABLE tratamiento ADD COLUMN IF NOT EXISTS requiere_laboratorio BOOLEAN NOT NULL DEFAULT FALSE;

-- ------------------------------------------------------------
-- 1. Insumos y lotes (trazabilidad, no inventario)
-- ------------------------------------------------------------
CREATE TABLE insumo (
    id             SERIAL PRIMARY KEY,
    codigo         VARCHAR(20) NOT NULL UNIQUE,
    nombre         VARCHAR(120) NOT NULL,
    unidad         VARCHAR(20) NOT NULL DEFAULT 'UNIDAD',
    requiere_lote  BOOLEAN NOT NULL DEFAULT FALSE,
    estado         VARCHAR(15) NOT NULL DEFAULT 'ACTIVO'
);

CREATE TABLE lote_insumo (
    id          BIGSERIAL PRIMARY KEY,
    insumo_id   INT NOT NULL REFERENCES insumo(id),
    numero      VARCHAR(60) NOT NULL,
    vence       DATE,
    CONSTRAINT lote_unico UNIQUE (insumo_id, numero)
);

CREATE TABLE consumo_lote (
    id                  BIGSERIAL PRIMARY KEY,
    ejecucion_item_id   BIGINT NOT NULL REFERENCES ejecucion_item(id),
    lote_insumo_id      BIGINT NOT NULL REFERENCES lote_insumo(id),
    cantidad            NUMERIC(10,2) NOT NULL CHECK (cantidad > 0),
    fecha               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id          INT NOT NULL REFERENCES usuario(id),
    CONSTRAINT consumo_unico UNIQUE (ejecucion_item_id, lote_insumo_id)
);
CREATE INDEX ix_consumo_lote ON consumo_lote (lote_insumo_id);

-- ------------------------------------------------------------
-- 2. Orden de laboratorio y su control
-- ------------------------------------------------------------
CREATE TABLE orden_laboratorio (
    id                   BIGSERIAL PRIMARY KEY,
    presupuesto_item_id  BIGINT NOT NULL REFERENCES presupuesto_item(id),
    ejecucion_item_id    BIGINT REFERENCES ejecucion_item(id),
    laboratorio          VARCHAR(120) NOT NULL,
    trabajo              VARCHAR(160) NOT NULL,
    material             VARCHAR(80),
    color                VARCHAR(40),
    fecha_solicitud      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_prometida      DATE,
    estado               estado_orden_lab NOT NULL DEFAULT 'SOLICITADA',
    costo                NUMERIC(10,2) CHECK (costo >= 0),
    orden_origen_id      BIGINT REFERENCES orden_laboratorio(id),
    motivo_rechazo       VARCHAR(200),
    motivo_anulacion     VARCHAR(200),
    CONSTRAINT orden_prometida_valida CHECK (
        fecha_prometida IS NULL OR fecha_prometida >= fecha_solicitud::date
    ),
    CONSTRAINT orden_rechazada_con_motivo CHECK (
        estado <> 'RECHAZADA' OR (motivo_rechazo IS NOT NULL AND btrim(motivo_rechazo) <> '')
    ),
    CONSTRAINT orden_anulada_con_motivo CHECK (
        estado <> 'ANULADA' OR (motivo_anulacion IS NOT NULL AND btrim(motivo_anulacion) <> '')
    )
);
CREATE INDEX ix_orden_item ON orden_laboratorio (presupuesto_item_id);
-- Una sola orden activa por ítem; el reproceso entra cuando la anterior quedó RECHAZADA.
CREATE UNIQUE INDEX ux_orden_activa ON orden_laboratorio (presupuesto_item_id)
    WHERE estado IN ('SOLICITADA','RECIBIDA','CONFORME');

CREATE TABLE control_laboratorio (
    id            BIGSERIAL PRIMARY KEY,
    orden_id      BIGINT NOT NULL REFERENCES orden_laboratorio(id),
    fecha         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ajuste        VARCHAR(40),
    color_ok      BOOLEAN,
    contactos_ok  BOOLEAN,
    conforme      BOOLEAN NOT NULL,
    observaciones TEXT,
    usuario_id    INT NOT NULL REFERENCES usuario(id),
    CONSTRAINT control_rechazo_con_observacion CHECK (
        conforme OR (observaciones IS NOT NULL AND btrim(observaciones) <> '')
    )
);
CREATE INDEX ix_control_orden ON control_laboratorio (orden_id);

-- ------------------------------------------------------------
-- Reglas en la base
-- ------------------------------------------------------------

-- R22 · no se borra; el consumo de una sesión en borrador sí se corrige.
CREATE OR REPLACE FUNCTION bloquea_delete_laboratorio() RETURNS TRIGGER AS $$
DECLARE v_estado estado_ejecucion;
BEGIN
    IF TG_TABLE_NAME = 'consumo_lote' THEN
        SELECT estado INTO v_estado FROM ejecucion_item WHERE id = OLD.ejecucion_item_id;
        IF v_estado = 'BORRADOR' THEN
            RETURN OLD;
        END IF;
    END IF;
    RAISE EXCEPTION 'R22: % no admite DELETE; use el estado de anulación', TG_TABLE_NAME;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_insumo_no_delete     BEFORE DELETE ON insumo              FOR EACH ROW EXECUTE FUNCTION bloquea_delete_laboratorio();
CREATE TRIGGER tr_lote_no_delete       BEFORE DELETE ON lote_insumo         FOR EACH ROW EXECUTE FUNCTION bloquea_delete_laboratorio();
CREATE TRIGGER tr_consumo_no_delete    BEFORE DELETE ON consumo_lote        FOR EACH ROW EXECUTE FUNCTION bloquea_delete_laboratorio();
CREATE TRIGGER tr_orden_no_delete      BEFORE DELETE ON orden_laboratorio   FOR EACH ROW EXECUTE FUNCTION bloquea_delete_laboratorio();
CREATE TRIGGER tr_control_lab_no_delete BEFORE DELETE ON control_laboratorio FOR EACH ROW EXECUTE FUNCTION bloquea_delete_laboratorio();

-- El consumo se registra mientras la sesión está abierta.
CREATE OR REPLACE FUNCTION consumo_valido() RETURNS TRIGGER AS $$
DECLARE v_estado estado_ejecucion;
BEGIN
    SELECT estado INTO v_estado FROM ejecucion_item WHERE id = NEW.ejecucion_item_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La sesión % no existe', NEW.ejecucion_item_id;
    END IF;
    IF v_estado <> 'BORRADOR' THEN
        RAISE EXCEPTION 'R19: la sesión % está % y ya no admite consumo', NEW.ejecucion_item_id, v_estado;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM lote_insumo WHERE id = NEW.lote_insumo_id) THEN
        RAISE EXCEPTION 'El lote % no existe', NEW.lote_insumo_id;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_consumo_valido BEFORE INSERT ON consumo_lote
FOR EACH ROW EXECUTE FUNCTION consumo_valido();

-- La orden nace sobre un ítem aceptado y antes de instalarlo.
CREATE OR REPLACE FUNCTION orden_lab_valida() RETURNS TRIGGER AS $$
DECLARE
    v_estado_item estado_item;
    v_pres        estado_presupuesto;
BEGIN
    SELECT estado INTO v_estado_item FROM presupuesto_item WHERE id = NEW.presupuesto_item_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El ítem % no existe', NEW.presupuesto_item_id;
    END IF;
    IF v_estado_item NOT IN ('ACEPTADO','EN_EJECUCION') THEN
        RAISE EXCEPTION 'R18: el ítem % está % y no admite una orden de laboratorio',
            NEW.presupuesto_item_id, v_estado_item;
    END IF;

    SELECT estado INTO v_pres
      FROM presupuesto
     WHERE id = (SELECT presupuesto_id FROM presupuesto_item WHERE id = NEW.presupuesto_item_id);
    IF v_pres <> 'ACEPTADO' THEN
        RAISE EXCEPTION 'R18: el presupuesto del ítem % está %', NEW.presupuesto_item_id, v_pres;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_orden_valida BEFORE INSERT ON orden_laboratorio
FOR EACH ROW EXECUTE FUNCTION orden_lab_valida();

-- Transiciones permitidas y contenido inmutable.
CREATE OR REPLACE FUNCTION orden_lab_inmutable() RETURNS TRIGGER AS $$
BEGIN
    IF (NEW.presupuesto_item_id, NEW.laboratorio, NEW.trabajo, NEW.material, NEW.color)
       IS DISTINCT FROM
       (OLD.presupuesto_item_id, OLD.laboratorio, OLD.trabajo, OLD.material, OLD.color)
    THEN
        RAISE EXCEPTION 'R18: la orden % no cambia de contenido; anule y cree otra', OLD.id;
    END IF;

    IF NEW.estado IS DISTINCT FROM OLD.estado THEN
        IF NOT (
            (OLD.estado = 'SOLICITADA' AND NEW.estado IN ('RECIBIDA','ANULADA'))
            OR (OLD.estado = 'RECIBIDA' AND NEW.estado IN ('CONFORME','RECHAZADA','ANULADA'))
            OR (OLD.estado = 'CONFORME' AND NEW.estado IN ('INSTALADA','ANULADA'))
        ) THEN
            RAISE EXCEPTION 'R18: la orden % no puede pasar de % a %', OLD.id, OLD.estado, NEW.estado;
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_orden_inmutable BEFORE UPDATE ON orden_laboratorio
FOR EACH ROW EXECUTE FUNCTION orden_lab_inmutable();

-- Solo se controla lo que llegó; el rechazo exige observación.
CREATE OR REPLACE FUNCTION control_lab_valido() RETURNS TRIGGER AS $$
DECLARE v_estado estado_orden_lab;
BEGIN
    SELECT estado INTO v_estado FROM orden_laboratorio WHERE id = NEW.orden_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La orden % no existe', NEW.orden_id;
    END IF;
    IF v_estado <> 'RECIBIDA' THEN
        RAISE EXCEPTION 'R18: la orden % está % y no se controla', NEW.orden_id, v_estado;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_control_lab_valido BEFORE INSERT ON control_laboratorio
FOR EACH ROW EXECUTE FUNCTION control_lab_valido();

-- El control decide: conforme o rechazada con su motivo.
CREATE OR REPLACE FUNCTION control_lab_aplica() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.conforme THEN
        UPDATE orden_laboratorio SET estado = 'CONFORME' WHERE id = NEW.orden_id;
    ELSE
        UPDATE orden_laboratorio
           SET estado = 'RECHAZADA', motivo_rechazo = NEW.observaciones
         WHERE id = NEW.orden_id;
    END IF;
    RETURN NULL;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_control_lab_aplica AFTER INSERT ON control_laboratorio
FOR EACH ROW EXECUTE FUNCTION control_lab_aplica();

-- CONFORME e INSTALADA exigen un control conforme registrado (R18).
CREATE OR REPLACE FUNCTION orden_conforme_valida() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.estado IN ('CONFORME','INSTALADA') AND OLD.estado NOT IN ('CONFORME','INSTALADA') THEN
        IF NOT EXISTS (SELECT 1 FROM control_laboratorio
                        WHERE orden_id = NEW.id AND conforme = TRUE) THEN
            RAISE EXCEPTION 'R18: la orden % no puede quedar % sin control conforme',
                NEW.id, NEW.estado;
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_orden_conforme BEFORE UPDATE OF estado ON orden_laboratorio
FOR EACH ROW EXECUTE FUNCTION orden_conforme_valida();

-- Un ítem con laboratorio no se declara ejecutado sin orden conforme (R18).
CREATE OR REPLACE FUNCTION item_requiere_laboratorio() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.estado IN ('EJECUTADO','CONTROLADO') AND OLD.estado IS DISTINCT FROM NEW.estado THEN
        IF (SELECT requiere_laboratorio FROM tratamiento WHERE id = NEW.tratamiento_id) THEN
            IF NOT EXISTS (SELECT 1 FROM orden_laboratorio
                            WHERE presupuesto_item_id = NEW.id
                              AND estado IN ('CONFORME','INSTALADA')) THEN
                RAISE EXCEPTION 'R18: el ítem % requiere laboratorio y no tiene orden conforme', NEW.id;
            END IF;
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_item_requiere_laboratorio BEFORE UPDATE OF estado ON presupuesto_item
FOR EACH ROW EXECUTE FUNCTION item_requiere_laboratorio();

-- ------------------------------------------------------------
-- Vistas
-- ------------------------------------------------------------
CREATE VIEW v_consumo_trazable AS
SELECT c.id,
       c.fecha,
       i.codigo                         AS insumo,
       i.nombre                         AS insumo_nombre,
       l.numero                         AS lote,
       l.vence,
       c.cantidad,
       p.paciente_id,
       e.pieza_fdi,
       e.profesional_id,
       e.presupuesto_item_id
  FROM consumo_lote c
  JOIN lote_insumo l        ON l.id = c.lote_insumo_id
  JOIN insumo i             ON i.id = l.insumo_id
  JOIN ejecucion_item e     ON e.id = c.ejecucion_item_id
  JOIN presupuesto_item pi  ON pi.id = e.presupuesto_item_id
  JOIN presupuesto p        ON p.id = pi.presupuesto_id;

CREATE VIEW v_ordenes_abiertas AS
SELECT o.id,
       o.presupuesto_item_id,
       p.paciente_id,
       o.laboratorio,
       o.trabajo,
       o.estado,
       o.fecha_solicitud,
       o.fecha_prometida,
       (o.fecha_prometida - CURRENT_DATE) AS dias_restantes
  FROM orden_laboratorio o
  JOIN presupuesto_item i ON i.id = o.presupuesto_item_id
  JOIN presupuesto p      ON p.id = i.presupuesto_id
 WHERE o.estado IN ('SOLICITADA','RECIBIDA','CONFORME');

-- ------------------------------------------------------------
-- Semilla mínima de insumos (sin inventario)
-- ------------------------------------------------------------
INSERT INTO insumo (codigo, nombre, unidad, requiere_lote) VALUES
    ('INS-01', 'Resina compuesta A2', 'JERINGA', FALSE),
    ('INS-02', 'Anestesia lidocaina 2%', 'CARTUCHO', TRUE),
    ('INS-03', 'Implante titanium 4.0 x 10', 'UNIDAD', TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO lote_insumo (insumo_id, numero, vence)
SELECT i.id, 'L-2026-001', DATE '2027-06-30' FROM insumo i WHERE i.codigo = 'INS-02'
ON CONFLICT (insumo_id, numero) DO NOTHING;
INSERT INTO lote_insumo (insumo_id, numero, vence)
SELECT i.id, 'IMP-8842', DATE '2029-12-31' FROM insumo i WHERE i.codigo = 'INS-03'
ON CONFLICT (insumo_id, numero) DO NOTHING;

COMMENT ON TABLE insumo              IS 'P4 · catálogo mínimo de insumos para trazabilidad';
COMMENT ON TABLE lote_insumo         IS 'P4 · lote del insumo con vencimiento (R19)';
COMMENT ON TABLE consumo_lote        IS 'P4 · consumo de un lote en una sesión; ligado a paciente, pieza, fecha y profesional vía v_consumo_trazable';
COMMENT ON TABLE orden_laboratorio   IS 'P4 · orden de trabajo al laboratorio (R18), con reproceso por orden_origen_id';
COMMENT ON TABLE control_laboratorio IS 'P4 · control de recepción (ajuste, color, contactos); decide CONFORME o RECHAZADA';
