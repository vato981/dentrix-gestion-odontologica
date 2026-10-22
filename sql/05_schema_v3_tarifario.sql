-- ============================================================
-- DENTRIX DENTAL · Esquema v3 · paso 5 de la migración: TARIFARIO CON VIGENCIA
-- Archivo ADITIVO: se ejecuta DESPUÉS de 04_schema_v3_diag_e_imputacion.sql
--   psql -U postgres -d dentrix -f sql/05_schema_v3_tarifario.sql
--
-- QUÉ RESUELVE (R21 · brecha 6 de P3)
--   1. El precio vivía en `tratamiento.precio`: una sola columna, sin historia.
--      Subir un precio reescribía el pasado y no había forma de saber cuánto
--      costaba una endodoncia en junio.
--   2. No existía versión publicada: cualquiera con UPDATE cambiaba la lista de
--      precios sin dejar rastro ni fecha de vigencia.
--   3. No había garantía de una sola lista vigente ni de vigencias sin solape.
--
-- CÓMO LO RESUELVE
--   - `tarifario` es la cabecera versionada (BORRADOR → PUBLICADO → CERRADO).
--     El precio sube abriendo una versión nueva, NUNCA editando la publicada.
--   - `tarifario_item` guarda el precio por tratamiento de cada versión.
--   - Los precios solo se tocan mientras la cabecera está en BORRADOR.
--   - Un solo PUBLICADO a la vez (índice único parcial) y vigencias sin solape
--     para PUBLICADO/CERRADO (restricción EXCLUDE con daterange).
--   - `publicar_tarifario()` cierra la versión vigente el día anterior al inicio
--     de la nueva y publica la nueva, todo en una transacción.
--   - `precio_a_fecha()` responde "cuánto costaba el tratamiento X el día Y"
--     contra versiones PUBLICADO o CERRADO.
--
-- QUÉ NO ENTRA (declarado, no olvidado)
--   - No se reescribe `tratamiento.precio`: sigue existiendo como precio base y
--     la semilla lo copia a la versión 1. El código JavaFX puede seguir leyéndolo.
--   - No hay precios por sede, convenio ni tipo de paciente (fuera de alcance v3).
--   - No hay descuento por ítem del tarifario ni IGV: el tarifario es el precio
--     de lista; el descuento vive en el presupuesto (R20).
--   - Si se publica una versión con fecha futura, hasta ese día sigue rigiendo la
--     anterior: `v_precio_vigente` incluye también las CERRADAS cuyo rango cubre
--     hoy, y `precio_a_fecha()` resuelve el histórico.
--   - Un tarifario en BORRADOR se puede descartar (DELETE); PUBLICADO y CERRADO no.
--     Un borrador nunca fue precio de lista ni evidencia.
--
-- COMPATIBILIDAD
--   No toca ninguna tabla, vista ni función de los pasos 1–4. Las funciones de
--   trigger llevan `SET search_path = dentrix` por la misma razón que el resto:
--   las reglas deben cumplirse desde cualquier cliente (psql, DBeaver, JDBC).
-- ============================================================
SET search_path TO dentrix;

-- Nota: el EXCLUDE compara daterange con &&, y su opclass GiST (range_ops)
-- vive en pg_catalog. No hace falta btree_gist mientras la clave sea solo el
-- rango; si algún día se agrega el enum al EXCLUDE, habrá que instalarlo.

-- ------------------------------------------------------------
-- Tipos enumerados nuevos
-- ------------------------------------------------------------
CREATE TYPE estado_tarifario AS ENUM ('BORRADOR','PUBLICADO','CERRADO');

