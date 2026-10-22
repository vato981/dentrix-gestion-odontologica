\set ON_ERROR_STOP off
\echo '=== SMOKE PASO 1 · COMPROBANTE EN BORRADOR, EMISION Y ANULACION ==='
BEGIN;

\echo '--- A) borrador sin numero con dos items ---'
INSERT INTO dentrix.pago (tipo, estado, paciente_id, usuario_caja_id, modalidad, numero)
VALUES ('BOLETA', 'BORRADOR', 2, 3, 'EFECTIVO', NULL)
RETURNING id AS doc \gset
INSERT INTO dentrix.comprobante_item (pago_id, presupuesto_item_id, descripcion, cantidad, precio_unitario)
VALUES (:doc, 58, 'Endodoncia pieza 36', 1, 112.00);
INSERT INTO dentrix.comprobante_item (pago_id, descripcion, cantidad, precio_unitario)
VALUES (:doc, 'Profilaxis', 1, 70.00);
SELECT 'A · borrador' AS caso, numero, estado, monto, subtotal, igv, saldo_deudor
  FROM dentrix.pago WHERE id = :doc;

\echo '--- B) editar el detalle de un borrador recalcula ---'
UPDATE dentrix.comprobante_item SET precio_unitario = 80.00 WHERE pago_id = :doc AND presupuesto_item_id IS NULL;
SELECT 'B · recalculado' AS caso, monto, saldo_deudor FROM dentrix.pago WHERE id = :doc;

\echo '--- C) emitir con numero ---'
UPDATE dentrix.pago SET numero = 'T999-000001', estado = 'EMITIDO' WHERE id = :doc;
SELECT 'C · emitido' AS caso, numero, estado, monto FROM dentrix.pago WHERE id = :doc;

\echo '--- D) editar el detalle de un emitido debe fallar ---'
SAVEPOINT sd;
UPDATE dentrix.comprobante_item SET precio_unitario = 90.00 WHERE pago_id = :doc AND presupuesto_item_id IS NULL;
ROLLBACK TO SAVEPOINT sd;

\echo '--- E) cambiar el numero de un emitido debe fallar ---'
SAVEPOINT se;
UPDATE dentrix.pago SET numero = 'T999-000002' WHERE id = :doc;
ROLLBACK TO SAVEPOINT se;

\echo '--- F) anular sin motivo debe fallar ---'
SAVEPOINT sf;
UPDATE dentrix.pago SET estado = 'ANULADO' WHERE id = :doc;
ROLLBACK TO SAVEPOINT sf;

\echo '--- G) anular con un pago registrado debe fallar ---'
INSERT INTO dentrix.pago (pago_origen_id, tipo, estado, paciente_id, usuario_caja_id, modalidad, monto)
VALUES (:doc, 'BOLETA', 'EMITIDO', 2, 3, 'EFECTIVO', 50.00);
SAVEPOINT sg;
UPDATE dentrix.pago SET estado = 'ANULADO', motivo_anulacion = 'Error de digitacion' WHERE id = :doc;
ROLLBACK TO SAVEPOINT sg;

\echo '--- H) un segundo documento sin pagos sí se anula con motivo ---'
INSERT INTO dentrix.pago (tipo, estado, paciente_id, usuario_caja_id, modalidad)
VALUES ('BOLETA', 'BORRADOR', 2, 3, 'EFECTIVO') RETURNING id AS doc2 \gset
INSERT INTO dentrix.comprobante_item (pago_id, descripcion, cantidad, precio_unitario)
VALUES (:doc2, 'Profilaxis', 1, 70.00);
UPDATE dentrix.pago SET numero = 'T999-000002', estado = 'EMITIDO' WHERE id = :doc2;
UPDATE dentrix.pago SET estado = 'ANULADO', motivo_anulacion = 'Error de digitacion',
       anulada_por = 3, fecha_anulacion = CURRENT_TIMESTAMP
 WHERE id = :doc2;
SELECT 'H · anulado' AS caso, estado, motivo_anulacion FROM dentrix.pago WHERE id = :doc2;

\echo '--- I) borrar un emitido o anulado debe fallar ---'
SAVEPOINT si;
DELETE FROM dentrix.pago WHERE id = :doc2;
ROLLBACK TO SAVEPOINT si;

\echo '--- J) auditoria del documento ---'
SELECT 'J · auditoria' AS caso, count(*) AS filas FROM dentrix.auditoria WHERE tabla = 'pago' AND registro_id = :doc;
SAVEPOINT sj;
DELETE FROM dentrix.auditoria;
ROLLBACK TO SAVEPOINT sj;

\echo '--- K) el ranking incluye lo facturado con el comprobante nuevo ---'
ROLLBACK TO SAVEPOINT sj;
SELECT 'K · ranking' AS caso, nombre, unidades_cobradas FROM dentrix.v_ranking_tratamientos
 WHERE tratamiento_id = 5;

ROLLBACK;
\echo '=== ROLLBACK: la base queda igual ==='
