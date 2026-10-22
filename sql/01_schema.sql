-- ============================================================
-- DENTRIX DENTAL · Esquema PostgreSQL (1ª versión)
-- Fuente de verdad: 13 wireframes corregidos (prototipos_html)
--                    + informe AVANCE S8 (RF-01..RF-15, RNF-01..05)
-- Módulos representados: M1 login · M2 dashboard · M3 triaje ·
--   M4 pacientes · M5 agenda · M6 historia clínica · M7/M7.1 caja
--   · M8 reportes (vistas) · M9.1-M9.3 configuración
-- Ejecutar: psql -U postgres -f 01_schema.sql
-- ============================================================

DROP SCHEMA IF EXISTS dentrix CASCADE;
CREATE SCHEMA dentrix;
SET search_path TO dentrix;

-- ------------------------------------------------------------
-- TIPOS (ENUM) — estados modelados en las láminas
-- ------------------------------------------------------------
CREATE TYPE estado_usuario     AS ENUM ('ACTIVO','INACTIVO','BLOQUEADO');
CREATE TYPE estado_paciente    AS ENUM ('ACTIVO','INACTIVO');
CREATE TYPE estado_cita        AS ENUM ('PENDIENTE','CONFIRMADA','EN_SALA','EN_ATENCION','ATENDIDA','FALTO','CANCELADA','BLOQUEADA');
CREATE TYPE estado_atencion    AS ENUM ('ABIERTA','DERIVADA','CERRADA');
CREATE TYPE estado_derivacion  AS ENUM ('PENDIENTE','ACEPTADA','RECHAZADA');   -- RF-07
CREATE TYPE estado_comprobante AS ENUM ('EMITIDO','ANULADO');
CREATE TYPE estado_pago        AS ENUM ('PENDIENTE','PARCIAL','PAGADO');
CREATE TYPE modalidad_pago     AS ENUM ('EFECTIVO','YAPE','PLIN','TARJETA');  -- RF-08
CREATE TYPE tipo_comprobante   AS ENUM ('BOLETA','NOTA_CREDITO','VOUCHER');  -- NOTA_CREDITO: reservado, fuera de alcance v3
CREATE TYPE estado_arqueo      AS ENUM ('ABIERTO','CONSOLIDADO');             -- M7.1 / Turno Cerrado
CREATE TYPE estado_backup      AS ENUM ('EXITOSO','FALLIDO','VERIFICADO');    -- RNF-05 / M9.3
CREATE TYPE tipo_bloque        AS ENUM ('ALMUERZO','CIERRE','BLOQUEADO');
CREATE TYPE estado_pieza       AS ENUM ('SANO','CARIES','OBTURADO','AUSENTE','CORONA','IMPLANTE','ENDODONCIA','PROTESIS');

-- ------------------------------------------------------------
-- M9.3 · Datos de la clínica (registro único) + consultorios
-- ------------------------------------------------------------
CREATE TABLE clinica_config (
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
    igv_porcentaje      NUMERIC(4,2) NOT NULL DEFAULT 18.00,
    CONSTRAINT clinica_singleton CHECK (id = 1)
);

CREATE TABLE consultorio (
    id        SERIAL PRIMARY KEY,
    codigo    VARCHAR(4) NOT NULL UNIQUE,          -- C01, C02
    nombre    VARCHAR(60) NOT NULL,
    especialidad VARCHAR(60),
    estado    VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE'
             CHECK (estado IN ('DISPONIBLE','OCUPADO','MANTENIMIENTO'))
);

-- ------------------------------------------------------------
-- M1 + M9.1 · Usuarios y roles (RBAC real de la matriz M9.1)
-- ------------------------------------------------------------
CREATE TABLE rol (
    id          SERIAL PRIMARY KEY,
    codigo      VARCHAR(20) NOT NULL UNIQUE,       -- ADMINISTRADOR | ODONTOLOGO | RECEPCION
    nombre      VARCHAR(40) NOT NULL,
    descripcion VARCHAR(120)
);

