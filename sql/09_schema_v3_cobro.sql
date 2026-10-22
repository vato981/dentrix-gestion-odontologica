-- ============================================================
-- DENTRIX DENTAL · Esquema v3 · COBRO CONTRA EL PRESUPUESTO
-- Archivo ADITIVO: se ejecuta DESPUÉS de 07_datos_demo_v3.sql
--   psql -U postgres -d dentrix -f sql/09_schema_v3_cobro.sql
--
-- QUÉ RESUELVE
--   1. El descuento de la cabecera no bajaba a los ítems: la boleta cobraba el
--      precio de lista y el paciente había aceptado menos. Ahora, al aceptar (y al
--      firmar una enmienda), el descuento se reparte entre los ítems vigentes y se
--      guarda en `presupuesto_item.monto_neto`. La boleta, la imputación y el saldo
--      usan ese monto neto, así que todo cuadra al centavo.
--   2. La boleta no sabía de qué ítem del presupuesto venía: se agrega
--      `plan_tratamiento.presupuesto_item_id` (nullable, para la venta directa) y un
--      índice único que impide facturar dos veces el mismo ítem.
--   3. RF-07 exigía `ficha_clinica.estado_derivacion = ACEPTADA`, pero ahora el plan
--      se genera y acepta en M10. Se permite facturar una ficha que tenga un
--      presupuesto ACEPTADO, sin perder el vínculo ficha → pago.
--
-- QUÉ NO CAMBIA
--   - `pago` sigue siendo comprobante (pago_origen_id NULL) o abono (apunta a la boleta).
--   - Las boletas viejas (sin ítem) siguen funcionando: `monto_neto` NULL se lee como subtotal.
--   - Nada se borra.
-- ============================================================
SET search_path TO dentrix;

-- ------------------------------------------------------------
-- 1. Descuento repartido en los ítems
-- ------------------------------------------------------------
ALTER TABLE presupuesto_item ADD COLUMN IF NOT EXISTS monto_neto NUMERIC(10,2);

ALTER TABLE presupuesto_item DROP CONSTRAINT IF EXISTS item_monto_neto_chk;
ALTER TABLE presupuesto_item ADD CONSTRAINT item_monto_neto_chk CHECK (
    monto_neto IS NULL OR (monto_neto >= 0 AND monto_neto <= subtotal)
);

-- Reparte el descuento de la cabecera entre los ítems vigentes.
-- Los redondeos se absorben en el último ítem, para que la suma sea exacta.
CREATE OR REPLACE FUNCTION repartir_descuento(p_presupuesto BIGINT) RETURNS VOID AS $$
DECLARE
    v_total  NUMERIC(10,2);
    v_desc   NUMERIC(10,2);
    v_items  INT;
    v_i      INT := 0;
    v_resto  NUMERIC(10,2);
    v_asig   NUMERIC(10,2);
    r        RECORD;
BEGIN
    SELECT subtotal, descuento INTO v_total, v_desc
      FROM presupuesto WHERE id = p_presupuesto;
    IF v_total IS NULL OR v_total <= 0 THEN
        UPDATE presupuesto_item SET monto_neto = subtotal
         WHERE presupuesto_id = p_presupuesto AND estado <> 'ANULADO';
        RETURN;
    END IF;

    SELECT COUNT(*) INTO v_items
      FROM presupuesto_item
     WHERE presupuesto_id = p_presupuesto AND estado <> 'ANULADO';
    IF v_items = 0 THEN
        RETURN;
    END IF;

    v_resto := v_desc;
    FOR r IN SELECT id, subtotal FROM presupuesto_item
              WHERE presupuesto_id = p_presupuesto AND estado <> 'ANULADO'
              ORDER BY id
    LOOP
        v_i := v_i + 1;
        IF v_i = v_items THEN
            v_asig := v_resto;
        ELSE
            v_asig := ROUND(v_desc * r.subtotal / v_total, 2);
        END IF;
        UPDATE presupuesto_item SET monto_neto = r.subtotal - v_asig WHERE id = r.id;
        v_resto := v_resto - v_asig;
    END LOOP;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

-- Al aceptar: congela precios y reparte el descuento.
CREATE OR REPLACE FUNCTION presupuesto_al_aceptar() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.estado = 'ACEPTADO' AND OLD.estado IS DISTINCT FROM 'ACEPTADO' THEN
        UPDATE presupuesto_item
           SET precio_congelado = TRUE,
               estado = CASE WHEN estado = 'PROPUESTO' THEN 'ACEPTADO'::estado_item ELSE estado END
         WHERE presupuesto_id = NEW.id;
        PERFORM repartir_descuento(NEW.id);
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

