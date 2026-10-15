-- ============================================================
-- DENTRIX DENTAL · Esquema relacional v2 (14 tablas)
-- Base: modelo APROBADO por la docente (13 entidades + FichaDiagnostico)
-- Absorbe el esquema v1 (sql/01_schema.sql, 21 tablas). NO reemplaza a v1:
-- v1 se conserva como referencia; v2 es el que usa el código JavaFX.
-- Ejecutar:  psql -U postgres -f sql/01_schema_v2.sql
-- ============================================================
DROP SCHEMA IF EXISTS dentrix CASCADE;
CREATE SCHEMA dentrix;
SET search_path TO dentrix;

-- ------------------------------------------------------------
-- Tipos enumerados
-- ------------------------------------------------------------
CREATE TYPE estado_usuario     AS ENUM ('ACTIVO','INACTIVO','BLOQUEADO');
CREATE TYPE estado_cita        AS ENUM ('PENDIENTE','CONFIRMADA','EN_SALA','EN_ATENCION','ATENDIDA','FALTO','CANCELADA','BLOQUEADA');
CREATE TYPE tipo_bloque        AS ENUM ('CITA','ALMUERZO','CIERRE','BLOQUEADO');   -- M5: bloques dentro de la misma agenda
CREATE TYPE estado_derivacion  AS ENUM ('PENDIENTE','ACEPTADA','RECHAZADA');       -- RF-07
CREATE TYPE estado_linea       AS ENUM ('PLANIFICADO','EJECUTADO','COBRADO');      -- plan vs ejecutado vs cobrado
CREATE TYPE estado_pago        AS ENUM ('PENDIENTE','PARCIAL','PAGADO');           -- RF-08
CREATE TYPE modalidad_pago     AS ENUM ('EFECTIVO','YAPE','PLIN','TARJETA');       -- RF-08
CREATE TYPE tipo_documento     AS ENUM ('BOLETA','NOTA_CREDITO');
CREATE TYPE estado_documento   AS ENUM ('EMITIDO','ANULADO');
CREATE TYPE estado_cierre      AS ENUM ('ABIERTO','CONSOLIDADO');                  -- M7.1
CREATE TYPE estado_pieza       AS ENUM ('SANO','CARIES','OBTURADO','AUSENTE','CORONA','IMPLANTE','ENDODONCIA','PROTESIS');

-- ------------------------------------------------------------
-- 1. Rol · 13. Configuracion · 3. Consultorio · 2. Usuario
-- ------------------------------------------------------------
CREATE TABLE rol (
    id          SERIAL PRIMARY KEY,
    codigo      VARCHAR(20) NOT NULL UNIQUE,   -- ADMINISTRADOR, ODONTOLOGO, RECEPCION
    nombre      VARCHAR(40) NOT NULL,
    descripcion VARCHAR(120)
);

CREATE TABLE configuracion (
    id                  INT PRIMARY KEY DEFAULT 1,
    razon_social        VARCHAR(120) NOT NULL,
    ruc                 CHAR(11) NOT NULL,
    direccion           VARCHAR(160) NOT NULL,
    telefono            VARCHAR(20),
    email               VARCHAR(80),
    representante_legal VARCHAR(80),
    medico_director     VARCHAR(80),
    licencia_sanitaria  VARCHAR(40),
    licencia_municipal  VARCHAR(40),
    hora_apertura       TIME NOT NULL DEFAULT '08:00',
    hora_cierre         TIME NOT NULL DEFAULT '20:00',
    almuerzo_inicio     TIME NOT NULL DEFAULT '12:00',
    almuerzo_fin        TIME NOT NULL DEFAULT '13:00',
    igv_porcentaje      NUMERIC(4,2) NOT NULL DEFAULT 18.00,
    serie_boleta        VARCHAR(10) NOT NULL DEFAULT 'B001',
    CONSTRAINT configuracion_singleton CHECK (id = 1)
);