-- ------------------------------------------------------------
-- 1. Tarifario (cabecera versionada)
-- ------------------------------------------------------------
CREATE TABLE tarifario (
    id                 BIGSERIAL PRIMARY KEY,
    version            INT NOT NULL,
    moneda             CHAR(3) NOT NULL DEFAULT 'PEN',
    vigencia_desde     DATE NOT NULL,
    vigencia_hasta     DATE,                                   -- NULL = abierto (solo PUBLICADO)
    estado             estado_tarifario NOT NULL DEFAULT 'BORRADOR',
    motivo             VARCHAR(200),
    creado_por         INT NOT NULL REFERENCES usuario(id),
    publicado_por      INT REFERENCES usuario(id),
    fecha_publicacion  TIMESTAMP,
    CONSTRAINT tarifario_version_unica UNIQUE (version),
    CONSTRAINT tarifario_vigencia_chk CHECK (
        vigencia_hasta IS NULL OR vigencia_hasta >= vigencia_desde
    ),
    CONSTRAINT tarifario_publicado_chk CHECK (
        estado <> 'PUBLICADO'
        OR (vigencia_hasta IS NULL AND publicado_por IS NOT NULL AND fecha_publicacion IS NOT NULL)
    ),
    CONSTRAINT tarifario_cerrado_chk CHECK (
        estado <> 'CERRADO' OR vigencia_hasta IS NOT NULL
    )
);

-- Un solo tarifario publicado a la vez.
CREATE UNIQUE INDEX ux_tarifario_un_publicado ON tarifario (estado) WHERE estado = 'PUBLICADO';

-- Dos versiones publicadas/cerradas no pueden cubrir la misma fecha.
ALTER TABLE tarifario
    ADD CONSTRAINT tarifario_sin_solape EXCLUDE USING gist (
        daterange(vigencia_desde, COALESCE(vigencia_hasta, 'infinity'::date), '[]') WITH &&
    ) WHERE (estado IN ('PUBLICADO','CERRADO'));

-- ------------------------------------------------------------
-- 2. Ítem del tarifario (precio por tratamiento y por versión)
-- ------------------------------------------------------------
CREATE TABLE tarifario_item (
    id              BIGSERIAL PRIMARY KEY,
    tarifario_id    BIGINT NOT NULL REFERENCES tarifario(id),
    tratamiento_id  INT NOT NULL REFERENCES tratamiento(id),
    precio          NUMERIC(10,2) NOT NULL CHECK (precio >= 0),
    CONSTRAINT tarifario_item_unico UNIQUE (tarifario_id, tratamiento_id)
);
CREATE INDEX ix_tarifario_item_tratamiento ON tarifario_item (tratamiento_id);

-- ------------------------------------------------------------
-- Reglas en la base, no solo en Java
-- ------------------------------------------------------------

-- R22 · una versión publicada o cerrada no se borra: se anula cambiando de estado.
-- Un BORRADOR sí se puede descartar: nunca llegó a ser precio de lista ni evidencia.
CREATE OR REPLACE FUNCTION bloquea_delete_tarifario() RETURNS TRIGGER AS $$
DECLARE v_estado estado_tarifario;
BEGIN
    IF TG_TABLE_NAME = 'tarifario' THEN
        v_estado := OLD.estado;
    ELSE
        SELECT estado INTO v_estado FROM tarifario WHERE id = OLD.tarifario_id;
    END IF;
    IF v_estado = 'BORRADOR' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'R22: % está % y no admite DELETE; se anula con el estado (BORRADOR → PUBLICADO → CERRADO)',
        TG_TABLE_NAME, v_estado;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_tarifario_no_delete      BEFORE DELETE ON tarifario      FOR EACH ROW EXECUTE FUNCTION bloquea_delete_tarifario();
CREATE TRIGGER tr_tarifario_item_no_delete BEFORE DELETE ON tarifario_item FOR EACH ROW EXECUTE FUNCTION bloquea_delete_tarifario();

-- Numeración correlativa: la versión se asigna sola si no se indica.
CREATE OR REPLACE FUNCTION numerar_tarifario() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.version IS NULL THEN
        SELECT COALESCE(MAX(version), 0) + 1 INTO NEW.version FROM tarifario;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_tarifario_numera BEFORE INSERT ON tarifario
FOR EACH ROW EXECUTE FUNCTION numerar_tarifario();

