-- ============================================================
-- DENTRIX DENTAL · Esquema v3 · P4-A: EJECUCIÓN DE SESIONES
-- Archivo ADITIVO: se ejecuta DESPUÉS de 11_schema_v3_enmienda_neto.sql
--   psql -U postgres -d dentrix -f sql/12_schema_v3_ejecucion.sql
--
-- QUÉ RESUELVE (procesos/P4_ejecucion_sesiones.md §11)
--   1. No existía registro de sesión: "ejecutado" se deducía de un campo clínico.
--      `ejecucion_item` guarda la sesión "n de m" con pieza, superficies, profesional y fecha.
--   2. La evolución firmada no existía como documento: `evolucion` la guarda y una
--      corrección es una fila nueva que referencia la anterior (R9). Firmada no se edita.
--   3. El ítem lo movía cualquiera: ahora lo mueve el sistema al firmar la evolución
--      (EN_EJECUCION → EJECUTADO → CONTROLADO) y el control post lo cierra (R26).
--   4. El plan no tenía estado de cierre: `cierre_plan` (TERMINADO / ABANDONADO / DERIVADO)
--      y no admite TERMINADO con sesiones pendientes (R17).
--
-- QUÉ NO ENTRA (declarado)
--   - Laboratorio y lote/insumos: van en P4-B (`13_schema_v3_laboratorio.sql`).
--   - Inventario, compras, esterilización, comisiones: fuera de los seis mapas.
-- ============================================================
SET search_path TO dentrix;

CREATE TYPE estado_ejecucion     AS ENUM ('BORRADOR','FIRMADA','ANULADA');
CREATE TYPE estado_evolucion     AS ENUM ('BORRADOR','FIRMADA');
CREATE TYPE estado_control_post  AS ENUM ('PENDIENTE','REALIZADO','CANCELADO');
CREATE TYPE estado_cierre_plan   AS ENUM ('TERMINADO','ABANDONADO','DERIVADO');

-- ------------------------------------------------------------
-- 1. Sesión ejecutada (n de m)
-- ------------------------------------------------------------
CREATE TABLE ejecucion_item (
    id                     BIGSERIAL PRIMARY KEY,
    presupuesto_item_id    BIGINT NOT NULL REFERENCES presupuesto_item(id),
    numero_sesion          INT NOT NULL CHECK (numero_sesion > 0),
    sesiones_planificadas  INT NOT NULL CHECK (sesiones_planificadas > 0),
    fecha                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    profesional_id         INT NOT NULL REFERENCES usuario(id),
    procedimiento          VARCHAR(200) NOT NULL,
    pieza_fdi              SMALLINT,
    superficies            VARCHAR(40),
    observaciones          TEXT,
    estado                 estado_ejecucion NOT NULL DEFAULT 'BORRADOR',
    motivo_anulacion       VARCHAR(200),
    CONSTRAINT ejecucion_sesion_unica UNIQUE (presupuesto_item_id, numero_sesion),
    CONSTRAINT ejecucion_anulada_con_motivo CHECK (
        estado <> 'ANULADA' OR (motivo_anulacion IS NOT NULL AND btrim(motivo_anulacion) <> '')
    ),
    CONSTRAINT ejecucion_pieza_fdi_chk CHECK (
        pieza_fdi IS NULL
        OR (pieza_fdi BETWEEN 11 AND 48 AND pieza_fdi % 10 BETWEEN 1 AND 8)
        OR (pieza_fdi BETWEEN 51 AND 85 AND pieza_fdi % 10 BETWEEN 1 AND 5)
    )
);
CREATE INDEX ix_ejecucion_item ON ejecucion_item (presupuesto_item_id, numero_sesion);

-- ------------------------------------------------------------
-- 2. Evolución firmada (R9)
-- ------------------------------------------------------------
CREATE TABLE evolucion (
    id                  BIGSERIAL PRIMARY KEY,
    ficha_id            BIGINT NOT NULL REFERENCES ficha_clinica(id),
    ejecucion_item_id   BIGINT REFERENCES ejecucion_item(id),
    texto               TEXT NOT NULL CHECK (btrim(texto) <> ''),
    estado              estado_evolucion NOT NULL DEFAULT 'BORRADOR',
    profesional_id      INT NOT NULL REFERENCES usuario(id),
    fecha               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_firma         TIMESTAMP,
    firma_hash          CHAR(64),
    enmienda_de         BIGINT REFERENCES evolucion(id),
    motivo_enmienda     VARCHAR(200),
    CONSTRAINT evolucion_firmada_completa CHECK (
        estado <> 'FIRMADA' OR (fecha_firma IS NOT NULL AND firma_hash IS NOT NULL)
    ),
    CONSTRAINT evolucion_enmienda_con_motivo CHECK (
        enmienda_de IS NULL OR (motivo_enmienda IS NOT NULL AND btrim(motivo_enmienda) <> '')
    )
);
CREATE INDEX ix_evolucion_ficha ON evolucion (ficha_id, fecha);
CREATE UNIQUE INDEX ux_evolucion_sesion ON evolucion (ejecucion_item_id)
    WHERE estado = 'FIRMADA' AND enmienda_de IS NULL AND ejecucion_item_id IS NOT NULL;

