\set ON_ERROR_STOP off
\echo '=== SMOKE v3 · PRESUPUESTO EN BORRADOR (todo con ROLLBACK) ==='
BEGIN;

\echo '--- A) en BORRADOR se puede quitar un ítem ---'
INSERT INTO dentrix.presupuesto (paciente_id, creado_por) VALUES (1, 1) RETURNING id AS pid \gset
INSERT INTO dentrix.presupuesto_item (presupuesto_id, tratamiento_id, precio_unitario)
VALUES (:pid, 1, 80.00) RETURNING id AS itema \gset
DELETE FROM dentrix.presupuesto_item WHERE id = :itema;
SELECT 'A · quitar en borrador' AS caso, count(*) AS items FROM dentrix.presupuesto_item WHERE presupuesto_id = :pid;

\echo '--- B) en BORRADOR se puede descartar el presupuesto ---'
DELETE FROM dentrix.presupuesto WHERE id = :pid;
SELECT 'B · borrador descartado' AS caso, count(*) AS filas FROM dentrix.presupuesto WHERE id = :pid;

\echo '--- B2) un PRESENTADO (oferta sin firmar) tambien se puede retirar ---'
INSERT INTO dentrix.presupuesto (paciente_id, creado_por) VALUES (1, 1) RETURNING id AS pidp \gset
INSERT INTO dentrix.presupuesto_item (presupuesto_id, tratamiento_id, precio_unitario)
VALUES (:pidp, 1, 80.00);
UPDATE dentrix.presupuesto SET estado = 'PRESENTADO' WHERE id = :pidp;
DELETE FROM dentrix.presupuesto WHERE id = :pidp;
SELECT 'B2 · presentado retirado' AS caso, count(*) AS filas FROM dentrix.presupuesto WHERE id = :pidp;

\echo '--- C) un presupuesto ACEPTADO no se borra (debe fallar) ---'
INSERT INTO dentrix.presupuesto (paciente_id, creado_por) VALUES (1, 1) RETURNING id AS pidb \gset
INSERT INTO dentrix.presupuesto_item (presupuesto_id, tratamiento_id, precio_unitario)
VALUES (:pidb, 1, 80.00);
UPDATE dentrix.presupuesto SET estado = 'ACEPTADO' WHERE id = :pidb;
SAVEPOINT sc;
DELETE FROM dentrix.presupuesto WHERE id = :pidb;
ROLLBACK TO SAVEPOINT sc;

\echo '--- D) rechazar exige motivo (debe fallar) ---'
SAVEPOINT sd;
UPDATE dentrix.presupuesto SET estado = 'RECHAZADO' WHERE id = :pidb;
ROLLBACK TO SAVEPOINT sd;

\echo '--- E) rechazar con motivo (debe pasar) ---'
UPDATE dentrix.presupuesto SET estado = 'RECHAZADO', motivo_rechazo = 'El paciente decidio no tratarse'
 WHERE id = :pidb;
SELECT 'E · rechazado' AS caso, estado, motivo_rechazo FROM dentrix.presupuesto WHERE id = :pidb;

ROLLBACK;
\echo '=== ROLLBACK: la base queda igual ==='