-- Saldo por ítem sobre el monto neto (con descuento ya aplicado).
CREATE OR REPLACE VIEW v_saldo_item AS
SELECT i.id                                                   AS item_id,
       i.presupuesto_id,
       COALESCE(i.monto_neto, i.subtotal)                     AS subtotal,
       COALESCE(SUM(imp.monto), 0)                            AS imputado,
       COALESCE(i.monto_neto, i.subtotal) - COALESCE(SUM(imp.monto), 0) AS saldo
  FROM presupuesto_item i
  LEFT JOIN imputacion imp ON imp.presupuesto_item_id = i.id
 GROUP BY i.id, i.presupuesto_id, i.subtotal, i.monto_neto;

-- La imputación también se topea contra el monto neto.
CREATE OR REPLACE FUNCTION imputacion_valida() RETURNS TRIGGER AS $$
DECLARE
    v_neto   NUMERIC(10,2);
    v_estado estado_item;
    v_ya     NUMERIC(10,2);
    v_saldo  NUMERIC(10,2);
BEGIN
    SELECT COALESCE(monto_neto, subtotal), estado INTO v_neto, v_estado
      FROM presupuesto_item WHERE id = NEW.presupuesto_item_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'R6: el ítem % no existe', NEW.presupuesto_item_id;
    END IF;
    IF v_estado IN ('ANULADO','RECHAZADO') THEN
        RAISE EXCEPTION 'R6: el ítem % está % y no admite pagos', NEW.presupuesto_item_id, v_estado;
    END IF;

    SELECT COALESCE(SUM(monto), 0) INTO v_ya
      FROM imputacion
     WHERE presupuesto_item_id = NEW.presupuesto_item_id
       AND id IS DISTINCT FROM NEW.id;

    v_saldo := v_neto - v_ya;
    IF NEW.monto > v_saldo THEN
        RAISE EXCEPTION 'P5 §2.1: el ítem % solo tiene S/ % pendiente; no se aceptan anticipos (imputó S/ %)',
            NEW.presupuesto_item_id, v_saldo, NEW.monto;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

-- Compatibilidad: la vista vieja compara contra el monto neto.
CREATE OR REPLACE VIEW v_plan_tratamiento AS
SELECT i.id,
       p.paciente_id,
       i.tratamiento_id,
       p.ficha_id,
       NULL::BIGINT AS pago_id,
       i.pieza_fdi,
       i.cantidad,
       i.precio_unitario,
       i.subtotal,
       CASE
           WHEN i.estado IN ('EN_EJECUCION','EJECUTADO','CONTROLADO')
                AND s.imputado >= COALESCE(i.monto_neto, i.subtotal) THEN 'COBRADO'::estado_linea
           WHEN i.estado IN ('EN_EJECUCION','EJECUTADO','CONTROLADO') THEN 'EJECUTADO'::estado_linea
           ELSE 'PLANIFICADO'::estado_linea
       END AS estado,
       p.fecha_emision AS fecha
  FROM presupuesto_item i
  JOIN presupuesto p ON p.id = i.presupuesto_id
  JOIN v_saldo_item s ON s.item_id = i.id;

-- ------------------------------------------------------------
-- 2. La boleta sabe de qué ítem viene
-- ------------------------------------------------------------
ALTER TABLE plan_tratamiento ADD COLUMN IF NOT EXISTS presupuesto_item_id BIGINT REFERENCES presupuesto_item(id);
CREATE INDEX IF NOT EXISTS ix_plan_item_presupuesto ON plan_tratamiento (presupuesto_item_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_plan_item_facturado
    ON plan_tratamiento (presupuesto_item_id)
    WHERE presupuesto_item_id IS NOT NULL;

-- ------------------------------------------------------------
-- 3. RF-07: se factura con derivación ACEPTADA o con presupuesto ACEPTADO
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION pago_requiere_derivacion() RETURNS TRIGGER AS $$
DECLARE
    v_est  estado_derivacion;
    v_pres INT;
BEGIN
    IF NEW.ficha_id IS NULL THEN RETURN NEW; END IF;
    SELECT estado_derivacion INTO v_est FROM ficha_clinica WHERE id = NEW.ficha_id;
    IF v_est IS DISTINCT FROM 'ACEPTADA' THEN
        SELECT COUNT(*) INTO v_pres FROM presupuesto
         WHERE ficha_id = NEW.ficha_id AND estado = 'ACEPTADO';
        IF v_pres = 0 THEN
            RAISE EXCEPTION 'RF-07: la ficha % no tiene derivacion ACEPTADA ni presupuesto ACEPTADO; no se factura',
                NEW.ficha_id;
        END IF;
    END IF;
    UPDATE ficha_clinica
       SET derivado_a_caja = TRUE,
           fecha_derivacion = COALESCE(fecha_derivacion, CURRENT_TIMESTAMP)
     WHERE id = NEW.ficha_id
       AND estado_derivacion = 'ACEPTADA';
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

COMMENT ON COLUMN presupuesto_item.monto_neto IS 'P3/P5 · subtotal menos la parte del descuento de la cabecera; es lo que se cobra e imputa';
COMMENT ON COLUMN plan_tratamiento.presupuesto_item_id IS 'P5 · ítem del presupuesto facturado; NULL en venta directa';
