\set ON_ERROR_STOP off
\echo '=== SMOKE v3 · TARIFARIO CON VIGENCIA (todo con ROLLBACK) ==='
BEGIN;

\echo '--- A) la vista devuelve los 5 precios vigentes hoy ---'
SELECT 'A · precios hoy' AS caso, count(*) AS filas FROM dentrix.v_precio_vigente;
SELECT 'A · detalle' AS caso, tratamiento_id, version, precio
  FROM dentrix.v_precio_vigente ORDER BY tratamiento_id;

\echo '--- B) v2 desde 2026-11-01, TRAT-05 a 300 y publicar ---'
INSERT INTO dentrix.tarifario (version, vigencia_desde, motivo, creado_por)
VALUES (2, DATE '2026-11-01', 'Reajuste de endodoncia', 1)
RETURNING id AS tar \gset

INSERT INTO dentrix.tarifario_item (tarifario_id, tratamiento_id, precio)
SELECT :tar, id, precio FROM dentrix.tratamiento;

UPDATE dentrix.tarifario_item SET precio = 300.00
 WHERE tarifario_id = :tar AND tratamiento_id = 5;

SELECT dentrix.publicar_tarifario(:tar, 1);

\echo '    Hoy la v2 todavia no rige: B1 debe dar el precio de la v1 CERRADA (120), no 300.'
SELECT 'B1 · vista hoy' AS caso, tratamiento_id, version, precio
  FROM dentrix.v_precio_vigente WHERE tratamiento_id = 5;
SELECT 'B2 · v1 quedo' AS caso, version, estado, vigencia_desde, vigencia_hasta
  FROM dentrix.tarifario WHERE version = 1;
SELECT 'B3 · precio 2026-06-01' AS caso, dentrix.precio_a_fecha(5, DATE '2026-06-01') AS precio;
SELECT 'B4 · precio 2026-11-05' AS caso, dentrix.precio_a_fecha(5, DATE '2026-11-05') AS precio;

\echo '--- C) editar el precio de un item del tarifario PUBLICADO (debe fallar) ---'
SAVEPOINT sc;
UPDATE dentrix.tarifario_item SET precio = 310.00
 WHERE tarifario_id = :tar AND tratamiento_id = 5;
ROLLBACK TO SAVEPOINT sc;

\echo '--- D) publicar un tarifario sin items (debe fallar) ---'
SAVEPOINT sd;
INSERT INTO dentrix.tarifario (version, vigencia_desde, motivo, creado_por)
VALUES (3, DATE '2027-01-01', 'Version vacia', 1)
RETURNING id AS vacio \gset
SELECT dentrix.publicar_tarifario(:vacio, 1);
ROLLBACK TO SAVEPOINT sd;

\echo '--- E) insertar un tarifario CERRADO solapado con la v1 (debe fallar) ---'
SAVEPOINT se;
INSERT INTO dentrix.tarifario (version, vigencia_desde, vigencia_hasta, estado, motivo, creado_por, publicado_por, fecha_publicacion)
VALUES (4, DATE '2026-06-01', DATE '2026-06-30', 'CERRADO', 'Solape con v1', 1, 1, CURRENT_TIMESTAMP);
ROLLBACK TO SAVEPOINT se;

\echo '--- F) DELETE de un tarifario (debe fallar) ---'
SAVEPOINT sf;
DELETE FROM dentrix.tarifario WHERE version = 1;
ROLLBACK TO SAVEPOINT sf;

\echo '--- G) publicar dos veces el mismo tarifario (debe fallar) ---'
SAVEPOINT sg;
SELECT dentrix.publicar_tarifario(:tar, 1);
ROLLBACK TO SAVEPOINT sg;

\echo '--- RESUMEN: versiones del tarifario ---'
SELECT version, estado, vigencia_desde, vigencia_hasta
  FROM dentrix.tarifario ORDER BY version;
SELECT version, count(*) AS items, sum(precio) AS suma
  FROM dentrix.tarifario t
  JOIN dentrix.tarifario_item i ON i.tarifario_id = t.id
 GROUP BY version ORDER BY version;

ROLLBACK;
\echo '=== ROLLBACK: la base queda igual ==='
