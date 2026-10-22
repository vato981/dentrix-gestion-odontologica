\set ON_ERROR_STOP off
\echo '=== SMOKE P4-B · LABORATORIO, CONTROL Y LOTE ==='
BEGIN;

\echo '--- Preparar sesión abierta de la endodoncia (item 58) ---'
INSERT INTO dentrix.ejecucion_item (presupuesto_item_id, numero_sesion, profesional_id, procedimiento, pieza_fdi)
VALUES (58, 1, 2, 'Preparacion para corona', 36)
RETURNING id AS ses \gset

\echo '--- A) consumir un lote en la sesión abierta ---'
INSERT INTO dentrix.consumo_lote (ejecucion_item_id, lote_insumo_id, cantidad, usuario_id)
SELECT :ses, l.id, 2, 3 FROM dentrix.lote_insumo l
  JOIN dentrix.insumo i ON i.id = l.insumo_id WHERE i.codigo = 'INS-02';
SELECT 'A · consumo' AS caso, cantidad, lote, paciente_id, pieza_fdi
  FROM dentrix.v_consumo_trazable WHERE presupuesto_item_id = 58;

\echo '--- B) cantidad 0 debe fallar ---'
SAVEPOINT sb;
INSERT INTO dentrix.consumo_lote (ejecucion_item_id, lote_insumo_id, cantidad, usuario_id)
SELECT :ses, l.id, 0, 3 FROM dentrix.lote_insumo l JOIN dentrix.insumo i ON i.id=l.insumo_id WHERE i.codigo='INS-02';
ROLLBACK TO SAVEPOINT sb;

\echo '--- C) consumir dos veces el mismo lote debe fallar ---'
SAVEPOINT sc;
INSERT INTO dentrix.consumo_lote (ejecucion_item_id, lote_insumo_id, cantidad, usuario_id)
SELECT :ses, l.id, 1, 3 FROM dentrix.lote_insumo l JOIN dentrix.insumo i ON i.id=l.insumo_id WHERE i.codigo='INS-02';
ROLLBACK TO SAVEPOINT sc;

\echo '--- D) abrir orden de laboratorio para el item 58 ---'
INSERT INTO dentrix.orden_laboratorio (presupuesto_item_id, laboratorio, trabajo, material, color, fecha_prometida)
VALUES (58, 'Laboratorio Dental Sur', 'Corona metal-ceramica', 'Zirconio', 'A2', CURRENT_DATE + 5)
RETURNING id AS orden \gset
SELECT 'D · orden' AS caso, id, estado, trabajo FROM dentrix.orden_laboratorio WHERE id = :orden;

\echo '--- E) segunda orden activa para el mismo ítem debe fallar ---'
SAVEPOINT se;
INSERT INTO dentrix.orden_laboratorio (presupuesto_item_id, laboratorio, trabajo)
VALUES (58, 'Otro laboratorio', 'Otra corona');
ROLLBACK TO SAVEPOINT se;

\echo '--- F) recibir y controlar conforme: la orden queda CONFORME ---'
UPDATE dentrix.orden_laboratorio SET estado = 'RECIBIDA' WHERE id = :orden;
INSERT INTO dentrix.control_laboratorio (orden_id, ajuste, color_ok, contactos_ok, conforme, usuario_id)
VALUES (:orden, 'Sin ajustes', TRUE, TRUE, TRUE, 3);
SELECT 'F · orden' AS caso, id, estado FROM dentrix.orden_laboratorio WHERE id = :orden;

\echo '--- G) instalar la corona ---'
UPDATE dentrix.orden_laboratorio SET estado = 'INSTALADA' WHERE id = :orden;
SELECT 'G · orden' AS caso, id, estado FROM dentrix.orden_laboratorio WHERE id = :orden;

\echo '--- H) marcar CONFORME sin control debe fallar (orden nueva en el item 59) ---'
INSERT INTO dentrix.orden_laboratorio (presupuesto_item_id, laboratorio, trabajo)
VALUES (59, 'Laboratorio Norte', 'Inlay') RETURNING id AS orden2 \gset
UPDATE dentrix.orden_laboratorio SET estado = 'RECIBIDA' WHERE id = :orden2;
SAVEPOINT sh;
UPDATE dentrix.orden_laboratorio SET estado = 'CONFORME' WHERE id = :orden2;
ROLLBACK TO SAVEPOINT sh;

\echo '--- I) DELETE de la orden debe fallar ---'
SAVEPOINT si;
DELETE FROM dentrix.orden_laboratorio WHERE id = :orden;
ROLLBACK TO SAVEPOINT si;

\echo '--- J) DELETE del consumo en sesión abierta sí se puede ---'
DELETE FROM dentrix.consumo_lote WHERE ejecucion_item_id = :ses;
SELECT 'J · consumos' AS caso, count(*) FROM dentrix.consumo_lote WHERE ejecucion_item_id = :ses;

\echo '--- K) un ítem con laboratorio no pasa a EJECUTADO sin orden conforme (R18) ---'
SAVEPOINT sk;
UPDATE dentrix.tratamiento SET requiere_laboratorio = TRUE WHERE id = 1;
UPDATE dentrix.presupuesto_item SET estado = 'EJECUTADO' WHERE id = 59;
ROLLBACK TO SAVEPOINT sk;

ROLLBACK;
\echo '=== ROLLBACK: la base queda igual ==='
