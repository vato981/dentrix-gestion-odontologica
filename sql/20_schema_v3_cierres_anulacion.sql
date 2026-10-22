-- 20_schema_v3_cierres_anulacion.sql
-- Cierra dos brechas declaradas de la interfaz sin tocar invariantes:
--   * M4: el motivo de anulación del paciente se persiste (R22: nada se borra, se anula con motivo).
--   * M7.1: la tolerancia y el motivo de la diferencia del arqueo se persisten (P6).
-- Aditivo e idempotente.

ALTER TABLE paciente ADD COLUMN IF NOT EXISTS motivo_anulacion VARCHAR(200);
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS fecha_anulacion TIMESTAMP;

COMMENT ON COLUMN paciente.motivo_anulacion IS 'R22: motivo obligatorio al anular (estado INACTIVO)';
COMMENT ON COLUMN paciente.fecha_anulacion  IS 'R22: fecha en que el paciente paso a INACTIVO';

ALTER TABLE cierre_caja ADD COLUMN IF NOT EXISTS tolerancia NUMERIC(10,2) NOT NULL DEFAULT 1.00
    CHECK (tolerancia >= 0);
ALTER TABLE cierre_caja ADD COLUMN IF NOT EXISTS motivo_diferencia VARCHAR(200);

COMMENT ON COLUMN cierre_caja.tolerancia        IS 'P6: tolerancia aceptada para la diferencia del arqueo';
COMMENT ON COLUMN cierre_caja.motivo_diferencia IS 'P6: motivo obligatorio cuando la diferencia supera la tolerancia';
