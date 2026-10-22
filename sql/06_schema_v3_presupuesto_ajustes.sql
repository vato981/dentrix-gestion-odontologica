-- ============================================================
-- DENTRIX DENTAL · Esquema v3 · AJUSTES DEL PRESUPUESTO EN BORRADOR
-- Archivo ADITIVO: se ejecuta DESPUÉS de 05_schema_v3_tarifario.sql
--   psql -U postgres -d dentrix -f sql/06_schema_v3_presupuesto_ajustes.sql
--
-- QUÉ RESUELVE
--   1. Un presupuesto RECHAZADO no tenía dónde guardar el motivo (P3 §13.1).
--   2. Mientras el presupuesto es BORRADOR o PRESENTADO, descartarlo es retirar una
--      oferta, no destruir evidencia: se permite DELETE. Desde ACEPTADO en adelante
--      (ACEPTADO, RECHAZADO, VENCIDO, REEMPLAZADO) no se borra nada.
--   3. Quitar un ítem solo se permite en esas dos etapas; después se anula con enmienda.
--
-- QUÉ NO CAMBIA
--   - Un presupuesto PRESENTADO/ACEPTADO/RECHAZADO/VENCIDO/REEMPLAZADO no se borra.
--   - Los ítems de un presupuesto no borrador no se borran: se anulan con enmienda.
-- ============================================================
SET search_path TO dentrix;

-- ------------------------------------------------------------
-- 1. Motivo del rechazo
-- ------------------------------------------------------------
ALTER TABLE presupuesto ADD COLUMN IF NOT EXISTS motivo_rechazo VARCHAR(200);

ALTER TABLE presupuesto DROP CONSTRAINT IF EXISTS presupuesto_rechazo_con_motivo;
ALTER TABLE presupuesto ADD CONSTRAINT presupuesto_rechazo_con_motivo CHECK (
    estado <> 'RECHAZADO' OR (motivo_rechazo IS NOT NULL AND btrim(motivo_rechazo) <> '')
);

-- ------------------------------------------------------------
-- 2. Borradores descartables
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION bloquea_delete_presupuesto() RETURNS TRIGGER AS $$
BEGIN
    IF OLD.estado IN ('BORRADOR','PRESENTADO') THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'R22: el presupuesto % está % y no se borra; retírelo antes de aceptarlo o abra otra versión',
        OLD.id, OLD.estado;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

DROP TRIGGER tr_presupuesto_no_delete ON presupuesto;
CREATE TRIGGER tr_presupuesto_no_delete BEFORE DELETE ON presupuesto
FOR EACH ROW EXECUTE FUNCTION bloquea_delete_presupuesto();

CREATE OR REPLACE FUNCTION bloquea_delete_item() RETURNS TRIGGER AS $$
DECLARE v_estado estado_presupuesto;
BEGIN
    SELECT estado INTO v_estado FROM presupuesto WHERE id = OLD.presupuesto_id;
    IF NOT FOUND OR v_estado IN ('BORRADOR','PRESENTADO') THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'R22: el ítem % pertenece a un presupuesto % y no se borra; anúlelo con una enmienda',
        OLD.id, v_estado;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

DROP TRIGGER tr_item_no_delete ON presupuesto_item;
CREATE TRIGGER tr_item_no_delete BEFORE DELETE ON presupuesto_item
FOR EACH ROW EXECUTE FUNCTION bloquea_delete_item();

COMMENT ON COLUMN presupuesto.motivo_rechazo IS 'P3 §13.1 · motivo cuando el paciente rechaza el presupuesto completo';
