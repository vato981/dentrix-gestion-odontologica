\set ON_ERROR_STOP off
\echo '=== SMOKE PASO 4 · AGENDA: SOLAPE, REPROGRAMACION, ESPERA Y RECORDATORIOS ==='
BEGIN;

\echo '--- A) dos citas del mismo consultorio que se solapan deben fallar ---'
INSERT INTO dentrix.cita (paciente_id, usuario_id, consultorio_id, fecha_cita, hora_inicio, hora_fin, estado)
VALUES (1, 2, 1, DATE '2026-12-01', TIME '09:00', TIME '10:00', 'PENDIENTE');
SAVEPOINT sa;
INSERT INTO dentrix.cita (paciente_id, usuario_id, consultorio_id, fecha_cita, hora_inicio, hora_fin, estado)
VALUES (2, 2, 1, DATE '2026-12-01', TIME '09:30', TIME '10:30', 'PENDIENTE');
ROLLBACK TO SAVEPOINT sa;

\echo '--- B) el mismo odontologo en otro consultorio a la misma hora debe fallar ---'
SAVEPOINT sb;
INSERT INTO dentrix.cita (paciente_id, usuario_id, consultorio_id, fecha_cita, hora_inicio, hora_fin, estado)
VALUES (2, 2, 2, DATE '2026-12-01', TIME '09:15', TIME '09:45', 'PENDIENTE');
ROLLBACK TO SAVEPOINT sb;

\echo '--- C) otra hora en el mismo consultorio sí se puede ---'
INSERT INTO dentrix.cita (paciente_id, usuario_id, consultorio_id, fecha_cita, hora_inicio, hora_fin, estado)
VALUES (2, 2, 1, DATE '2026-12-01', TIME '10:00', TIME '10:45', 'PENDIENTE');

\echo '--- D) una cita CANCELADA no bloquea el horario ---'
INSERT INTO dentrix.cita (paciente_id, usuario_id, consultorio_id, fecha_cita, hora_inicio, hora_fin, estado)
VALUES (3, 2, 1, DATE '2026-12-01', TIME '11:00', TIME '11:30', 'CANCELADA');
INSERT INTO dentrix.cita (paciente_id, usuario_id, consultorio_id, fecha_cita, hora_inicio, hora_fin, estado)
VALUES (3, 2, 1, DATE '2026-12-01', TIME '11:00', TIME '11:30', 'PENDIENTE');

\echo '--- E) reprogramar conserva la original ---'
SELECT id AS cita FROM dentrix.cita WHERE fecha_cita = DATE '2026-12-01' AND hora_inicio = TIME '09:00' \gset
SELECT dentrix.reprogramar_cita(:cita, DATE '2026-12-02', TIME '15:00', NULL, 3, 'El paciente no pudo') AS nueva \gset
SELECT 'E · original' AS caso, id, estado FROM dentrix.cita WHERE id = :cita;
SELECT 'E · nueva' AS caso, id, estado, cita_origen_id, hora_inicio, hora_fin FROM dentrix.cita WHERE id = :nueva;
SELECT 'E · historial' AS caso, estado_anterior, estado_nuevo, motivo FROM dentrix.cita_historial WHERE cita_id = :cita ORDER BY id DESC LIMIT 1;

\echo '--- F) borrar el historial debe fallar ---'
SAVEPOINT sf;
DELETE FROM dentrix.cita_historial WHERE cita_id = :cita;
ROLLBACK TO SAVEPOINT sf;

\echo '--- G) lista de espera: un paciente una sola vez ---'
INSERT INTO dentrix.lista_espera (paciente_id, tratamiento_id, prioridad, usuario_id)
VALUES (1, 5, 1, 3);
SAVEPOINT sg;
INSERT INTO dentrix.lista_espera (paciente_id, tratamiento_id, prioridad, usuario_id)
VALUES (1, 4, 2, 3);
ROLLBACK TO SAVEPOINT sg;

\echo '--- H) recordatorio pendiente y luego enviado ---'
INSERT INTO dentrix.recordatorio (cita_id, canal, programado)
VALUES (:nueva, 'WHATSAPP', CURRENT_TIMESTAMP + INTERVAL '1 day')
RETURNING id AS rec \gset
UPDATE dentrix.recordatorio SET estado = 'ENVIADO', enviado = CURRENT_TIMESTAMP WHERE id = :rec;
SELECT 'H · recordatorio' AS caso, canal, estado FROM dentrix.recordatorio WHERE id = :rec;

\echo '--- I) consentimiento de datos: rechazo exige motivo ---'
SAVEPOINT si;
INSERT INTO dentrix.consentimiento_datos (paciente_id, version, aceptado, medio)
VALUES (1, '2026-01', FALSE, 'PAPEL');
ROLLBACK TO SAVEPOINT si;
INSERT INTO dentrix.consentimiento_datos (paciente_id, version, aceptado, medio)
VALUES (1, '2026-01', TRUE, 'PAPEL');

\echo '--- J) inasistencias recientes del paciente ---'
SELECT 'J · inasistencias' AS caso, dentrix.inasistencias_recientes(1) AS total;

ROLLBACK;
\echo '=== ROLLBACK: la base queda igual ==='
