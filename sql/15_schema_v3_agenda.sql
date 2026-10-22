-- ============================================================
-- DENTRIX DENTAL · Esquema v3 · PASO 4: AGENDA
-- Archivo ADITIVO: se ejecuta DESPUÉS de 14_schema_v3_laboratorio.sql
--   psql -U postgres -d dentrix -f sql/15_schema_v3_agenda.sql
--
-- QUÉ RESUELVE (P1 · R1, R2, R24, R29 y Ley 29733)
--   1. Dos citas del mismo sillón (R1) o del mismo odontólogo (R2) no se solapan:
--      restricción de exclusión sobre el rango horario, no solo sobre la hora de inicio.
--   2. Reprogramar conserva la cita original (R24): la vieja queda REPROGRAMADA y la
--      nueva apunta a ella con `cita_origen_id`.
--   3. El estado de la cita deja historial append-only: quién lo cambió, cuándo y por qué.
--   4. `lista_espera`, `recordatorio` (simulado) y `consentimiento_datos` (Ley 29733).
--
-- QUÉ NO ENTRA (declarado)
--   - Envío real de WhatsApp/SMS: el recordatorio se registra y se marca como enviado.
--   - Multi-sede (fuera de alcance v3).
-- ============================================================
SET search_path TO dentrix;

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TYPE estado_cita ADD VALUE IF NOT EXISTS 'REPROGRAMADA';

CREATE TYPE estado_espera       AS ENUM ('ESPERANDO','AGENDADO','RETIRADO');
CREATE TYPE canal_recordatorio  AS ENUM ('WHATSAPP','SMS','LLAMADA','EMAIL');
CREATE TYPE estado_recordatorio AS ENUM ('PENDIENTE','ENVIADO','FALLIDO','CONFIRMADO','RECHAZADO');

-- ------------------------------------------------------------
-- 1. Solape de citas (R1 y R2)
-- ------------------------------------------------------------
ALTER TABLE cita ADD COLUMN IF NOT EXISTS rango tsrange
    GENERATED ALWAYS AS (tsrange(fecha_cita + hora_inicio, fecha_cita + hora_fin, '[)')) STORED;

ALTER TABLE cita ADD COLUMN IF NOT EXISTS cita_origen_id BIGINT REFERENCES cita(id);

-- Las unicidades del v2 miran solo la hora de inicio y no distinguen estado: una cita
-- CANCELADA seguía bloqueando el cupo. Se reemplazan por exclusiones por rango y estado.
ALTER TABLE cita DROP CONSTRAINT IF EXISTS cita_unica_consultorio;
ALTER TABLE cita DROP CONSTRAINT IF EXISTS cita_unica_paciente;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'cita_sin_solape_consultorio') THEN
        ALTER TABLE cita ADD CONSTRAINT cita_sin_solape_consultorio
            EXCLUDE USING gist (consultorio_id WITH =, rango WITH &&)
            WHERE (estado <> 'CANCELADA' AND tipo = 'CITA');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'cita_sin_solape_odontologo') THEN
        ALTER TABLE cita ADD CONSTRAINT cita_sin_solape_odontologo
            EXCLUDE USING gist (usuario_id WITH =, rango WITH &&)
            WHERE (estado <> 'CANCELADA' AND tipo = 'CITA');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'cita_sin_solape_paciente') THEN
        ALTER TABLE cita ADD CONSTRAINT cita_sin_solape_paciente
            EXCLUDE USING gist (paciente_id WITH =, rango WITH &&)
            WHERE (estado <> 'CANCELADA' AND tipo = 'CITA' AND paciente_id IS NOT NULL);
    END IF;
END $$;