-- Publicado o cerrado, el tarifario no cambia de contenido: se abre otra versión.
CREATE OR REPLACE FUNCTION tarifario_inmutable() RETURNS TRIGGER AS $$
BEGIN
    IF OLD.estado <> 'BORRADOR'
       AND (NEW.version, NEW.moneda, NEW.vigencia_desde)
           IS DISTINCT FROM (OLD.version, OLD.moneda, OLD.vigencia_desde)
    THEN
        RAISE EXCEPTION 'R21: el tarifario % está % y no se edita; abra una versión nueva',
            OLD.id, OLD.estado;
    END IF;

    IF NEW.estado IS DISTINCT FROM OLD.estado THEN
        IF NOT (
            (OLD.estado = 'BORRADOR'  AND NEW.estado IN ('PUBLICADO','CERRADO'))
            OR (OLD.estado = 'PUBLICADO' AND NEW.estado = 'CERRADO')
        ) THEN
            RAISE EXCEPTION 'R21: el tarifario % no puede pasar de % a %',
                OLD.id, OLD.estado, NEW.estado;
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_tarifario_inmutable BEFORE UPDATE ON tarifario
FOR EACH ROW EXECUTE FUNCTION tarifario_inmutable();

-- Los precios solo se editan mientras la cabecera está en BORRADOR.
CREATE OR REPLACE FUNCTION tarifario_item_control() RETURNS TRIGGER AS $$
DECLARE
    v_tarifario BIGINT;
    v_estado    estado_tarifario;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_tarifario := OLD.tarifario_id;
    ELSE
        v_tarifario := NEW.tarifario_id;
    END IF;

    SELECT estado INTO v_estado FROM tarifario WHERE id = v_tarifario;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El tarifario % no existe', v_tarifario;
    END IF;
    IF v_estado <> 'BORRADOR' THEN
        RAISE EXCEPTION 'el tarifario % está % y sus precios no se editan', v_tarifario, v_estado;
    END IF;

    -- Mover un ítem de un tarifario publicado también es editar sus precios.
    IF TG_OP = 'UPDATE' AND NEW.tarifario_id IS DISTINCT FROM OLD.tarifario_id THEN
        SELECT estado INTO v_estado FROM tarifario WHERE id = OLD.tarifario_id;
        IF v_estado <> 'BORRADOR' THEN
            RAISE EXCEPTION 'el tarifario % está % y sus precios no se editan', OLD.tarifario_id, v_estado;
        END IF;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_tarifario_item_control BEFORE INSERT OR UPDATE OR DELETE ON tarifario_item
FOR EACH ROW EXECUTE FUNCTION tarifario_item_control();

-- ------------------------------------------------------------
-- publicar_tarifario: el único camino para poner precios en vigencia
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION publicar_tarifario(p_id BIGINT, p_usuario INT)
RETURNS VOID AS $$
DECLARE
    v_estado estado_tarifario;
    v_desde  DATE;
    v_items  INT;
    v_otro   RECORD;
BEGIN
    SELECT estado, vigencia_desde INTO v_estado, v_desde
      FROM tarifario WHERE id = p_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El tarifario % no existe', p_id;
    END IF;
    IF v_estado <> 'BORRADOR' THEN
        RAISE EXCEPTION 'El tarifario % está % y no se publica', p_id, v_estado;
    END IF;

    SELECT COUNT(*) INTO v_items FROM tarifario_item WHERE tarifario_id = p_id;
    IF v_items = 0 THEN
        RAISE EXCEPTION 'El tarifario % no tiene ítems: no hay nada que publicar', p_id;
    END IF;

    -- Cierra la versión vigente el día anterior al inicio de la nueva.
    FOR v_otro IN
        SELECT id, vigencia_desde FROM tarifario
         WHERE estado = 'PUBLICADO' AND id <> p_id
         FOR UPDATE
    LOOP
        IF v_otro.vigencia_desde >= v_desde THEN
            RAISE EXCEPTION 'La versión nueva (desde %) debe empezar después del tarifario vigente % (desde %)',
                v_desde, v_otro.id, v_otro.vigencia_desde;
        END IF;
        UPDATE tarifario
           SET estado = 'CERRADO',
               vigencia_hasta = v_desde - 1
         WHERE id = v_otro.id;
    END LOOP;

    UPDATE tarifario
       SET estado = 'PUBLICADO',
           publicado_por = p_usuario,
           fecha_publicacion = CURRENT_TIMESTAMP
     WHERE id = p_id;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

