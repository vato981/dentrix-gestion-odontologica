\set ON_ERROR_STOP off
\echo '=== SMOKE v3 · ENMIENDA + RF-06 + TOPE DE IMPUTACION (todo con ROLLBACK) ==='
BEGIN;

\echo '--- PREVIO: presupuesto aceptado (curación 80 + endodoncia 250, descuento 30) ---'
INSERT INTO dentrix.presupuesto (paciente_id, ficha_id, version, creado_por)
VALUES (1, (SELECT min(id) FROM dentrix.ficha_clinica WHERE paciente_id = 1), 1, 1)
RETURNING id AS pid \gset

INSERT INTO dentrix.presupuesto_item (presupuesto_id, tratamiento_id, pieza_fdi, precio_unitario)
VALUES (:pid, 1, 16, 80.00) RETURNING id AS item_cura \gset
INSERT INTO dentrix.presupuesto_item (presupuesto_id, tratamiento_id, pieza_fdi, precio_unitario, requiere_consentimiento)
VALUES (:pid, 5, 36, 250.00, TRUE) RETURNING id AS item_endo \gset

UPDATE dentrix.presupuesto SET descuento = 30.00, descuento_motivo = 'Gerencia',
       descuento_autorizado_por = 1 WHERE id = :pid;
UPDATE dentrix.presupuesto SET estado = 'ACEPTADO' WHERE id = :pid;
SELECT 'PREVIO' AS caso, subtotal, descuento, total, estado FROM dentrix.presupuesto WHERE id = :pid;

\echo '--- ENMIENDA 1: anular endodoncia + agregar exodoncia 90 (debe quedar 170 - 30 = 140) ---'
INSERT INTO dentrix.enmienda (presupuesto_id, motivo, firmante_nombre, firmante_documento,
                             firmante_rol, firma_hash, creado_por)
VALUES (:pid, 'El paciente opta por exodoncia', 'Juan Carlos Perez Alania', '45123698',
        'PACIENTE', repeat('b', 64), 2)
RETURNING id AS enm \gset

INSERT INTO dentrix.presupuesto_item (presupuesto_id, tratamiento_id, pieza_fdi, precio_unitario, enmienda_id)
VALUES (:pid, 4, 36, 90.00, :enm) RETURNING id AS item_exo \gset

INSERT INTO dentrix.enmienda_item (enmienda_id, presupuesto_item_id, accion, motivo)
VALUES (:enm, :item_endo, 'ANULAR', 'Renuncia a la endodoncia');
INSERT INTO dentrix.enmienda_item (enmienda_id, presupuesto_item_id, accion)
VALUES (:enm, :item_exo, 'AGREGAR');

SELECT dentrix.firmar_enmienda(:enm, 2);
SELECT 'ENMIENDA 1' AS caso, subtotal, descuento, total FROM dentrix.presupuesto WHERE id = :pid;
SELECT 'ITEMS' AS caso, id, pieza_fdi, estado, precio_unitario, precio_congelado, enmienda_id
  FROM dentrix.presupuesto_item WHERE presupuesto_id = :pid ORDER BY id;
SELECT 'RESUMEN' AS caso, numero, tipo, estado, items_agregados, items_anulados
  FROM dentrix.v_enmienda_resumen WHERE presupuesto_id = :pid;
SELECT 'PLAN VIGENTE' AS caso, item_id, pieza_fdi, subtotal, imputado, saldo
  FROM dentrix.v_plan_vigente WHERE presupuesto_id = :pid ORDER BY item_id;

\echo '--- A) anular un ítem ya pagado (debe fallar) ---'
INSERT INTO dentrix.imputacion (pago_id, presupuesto_item_id, monto, usuario_id)
SELECT (SELECT min(id) FROM dentrix.pago), :item_cura, 50.00, 3;
SAVEPOINT sa;
INSERT INTO dentrix.enmienda (presupuesto_id, motivo, creado_por)
VALUES (:pid, 'Intento anular ítem pagado', 2) RETURNING id AS enma \gset
INSERT INTO dentrix.enmienda_item (enmienda_id, presupuesto_item_id, accion)
VALUES (:enma, :item_cura, 'ANULAR');
SELECT dentrix.firmar_enmienda(:enma, 2);
ROLLBACK TO SAVEPOINT sa;

