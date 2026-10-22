-- ============================================================
-- DENTRIX DENTAL · Esquema v3 · BORRADOR DE ENMIENDA
-- Archivo ADITIVO: se ejecuta DESPUÉS de 09_schema_v3_cobro.sql
--   psql -U postgres -d dentrix -f sql/10_schema_v3_enmienda_borrador.sql
--
-- QUÉ RESUELVE
--   La pantalla de enmienda abre un borrador, deja agregar ítems y anular otros, y
--   recién firma. Si el usuario cierra sin firmar, ese borrador debe desaparecer:
--   un ítem PROPUESTO colgado en un plan ACEPTADO es basura visible.
--   Ahora un BORRADOR de enmienda se puede descartar (con sus ítems y detalle);
--   una enmienda FIRMADA/RECHAZADA/ANULADA sigue sin admitir DELETE.
--
-- QUÉ NO CAMBIA
--   - Los ítems de un presupuesto ACEPTADO solo se borran si nacieron de un borrador
--     de enmienda. Los demás se anulan con una enmienda firmada.
-- ============================================================
SET search_path TO dentrix;

-- Enmienda: el borrador se descarta; lo firmado no.
CREATE OR REPLACE FUNCTION bloquea_delete_enmienda() RETURNS TRIGGER AS $$
DECLARE v_estado estado_enmienda;
BEGIN
    IF TG_TABLE_NAME = 'enmienda' THEN
        v_estado := OLD.estado;
    ELSE
        SELECT estado INTO v_estado FROM enmienda WHERE id = OLD.enmienda_id;
    END IF;
    IF v_estado = 'BORRADOR' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'R22: % está % y no admite DELETE; use el estado de anulación',
        TG_TABLE_NAME, v_estado;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

-- Ítem: se borra solo si lo creó un borrador de enmienda.
CREATE OR REPLACE FUNCTION bloquea_delete_item() RETURNS TRIGGER AS $$
DECLARE
    v_estado_pres estado_presupuesto;
    v_estado_enm  estado_enmienda;
BEGIN
    SELECT estado INTO v_estado_pres FROM presupuesto WHERE id = OLD.presupuesto_id;
    IF NOT FOUND OR v_estado_pres IN ('BORRADOR','PRESENTADO') THEN
        RETURN OLD;
    END IF;
    IF OLD.enmienda_id IS NOT NULL THEN
        SELECT estado INTO v_estado_enm FROM enmienda WHERE id = OLD.enmienda_id;
        IF v_estado_enm = 'BORRADOR' THEN
            RETURN OLD;
        END IF;
    END IF;
    RAISE EXCEPTION 'R22: el ítem % pertenece a un presupuesto % y no se borra; anúlelo con una enmienda',
        OLD.id, v_estado_pres;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

-- Descarta un borrador completo: ítems agregados, detalle y cabecera.
CREATE OR REPLACE FUNCTION descartar_enmienda(p_id BIGINT) RETURNS VOID AS $$
DECLARE v_estado estado_enmienda;
BEGIN
    SELECT estado INTO v_estado FROM enmienda WHERE id = p_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La enmienda % no existe', p_id;
    END IF;
    IF v_estado <> 'BORRADOR' THEN
        RAISE EXCEPTION 'La enmienda % está % y no se descarta', p_id, v_estado;
    END IF;

    DELETE FROM enmienda_item WHERE enmienda_id = p_id;
    DELETE FROM presupuesto_item WHERE enmienda_id = p_id;
    DELETE FROM enmienda WHERE id = p_id;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

COMMENT ON FUNCTION descartar_enmienda(BIGINT) IS
    'P3 · borra un borrador de enmienda con sus ítems agregados; lo firmado no se toca';
