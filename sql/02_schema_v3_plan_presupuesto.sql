-- ============================================================
-- DENTRIX DENTAL · Esquema v3 · paso 3 de la migración: PLAN / PRESUPUESTO / CONSENTIMIENTO
-- Archivo ADITIVO: no modifica ni borra nada del v2. Se ejecuta DESPUÉS de 01_schema_v2.sql
--   psql -U postgres -f sql/01_schema_v2.sql
--   psql -U postgres -f sql/02_schema_v3_plan_presupuesto.sql
--
-- QUÉ CORRIGE (procesos/P3_plan_presupuesto_consentimiento.md §15)
--   1. No existía `presupuesto`: la aceptación del paciente era un flag
--      (`ficha_clinica.derivado_a_caja`) + un enum (`estado_derivacion`).
--   2. No existía `consentimiento`: R7 y R12 no tenían dónde apoyarse.
--   3. `plan_tratamiento` mezclaba plan + ejecución + cobro en `estado_linea`
--      (PLANIFICADO→EJECUTADO→COBRADO) y ataba la línea al comprobante con `pago_id`.
--   4. No había versionado ni vigencia del presupuesto (R22).
--   5. El ítem no tenía fase, prioridad ni secuencia (03 §4.1-4.2).
--   8. El descuento no tenía autorizador (R20).
--
-- QUÉ NO ENTRA (declarado, no olvidado)
--   - `tarifario` con vigencia (R21): el precio se sigue tomando de `tratamiento.precio`
--     hasta el paso 5 de la migración; el congelamiento al aceptar SÍ se implementa aquí.
--   - `cuota`: se modela en el paso 6 junto con caja (P5).
--   - Nota de crédito / reverso: fuera de alcance v3.
--
-- COMPATIBILIDAD
--   `v_plan_tratamiento` reproduce la forma de la vieja tabla para que el código
--   JavaFX siga leyendo sin cambios. `plan_tratamiento` NO se toca ni se borra.
--   Las funciones de trigger llevan `SET search_path = dentrix` para que las reglas
--   se cumplan desde cualquier cliente (psql, DBeaver, JDBC sin currentSchema) y no
--   dependan del search_path de la sesión que dispara el trigger.
-- ============================================================
SET search_path TO dentrix;

-- ------------------------------------------------------------
-- Tipos enumerados nuevos
-- ------------------------------------------------------------
CREATE TYPE fase_tratamiento       AS ENUM ('URGENTE','BASICA','RECONSTRUCTIVA','QUIRURGICA','REHABILITACION','MANTENIMIENTO');
CREATE TYPE estado_presupuesto     AS ENUM ('BORRADOR','PRESENTADO','ACEPTADO','RECHAZADO','VENCIDO','REEMPLAZADO');
CREATE TYPE tipo_aceptacion        AS ENUM ('TOTAL','PARCIAL');
CREATE TYPE estado_item            AS ENUM ('PROPUESTO','ACEPTADO','RECHAZADO','DIFERIDO','ANULADO','EN_EJECUCION','EJECUTADO','CONTROLADO');
CREATE TYPE estado_consentimiento  AS ENUM ('PENDIENTE','FIRMADO','RECHAZADO');

