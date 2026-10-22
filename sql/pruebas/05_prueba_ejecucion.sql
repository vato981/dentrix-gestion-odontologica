\set ON_ERROR_STOP off
\echo '=== SMOKE P4-A · SESIONES, EVOLUCION, CONTROL Y CIERRE ==='
BEGIN;

\echo '--- A) sesión 1 de 2 de la endodoncia (item 58) ---'
INSERT INTO dentrix.ejecucion_item
       (presupuesto_item_id, numero_sesion, profesional_id, procedimiento, pieza_fdi, superficies)
VALUES (58, 1, 2, 'Apertura y conductometria', 36, 'oclusal')
RETURNING id AS ses1 \gset
SELECT 'A · sesion creada' AS caso, id, estado, numero_sesion, sesiones_planificadas FROM dentrix.ejecucion_item WHERE id = :ses1;

\echo '--- B) firmar la evolución: la sesión pasa a FIRMADA y el ítem a EN_EJECUCION ---'
INSERT INTO dentrix.evolucion (ficha_id, ejecucion_item_id, texto, estado, profesional_id, fecha_firma, firma_hash)
SELECT p.ficha_id, :ses1, 'Se realizo apertura y conductometria; paciente estable', 'FIRMADA', 2,
       CURRENT_TIMESTAMP, repeat('c', 64)
  FROM dentrix.presupuesto p WHERE p.id = 53;
SELECT 'B · sesion' AS caso, id, estado FROM dentrix.ejecucion_item WHERE id = :ses1;
SELECT 'B · item' AS caso, id, estado FROM dentrix.presupuesto_item WHERE id = 58;

\echo '--- C) una tercera sesión debe fallar (planificadas = 2) ---'
SAVEPOINT sd;
INSERT INTO dentrix.ejecucion_item (presupuesto_item_id, numero_sesion, profesional_id, procedimiento)
VALUES (58, 3, 2, 'Sesion extra');
ROLLBACK TO SAVEPOINT sd;

\echo '--- D) sesión 2 de 2 + evolución firmada: el ítem queda EJECUTADO ---'
INSERT INTO dentrix.ejecucion_item (presupuesto_item_id, numero_sesion, profesional_id, procedimiento, pieza_fdi)
VALUES (58, 2, 2, 'Obturacion de conductos', 36)
RETURNING id AS ses2 \gset
INSERT INTO dentrix.evolucion (ficha_id, ejecucion_item_id, texto, estado, profesional_id, fecha_firma, firma_hash)
SELECT p.ficha_id, :ses2, 'Obturacion de conductos y control radiografico', 'FIRMADA', 2,
       CURRENT_TIMESTAMP, repeat('d', 64)
  FROM dentrix.presupuesto p WHERE p.id = 53;
SELECT 'D · item' AS caso, id, estado FROM dentrix.presupuesto_item WHERE id = 58;
SELECT 'D · sesiones' AS caso, sesiones_firmadas, sesiones_restantes FROM dentrix.v_sesiones_item WHERE item_id = 58;

\echo '--- E) control post programado y realizado: el ítem queda CONTROLADO ---'
INSERT INTO dentrix.control_post (presupuesto_item_id, fecha_programada)
VALUES (58, CURRENT_DATE + 7) RETURNING id AS ctrl \gset
UPDATE dentrix.control_post SET estado = 'REALIZADO', fecha_realizada = CURRENT_DATE,
       hallazgo = 'Sin dolor ni inflamacion', profesional_id = 2
 WHERE id = :ctrl;
SELECT 'E · item' AS caso, id, estado FROM dentrix.presupuesto_item WHERE id = 58;

\echo '--- F) cerrar TERMINADO con ítems abiertos (59 y 60) debe fallar ---'
SAVEPOINT sf;
INSERT INTO dentrix.cierre_plan (presupuesto_id, estado, motivo, usuario_id, fecha_recall)
VALUES (53, 'TERMINADO', 'Todo listo', 2, CURRENT_DATE + 180);
ROLLBACK TO SAVEPOINT sf;

\echo '--- G) editar el texto de una evolución firmada debe fallar ---'
SAVEPOINT sg;
UPDATE dentrix.evolucion SET texto = 'Otro texto' WHERE ejecucion_item_id = :ses1;
ROLLBACK TO SAVEPOINT sg;

\echo '--- H) DELETE de la sesión debe fallar ---'
SAVEPOINT sh;
DELETE FROM dentrix.ejecucion_item WHERE id = :ses2;
ROLLBACK TO SAVEPOINT sh;

\echo '--- I) cerrar ABANDONADO sí se puede ---'
INSERT INTO dentrix.cierre_plan (presupuesto_id, estado, motivo, usuario_id)
VALUES (53, 'ABANDONADO', 'El paciente no continua', 2);
SELECT 'I · cierre' AS caso, estado, motivo FROM dentrix.cierre_plan WHERE presupuesto_id = 53;

ROLLBACK;
\echo '=== ROLLBACK: la base queda igual ==='