-- ------------------------------------------------------------
-- 3. Control post-instalación (R26)
-- ------------------------------------------------------------
CREATE TABLE control_post (
    id                   BIGSERIAL PRIMARY KEY,
    presupuesto_item_id  BIGINT NOT NULL REFERENCES presupuesto_item(id),
    fecha_programada     DATE NOT NULL,
    fecha_realizada      DATE,
    estado               estado_control_post NOT NULL DEFAULT 'PENDIENTE',
    hallazgo             TEXT,
    profesional_id       INT REFERENCES usuario(id),
    motivo_cancelacion   VARCHAR(200),
    CONSTRAINT control_realizado_completo CHECK (
        estado <> 'REALIZADO' OR (fecha_realizada IS NOT NULL AND hallazgo IS NOT NULL)
    ),
    CONSTRAINT control_cancelado_con_motivo CHECK (
        estado <> 'CANCELADO' OR (motivo_cancelacion IS NOT NULL AND btrim(motivo_cancelacion) <> '')
    )
);
CREATE UNIQUE INDEX ux_control_pendiente ON control_post (presupuesto_item_id)
    WHERE estado = 'PENDIENTE';

-- ------------------------------------------------------------
-- 4. Cierre del plan (R17)
-- ------------------------------------------------------------
CREATE TABLE cierre_plan (
    id              BIGSERIAL PRIMARY KEY,
    presupuesto_id  BIGINT NOT NULL UNIQUE REFERENCES presupuesto(id),
    estado          estado_cierre_plan NOT NULL,
    motivo          VARCHAR(200) NOT NULL CHECK (btrim(motivo) <> ''),
    fecha           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id      INT NOT NULL REFERENCES usuario(id),
    fecha_recall    DATE,
    CONSTRAINT cierre_terminado_con_recall CHECK (
        estado <> 'TERMINADO' OR fecha_recall IS NOT NULL
    )
);

-- ------------------------------------------------------------
-- Reglas en la base
-- ------------------------------------------------------------

-- R22 · nada se borra.
CREATE OR REPLACE FUNCTION bloquea_delete_ejecucion() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'R22: % no admite DELETE; se anula con motivo', TG_TABLE_NAME;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_ejecucion_no_delete      BEFORE DELETE ON ejecucion_item FOR EACH ROW EXECUTE FUNCTION bloquea_delete_ejecucion();
CREATE TRIGGER tr_evolucion_no_delete      BEFORE DELETE ON evolucion      FOR EACH ROW EXECUTE FUNCTION bloquea_delete_ejecucion();
CREATE TRIGGER tr_control_post_no_delete   BEFORE DELETE ON control_post   FOR EACH ROW EXECUTE FUNCTION bloquea_delete_ejecucion();
CREATE TRIGGER tr_cierre_plan_no_delete    BEFORE DELETE ON cierre_plan    FOR EACH ROW EXECUTE FUNCTION bloquea_delete_ejecucion();

-- Calcula el estado del ítem a partir de las sesiones firmadas.
CREATE OR REPLACE FUNCTION recalcular_item(p_item BIGINT) RETURNS VOID AS $$
DECLARE
    v_plan     INT;
    v_estado   estado_item;
    v_firmadas INT;
BEGIN
    SELECT sesiones_planificadas, estado INTO v_plan, v_estado
      FROM presupuesto_item WHERE id = p_item;
    IF NOT FOUND OR v_estado = 'ANULADO' THEN
        RETURN;
    END IF;

    SELECT COUNT(*) INTO v_firmadas
      FROM ejecucion_item WHERE presupuesto_item_id = p_item AND estado = 'FIRMADA';

    IF v_firmadas >= v_plan THEN
        IF v_estado = 'EN_EJECUCION' THEN
            UPDATE presupuesto_item SET estado = 'EJECUTADO' WHERE id = p_item;
        END IF;
    ELSIF v_firmadas > 0 THEN
        IF v_estado = 'ACEPTADO' THEN
            UPDATE presupuesto_item SET estado = 'EN_EJECUCION' WHERE id = p_item;
        END IF;
    ELSE
        IF v_estado = 'EN_EJECUCION' THEN
            UPDATE presupuesto_item SET estado = 'ACEPTADO' WHERE id = p_item;
        END IF;
    END IF;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