-- ------------------------------------------------------------
-- 2. Lista de espera
-- ------------------------------------------------------------
CREATE TABLE lista_espera (
    id              BIGSERIAL PRIMARY KEY,
    paciente_id     INT NOT NULL REFERENCES paciente(id),
    tratamiento_id  INT REFERENCES tratamiento(id),
    prioridad       SMALLINT NOT NULL DEFAULT 3 CHECK (prioridad BETWEEN 1 AND 5),
    nota            VARCHAR(200),
    fecha_alta      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado          estado_espera NOT NULL DEFAULT 'ESPERANDO',
    cita_id         BIGINT REFERENCES cita(id),
    motivo_retiro   VARCHAR(200),
    usuario_id      INT NOT NULL REFERENCES usuario(id),
    CONSTRAINT espera_retiro_con_motivo CHECK (
        estado <> 'RETIRADO' OR (motivo_retiro IS NOT NULL AND btrim(motivo_retiro) <> '')
    ),
    CONSTRAINT espera_agendada_con_cita CHECK (
        estado <> 'AGENDADO' OR cita_id IS NOT NULL
    )
);
CREATE UNIQUE INDEX ux_espera_paciente ON lista_espera (paciente_id)
    WHERE estado = 'ESPERANDO';
CREATE INDEX ix_espera_estado ON lista_espera (estado, prioridad);

-- ------------------------------------------------------------
-- 3. Recordatorios (simulados)
-- ------------------------------------------------------------
CREATE TABLE recordatorio (
    id          BIGSERIAL PRIMARY KEY,
    cita_id     BIGINT NOT NULL REFERENCES cita(id),
    canal       canal_recordatorio NOT NULL DEFAULT 'WHATSAPP',
    programado  TIMESTAMP NOT NULL,
    enviado     TIMESTAMP,
    estado      estado_recordatorio NOT NULL DEFAULT 'PENDIENTE',
    respuesta   VARCHAR(200),
    usuario_id  INT REFERENCES usuario(id),
    CONSTRAINT recordatorio_enviado_con_fecha CHECK (
        estado = 'PENDIENTE' OR enviado IS NOT NULL
    )
);
CREATE INDEX ix_recordatorio_cita ON recordatorio (cita_id);
CREATE UNIQUE INDEX ux_recordatorio_pendiente ON recordatorio (cita_id, canal)
    WHERE estado = 'PENDIENTE';

-- ------------------------------------------------------------
-- 4. Consentimiento de datos personales (Ley 29733)
-- ------------------------------------------------------------
CREATE TABLE consentimiento_datos (
    id              BIGSERIAL PRIMARY KEY,
    paciente_id     INT NOT NULL REFERENCES paciente(id),
    version         VARCHAR(20) NOT NULL,
    aceptado        BOOLEAN NOT NULL,
    fecha           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    medio           VARCHAR(40) NOT NULL,
    evidencia       VARCHAR(200),
    motivo_rechazo  VARCHAR(200),
    CONSTRAINT consent_datos_unico UNIQUE (paciente_id, version),
    CONSTRAINT consent_datos_rechazo_con_motivo CHECK (
        aceptado OR (motivo_rechazo IS NOT NULL AND btrim(motivo_rechazo) <> '')
    )
);

