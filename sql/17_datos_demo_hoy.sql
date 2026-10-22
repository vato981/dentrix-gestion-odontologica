-- ============================================================
-- DENTRIX DENTAL · Datos de demostración para HOY (agenda y M3)
--   psql -U postgres -d dentrix -f sql/17_datos_demo_hoy.sql
--
-- La semilla del v2 trae el día operativo del 28/09/2026; para revisar la agenda
-- y la sala de espera "hoy" hace falta un día con citas. Es idempotente: si ya
-- hay citas para la fecha actual, no inserta nada.
-- ============================================================
SET search_path TO dentrix;

DO $$
DECLARE
    v_hoy   DATE := CURRENT_DATE;
    v_count INT;
BEGIN
    SELECT COUNT(*) INTO v_count FROM cita WHERE fecha_cita = v_hoy;
    IF v_count > 0 THEN
        RAISE NOTICE 'Ya hay % citas para hoy; no se inserta el demo', v_count;
        RETURN;
    END IF;

    INSERT INTO cita (tipo, paciente_id, usuario_id, consultorio_id, fecha_cita, hora_inicio, hora_fin,
                      tratamiento_id, estado, motivo)
    VALUES
        ('CITA', 1, 2, 1, v_hoy, TIME '09:00', TIME '09:45', 1, 'ATENDIDA', 'Curacion oclusal'),
        ('CITA', 2, 2, 1, v_hoy, TIME '10:00', TIME '10:45', 3, 'EN_SALA', 'Control de ortodoncia'),
        ('CITA', 3, 2, 2, v_hoy, TIME '11:00', TIME '11:30', 4, 'CONFIRMADA', 'Exodoncia simple'),
        ('CITA', 4, 2, 2, v_hoy, TIME '12:00', TIME '12:30', 2, 'PENDIENTE', 'Profilaxis'),
        ('CITA', 5, 2, 1, v_hoy, TIME '15:00', TIME '16:00', 5, 'PENDIENTE', 'Endodoncia');

    RAISE NOTICE 'Demo de hoy creado con 5 citas';
END $$;

SELECT c.id, c.hora_inicio, c.estado, p.apellidos, p.nombres, t.nombre AS tratamiento
  FROM cita c
  LEFT JOIN paciente p ON p.id = c.paciente_id
  LEFT JOIN tratamiento t ON t.id = c.tratamiento_id
 WHERE c.fecha_cita = CURRENT_DATE
 ORDER BY c.hora_inicio;