-- Solo se ejecuta un ítem aceptado, con su número de sesión válido.
CREATE OR REPLACE FUNCTION ejecucion_item_valida() RETURNS TRIGGER AS $$
DECLARE
    v_estado_item estado_item;
    v_plan        INT;
    v_pres        estado_presupuesto;
BEGIN
    SELECT estado, sesiones_planificadas INTO v_estado_item, v_plan
      FROM presupuesto_item WHERE id = NEW.presupuesto_item_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El ítem % no existe', NEW.presupuesto_item_id;
    END IF;
    IF v_estado_item NOT IN ('ACEPTADO','EN_EJECUCION') THEN
        RAISE EXCEPTION 'R7: el ítem % está % y no se puede ejecutar', NEW.presupuesto_item_id, v_estado_item;
    END IF;

    SELECT estado INTO v_pres
      FROM presupuesto
     WHERE id = (SELECT presupuesto_id FROM presupuesto_item WHERE id = NEW.presupuesto_item_id);
    IF v_pres <> 'ACEPTADO' THEN
        RAISE EXCEPTION 'R7: el presupuesto del ítem % está % y no se ejecuta', NEW.presupuesto_item_id, v_pres;
    END IF;

    NEW.sesiones_planificadas := v_plan;
    IF NEW.numero_sesion > v_plan THEN
        RAISE EXCEPTION 'La sesión % supera las % planificadas del ítem %',
            NEW.numero_sesion, v_plan, NEW.presupuesto_item_id;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_ejecucion_valida BEFORE INSERT ON ejecucion_item
FOR EACH ROW EXECUTE FUNCTION ejecucion_item_valida();

-- Firmada o anulada, la sesión no se edita.
CREATE OR REPLACE FUNCTION ejecucion_item_inmutable() RETURNS TRIGGER AS $$
BEGIN
    IF OLD.estado IN ('FIRMADA','ANULADA')
       AND (NEW.numero_sesion, NEW.procedimiento, NEW.pieza_fdi, NEW.superficies,
            NEW.observaciones, NEW.profesional_id, NEW.fecha)
           IS DISTINCT FROM
           (OLD.numero_sesion, OLD.procedimiento, OLD.pieza_fdi, OLD.superficies,
            OLD.observaciones, OLD.profesional_id, OLD.fecha)
    THEN
        RAISE EXCEPTION 'R9: la sesión % está % y no se edita', OLD.id, OLD.estado;
    END IF;
    IF OLD.estado = 'ANULADA' AND NEW.estado <> 'ANULADA' THEN
        RAISE EXCEPTION 'R22: la sesión % está anulada y no se reactiva', OLD.id;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_ejecucion_inmutable BEFORE UPDATE ON ejecucion_item
FOR EACH ROW EXECUTE FUNCTION ejecucion_item_inmutable();

-- Al cambiar la sesión, el ítem se recalcula.
CREATE OR REPLACE FUNCTION ejecucion_item_estado() RETURNS TRIGGER AS $$
BEGIN
    PERFORM recalcular_item(COALESCE(NEW.presupuesto_item_id, OLD.presupuesto_item_id));
    RETURN NULL;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_ejecucion_estado AFTER INSERT OR UPDATE OF estado OR DELETE ON ejecucion_item
FOR EACH ROW EXECUTE FUNCTION ejecucion_item_estado();

-- La evolución se firma sobre una sesión abierta.
CREATE OR REPLACE FUNCTION evolucion_valida() RETURNS TRIGGER AS $$
DECLARE v_estado estado_ejecucion;
BEGIN
    IF NEW.ejecucion_item_id IS NOT NULL THEN
        SELECT estado INTO v_estado FROM ejecucion_item WHERE id = NEW.ejecucion_item_id;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'La sesión % no existe', NEW.ejecucion_item_id;
        END IF;
        IF v_estado = 'ANULADA' THEN
            RAISE EXCEPTION 'La sesión % está anulada; no se le firma evolución', NEW.ejecucion_item_id;
        END IF;
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.estado = 'FIRMADA' THEN
        IF (NEW.texto, NEW.ejecucion_item_id, NEW.enmienda_de)
           IS DISTINCT FROM (OLD.texto, OLD.ejecucion_item_id, OLD.enmienda_de)
        THEN
            RAISE EXCEPTION 'R9: la evolución % está firmada y no se edita; cree una enmienda', OLD.id;
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_evolucion_valida BEFORE INSERT OR UPDATE ON evolucion
FOR EACH ROW EXECUTE FUNCTION evolucion_valida();

