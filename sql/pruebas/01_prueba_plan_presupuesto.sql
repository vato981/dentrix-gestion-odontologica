\set ON_ERROR_STOP off
\echo '=== SMOKE TEST v3 SOBRE dentrix (TODO SE REVIERTE CON ROLLBACK) ==='
BEGIN;

\echo '--- CASO 1: presupuesto borrador, 2 items y descuento autorizado ---'
INSERT INTO dentrix.presupuesto (paciente_id, ficha_id, version, creado_por)
VALUES (1, (SELECT min(id) FROM dentrix.ficha_clinica WHERE paciente_id = 1), 1, 1)
RETURNING id AS pid \gset

INSERT INTO dentrix.presupuesto_item (presupuesto_id, tratamiento_id, pieza_fdi, precio_unitario)
VALUES (:pid, 1, 16, 80.00);
INSERT INTO dentrix.presupuesto_item (presupuesto_id, tratamiento_id, pieza_fdi, precio_unitario, requiere_consentimiento)
VALUES (:pid, 5, 36, 250.00, TRUE);

UPDATE dentrix.presupuesto
   SET descuento = 30.00, descuento_motivo = 'Descuento de gerencia', descuento_autorizado_por = 1
 WHERE id = :pid;

SELECT 'CASO 1' AS caso, subtotal, descuento, total, estado FROM dentrix.presupuesto WHERE id = :pid;

\echo '--- CASO 2: PRESENTADO + editar precio del item (debe fallar R22) ---'
UPDATE dentrix.presupuesto SET estado = 'PRESENTADO' WHERE id = :pid;
SAVEPOINT s2;
UPDATE dentrix.presupuesto_item SET precio_unitario = 999.00
 WHERE presupuesto_id = :pid AND pieza_fdi = 16;
ROLLBACK TO SAVEPOINT s2;

\echo '--- CASO 3: aceptar (congela precio y pasa items a ACEPTADO) ---'
UPDATE dentrix.presupuesto SET estado = 'ACEPTADO' WHERE id = :pid;
SELECT 'CASO 3' AS caso, id, pieza_fdi, estado, precio_unitario, precio_congelado
  FROM dentrix.presupuesto_item WHERE presupuesto_id = :pid ORDER BY id;

\echo '--- CASO 4: ejecutar item de riesgo SIN consentimiento (debe fallar R7) ---'
SAVEPOINT s4;
UPDATE dentrix.presupuesto_item SET estado = 'EN_EJECUCION'
 WHERE presupuesto_id = :pid AND requiere_consentimiento;
ROLLBACK TO SAVEPOINT s4;

\echo '--- CASO 5: consentimiento firmado -> ahora si pasa a EN_EJECUCION ---'
INSERT INTO dentrix.consentimiento
       (paciente_id, presupuesto_item_id, procedimiento, riesgos, alternativas,
        estado, firmante_nombre, firmante_documento, profesional_id, fecha_firma, firma_hash)
SELECT 1, i.id, 'Endodoncia unirradicular pieza 36',
       'Fractura radicular, dolor postoperatorio', 'Exodoncia de la pieza',
       'FIRMADO', 'Juan Carlos Perez Alania', '45123698', 2, CURRENT_TIMESTAMP, repeat('a', 64)
  FROM dentrix.presupuesto_item i
 WHERE i.presupuesto_id = :pid AND i.requiere_consentimiento;

UPDATE dentrix.presupuesto_item SET estado = 'EN_EJECUCION'
 WHERE presupuesto_id = :pid AND requiere_consentimiento;
SELECT 'CASO 5' AS caso, id, estado FROM dentrix.presupuesto_item WHERE presupuesto_id = :pid ORDER BY id;

\echo '--- CASO 6: DELETE del presupuesto (debe fallar R22) ---'
SAVEPOINT s6;
DELETE FROM dentrix.presupuesto WHERE id = :pid;
ROLLBACK TO SAVEPOINT s6;

\echo '--- CASO 7: version 2 sobre un presupuesto ACEPTADO (no lo altera) ---'
INSERT INTO dentrix.presupuesto (paciente_id, ficha_id, presupuesto_anterior_id, version, creado_por)
SELECT paciente_id, ficha_id, id, version + 1, creado_por FROM dentrix.presupuesto WHERE id = :pid
RETURNING id AS pid2 \gset
SELECT 'CASO 7' AS caso, id, version, estado, presupuesto_anterior_id
  FROM dentrix.presupuesto WHERE id IN (:pid, :pid2) ORDER BY id;

\echo '--- CASO 8: imputar 150 al item de 250 -> saldo derivado 100 (tope de P5) ---'
INSERT INTO dentrix.imputacion (pago_id, presupuesto_item_id, monto, usuario_id)
SELECT (SELECT min(id) FROM dentrix.pago), i.id, 150.00, 3
  FROM dentrix.presupuesto_item i
 WHERE i.presupuesto_id = :pid AND i.requiere_consentimiento;
SELECT 'CASO 8' AS caso, item_id, subtotal, imputado, saldo
  FROM dentrix.v_saldo_item WHERE presupuesto_id = :pid ORDER BY item_id;

\echo '--- CASO 9 (compat): v_plan_tratamiento lee con la forma vieja ---'
SELECT 'CASO 9' AS caso, count(*) AS filas FROM dentrix.v_plan_tratamiento;

ROLLBACK;
\echo '=== ROLLBACK: la base queda igual que antes ==='
