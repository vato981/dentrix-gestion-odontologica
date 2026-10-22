-- ============================================================
-- DENTRIX DENTAL · Esquema v3 · ENMIENDA: REPARTO DEL DESCUENTO
-- Archivo ADITIVO: se ejecuta DESPUÉS de 10_schema_v3_enmienda_borrador.sql
--   psql -U postgres -d dentrix -f sql/11_schema_v3_enmienda_neto.sql
--
-- Al firmar una enmienda cambian los ítems vigentes (se anulan y se agregan),
-- así que el descuento de la cabecera debe repartirse otra vez. Sin esto los
-- montos netos conservaban el reparto del momento de la aceptación y su suma
-- no coincidía con el total (256.67 en vez de 250.00 en la verificación).
-- ============================================================
SET search_path TO dentrix;

CREATE OR REPLACE FUNCTION firmar_enmienda(p_enmienda BIGINT, p_usuario INT)
RETURNS VOID AS $$
DECLARE
    v_presupuesto BIGINT;
    v_estado      estado_enmienda;
    v_est_pres    estado_presupuesto;
    v_descuento   NUMERIC(10,2);
    v_motivo      VARCHAR(200);
    v_agrega      INT;
    v_anula       INT;
    v_item        RECORD;
    v_suma        NUMERIC(10,2);
BEGIN
    SELECT presupuesto_id, estado, descuento_adicional, motivo
      INTO v_presupuesto, v_estado, v_descuento, v_motivo
      FROM enmienda WHERE id = p_enmienda FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La enmienda % no existe', p_enmienda;
    END IF;
    IF v_estado <> 'BORRADOR' THEN
        RAISE EXCEPTION 'La enmienda % está en % y no se puede firmar', p_enmienda, v_estado;
    END IF;

    SELECT estado INTO v_est_pres FROM presupuesto WHERE id = v_presupuesto FOR UPDATE;
    IF v_est_pres <> 'ACEPTADO' THEN
        RAISE EXCEPTION 'Solo se enmienda un presupuesto ACEPTADO; el % está en %',
            v_presupuesto, v_est_pres;
    END IF;

    SELECT COUNT(*) FILTER (WHERE accion = 'AGREGAR'),
           COUNT(*) FILTER (WHERE accion = 'ANULAR')
      INTO v_agrega, v_anula
      FROM enmienda_item WHERE enmienda_id = p_enmienda;
    IF v_agrega + v_anula = 0 THEN
        RAISE EXCEPTION 'La enmienda % no tiene ítems: no hay nada que firmar', p_enmienda;
    END IF;

    -- Validación de lo que se anula: no pagado, no ejecutado, estado permitido.
    FOR v_item IN
        SELECT i.id, i.estado, COALESCE(ei.motivo, v_motivo) AS motivo,
               (SELECT COUNT(*) FROM imputacion m WHERE m.presupuesto_item_id = i.id) AS pagos
          FROM enmienda_item ei
          JOIN presupuesto_item i ON i.id = ei.presupuesto_item_id
         WHERE ei.enmienda_id = p_enmienda AND ei.accion = 'ANULAR'
    LOOP
        IF v_item.estado IN ('EN_EJECUCION','EJECUTADO','CONTROLADO') THEN
            RAISE EXCEPTION 'No se anula el ítem %: ya está %', v_item.id, v_item.estado;
        END IF;
        IF v_item.estado NOT IN ('PROPUESTO','ACEPTADO','DIFERIDO') THEN
            RAISE EXCEPTION 'No se anula el ítem %: está %', v_item.id, v_item.estado;
        END IF;
        IF v_item.pagos > 0 THEN
            RAISE EXCEPTION 'No se anula el ítem %: tiene % imputación(es); resuélvalo en caja (fuera de alcance v3)',
                v_item.id, v_item.pagos;
        END IF;
    END LOOP;

    -- Validación de lo que se agrega: debe haber nacido con esta enmienda.
    FOR v_item IN
        SELECT i.id, i.estado, i.enmienda_id
          FROM enmienda_item ei
          JOIN presupuesto_item i ON i.id = ei.presupuesto_item_id
         WHERE ei.enmienda_id = p_enmienda AND ei.accion = 'AGREGAR'
    LOOP
        IF v_item.enmienda_id IS DISTINCT FROM p_enmienda THEN
            RAISE EXCEPTION 'El ítem % no fue creado por la enmienda %', v_item.id, p_enmienda;
        END IF;
        IF v_item.estado <> 'PROPUESTO' THEN
            RAISE EXCEPTION 'El ítem agregado % debe estar PROPUESTO; está %', v_item.id, v_item.estado;
        END IF;
    END LOOP;

    -- 1) La enmienda queda firmada (el encabezado la leerá así).
    UPDATE enmienda
       SET estado = 'FIRMADA',
           tipo = CASE WHEN v_agrega > 0 AND v_anula > 0 THEN 'MIXTA'::tipo_enmienda
                       WHEN v_agrega > 0 THEN 'AMPLIACION'::tipo_enmienda
                       ELSE 'REDUCCION'::tipo_enmienda END,
           fecha_firma = COALESCE(fecha_firma, CURRENT_TIMESTAMP),
           firmado_por = p_usuario
     WHERE id = p_enmienda;

    -- 2) Ítems anulados (el trigger exige el contexto de enmienda).
    PERFORM set_config('dentrix.enmienda', p_enmienda::text, true);
    FOR v_item IN
        SELECT i.id, COALESCE(ei.motivo, v_motivo) AS motivo
          FROM enmienda_item ei
          JOIN presupuesto_item i ON i.id = ei.presupuesto_item_id
         WHERE ei.enmienda_id = p_enmienda AND ei.accion = 'ANULAR'
    LOOP
        UPDATE presupuesto_item
           SET estado = 'ANULADO', motivo_anulacion = v_item.motivo
         WHERE id = v_item.id;
    END LOOP;

    -- 3) Ítems agregados: precio congelado desde la firma (R21).
    UPDATE presupuesto_item i
       SET estado = 'ACEPTADO', precio_congelado = TRUE
      FROM enmienda_item ei
     WHERE ei.enmienda_id = p_enmienda AND ei.accion = 'AGREGAR'
       AND ei.presupuesto_item_id = i.id;

    -- 4) Encabezado = plan vigente (solo permitido dentro de esta enmienda).
    SELECT COALESCE(SUM(subtotal), 0) INTO v_suma
      FROM presupuesto_item
     WHERE presupuesto_id = v_presupuesto AND estado <> 'ANULADO';
    UPDATE presupuesto
       SET subtotal = v_suma,
           descuento = descuento + v_descuento
     WHERE id = v_presupuesto;

    -- 5) El descuento se reparte otra vez entre los ítems vigentes.
    PERFORM repartir_descuento(v_presupuesto);
    PERFORM set_config('dentrix.enmienda', '', true);
END $$ LANGUAGE plpgsql SET search_path = dentrix;

COMMENT ON FUNCTION firmar_enmienda(BIGINT, INT) IS
    'P3 · aplica la enmienda, recalcula el encabezado y reparte el descuento en los ítems vigentes';