\echo '--- B) anular un ítem ya ejecutado (debe fallar) ---'
SAVEPOINT sb;
UPDATE dentrix.presupuesto_item SET estado = 'EN_EJECUCION' WHERE id = :item_exo;
INSERT INTO dentrix.enmienda (presupuesto_id, motivo, creado_por)
VALUES (:pid, 'Intento anular ítem ejecutado', 2) RETURNING id AS enmb \gset
INSERT INTO dentrix.enmienda_item (enmienda_id, presupuesto_item_id, accion)
VALUES (:enmb, :item_exo, 'ANULAR');
SELECT dentrix.firmar_enmienda(:enmb, 2);
ROLLBACK TO SAVEPOINT sb;

\echo '--- C) ANULADO por UPDATE directo, sin enmienda (debe fallar) ---'
SAVEPOINT sc;
UPDATE dentrix.presupuesto_item SET estado = 'ANULADO', motivo_anulacion = 'porque si'
 WHERE id = :item_cura;
ROLLBACK TO SAVEPOINT sc;

\echo '--- D) imputar por encima del saldo (debe fallar: saldo 30, imputa 40) ---'
SAVEPOINT sd;
INSERT INTO dentrix.imputacion (pago_id, presupuesto_item_id, monto, usuario_id)
SELECT (SELECT min(id) FROM dentrix.pago), :item_cura, 40.00, 3;
ROLLBACK TO SAVEPOINT sd;

\echo '--- E) imputar exactamente el saldo (debe pasar y dejar 0) ---'
INSERT INTO dentrix.imputacion (pago_id, presupuesto_item_id, monto, usuario_id)
SELECT (SELECT max(id) FROM dentrix.pago), :item_cura, 30.00, 3;
SELECT 'IMPUTACION OK' AS caso, item_id, subtotal, imputado, saldo
  FROM dentrix.v_saldo_item WHERE item_id = :item_cura;

\echo '--- F) enmienda sobre un presupuesto que no está ACEPTADO (debe fallar) ---'
SAVEPOINT sf;
INSERT INTO dentrix.presupuesto (paciente_id, creado_por) VALUES (1, 1) RETURNING id AS pidb \gset
INSERT INTO dentrix.presupuesto_item (presupuesto_id, tratamiento_id, precio_unitario)
VALUES (:pidb, 1, 80.00) RETURNING id AS itemf \gset
INSERT INTO dentrix.enmienda (presupuesto_id, motivo, creado_por)
VALUES (:pidb, 'Sobre un borrador', 2) RETURNING id AS enmf \gset
INSERT INTO dentrix.enmienda_item (enmienda_id, presupuesto_item_id, accion)
VALUES (:enmf, :itemf, 'ANULAR');
SELECT dentrix.firmar_enmienda(:enmf, 2);
ROLLBACK TO SAVEPOINT sf;

\echo '--- G) DELETE de la enmienda (debe fallar) ---'
SAVEPOINT sg;
DELETE FROM dentrix.enmienda WHERE id = :enm;
ROLLBACK TO SAVEPOINT sg;

\echo '--- H) historial del diagnóstico principal: se inserta y se lee ---'
INSERT INTO dentrix.historial_diagnostico_principal (ficha_id, cie10_anterior, cie10_nuevo, motivo, usuario_id)
SELECT min(id), 'K02.1', 'K05.1', 'Corrección del principal', 2 FROM dentrix.ficha_clinica;
SELECT 'HISTORIAL' AS caso, count(*) AS filas FROM dentrix.historial_diagnostico_principal;

\echo '--- I) ítem nuevo en presupuesto ACEPTADO sin enmienda (debe fallar) ---'
SAVEPOINT si;
INSERT INTO dentrix.presupuesto_item (presupuesto_id, tratamiento_id, precio_unitario)
VALUES (:pid, 2, 70.00);
ROLLBACK TO SAVEPOINT si;

\echo '--- J) ejecutar un ítem agregado sin firmar la enmienda (debe fallar R7) ---'
SAVEPOINT sj;
INSERT INTO dentrix.enmienda (presupuesto_id, motivo, creado_por)
VALUES (:pid, 'Ampliación sin firmar', 2) RETURNING id AS enmj \gset
INSERT INTO dentrix.presupuesto_item (presupuesto_id, tratamiento_id, pieza_fdi, precio_unitario, enmienda_id)
VALUES (:pid, 2, 26, 70.00, :enmj) RETURNING id AS itemj \gset
INSERT INTO dentrix.enmienda_item (enmienda_id, presupuesto_item_id, accion)
VALUES (:enmj, :itemj, 'AGREGAR');
UPDATE dentrix.presupuesto_item SET estado = 'EN_EJECUCION' WHERE id = :itemj;
ROLLBACK TO SAVEPOINT sj;

ROLLBACK;
\echo '=== ROLLBACK: la base queda igual ==='