-- ------------------------------------------------------------
-- 5. Historial de estados de la cita (R24)
-- ------------------------------------------------------------
CREATE TABLE cita_historial (
    id               BIGSERIAL PRIMARY KEY,
    cita_id          BIGINT NOT NULL REFERENCES cita(id),
    estado_anterior  estado_cita,
    estado_nuevo     estado_cita NOT NULL,
    motivo           VARCHAR(200),
    usuario_id       INT REFERENCES usuario(id),
    fecha            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_cita_historial ON cita_historial (cita_id, fecha);

-- ------------------------------------------------------------
-- Reglas en la base
-- ------------------------------------------------------------

-- Historial append-only y nada se borra en agenda.
CREATE OR REPLACE FUNCTION bloquea_delete_agenda() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'R22: % no admite DELETE; se anula con estado', TG_TABLE_NAME;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_cita_historial_no_delete BEFORE DELETE ON cita_historial FOR EACH ROW EXECUTE FUNCTION bloquea_delete_agenda();
CREATE TRIGGER tr_lista_espera_no_delete  BEFORE DELETE ON lista_espera  FOR EACH ROW EXECUTE FUNCTION bloquea_delete_agenda();
CREATE TRIGGER tr_recordatorio_no_delete  BEFORE DELETE ON recordatorio  FOR EACH ROW EXECUTE FUNCTION bloquea_delete_agenda();
CREATE TRIGGER tr_consent_datos_no_delete BEFORE DELETE ON consentimiento_datos FOR EACH ROW EXECUTE FUNCTION bloquea_delete_agenda();

-- Cada cambio de estado de la cita deja su fila.
CREATE OR REPLACE FUNCTION cita_historial_registra() RETURNS TRIGGER AS $$
DECLARE v_usuario INT;
BEGIN
    v_usuario := NULLIF(coalesce(current_setting('dentrix.usuario', true), ''), '')::INT;
    IF TG_OP = 'INSERT' THEN
        INSERT INTO cita_historial (cita_id, estado_anterior, estado_nuevo, usuario_id)
        VALUES (NEW.id, NULL, NEW.estado, COALESCE(v_usuario, NEW.usuario_id));
    ELSIF NEW.estado IS DISTINCT FROM OLD.estado THEN
        INSERT INTO cita_historial (cita_id, estado_anterior, estado_nuevo, usuario_id)
        VALUES (NEW.id, OLD.estado, NEW.estado, COALESCE(v_usuario, NEW.usuario_id));
    END IF;
    RETURN NULL;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_cita_historial AFTER INSERT OR UPDATE OF estado ON cita
FOR EACH ROW EXECUTE FUNCTION cita_historial_registra();

-- Inasistencias de los últimos 180 días (R29).
CREATE OR REPLACE FUNCTION inasistencias_recientes(p_paciente INT) RETURNS INT AS $$
DECLARE v_total INT;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM cita
     WHERE paciente_id = p_paciente
       AND estado = 'FALTO'
       AND fecha_cita >= CURRENT_DATE - 180;
    RETURN v_total;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

-- Reprogramar conserva la cita original y crea la nueva (R24).
CREATE OR REPLACE FUNCTION reprogramar_cita(
    p_cita           BIGINT,
    p_fecha          DATE,
    p_hora_inicio    TIME,
    p_consultorio    INT,
    p_usuario        INT,
    p_motivo         VARCHAR DEFAULT 'Reprogramacion'
) RETURNS BIGINT AS $$
DECLARE
    v_cita      cita%ROWTYPE;
    v_nueva     BIGINT;
    v_duracion  INTERVAL;
BEGIN
    SELECT * INTO v_cita FROM cita WHERE id = p_cita FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La cita % no existe', p_cita;
    END IF;
    IF v_cita.estado NOT IN ('PENDIENTE','CONFIRMADA') THEN
        RAISE EXCEPTION 'Solo se reprograma una cita PENDIENTE o CONFIRMADA; la % está %',
            p_cita, v_cita.estado;
    END IF;

    v_duracion := v_cita.hora_fin - v_cita.hora_inicio;

    UPDATE cita SET estado = 'REPROGRAMADA' WHERE id = p_cita;

    INSERT INTO cita (tipo, paciente_id, usuario_id, consultorio_id, fecha_cita, hora_inicio, hora_fin,
                      tratamiento_id, estado, motivo, cita_origen_id)
    VALUES (v_cita.tipo, v_cita.paciente_id, v_cita.usuario_id,
            COALESCE(p_consultorio, v_cita.consultorio_id), p_fecha, p_hora_inicio,
            p_hora_inicio + v_duracion, v_cita.tratamiento_id, 'PENDIENTE', p_motivo, p_cita)
    RETURNING id INTO v_nueva;

    INSERT INTO cita_historial (cita_id, estado_anterior, estado_nuevo, motivo, usuario_id)
    VALUES (p_cita, v_cita.estado, 'REPROGRAMADA', p_motivo, p_usuario);

    RETURN v_nueva;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

COMMENT ON TABLE lista_espera         IS 'P1 · paciente esperando un cupo; pasa a AGENDADO al asignarle cita';
COMMENT ON TABLE recordatorio         IS 'P1 · recordatorio simulado por canal; el envío real queda fuera';
COMMENT ON TABLE consentimiento_datos IS 'Ley 29733 · consentimiento de datos por version y paciente';
COMMENT ON TABLE cita_historial       IS 'P1/R24 · historial append-only de estados de la cita';
COMMENT ON FUNCTION reprogramar_cita(BIGINT, DATE, TIME, INT, INT, VARCHAR)
    IS 'R24 · deja la cita original REPROGRAMADA y crea la nueva conservando paciente, odontologo y duracion';
