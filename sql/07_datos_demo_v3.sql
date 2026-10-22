-- ============================================================
-- DENTRIX DENTAL · Datos de demostración del presupuesto (P3 / M10)
-- Se ejecuta DESPUÉS de 06_schema_v3_presupuesto_ajustes.sql
--   psql -U postgres -d dentrix -f sql/07_datos_demo_v3.sql
--
-- Deja UN presupuesto aceptado y firmado para el paciente 2 (Gómez Mendoza),
-- con un ítem que exige consentimiento ya firmado. Sirve para mostrar M10 y
-- el cobro por ítem con precio congelado. Es idempotente: si el paciente ya
-- tiene un presupuesto PRESENTADO o ACEPTADO, no hace nada.
-- ============================================================
SET search_path TO dentrix;

DO $$
DECLARE
    v_pac     INT := 2;
    v_ficha   BIGINT;
    v_pres    BIGINT;
    v_endo    BIGINT;
    v_cura    BIGINT;
    v_control BIGINT;
    v_hash    CHAR(64);
BEGIN
    IF EXISTS (SELECT 1 FROM presupuesto
                WHERE paciente_id = v_pac AND estado IN ('PRESENTADO','ACEPTADO')) THEN
        RAISE NOTICE 'El paciente % ya tiene un presupuesto de demo; no se crea otro', v_pac;
        RETURN;
    END IF;

    SELECT MAX(id) INTO v_ficha FROM ficha_clinica WHERE paciente_id = v_pac;
    IF v_ficha IS NULL THEN
        RAISE NOTICE 'El paciente % no tiene ficha clinica; no se crea el demo', v_pac;
        RETURN;
    END IF;

    INSERT INTO presupuesto (paciente_id, ficha_id, creado_por)
    VALUES (v_pac, v_ficha, 2)
    RETURNING id INTO v_pres;

    INSERT INTO presupuesto_item
        (presupuesto_id, tratamiento_id, pieza_fdi, fase, prioridad, secuencia, cantidad,
         sesiones_planificadas, precio_unitario, requiere_consentimiento)
    VALUES (v_pres, 5, 36, 'URGENTE', 1, 1, 1, 2, 120.00, TRUE)
    RETURNING id INTO v_endo;

    INSERT INTO presupuesto_item
        (presupuesto_id, tratamiento_id, pieza_fdi, fase, prioridad, secuencia, cantidad,
         sesiones_planificadas, precio_unitario)
    VALUES (v_pres, 1, 16, 'BASICA', 2, 2, 1, 1, 80.00)
    RETURNING id INTO v_cura;

    INSERT INTO presupuesto_item
        (presupuesto_id, tratamiento_id, fase, prioridad, secuencia, cantidad,
         sesiones_planificadas, precio_unitario)
    VALUES (v_pres, 3, 'MANTENIMIENTO', 5, 3, 1, 1, 100.00)
    RETURNING id INTO v_control;

    UPDATE presupuesto
       SET descuento = 20.00,
           descuento_motivo = 'Cortesia de bienvenida',
           descuento_autorizado_por = 1
     WHERE id = v_pres;

    UPDATE presupuesto SET estado = 'PRESENTADO' WHERE id = v_pres;

    v_hash := md5('demo-' || v_pres::text) || md5('firma-' || v_pres::text);

    INSERT INTO aceptacion
        (presupuesto_id, tipo, firmante_nombre, firmante_documento, firmante_rol,
         firma_hash, alternativas_ofrecidas, riesgos_informados, registrado_por)
    VALUES (v_pres, 'TOTAL', 'María Gómez Mendoza', '70124589', 'PACIENTE',
            v_hash, 'Exodoncia como alternativa a la endodoncia',
            'Fractura radicular, dolor postoperatorio, necesidad de segunda sesion', 2);

    UPDATE presupuesto SET estado = 'ACEPTADO' WHERE id = v_pres;

    INSERT INTO consentimiento
        (paciente_id, presupuesto_item_id, tratamiento_id, procedimiento, riesgos, alternativas,
         estado, firmante_nombre, firmante_documento, profesional_id, fecha_firma, firma_hash)
    VALUES (v_pac, v_endo, 5, 'Endodoncia unirradicular pieza 36',
            'Fractura radicular, dolor postoperatorio, necesidad de segunda sesion',
            'Exodoncia de la pieza', 'FIRMADO', 'María Gómez Mendoza', '70124589', 2,
            CURRENT_TIMESTAMP, v_hash);

    RAISE NOTICE 'Presupuesto demo % creado para el paciente %', v_pres, v_pac;
END $$;

SELECT p.id, p.paciente_id, p.version, p.estado, p.subtotal, p.descuento, p.total,
       (SELECT COUNT(*) FROM presupuesto_item i WHERE i.presupuesto_id = p.id) AS items
  FROM presupuesto p
 WHERE p.paciente_id = 2
 ORDER BY p.id DESC;