CREATE TABLE usuario (
    id              SERIAL PRIMARY KEY,
    username        VARCHAR(30) NOT NULL UNIQUE,
    password_hash   VARCHAR(120) NOT NULL,         -- bcrypt en producción (RNF-01)
    nombres         VARCHAR(60) NOT NULL,
    apellidos       VARCHAR(60) NOT NULL,
    dni             CHAR(8) NOT NULL UNIQUE,
    rol_id          INT NOT NULL REFERENCES rol(id),
    estado          estado_usuario NOT NULL DEFAULT 'ACTIVO',
    intentos_fallidos INT NOT NULL DEFAULT 0,      -- M1: máx. 3 → BLOQUEADO
    bloqueado_hasta TIMESTAMP,
    ultimo_acceso   TIMESTAMP,
    fecha_creacion  DATE NOT NULL DEFAULT CURRENT_DATE
);

CREATE TABLE odontologo (
    id             SERIAL PRIMARY KEY,
    usuario_id     INT UNIQUE REFERENCES usuario(id),  -- el odontólogo también inicia sesión
    dni            CHAR(8) NOT NULL UNIQUE,
    nombres        VARCHAR(60) NOT NULL,
    apellidos      VARCHAR(60) NOT NULL,
    colegiatura    VARCHAR(30),                         -- COP
    especialidad   VARCHAR(60),
    consultorio_id INT REFERENCES consultorio(id),
    estado         VARCHAR(12) NOT NULL DEFAULT 'ACTIVO'
                   CHECK (estado IN ('ACTIVO','INACTIVO'))
);

-- ------------------------------------------------------------
-- M4 · Pacientes (RF-04: DNI único, alta y borrado lógico)
-- ------------------------------------------------------------
CREATE TABLE paciente (
    id            SERIAL PRIMARY KEY,
    codigo        VARCHAR(7) GENERATED ALWAYS AS ('P-' || LPAD(id::TEXT, 3, '0')) STORED,  -- P-001…
    dni           CHAR(8) NOT NULL UNIQUE,           -- ← anti-duplicado RF-04/RF-08
    apellidos     VARCHAR(60) NOT NULL,
    nombres       VARCHAR(60) NOT NULL,
    fecha_nac     DATE,
    celular       VARCHAR(15),
    email         VARCHAR(80),
    direccion     VARCHAR(160),
    alergias      TEXT,                              -- ⚠ alerta farmacológica (visible en M4/M6)
    estado        estado_paciente NOT NULL DEFAULT 'ACTIVO',
    fecha_registro DATE NOT NULL DEFAULT CURRENT_DATE,
    usuario_registro_id INT REFERENCES usuario(id)
);
CREATE INDEX idx_paciente_apellidos ON paciente (apellidos, nombres);

-- ------------------------------------------------------------
-- M6 · Historia clínica (1:1 con paciente) + CIE-10 (M9.2.2, RF-13)
-- ------------------------------------------------------------
CREATE TABLE historia_clinica (
    id            SERIAL PRIMARY KEY,
    paciente_id   INT NOT NULL UNIQUE REFERENCES paciente(id),
    fecha_apertura DATE NOT NULL DEFAULT CURRENT_DATE,
    observaciones_generales TEXT
);

CREATE TABLE cie10 (
    codigo     VARCHAR(7) PRIMARY KEY,               -- K02.1, K05.1…
    descripcion VARCHAR(120) NOT NULL,
    capitulo   VARCHAR(60) NOT NULL,
    es_frecuente BOOLEAN NOT NULL DEFAULT FALSE       -- ⭐ M9.2.2
);