CREATE TABLE consultorio (
    id           SERIAL PRIMARY KEY,
    codigo       VARCHAR(4) NOT NULL UNIQUE,   -- C01, C02
    nombre       VARCHAR(60) NOT NULL,
    especialidad VARCHAR(60),
    estado       VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
);

CREATE TABLE usuario (
    id                SERIAL PRIMARY KEY,
    username          VARCHAR(30) NOT NULL UNIQUE,
    password_hash     VARCHAR(80) NOT NULL,    -- RNF-01: bcrypt, nunca texto plano
    nombres           VARCHAR(60) NOT NULL,
    apellidos         VARCHAR(60) NOT NULL,
    dni               CHAR(8) NOT NULL UNIQUE,
    rol_id            INT NOT NULL REFERENCES rol(id),
    colegiatura       VARCHAR(20),             -- solo odontólogos (COP/RNE, RF-14)
    especialidad      VARCHAR(60),             -- solo odontólogos
    consultorio_id    INT REFERENCES consultorio(id),
    estado            estado_usuario NOT NULL DEFAULT 'ACTIVO',
    intentos_fallidos INT NOT NULL DEFAULT 0,
    bloqueado_hasta   TIMESTAMP,
    ultimo_acceso     TIMESTAMP,
    fecha_creacion    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 4. Paciente (absorbe historia_clinica: expediente 1:1)
-- ------------------------------------------------------------
CREATE TABLE paciente (
    id                  SERIAL PRIMARY KEY,
    codigo_hc           VARCHAR(15) NOT NULL UNIQUE,   -- HC-000001
    dni                 CHAR(8) NOT NULL UNIQUE,
    apellidos           VARCHAR(60) NOT NULL,
    nombres             VARCHAR(60) NOT NULL,
    fecha_nac           DATE NOT NULL,
    celular             VARCHAR(15),
    email               VARCHAR(80),
    direccion           VARCHAR(120),
    alergias            VARCHAR(200),                  -- alertas farmacológicas (RF-04)
    antecedentes        TEXT,                          -- observaciones generales del expediente
    fecha_apertura      DATE NOT NULL DEFAULT CURRENT_DATE,
    estado              VARCHAR(15) NOT NULL DEFAULT 'ACTIVO',
    usuario_registro_id INT REFERENCES usuario(id),
    fecha_registro      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 9. DiagnosticoCIE10 (catálogo OMS/MINSA, RF-13)
-- ------------------------------------------------------------
CREATE TABLE diagnostico_cie10 (
    codigo       VARCHAR(7) PRIMARY KEY,               -- K02.1
    descripcion  VARCHAR(160) NOT NULL,
    capitulo     VARCHAR(80),
    es_frecuente BOOLEAN NOT NULL DEFAULT FALSE
);

-- ------------------------------------------------------------
-- 5. Tratamiento (tarifario maestro, RF-12)
-- ------------------------------------------------------------
CREATE TABLE tratamiento (
    id           SERIAL PRIMARY KEY,
    codigo       VARCHAR(10) NOT NULL UNIQUE,          -- TRAT-01 … TRAT-05
    nombre       VARCHAR(80) NOT NULL,
    especialidad VARCHAR(60) NOT NULL,
    calculo      VARCHAR(20) NOT NULL DEFAULT 'PIEZA', -- PIEZA | SESION | MENSUAL
    precio       NUMERIC(10,2) NOT NULL CHECK (precio >= 0),
    estado       VARCHAR(15) NOT NULL DEFAULT 'ACTIVO'
);

-- ------------------------------------------------------------
-- 6. Cita (absorbe asistencia y bloques de agenda, RF-03/RF-05)
-- ------------------------------------------------------------
CREATE TABLE cita (
    id             BIGSERIAL PRIMARY KEY,
    tipo           tipo_bloque NOT NULL DEFAULT 'CITA',
    paciente_id    INT REFERENCES paciente(id),
    usuario_id     INT NOT NULL REFERENCES usuario(id),      -- odontólogo responsable
    consultorio_id INT NOT NULL REFERENCES consultorio(id),
    fecha_cita     DATE NOT NULL,
    hora_inicio    TIME NOT NULL,
    hora_fin       TIME NOT NULL,
    tratamiento_id INT REFERENCES tratamiento(id),
    estado         estado_cita NOT NULL DEFAULT 'PENDIENTE',
    motivo         VARCHAR(160),
    CONSTRAINT cita_hora_chk CHECK (hora_fin > hora_inicio),
    CONSTRAINT cita_con_paciente_chk CHECK (tipo <> 'CITA' OR paciente_id IS NOT NULL),
    CONSTRAINT cita_unica_consultorio UNIQUE (consultorio_id, fecha_cita, hora_inicio),  -- RF-05: no doble reserva
    CONSTRAINT cita_unica_paciente UNIQUE (paciente_id, fecha_cita, hora_inicio)
);

-- ------------------------------------------------------------
-- 7. FichaClinica (absorbe atencion: 1 fila = 1 sesión clínica)
-- ------------------------------------------------------------
CREATE TABLE ficha_clinica (
    id                BIGSERIAL PRIMARY KEY,
    paciente_id       INT NOT NULL REFERENCES paciente(id),
    cita_id           BIGINT REFERENCES cita(id),
    odontologo_id     INT NOT NULL REFERENCES usuario(id),
    fecha             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo_consulta   VARCHAR(200),
    examen_clinico    TEXT,
    prescripcion      TEXT,
    plan_indicaciones TEXT,
    estado_derivacion estado_derivacion NOT NULL DEFAULT 'PENDIENTE',  -- RF-07
    derivado_a_caja   BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_derivacion  TIMESTAMP,
    CONSTRAINT ficha_derivada_requiere_aceptada CHECK (
        derivado_a_caja = FALSE OR estado_derivacion = 'ACEPTADA'
    ),
    CONSTRAINT ficha_derivada_con_fecha CHECK (
        derivado_a_caja = FALSE OR fecha_derivacion IS NOT NULL
    )
);

-- ------------------------------------------------------------
-- 8. FichaDiagnostico (14ª tabla: N:N ficha × CIE-10, RF-06 múltiple)
-- ------------------------------------------------------------
CREATE TABLE ficha_diagnostico (
    ficha_id     BIGINT NOT NULL REFERENCES ficha_clinica(id) ON DELETE CASCADE,
    cie10_codigo VARCHAR(7) NOT NULL REFERENCES diagnostico_cie10(codigo),
    pieza_fdi    SMALLINT,
    es_principal BOOLEAN NOT NULL DEFAULT FALSE,
    observacion  VARCHAR(120),
    PRIMARY KEY (ficha_id, cie10_codigo),
    CONSTRAINT ficha_diag_fdi_chk CHECK (
        pieza_fdi IS NULL OR (pieza_fdi BETWEEN 11 AND 48 AND pieza_fdi % 10 BETWEEN 1 AND 8)
    )
);

-- ------------------------------------------------------------
-- 10. HallazgoDental (absorbe odontograma_pieza; expediente por paciente)
-- ------------------------------------------------------------
CREATE TABLE hallazgo_dental (
    paciente_id      INT NOT NULL REFERENCES paciente(id),
    pieza_fdi        SMALLINT NOT NULL,
    estado           estado_pieza NOT NULL DEFAULT 'SANO',
    cara             VARCHAR(20),                    -- oclusal, mesial, distal, vestibular, lingual
    observacion      VARCHAR(120),
    ultima_ficha_id  BIGINT REFERENCES ficha_clinica(id),
    fecha_registro   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (paciente_id, pieza_fdi),
    CONSTRAINT hallazgo_fdi_chk CHECK (pieza_fdi BETWEEN 11 AND 48 AND pieza_fdi % 10 BETWEEN 1 AND 8)
);

-- ------------------------------------------------------------
-- 13. CierreCaja (absorbe arqueo_caja + historial de fondos en JSONB, RF-09)
-- ------------------------------------------------------------
CREATE TABLE cierre_caja (
    id                 BIGSERIAL PRIMARY KEY,
    fecha              DATE NOT NULL,
    turno              VARCHAR(10) NOT NULL DEFAULT 'DIURNO',
    cajero_id          INT NOT NULL REFERENCES usuario(id),
    fondo_inicial      NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (fondo_inicial >= 0),
    historial_fondos   JSONB NOT NULL DEFAULT '[]'::jsonb,
    efectivo_sistema   NUMERIC(12,2) NOT NULL DEFAULT 0,
    billeteras_sistema NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_sistema      NUMERIC(12,2) NOT NULL DEFAULT 0,
    efectivo_contado   NUMERIC(12,2),
    diferencia         NUMERIC(12,2) GENERATED ALWAYS AS (efectivo_contado - fondo_inicial - efectivo_sistema) STORED,
    estado             estado_cierre NOT NULL DEFAULT 'ABIERTO',
    consolidado_por    INT REFERENCES usuario(id),
    consolidado_en     TIMESTAMP,
    acta_generada      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT cierre_unico_dia UNIQUE (fecha, turno)
);

-- ------------------------------------------------------------
-- 12. Pago (absorbe comprobante + abonos; abono = fila con pago_origen_id)
-- ------------------------------------------------------------
CREATE TABLE pago (
    id              BIGSERIAL PRIMARY KEY,
    numero          VARCHAR(20) UNIQUE,                  -- B001-000458 · NULL en abonos
    tipo            tipo_documento NOT NULL DEFAULT 'BOLETA',
    estado          estado_documento NOT NULL DEFAULT 'EMITIDO',
    pago_origen_id  BIGINT REFERENCES pago(id),          -- NULL = documento · NOT NULL = abono
    fecha           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paciente_id     INT NOT NULL REFERENCES paciente(id),
    ficha_id        BIGINT REFERENCES ficha_clinica(id),
    odontologo_id   INT REFERENCES usuario(id),
    usuario_caja_id INT NOT NULL REFERENCES usuario(id),
    cierre_id       BIGINT REFERENCES cierre_caja(id),
    modalidad       modalidad_pago NOT NULL DEFAULT 'EFECTIVO',
    operacion_ref   VARCHAR(40),                         -- N° Yape/Plin/Tarjeta (RF-08)
    subtotal        NUMERIC(10,2) NOT NULL DEFAULT 0,
    igv             NUMERIC(10,2) NOT NULL DEFAULT 0,
    monto           NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (monto >= 0),
    saldo_deudor    NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (saldo_deudor >= 0),
    estado_pago     estado_pago NOT NULL DEFAULT 'PENDIENTE',
    CONSTRAINT pago_billetera_requiere_operacion CHECK (     -- RF-08
        modalidad = 'EFECTIVO' OR operacion_ref IS NOT NULL
    ),
    CONSTRAINT pago_documento_o_abono CHECK (                -- boleta tiene número; abono apunta a un documento
        (pago_origen_id IS NULL AND numero IS NOT NULL)
        OR (pago_origen_id IS NOT NULL)
    )
);

-- ------------------------------------------------------------
-- 11. PlanTratamiento (absorbe atencion_procedimiento + comprobante_detalle)
--      1 fila viaja: PLANIFICADO → EJECUTADO → COBRADO
-- ------------------------------------------------------------
CREATE TABLE plan_tratamiento (
    id              BIGSERIAL PRIMARY KEY,
    paciente_id     INT NOT NULL REFERENCES paciente(id),
    tratamiento_id  INT NOT NULL REFERENCES tratamiento(id),
    ficha_id        BIGINT REFERENCES ficha_clinica(id),
    pago_id         BIGINT,                              -- FK se agrega tras crear pago
    pieza_fdi       SMALLINT,
    cantidad        INT NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    precio_unitario NUMERIC(10,2) NOT NULL CHECK (precio_unitario >= 0),
    subtotal        NUMERIC(10,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED,
    estado          estado_linea NOT NULL DEFAULT 'PLANIFICADO',
    fecha           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT plan_fdi_chk CHECK (
        pieza_fdi IS NULL OR (pieza_fdi BETWEEN 11 AND 48 AND pieza_fdi % 10 BETWEEN 1 AND 8)
    ),
    CONSTRAINT plan_cobrado_requiere_boleta CHECK (
        estado <> 'COBRADO' OR pago_id IS NOT NULL
    )
);

ALTER TABLE plan_tratamiento
    ADD CONSTRAINT plan_pago_fk FOREIGN KEY (pago_id) REFERENCES pago(id);

-- ============================================================
-- Funciones y triggers (RF-06, RF-07, RF-08, RF-09)
-- ============================================================

-- RF-06 · solo un diagnóstico principal por ficha --------------------
CREATE OR REPLACE FUNCTION ficha_diag_un_principal() RETURNS TRIGGER AS $$
DECLARE v_otros INT;
BEGIN
    IF NEW.es_principal THEN
        SELECT COUNT(*) INTO v_otros
          FROM ficha_diagnostico
         WHERE ficha_id = NEW.ficha_id AND es_principal AND cie10_codigo <> NEW.cie10_codigo;
        IF v_otros > 0 THEN
            RAISE EXCEPTION 'RF-06: la ficha % ya tiene un diagnostico principal', NEW.ficha_id;
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER tr_ficha_diag_principal
BEFORE INSERT OR UPDATE ON ficha_diagnostico
FOR EACH ROW EXECUTE FUNCTION ficha_diag_un_principal();

-- RF-07 · no se factura una ficha sin derivación ACEPTADA ------------
CREATE OR REPLACE FUNCTION pago_requiere_derivacion() RETURNS TRIGGER AS $$
DECLARE v_est estado_derivacion;
BEGIN
    IF NEW.ficha_id IS NULL THEN RETURN NEW; END IF;
    SELECT estado_derivacion INTO v_est FROM ficha_clinica WHERE id = NEW.ficha_id;
    IF v_est IS DISTINCT FROM 'ACEPTADA' THEN
        RAISE EXCEPTION 'RF-07: la ficha % esta en derivacion %; solo se factura con derivacion ACEPTADA',
            NEW.ficha_id, COALESCE(v_est::TEXT, 'INEXISTENTE');
    END IF;
    UPDATE ficha_clinica
       SET derivado_a_caja = TRUE,
           fecha_derivacion = COALESCE(fecha_derivacion, CURRENT_TIMESTAMP)
     WHERE id = NEW.ficha_id;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER tr_pago_requiere_derivacion
BEFORE INSERT ON pago
FOR EACH ROW EXECUTE FUNCTION pago_requiere_derivacion();

-- RF-08 · un abono apunta a una boleta, nunca a otro abono ----------
CREATE OR REPLACE FUNCTION pago_abono_valido() RETURNS TRIGGER AS $$
DECLARE v_origen_de_origen BIGINT;
BEGIN
    SELECT pago_origen_id INTO v_origen_de_origen FROM pago WHERE id = NEW.pago_origen_id;
    IF v_origen_de_origen IS NOT NULL THEN
        RAISE EXCEPTION 'RF-08: el abono % apunta a otro abono; debe apuntar a la boleta', NEW.pago_origen_id;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER tr_pago_abono_valido
BEFORE INSERT ON pago
FOR EACH ROW WHEN (NEW.pago_origen_id IS NOT NULL)
EXECUTE FUNCTION pago_abono_valido();

-- RF-08 · los abonos recalculan saldo y estado del documento ---------
CREATE OR REPLACE FUNCTION abono_actualiza_saldo() RETURNS TRIGGER AS $$
DECLARE v_monto NUMERIC(10,2); v_abonos NUMERIC(10,2);
BEGIN
    SELECT monto INTO v_monto FROM pago WHERE id = NEW.pago_origen_id;
    SELECT COALESCE(SUM(monto), 0) INTO v_abonos FROM pago WHERE pago_origen_id = NEW.pago_origen_id;
    UPDATE pago
       SET saldo_deudor = GREATEST(v_monto - v_abonos, 0),
           estado_pago = CASE WHEN v_abonos >= v_monto THEN 'PAGADO'::estado_pago
                              ELSE 'PARCIAL'::estado_pago END
     WHERE id = NEW.pago_origen_id;
    RETURN NULL;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER tr_abono_actualiza_saldo
AFTER INSERT ON pago
FOR EACH ROW WHEN (NEW.pago_origen_id IS NOT NULL)
EXECUTE FUNCTION abono_actualiza_saldo();

-- RF-08 · las líneas de la boleta recalculan monto, IGV y saldo ------
CREATE OR REPLACE FUNCTION plan_recalcular_pago() RETURNS TRIGGER AS $$
DECLARE
    v_pago_id BIGINT;
    v_monto   NUMERIC(10,2);
    v_abonos  NUMERIC(10,2);
    v_igv     NUMERIC(4,2);
BEGIN
    v_pago_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.pago_id ELSE NEW.pago_id END;
    IF v_pago_id IS NULL THEN RETURN NULL; END IF;
    SELECT COALESCE(SUM(subtotal), 0) INTO v_monto FROM plan_tratamiento WHERE pago_id = v_pago_id;
    SELECT COALESCE(SUM(monto), 0) INTO v_abonos FROM pago WHERE pago_origen_id = v_pago_id;
    SELECT igv_porcentaje INTO v_igv FROM configuracion WHERE id = 1;
    UPDATE pago
       SET monto = v_monto,
           subtotal = ROUND(v_monto / (1 + v_igv / 100), 2),
           igv = v_monto - ROUND(v_monto / (1 + v_igv / 100), 2),
           saldo_deudor = GREATEST(v_monto - v_abonos, 0),
           estado_pago = CASE WHEN v_abonos = 0 THEN 'PENDIENTE'::estado_pago
                              WHEN v_abonos >= v_monto THEN 'PAGADO'::estado_pago
                              ELSE 'PARCIAL'::estado_pago END
     WHERE id = v_pago_id;
    RETURN NULL;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER tr_plan_recalcular_pago
AFTER INSERT OR UPDATE OF pago_id, subtotal OR DELETE ON plan_tratamiento
FOR EACH ROW EXECUTE FUNCTION plan_recalcular_pago();

-- RF-09 · historial del fondo inicial (JSONB) ------------------------
CREATE OR REPLACE FUNCTION cierre_historial_fondo() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.fondo_inicial IS DISTINCT FROM OLD.fondo_inicial THEN
        NEW.historial_fondos := OLD.historial_fondos || jsonb_build_array(jsonb_build_object(
            'monto_anterior', OLD.fondo_inicial,
            'monto_nuevo',    NEW.fondo_inicial,
            'fecha',          CURRENT_TIMESTAMP
        ));
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER tr_cierre_historial_fondo
BEFORE UPDATE ON cierre_caja
FOR EACH ROW EXECUTE FUNCTION cierre_historial_fondo();

-- ============================================================
-- Índices de apoyo
-- ============================================================
CREATE INDEX idx_cita_fecha        ON cita (fecha_cita);
CREATE INDEX idx_cita_paciente     ON cita (paciente_id, fecha_cita);
CREATE INDEX idx_ficha_paciente    ON ficha_clinica (paciente_id, fecha);
CREATE INDEX idx_ficha_diag_codigo ON ficha_diagnostico (cie10_codigo);
CREATE INDEX idx_plan_ficha        ON plan_tratamiento (ficha_id);
CREATE INDEX idx_plan_pago         ON plan_tratamiento (pago_id);
CREATE INDEX idx_pago_paciente     ON pago (paciente_id, fecha);
CREATE INDEX idx_pago_cierre       ON pago (cierre_id);
CREATE INDEX idx_hallazgo_paciente ON hallazgo_dental (paciente_id);

-- ============================================================
-- Vistas del Módulo 8 (reportes y KPIs)
-- ============================================================
CREATE VIEW v_saldos_paciente AS
SELECT p.id AS paciente_id, p.codigo_hc, p.dni, p.apellidos, p.nombres,
       COALESCE(SUM(pg.saldo_deudor) FILTER (WHERE pg.pago_origen_id IS NULL AND pg.estado = 'EMITIDO'), 0) AS saldo_total,
       COUNT(pg.id) FILTER (WHERE pg.pago_origen_id IS NULL AND pg.estado_pago <> 'PAGADO') AS documentos_pendientes
  FROM paciente p
  LEFT JOIN pago pg ON pg.paciente_id = p.id
 GROUP BY p.id, p.codigo_hc, p.dni, p.apellidos, p.nombres;

CREATE VIEW v_kpis_dashboard AS
SELECT (SELECT COUNT(*) FROM cita WHERE fecha_cita = CURRENT_DATE AND tipo = 'CITA') AS citas_hoy,
       (SELECT COUNT(*) FROM cita WHERE fecha_cita = CURRENT_DATE AND estado = 'EN_SALA') AS sala_espera,
       (SELECT COALESCE(SUM(monto), 0) FROM pago
         WHERE fecha::date = CURRENT_DATE AND pago_origen_id IS NULL AND estado = 'EMITIDO') AS recaudado_hoy,
       (SELECT COUNT(DISTINCT paciente_id) FROM pago
         WHERE pago_origen_id IS NULL AND saldo_deudor > 0 AND estado = 'EMITIDO') AS pacientes_deudores;

CREATE VIEW v_ranking_tratamientos AS
SELECT t.id AS tratamiento_id, t.codigo, t.nombre, t.especialidad,
       SUM(pl.cantidad) AS unidades_cobradas,
       SUM(pl.subtotal) AS monto_cobrado
  FROM plan_tratamiento pl
  JOIN tratamiento t ON t.id = pl.tratamiento_id
 WHERE pl.estado = 'COBRADO'
 GROUP BY t.id, t.codigo, t.nombre, t.especialidad
 ORDER BY monto_cobrado DESC;

CREATE VIEW v_productividad_odontologo AS
SELECT u.id AS usuario_id, u.nombres, u.apellidos, u.colegiatura,
       COUNT(f.id) AS atenciones,
       COUNT(DISTINCT f.paciente_id) AS pacientes_atendidos
  FROM usuario u
  LEFT JOIN ficha_clinica f ON f.odontologo_id = u.id
 WHERE u.colegiatura IS NOT NULL
 GROUP BY u.id, u.nombres, u.apellidos, u.colegiatura
 ORDER BY atenciones DESC;

CREATE VIEW v_ingresos_mes AS
SELECT TO_CHAR(pg.fecha, 'YYYY-MM') AS mes,
       pg.modalidad,
       COUNT(*) FILTER (WHERE pg.pago_origen_id IS NULL) AS documentos,
       COALESCE(SUM(pg.monto) FILTER (WHERE pg.pago_origen_id IS NULL), 0) AS ingresado,
       COALESCE(SUM(pg.monto) FILTER (WHERE pg.pago_origen_id IS NOT NULL), 0) AS abonado
  FROM pago pg
 WHERE pg.estado = 'EMITIDO'
 GROUP BY TO_CHAR(pg.fecha, 'YYYY-MM'), pg.modalidad
 ORDER BY mes DESC, ingresado DESC;

CREATE VIEW v_no_show AS
SELECT fecha_cita,
       COUNT(*) FILTER (WHERE tipo = 'CITA') AS citas_programadas,
       COUNT(*) FILTER (WHERE estado = 'FALTO') AS ausentes,
       ROUND(100.0 * COUNT(*) FILTER (WHERE estado = 'FALTO')
             / NULLIF(COUNT(*) FILTER (WHERE tipo = 'CITA'), 0), 2) AS tasa_ausentismo_pct
  FROM cita
 GROUP BY fecha_cita
 ORDER BY fecha_cita DESC;

-- ============================================================
-- Fin del esquema v2 (14 tablas + 6 vistas + 6 triggers)
-- Siguiente: sql/02_datos_prueba_v2.sql
-- ============================================================