-- ------------------------------------------------------------
-- 1. Presupuesto (cabecera versionada)
-- ------------------------------------------------------------
CREATE TABLE presupuesto (
    id                        BIGSERIAL PRIMARY KEY,
    paciente_id               INT NOT NULL REFERENCES paciente(id),
    ficha_id                  BIGINT REFERENCES ficha_clinica(id),   -- diagnóstico de origen (P2)
    presupuesto_anterior_id   BIGINT REFERENCES presupuesto(id),     -- versión que reemplaza
    version                   INT NOT NULL DEFAULT 1 CHECK (version > 0),
    estado                    estado_presupuesto NOT NULL DEFAULT 'BORRADOR',
    moneda                    CHAR(3) NOT NULL DEFAULT 'PEN',
    fecha_emision             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vigencia_dias             INT NOT NULL DEFAULT 15 CHECK (vigencia_dias > 0),
    vigencia_hasta            DATE GENERATED ALWAYS AS (fecha_emision::date + vigencia_dias) STORED,
    subtotal                  NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    descuento                 NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (descuento >= 0),
    total                     NUMERIC(10,2) GENERATED ALWAYS AS (subtotal - descuento) STORED,
    descuento_motivo          VARCHAR(200),
    descuento_autorizado_por  INT REFERENCES usuario(id),            -- R20
    creado_por                INT NOT NULL REFERENCES usuario(id),
    CONSTRAINT presupuesto_version_unica UNIQUE (paciente_id, ficha_id, version),
    CONSTRAINT presupuesto_descuento_cabe CHECK (descuento <= subtotal),
    CONSTRAINT presupuesto_descuento_autorizado CHECK (
        descuento = 0 OR descuento_autorizado_por IS NOT NULL
    )
);
CREATE INDEX ix_presupuesto_paciente ON presupuesto (paciente_id, estado);