CREATE TABLE odontograma_pieza (
    historia_id    INT NOT NULL REFERENCES historia_clinica(id),
    pieza_fdi      SMALLINT NOT NULL CHECK (pieza_fdi BETWEEN 11 AND 48 AND pieza_fdi % 10 BETWEEN 1 AND 8),
    estado         estado_pieza NOT NULL,
    observacion    VARCHAR(120),
    ultima_atencion_id BIGINT,                        -- FK se agrega tras crear atencion
    PRIMARY KEY (historia_id, pieza_fdi)
);

-- ------------------------------------------------------------
-- M5 · Agenda (citas + bloques no atendibles de la grilla)
-- ------------------------------------------------------------
CREATE TABLE cita (
    id             BIGSERIAL PRIMARY KEY,
    paciente_id    INT NOT NULL REFERENCES paciente(id),
    odontologo_id  INT NOT NULL REFERENCES odontologo(id),
    consultorio_id INT NOT NULL REFERENCES consultorio(id),
    fecha_cita     DATE NOT NULL,
    hora_inicio    TIME NOT NULL,
    hora_fin       TIME NOT NULL,
    procedimiento_planificado_id INT REFERENCES procedimiento(id),  -- FK agregada abajo
    estado         estado_cita NOT NULL DEFAULT 'PENDIENTE',
    observaciones  VARCHAR(200),
    CONSTRAINT cita_horario_valido CHECK (hora_fin > hora_inicio),
    CONSTRAINT cita_unica_por_consultorio UNIQUE (consultorio_id, fecha_cita, hora_inicio)
);
CREATE INDEX idx_cita_fecha ON cita (fecha_cita);
CREATE INDEX idx_cita_paciente ON cita (paciente_id);

CREATE TABLE bloque_agenda (
    id             SERIAL PRIMARY KEY,
    consultorio_id INT NOT NULL REFERENCES consultorio(id),
    dia_semana     SMALLINT NOT NULL CHECK (dia_semana BETWEEN 0 AND 6),  -- 0=domingo
    hora_inicio    TIME NOT NULL,
    hora_fin       TIME NOT NULL,
    tipo           tipo_bloque NOT NULL,
    motivo         VARCHAR(80),
    CONSTRAINT bloque_unico UNIQUE (consultorio_id, dia_semana, hora_inicio)
);

-- ------------------------------------------------------------
-- M6 · Atención odontológica (encuentro clínico; RF-07 derivación)
-- ------------------------------------------------------------
CREATE TABLE atencion (
    id             BIGSERIAL PRIMARY KEY,
    historia_id    INT NOT NULL REFERENCES historia_clinica(id),
    cita_id        BIGINT UNIQUE REFERENCES cita(id),
    odontologo_id  INT NOT NULL REFERENCES odontologo(id),
    fecha          DATE NOT NULL,
    motivo_consulta VARCHAR(160),
    examen_clinico  TEXT,
    plan_tratamiento TEXT,
    estado          estado_atencion NOT NULL DEFAULT 'ABIERTA',
    derivado_a_caja BOOLEAN NOT NULL DEFAULT FALSE,
    estado_derivacion estado_derivacion,              -- NULL hasta derivar; ACEPTADA habilita cobro (RF-07)
    fecha_derivacion TIMESTAMP
);
CREATE INDEX idx_atencion_fecha ON atencion (fecha);

ALTER TABLE odontograma_pieza
    ADD CONSTRAINT fk_odonto_atencion FOREIGN KEY (ultima_atencion_id) REFERENCES atencion(id);

CREATE TABLE atencion_diagnostico (
    atencion_id  BIGINT NOT NULL REFERENCES atencion(id) ON DELETE CASCADE,
    cie10_codigo VARCHAR(7) NOT NULL REFERENCES cie10(codigo),
    pieza_fdi    SMALLINT,
    es_principal BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (atencion_id, cie10_codigo)
);