-- ------------------------------------------------------------
-- Vistas y consultas de precio
-- ------------------------------------------------------------

-- Precio de lista que rige HOY: la versión cuya vigencia cubre la fecha actual,
-- sea la PUBLICADA (vigente hacia adelante) o una CERRADA (su rango ya terminó).
CREATE VIEW v_precio_vigente AS
SELECT i.tratamiento_id,
       t.id            AS tarifario_id,
       t.version,
       i.precio,
       t.vigencia_desde,
       t.vigencia_hasta
  FROM tarifario t
  JOIN tarifario_item i ON i.tarifario_id = t.id
 WHERE t.estado IN ('PUBLICADO','CERRADO')
   AND CURRENT_DATE >= t.vigencia_desde
   AND (t.vigencia_hasta IS NULL OR CURRENT_DATE <= t.vigencia_hasta);

-- Precio del tratamiento a una fecha cualquiera (histórico incluido).
CREATE OR REPLACE FUNCTION precio_a_fecha(p_tratamiento INT, p_fecha DATE)
RETURNS NUMERIC(10,2) AS $$
DECLARE v_precio NUMERIC(10,2);
BEGIN
    SELECT i.precio INTO v_precio
      FROM tarifario t
      JOIN tarifario_item i ON i.tarifario_id = t.id
     WHERE i.tratamiento_id = p_tratamiento
       AND t.estado IN ('PUBLICADO','CERRADO')
       AND p_fecha >= t.vigencia_desde
       AND (t.vigencia_hasta IS NULL OR p_fecha <= t.vigencia_hasta)
     ORDER BY t.vigencia_desde DESC
     LIMIT 1;
    RETURN v_precio;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

-- ------------------------------------------------------------
-- Semilla: versión 1 = copia de tratamiento.precio, ya publicada
-- ------------------------------------------------------------
-- Se inserta en BORRADOR y se publica con UPDATE porque los ítems solo pueden
-- entrar mientras la cabecera está en borrador (tarifario_item_control).
INSERT INTO tarifario (version, moneda, vigencia_desde, estado, motivo, creado_por)
VALUES (1, 'PEN', DATE '2026-01-01', 'BORRADOR', 'Carga inicial desde tratamiento.precio', 1);

INSERT INTO tarifario_item (tarifario_id, tratamiento_id, precio)
SELECT t.id, tr.id, tr.precio
  FROM tarifario t
 CROSS JOIN tratamiento tr
 WHERE t.version = 1;

UPDATE tarifario
   SET estado = 'PUBLICADO',
       publicado_por = 1,
       fecha_publicacion = CURRENT_TIMESTAMP
 WHERE version = 1;

-- ------------------------------------------------------------
-- Documentación
-- ------------------------------------------------------------
COMMENT ON TABLE tarifario       IS 'R21 · versión del tarifario maestro; el precio sube abriendo versión nueva, nunca editando la publicada';
COMMENT ON TABLE tarifario_item  IS 'R21 · precio de lista por tratamiento dentro de una versión (editable solo en BORRADOR)';
COMMENT ON VIEW  v_precio_vigente IS 'R21 · precio que rige hoy: ítems de la versión (PUBLICADA o CERRADA) cuyo rango de vigencia cubre CURRENT_DATE';
COMMENT ON FUNCTION publicar_tarifario(BIGINT, INT) IS 'R21 · cierra la versión vigente (hasta = nueva vigencia_desde - 1) y publica la nueva; exige al menos un ítem';
COMMENT ON FUNCTION precio_a_fecha(INT, DATE) IS 'R21 · precio del tratamiento en una fecha, contra versiones PUBLICADO y CERRADO; NULL si ninguna la cubre';
