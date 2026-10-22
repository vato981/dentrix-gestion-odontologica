-- ============================================================
-- DENTRIX DENTAL · Esquema v3 · SESIÓN EN BORRADOR
-- Archivo ADITIVO: se ejecuta DESPUÉS de 12_schema_v3_ejecucion.sql
--   psql -U postgres -d dentrix -f sql/13_schema_v3_sesion_borrador.sql
--
-- Una sesión abierta y no firmada es un borrador: se puede descartar. Una vez
-- firmada (la evolución la firma) es evidencia y solo se anula con motivo.
-- Sin esto, abrir una sesión por error dejaba un registro imposible de quitar.
-- ============================================================
SET search_path TO dentrix;

CREATE OR REPLACE FUNCTION bloquea_delete_ejecucion() RETURNS TRIGGER AS $$
BEGIN
    IF TG_TABLE_NAME = 'ejecucion_item' AND OLD.estado = 'BORRADOR' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'R22: % no admite DELETE; se anula con motivo', TG_TABLE_NAME;
END $$ LANGUAGE plpgsql SET search_path = dentrix;