-- ------------------------------------------------------------
-- M9.2.1 · Tarifario de procedimientos
-- ------------------------------------------------------------
CREATE TABLE procedimiento (
    id           SERIAL PRIMARY KEY,
    codigo       VARCHAR(10) NOT NULL UNIQUE,        -- TRAT-01…
    nombre       VARCHAR(80) NOT NULL,
    especialidad VARCHAR(40) NOT NULL,
    calculo      VARCHAR(20) NOT NULL CHECK (calculo IN ('PIEZA','SESION','MENSUAL')),
    precio       NUMERIC(10,2) NOT NULL CHECK (precio >= 0),
    estado       VARCHAR(12) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO','INACTIVO'))
);

CREATE TABLE atencion_procedimiento (
    id                 BIGSERIAL PRIMARY KEY,
    atencion_id        BIGINT NOT NULL REFERENCES atencion(id) ON DELETE CASCADE,
    procedimiento_id   INT NOT NULL REFERENCES procedimiento(id),
    pieza_fdi          SMALLINT,                     -- requerido si calculo = PIEZA
    cantidad           INT NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    precio_unitario    NUMERIC(10,2) NOT NULL,       -- precio del tarifario al momento de atender
    subtotal           NUMERIC(10,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED
);

-- ------------------------------------------------------------
-- M7/M7.1 · Caja: comprobantes, abonos (saldos) y arqueo
-- ------------------------------------------------------------
CREATE TABLE arqueo_caja (
    id             SERIAL PRIMARY KEY,
    fecha          DATE NOT NULL,
    turno          VARCHAR(12) NOT NULL DEFAULT 'DIURNO',
    cajero_id      INT NOT NULL REFERENCES usuario(id),
    fondo_inicial  NUMERIC(10,2) NOT NULL DEFAULT 0,
    efectivo_sistema NUMERIC(10,2) NOT NULL DEFAULT 0,   -- cobros en efectivo del turno
    billeteras_sistema NUMERIC(10,2) NOT NULL DEFAULT 0, -- Yape+Plin del turno
    total_sistema  NUMERIC(10,2) NOT NULL DEFAULT 0,
    efectivo_contado NUMERIC(10,2),                      -- NULL mientras el turno está abierto
    diferencia     NUMERIC(10,2),
    estado         estado_arqueo NOT NULL DEFAULT 'ABIERTO',
    consolidado_por INT REFERENCES usuario(id),
    consolidado_en TIMESTAMP,
    acta_generada  BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT arqueo_unico_por_dia UNIQUE (fecha, turno)
);

CREATE TABLE fondo_inicial_historial (
    id          BIGSERIAL PRIMARY KEY,
    arqueo_id   INT NOT NULL REFERENCES arqueo_caja(id),
    monto       NUMERIC(10,2) NOT NULL,
    usuario_id  INT NOT NULL REFERENCES usuario(id),
    motivo      VARCHAR(120),
    fecha       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);                                                          -- RF-09: auditoría del fondo inicial

CREATE TABLE comprobante (
    id             BIGSERIAL PRIMARY KEY,
    numero         VARCHAR(12) NOT NULL UNIQUE,            -- B001-000458 (SUNAT)
    tipo           tipo_comprobante NOT NULL DEFAULT 'BOLETA',
    fecha          DATE NOT NULL,
    paciente_id    INT NOT NULL REFERENCES paciente(id),
    atencion_id    BIGINT REFERENCES atencion(id),
    odontologo_id  INT REFERENCES odontologo(id),
    usuario_caja_id INT NOT NULL REFERENCES usuario(id),
    arqueo_id      INT REFERENCES arqueo_caja(id),
    modalidad      modalidad_pago NOT NULL,
    operacion_ref  VARCHAR(30),                            -- N° Yape/Plin/Tarjeta
    subtotal       NUMERIC(10,2) NOT NULL,
    igv            NUMERIC(10,2) NOT NULL,
    total          NUMERIC(10,2) NOT NULL,
    estado         estado_comprobante NOT NULL DEFAULT 'EMITIDO',
    estado_pago    estado_pago NOT NULL DEFAULT 'PENDIENTE',
    saldo_pendiente NUMERIC(10,2) NOT NULL DEFAULT 0,
    -- RF-08: la operación de billetera digital no puede repetirse (boleta de pago Yape/Plin)
    CONSTRAINT uq_operacion_ref UNIQUE (operacion_ref),
    CONSTRAINT ck_operacion_requerida CHECK (
        (modalidad IN ('YAPE','PLIN','TARJETA') AND operacion_ref IS NOT NULL)
        OR (modalidad = 'EFECTIVO' AND operacion_ref IS NULL)
    ),
    CONSTRAINT ck_totales CHECK (subtotal + igv = total)
);
CREATE INDEX idx_comprobante_fecha ON comprobante (fecha);
CREATE INDEX idx_comprobante_paciente ON comprobante (paciente_id);

CREATE TABLE comprobante_detalle (                     -- líneas impresas en la boleta (M7)
    id               BIGSERIAL PRIMARY KEY,
    comprobante_id   BIGINT NOT NULL REFERENCES comprobante(id) ON DELETE CASCADE,
    procedimiento_id INT NOT NULL REFERENCES procedimiento(id),
    cantidad         INT NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    precio_unitario  NUMERIC(10,2) NOT NULL,
    subtotal         NUMERIC(10,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED
);

CREATE TABLE abono (
    id             BIGSERIAL PRIMARY KEY,
    comprobante_id BIGINT NOT NULL REFERENCES comprobante(id),
    monto          NUMERIC(10,2) NOT NULL CHECK (monto > 0),
    fecha          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modalidad      modalidad_pago NOT NULL,
    referencia     VARCHAR(30),
    usuario_id     INT NOT NULL REFERENCES usuario(id)
);

-- Trigger: al emitir un comprobante su saldo = total; cada abono lo reduce (M2/M6 saldos)
CREATE OR REPLACE FUNCTION fn_actualizar_saldo() RETURNS TRIGGER AS $$
BEGIN
    UPDATE comprobante c
       SET saldo_pendiente = c.total - COALESCE((
             SELECT SUM(a.monto) FROM abono a WHERE a.comprobante_id = c.id), 0),
           estado_pago = CASE
             WHEN c.total - COALESCE((SELECT SUM(a.monto) FROM abono a WHERE a.comprobante_id = c.id), 0) <= 0 THEN 'PAGADO'::estado_pago
             WHEN COALESCE((SELECT SUM(a.monto) FROM abono a WHERE a.comprobante_id = c.id), 0) > 0 THEN 'PARCIAL'::estado_pago
             ELSE 'PENDIENTE'::estado_pago END
     WHERE c.id = NEW.comprobante_id;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER tr_abono_actualiza_saldo AFTER INSERT ON abono
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_saldo();

CREATE OR REPLACE FUNCTION fn_saldo_al_emitir() RETURNS TRIGGER AS $$
BEGIN
    NEW.saldo_pendiente := NEW.total;
    NEW.estado_pago := 'PENDIENTE'::estado_pago;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER tr_comprobante_saldo_emitir BEFORE INSERT ON comprobante
    FOR EACH ROW WHEN (NEW.estado = 'EMITIDO') EXECUTE FUNCTION fn_saldo_al_emitir();

-- ------------------------------------------------------------
-- M9.3 · Bitácora de backups (RF-15 / RNF-05)
-- ------------------------------------------------------------
CREATE TABLE backup_log (
    id          BIGSERIAL PRIMARY KEY,
    fecha       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archivo     VARCHAR(80) NOT NULL,
    tamano_mb   NUMERIC(8,2),
    estado      estado_backup NOT NULL,
    usuario_id  INT REFERENCES usuario(id),
    notas       VARCHAR(160)
);

-- ------------------------------------------------------------
-- M8 · Vistas de reportes/estadística (las consulta el módulo M8)
-- ------------------------------------------------------------
CREATE VIEW v_saldos_paciente AS
SELECT p.id AS paciente_id, p.codigo, p.dni, p.apellidos, p.nombres,
       COALESCE(SUM(c.saldo_pendiente), 0) AS saldo_total,
       COUNT(c.id) FILTER (WHERE c.saldo_pendiente > 0) AS comprobantes_pendientes
  FROM paciente p
  LEFT JOIN comprobante c ON c.paciente_id = p.id AND c.estado = 'EMITIDO'
 GROUP BY p.id;

CREATE VIEW v_kpis_dashboard AS
SELECT (SELECT COUNT(*) FROM cita WHERE fecha_cita = CURRENT_DATE) AS citados_hoy,
       (SELECT COUNT(*) FROM cita WHERE fecha_cita = CURRENT_DATE AND estado = 'EN_SALA') AS en_sala,
       (SELECT COALESCE(SUM(total), 0) FROM comprobante
         WHERE fecha = CURRENT_DATE AND estado = 'EMITIDO') AS recaudacion_hoy,
       (SELECT COALESCE(SUM(saldo_pendiente), 0) FROM comprobante
         WHERE estado = 'EMITIDO' AND saldo_pendiente > 0) AS saldos_por_cobrar;

CREATE VIEW v_ranking_procedimientos AS
SELECT pr.codigo, pr.nombre, pr.especialidad,
       SUM(ap.cantidad) AS atenciones,
       SUM(ap.subtotal) AS monto_total,
       ROUND(100.0 * SUM(ap.subtotal) / NULLIF((SELECT SUM(subtotal) FROM atencion_procedimiento), 0), 1) AS pct_ingresos
  FROM atencion_procedimiento ap
  JOIN procedimiento pr ON pr.id = ap.procedimiento_id
 GROUP BY pr.codigo, pr.nombre, pr.especialidad
 ORDER BY monto_total DESC;

CREATE VIEW v_productividad_odontologo AS
SELECT o.id, o.nombres || ' ' || o.apellidos AS odontologo,
       COUNT(DISTINCT ap.atencion_id) AS procedimientos,
       COALESCE(SUM(ap.subtotal), 0) AS facturacion,
       ROUND(COALESCE(SUM(ap.subtotal), 0) / NULLIF(COUNT(DISTINCT ap.atencion_id), 0), 2) AS ticket_promedio
  FROM odontologo o
  LEFT JOIN atencion a ON a.odontologo_id = o.id
  LEFT JOIN atencion_procedimiento ap ON ap.atencion_id = a.id
 GROUP BY o.id, o.nombres, o.apellidos
 ORDER BY facturacion DESC;

CREATE VIEW v_ingresos_mes AS
SELECT DATE_TRUNC('month', fecha) AS mes,
       SUM(total) FILTER (WHERE modalidad = 'EFECTIVO') AS efectivo,
       SUM(total) FILTER (WHERE modalidad IN ('YAPE','PLIN')) AS billeteras,
       SUM(total) FILTER (WHERE modalidad = 'TARJETA') AS tarjetas,
       SUM(total) AS total_mes
  FROM comprobante
 WHERE estado = 'EMITIDO'
 GROUP BY 1
 ORDER BY 1 DESC;

CREATE VIEW v_no_show AS
SELECT fecha_cita,
       COUNT(*) AS citas,
       COUNT(*) FILTER (WHERE estado IN ('ATENDIDA','EN_ATENCION')) AS atendidas,
       COUNT(*) FILTER (WHERE estado = 'FALTO') AS faltaron,
       ROUND(100.0 * COUNT(*) FILTER (WHERE estado = 'FALTO') / NULLIF(COUNT(*), 0), 1) AS pct_no_show
  FROM cita
 GROUP BY fecha_cita
 ORDER BY fecha_cita DESC;

-- FK postergada de cita → procedimiento (la tabla se crea después en el guión)
ALTER TABLE cita
    ADD CONSTRAINT fk_cita_procedimiento
    FOREIGN KEY (procedimiento_planificado_id) REFERENCES procedimiento(id);

-- Fin del esquema