-- Al firmar: la sesión queda FIRMADA y el ítem avanza.
CREATE OR REPLACE FUNCTION evolucion_al_firmar() RETURNS TRIGGER AS $$
DECLARE v_item BIGINT;
BEGIN
    IF NEW.estado = 'FIRMADA' AND (TG_OP = 'INSERT' OR OLD.estado <> 'FIRMADA') THEN
        IF NEW.ejecucion_item_id IS NOT NULL THEN
            UPDATE ejecucion_item
               SET estado = 'FIRMADA'
             WHERE id = NEW.ejecucion_item_id AND estado = 'BORRADOR';

            SELECT presupuesto_item_id INTO v_item
              FROM ejecucion_item WHERE id = NEW.ejecucion_item_id;
            IF v_item IS NOT NULL THEN
                PERFORM recalcular_item(v_item);
            END IF;
        END IF;
    END IF;
    RETURN NULL;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_evolucion_firma AFTER INSERT OR UPDATE ON evolucion
FOR EACH ROW EXECUTE FUNCTION evolucion_al_firmar();

-- Control post: solo sobre un ítem ejecutado y uno pendiente a la vez.
CREATE OR REPLACE FUNCTION control_post_valido() RETURNS TRIGGER AS $$
DECLARE v_estado estado_item;
BEGIN
    SELECT estado INTO v_estado FROM presupuesto_item WHERE id = NEW.presupuesto_item_id;
    IF v_estado NOT IN ('EJECUTADO','CONTROLADO') THEN
        RAISE EXCEPTION 'R26: el ítem % está % y todavía no admite control post', NEW.presupuesto_item_id, v_estado;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_control_post_valido BEFORE INSERT ON control_post
FOR EACH ROW EXECUTE FUNCTION control_post_valido();

-- Control realizado: el ítem queda CONTROLADO.
CREATE OR REPLACE FUNCTION control_post_cierra() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.estado = 'REALIZADO' AND (TG_OP = 'INSERT' OR OLD.estado <> 'REALIZADO') THEN
        UPDATE presupuesto_item
           SET estado = 'CONTROLADO'
         WHERE id = NEW.presupuesto_item_id AND estado = 'EJECUTADO';
    END IF;
    RETURN NULL;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_control_post_cierra AFTER INSERT OR UPDATE ON control_post
FOR EACH ROW EXECUTE FUNCTION control_post_cierra();

-- Cierre: solo un plan aceptado; TERMINADO exige todo cerrado y recall.
CREATE OR REPLACE FUNCTION cierre_plan_valido() RETURNS TRIGGER AS $$
DECLARE
    v_estado estado_presupuesto;
    v_abiertos INT;
BEGIN
    SELECT estado INTO v_estado FROM presupuesto WHERE id = NEW.presupuesto_id;
    IF v_estado <> 'ACEPTADO' THEN
        RAISE EXCEPTION 'Solo se cierra un plan ACEPTADO; el % está %', NEW.presupuesto_id, v_estado;
    END IF;

    IF NEW.estado = 'TERMINADO' THEN
        SELECT COUNT(*) INTO v_abiertos
          FROM presupuesto_item
         WHERE presupuesto_id = NEW.presupuesto_id
           AND estado IN ('PROPUESTO','ACEPTADO','DIFERIDO','EN_EJECUCION');
        IF v_abiertos > 0 THEN
            RAISE EXCEPTION 'R17: el plan % tiene % ítem(s) sin cerrar; no se cierra TERMINADO',
                NEW.presupuesto_id, v_abiertos;
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_cierre_plan_valido BEFORE INSERT ON cierre_plan
FOR EACH ROW EXECUTE FUNCTION cierre_plan_valido();

-- ------------------------------------------------------------
-- Vistas
-- ------------------------------------------------------------
CREATE VIEW v_sesiones_item AS
SELECT i.id                    AS item_id,
       i.presupuesto_id,
       i.tratamiento_id,
       i.pieza_fdi,
       i.sesiones_planificadas,
       COUNT(e.id) FILTER (WHERE e.estado = 'FIRMADA') AS sesiones_firmadas,
       i.sesiones_planificadas - COUNT(e.id) FILTER (WHERE e.estado = 'FIRMADA') AS sesiones_restantes,
       i.estado
  FROM presupuesto_item i
  LEFT JOIN ejecucion_item e ON e.presupuesto_item_id = i.id
 WHERE i.estado <> 'ANULADO'
 GROUP BY i.id;

COMMENT ON TABLE ejecucion_item IS 'P4 · sesión ejecutada del ítem (n de m) con pieza, superficies y profesional';
COMMENT ON TABLE evolucion      IS 'P4 · evolución firmada (R9); una corrección es una fila nueva con enmienda_de';
COMMENT ON TABLE control_post   IS 'P4/P5 · control post-instalación programado (R26); al realizarse el ítem queda CONTROLADO';
COMMENT ON TABLE cierre_plan    IS 'P4 · cierre del plan TERMINADO/ABANDONADO/DERIVADO (R17) con fecha de recall';
COMMENT ON VIEW  v_sesiones_item IS 'P4 · sesiones firmadas y restantes por ítem';
