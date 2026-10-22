-- ============================================================
-- DENTRIX DENTAL · Esquema v3 · ALINEACION DE PANTALLAS (M4 y M5)
-- Archivo ADITIVO: se ejecuta DESPUÉS de 17_datos_demo_hoy.sql
--   psql -U postgres -d dentrix -f sql/18_schema_v3_alineacion_ui.sql
--
-- QUÉ RESUELVE
--   - M4 corregido pide tutor (paciente menor) y pagador distinto del paciente.
--   - M5 corregido calcula el fin de la cita con la duración del catálogo.
--   Ambas cosas no existían en el v2; se agregan como columnas opcionales.
-- ============================================================
SET search_path TO dentrix;

ALTER TABLE paciente ADD COLUMN IF NOT EXISTS tutor_nombre VARCHAR(120);
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS tutor_documento VARCHAR(20);
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS tutor_vinculo VARCHAR(20);
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS pagador_nombre VARCHAR(120);
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS pagador_documento VARCHAR(20);

ALTER TABLE tratamiento ADD COLUMN IF NOT EXISTS duracion_minutos INT NOT NULL DEFAULT 30;

ALTER TABLE tratamiento DROP CONSTRAINT IF EXISTS tratamiento_duracion_chk;
ALTER TABLE tratamiento ADD CONSTRAINT tratamiento_duracion_chk CHECK (
    duracion_minutos BETWEEN 5 AND 480
);

COMMENT ON COLUMN paciente.tutor_nombre IS 'M4 · tutor responsable cuando el paciente es menor';
COMMENT ON COLUMN paciente.pagador_nombre IS 'M4 · responsable de pago distinto del paciente';
COMMENT ON COLUMN tratamiento.duracion_minutos IS 'M5 · duración referencial para agendar la cita';
