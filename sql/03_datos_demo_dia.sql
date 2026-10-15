-- ------------------------------------------------------------
-- 03_datos_demo_dia.sql
-- Datos de demostración para el DÍA ACTUAL (re-ejecutable).
-- Agenda, Sala de Espera y el Dashboard muestran LocalDate.now(),
-- así que la demo en vivo necesita citas de hoy. Todas las filas
-- llevan el marcador [DEMO] en el motivo para poder limpiarse.
-- Uso: psql -U postgres -d dentrix -f 03_datos_demo_dia.sql
-- ------------------------------------------------------------
BEGIN;
SET search_path TO dentrix;

DELETE FROM cita WHERE fecha_cita = CURRENT_DATE AND motivo LIKE '[DEMO]%';

INSERT INTO cita (tipo, paciente_id, usuario_id, consultorio_id, fecha_cita, hora_inicio, hora_fin, estado, motivo) VALUES
    ('ALMUERZO',  NULL, (SELECT id FROM usuario WHERE username='jvallejos'), (SELECT id FROM consultorio WHERE codigo='C01'), CURRENT_DATE, '12:00', '13:00', 'BLOQUEADA', '[DEMO] Almuerzo del personal'),
    ('CIERRE',    NULL, (SELECT id FROM usuario WHERE username='jperez'),    (SELECT id FROM consultorio WHERE codigo='C02'), CURRENT_DATE, '18:00', '20:00', 'BLOQUEADA', '[DEMO] Cierre de jornada');

INSERT INTO cita (tipo, paciente_id, usuario_id, consultorio_id, fecha_cita, hora_inicio, hora_fin, tratamiento_id, estado, motivo) VALUES
    ('CITA', (SELECT id FROM paciente WHERE dni='47895612'), (SELECT id FROM usuario WHERE username='jvallejos'), (SELECT id FROM consultorio WHERE codigo='C01'), CURRENT_DATE, '09:00', '09:45', (SELECT id FROM tratamiento WHERE codigo='TRAT-03'), 'EN_ATENCION', '[DEMO] Control mensual de ortodoncia'),
    ('CITA', (SELECT id FROM paciente WHERE dni='73658291'), (SELECT id FROM usuario WHERE username='jperez'),    (SELECT id FROM consultorio WHERE codigo='C02'), CURRENT_DATE, '09:00', '09:45', (SELECT id FROM tratamiento WHERE codigo='TRAT-02'), 'EN_SALA',     '[DEMO] Profilaxis y destartraje'),
    ('CITA', (SELECT id FROM paciente WHERE dni='70124589'), (SELECT id FROM usuario WHERE username='jperez'),    (SELECT id FROM consultorio WHERE codigo='C02'), CURRENT_DATE, '10:00', '10:45', (SELECT id FROM tratamiento WHERE codigo='TRAT-01'), 'EN_SALA',     '[DEMO] Curación resina oclusal'),
    ('CITA', (SELECT id FROM paciente WHERE dni='19854236'), (SELECT id FROM usuario WHERE username='jvallejos'), (SELECT id FROM consultorio WHERE codigo='C01'), CURRENT_DATE, '11:15', '12:00', (SELECT id FROM tratamiento WHERE codigo='TRAT-04'), 'FALTO',       '[DEMO] Exodoncia simple'),
    ('CITA', (SELECT id FROM paciente WHERE dni='45896321'), (SELECT id FROM usuario WHERE username='jperez'),    (SELECT id FROM consultorio WHERE codigo='C02'), CURRENT_DATE, '15:30', '16:15', NULL, 'PENDIENTE',  '[DEMO] Evaluación inicial');

COMMIT;

SELECT fecha_cita, tipo, estado, motivo
  FROM cita
 WHERE fecha_cita = CURRENT_DATE
 ORDER BY hora_inicio, tipo;