-- ------------------------------------------------------------
-- 2. Ítem de presupuesto / plan (un solo eje: clínico; el cobro NO vive aquí)
-- ------------------------------------------------------------
CREATE TABLE presupuesto_item (
    id                     BIGSERIAL PRIMARY KEY,
    presupuesto_id         BIGINT NOT NULL REFERENCES presupuesto(id) ON DELETE CASCADE,
    tratamiento_id         INT NOT NULL REFERENCES tratamiento(id),
    pieza_fdi              SMALLINT,                                  -- NULL = procedimiento sin pieza
    fase                   fase_tratamiento NOT NULL DEFAULT 'BASICA',
    prioridad              SMALLINT NOT NULL DEFAULT 3 CHECK (prioridad BETWEEN 1 AND 6),
    secuencia              INT NOT NULL DEFAULT 1 CHECK (secuencia > 0),
    cantidad               INT NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    sesiones_planificadas  INT NOT NULL DEFAULT 1 CHECK (sesiones_planificadas > 0),
    precio_unitario        NUMERIC(10,2) NOT NULL CHECK (precio_unitario >= 0),
    precio_congelado       BOOLEAN NOT NULL DEFAULT FALSE,            -- R21: se congela al aceptar
    subtotal               NUMERIC(10,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED,
    estado                 estado_item NOT NULL DEFAULT 'PROPUESTO',
    requiere_consentimiento BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_rechazo         VARCHAR(200),
    -- FDI: permanente 11-48 (diente 1-8) y temporal 51-85 (diente 1-5)
    CONSTRAINT item_pieza_fdi_chk CHECK (
        pieza_fdi IS NULL
        OR (pieza_fdi BETWEEN 11 AND 48 AND pieza_fdi % 10 BETWEEN 1 AND 8)
        OR (pieza_fdi BETWEEN 51 AND 85 AND pieza_fdi % 10 BETWEEN 1 AND 5)
    ),
    CONSTRAINT item_rechazo_con_motivo CHECK (
        estado <> 'RECHAZADO' OR motivo_rechazo IS NOT NULL
    )
);
CREATE INDEX ix_item_presupuesto ON presupuesto_item (presupuesto_id, fase, secuencia);

-- ------------------------------------------------------------
-- 3. Aceptación (la firma del paciente sobre el presupuesto)
-- ------------------------------------------------------------
CREATE TABLE aceptacion (
    id                      BIGSERIAL PRIMARY KEY,
    presupuesto_id          BIGINT NOT NULL REFERENCES presupuesto(id),
    tipo                    tipo_aceptacion NOT NULL,
    firmante_nombre         VARCHAR(120) NOT NULL,
    firmante_documento      VARCHAR(20)  NOT NULL,
    firmante_rol            VARCHAR(10)  NOT NULL CHECK (firmante_rol IN ('PACIENTE','TUTOR','PAGADOR')),
    fecha                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    firma_hash              CHAR(64) NOT NULL,                        -- hash de la firma simulada
    alternativas_ofrecidas  TEXT NOT NULL,                            -- 03 §4.2: qué se ofreció
    riesgos_informados      TEXT NOT NULL,                            -- y qué riesgos se explicaron
    registrado_por          INT NOT NULL REFERENCES usuario(id)
);
CREATE UNIQUE INDEX ux_aceptacion_presupuesto ON aceptacion (presupuesto_id);

-- ------------------------------------------------------------
-- 4. Consentimiento informado (firma DISTINTA de la de atención, R12)
-- ------------------------------------------------------------
CREATE TABLE consentimiento (
    id                    BIGSERIAL PRIMARY KEY,
    paciente_id           INT NOT NULL REFERENCES paciente(id),
    presupuesto_item_id   BIGINT REFERENCES presupuesto_item(id),
    tratamiento_id        INT REFERENCES tratamiento(id),
    procedimiento         VARCHAR(160) NOT NULL,
    riesgos               TEXT NOT NULL,
    alternativas          TEXT NOT NULL,
    estado                estado_consentimiento NOT NULL DEFAULT 'PENDIENTE',
    firmante_nombre       VARCHAR(120),
    firmante_documento    VARCHAR(20),
    representante_vinculo VARCHAR(20),                                -- TUTOR | APODERADO
    profesional_id        INT REFERENCES usuario(id),
    fecha_firma           TIMESTAMP,
    firma_hash            CHAR(64),
    motivo_rechazo        VARCHAR(200),
    CONSTRAINT consent_destino_chk CHECK (
        presupuesto_item_id IS NOT NULL OR tratamiento_id IS NOT NULL
    ),
    CONSTRAINT consent_firmado_chk CHECK (
        estado <> 'FIRMADO'
        OR (firmante_nombre IS NOT NULL AND fecha_firma IS NOT NULL AND firma_hash IS NOT NULL)
    ),
    CONSTRAINT consent_rechazado_chk CHECK (
        estado <> 'RECHAZADO' OR motivo_rechazo IS NOT NULL
    )
);
CREATE UNIQUE INDEX ux_consent_firmado_item ON consentimiento (presupuesto_item_id)
    WHERE estado = 'FIRMADO' AND presupuesto_item_id IS NOT NULL;

-- ------------------------------------------------------------
-- 5. Imputación (pago ↔ ítem, N:N) · el saldo se deriva, no se guarda (R6)
-- ------------------------------------------------------------
CREATE TABLE imputacion (
    id                    BIGSERIAL PRIMARY KEY,
    pago_id               BIGINT NOT NULL REFERENCES pago(id),
    presupuesto_item_id   BIGINT NOT NULL REFERENCES presupuesto_item(id),
    monto                 NUMERIC(10,2) NOT NULL CHECK (monto > 0),
    fecha                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id            INT NOT NULL REFERENCES usuario(id),
    CONSTRAINT imputacion_unica UNIQUE (pago_id, presupuesto_item_id)
);
CREATE INDEX ix_imputacion_item ON imputacion (presupuesto_item_id);

-- ------------------------------------------------------------
-- Reglas en la base, no solo en Java
-- ------------------------------------------------------------

-- R22 · nada clínico ni financiero se borra: se anula con estado.
CREATE OR REPLACE FUNCTION bloquea_delete() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'R22: % no admite DELETE; use el estado de anulación', TG_TABLE_NAME;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_presupuesto_no_delete   BEFORE DELETE ON presupuesto       FOR EACH ROW EXECUTE FUNCTION bloquea_delete();
CREATE TRIGGER tr_item_no_delete          BEFORE DELETE ON presupuesto_item  FOR EACH ROW EXECUTE FUNCTION bloquea_delete();
CREATE TRIGGER tr_aceptacion_no_delete    BEFORE DELETE ON aceptacion        FOR EACH ROW EXECUTE FUNCTION bloquea_delete();
CREATE TRIGGER tr_consent_no_delete       BEFORE DELETE ON consentimiento    FOR EACH ROW EXECUTE FUNCTION bloquea_delete();
CREATE TRIGGER tr_imputacion_no_delete    BEFORE DELETE ON imputacion        FOR EACH ROW EXECUTE FUNCTION bloquea_delete();

-- R22 · un presupuesto presentado no cambia de contenido: se versiona.
CREATE OR REPLACE FUNCTION presupuesto_inmutable() RETURNS TRIGGER AS $$
BEGIN
    IF OLD.estado <> 'BORRADOR' THEN
        IF (NEW.subtotal, NEW.descuento, NEW.vigencia_dias, NEW.moneda)
           IS DISTINCT FROM (OLD.subtotal, OLD.descuento, OLD.vigencia_dias, OLD.moneda)
        THEN
            RAISE EXCEPTION 'R22: el presupuesto % está en % y no se edita; cree una versión nueva',
                OLD.id, OLD.estado;
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_presupuesto_inmutable BEFORE UPDATE ON presupuesto
FOR EACH ROW EXECUTE FUNCTION presupuesto_inmutable();

-- R22 · ítem inmutable cuando el presupuesto salió de borrador + R21 precio congelado.
CREATE OR REPLACE FUNCTION item_inmutable() RETURNS TRIGGER AS $$
DECLARE v_estado estado_presupuesto;
BEGIN
    SELECT estado INTO v_estado FROM presupuesto WHERE id = NEW.presupuesto_id;
    IF v_estado <> 'BORRADOR'
       AND (NEW.tratamiento_id, NEW.pieza_fdi, NEW.cantidad, NEW.precio_unitario,
            NEW.fase, NEW.prioridad, NEW.secuencia, NEW.sesiones_planificadas)
           IS DISTINCT FROM
           (OLD.tratamiento_id, OLD.pieza_fdi, OLD.cantidad, OLD.precio_unitario,
            OLD.fase, OLD.prioridad, OLD.secuencia, OLD.sesiones_planificadas)
    THEN
        RAISE EXCEPTION 'R22: el ítem % pertenece a un presupuesto % y no se edita', OLD.id, v_estado;
    END IF;
    IF OLD.precio_congelado AND NEW.precio_unitario <> OLD.precio_unitario THEN
        RAISE EXCEPTION 'R21: el precio del ítem % está congelado', OLD.id;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_item_inmutable BEFORE UPDATE ON presupuesto_item
FOR EACH ROW EXECUTE FUNCTION item_inmutable();

-- Subtotal del presupuesto = suma de ítems, mientras esté en borrador.
CREATE OR REPLACE FUNCTION presupuesto_recalcula() RETURNS TRIGGER AS $$
DECLARE v_id BIGINT; v_sub NUMERIC(10,2);
BEGIN
    v_id := COALESCE(NEW.presupuesto_id, OLD.presupuesto_id);
    SELECT COALESCE(SUM(subtotal), 0) INTO v_sub
      FROM presupuesto_item WHERE presupuesto_id = v_id;
    UPDATE presupuesto SET subtotal = v_sub
     WHERE id = v_id AND estado = 'BORRADOR';
    RETURN NULL;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_item_recalcula AFTER INSERT OR UPDATE OF cantidad, precio_unitario OR DELETE
ON presupuesto_item FOR EACH ROW EXECUTE FUNCTION presupuesto_recalcula();

-- R7 · no se ejecuta un ítem sin presupuesto ACEPTADO y consentimiento firmado si lo exige.
CREATE OR REPLACE FUNCTION item_requiere_aceptacion() RETURNS TRIGGER AS $$
DECLARE v_pres estado_presupuesto; v_cons INT;
BEGIN
    IF NEW.estado IN ('EN_EJECUCION','EJECUTADO','CONTROLADO')
       AND OLD.estado NOT IN ('EN_EJECUCION','EJECUTADO','CONTROLADO')
    THEN
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

CREATE TRIGGER tr_item_r7 BEFORE UPDATE ON presupuesto_item
FOR EACH ROW EXECUTE FUNCTION item_requiere_aceptacion();

-- Al aceptar: congela precios y pasa los ítems propuestos a ACEPTADO.
CREATE OR REPLACE FUNCTION presupuesto_al_aceptar() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.estado = 'ACEPTADO' AND OLD.estado IS DISTINCT FROM 'ACEPTADO' THEN
        UPDATE presupuesto_item
           SET precio_congelado = TRUE,
               estado = CASE WHEN estado = 'PROPUESTO' THEN 'ACEPTADO'::estado_item ELSE estado END
         WHERE presupuesto_id = NEW.id;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_presupuesto_acepta AFTER UPDATE ON presupuesto
FOR EACH ROW EXECUTE FUNCTION presupuesto_al_aceptar();

-- Una versión nueva reemplaza a la anterior presentada (no la borra).
CREATE OR REPLACE FUNCTION presupuesto_versiona() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.presupuesto_anterior_id IS NOT NULL THEN
        UPDATE presupuesto SET estado = 'REEMPLAZADO'
         WHERE id = NEW.presupuesto_anterior_id
           AND estado IN ('BORRADOR','PRESENTADO');
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_presupuesto_versiona AFTER INSERT ON presupuesto
FOR EACH ROW EXECUTE FUNCTION presupuesto_versiona();

-- ------------------------------------------------------------
-- Vistas
-- ------------------------------------------------------------

-- Saldo por ítem: derivado de imputaciones (R6), nunca editable.
CREATE VIEW v_saldo_item AS
SELECT i.id                                            AS item_id,
       i.presupuesto_id,
       i.subtotal,
       COALESCE(SUM(imp.monto), 0)                     AS imputado,
       i.subtotal - COALESCE(SUM(imp.monto), 0)        AS saldo
  FROM presupuesto_item i
  LEFT JOIN imputacion imp ON imp.presupuesto_item_id = i.id
 GROUP BY i.id, i.presupuesto_id, i.subtotal;

-- Compatibilidad con el código actual: misma forma que `plan_tratamiento`,
-- con "COBRADO" derivado de las imputaciones en lugar de guardado.
CREATE VIEW v_plan_tratamiento AS
SELECT i.id,
       p.paciente_id,
       i.tratamiento_id,
       p.ficha_id,
       NULL::BIGINT AS pago_id,
       i.pieza_fdi,
       i.cantidad,
       i.precio_unitario,
       i.subtotal,
       CASE
           WHEN i.estado IN ('EN_EJECUCION','EJECUTADO','CONTROLADO')
                AND s.imputado >= i.subtotal THEN 'COBRADO'::estado_linea
           WHEN i.estado IN ('EN_EJECUCION','EJECUTADO','CONTROLADO') THEN 'EJECUTADO'::estado_linea
           ELSE 'PLANIFICADO'::estado_linea
       END AS estado,
       p.fecha_emision AS fecha
  FROM presupuesto_item i
  JOIN presupuesto p ON p.id = i.presupuesto_id
  JOIN v_saldo_item s ON s.item_id = i.id;

COMMENT ON TABLE presupuesto       IS 'P3 · cabecera versionada del presupuesto; presentada = inmutable (R22)';
COMMENT ON TABLE presupuesto_item  IS 'P3/P4 · ítem con fase, prioridad, secuencia y precio congelado (R21)';
COMMENT ON TABLE aceptacion        IS 'P3 · firma del paciente/tutor/pagador sobre el presupuesto (R7, R12)';
COMMENT ON TABLE consentimiento    IS 'P3 · consentimiento informado por ítem; firma distinta de la de atención (R12)';
COMMENT ON TABLE imputacion        IS 'P5 · N:N pago↔ítem; el saldo se deriva en v_saldo_item (R6)';
