-- ============================================================
-- DENTRIX DENTAL · Esquema v3 · AJUSTES DE FICHA Y COBRO
-- Archivo ADITIVO: se ejecuta DESPUÉS de 03_schema_v3_enmienda.sql
--   psql -U postgres -d dentrix -f sql/04_schema_v3_diag_e_imputacion.sql
--
-- QUÉ RESUELVE
--   A) RF-06 · el diagnóstico principal se puede CORREGIR, pero queda registrado:
--      quién lo cambió, cuándo, de qué código a cuál y por qué (P2 / RF-06).
--      Antes el servicio lo reemplazaba en silencio (desmarcarPrincipales) y no había rastro.
--   B) R6 / P5 §2.1 · un pago no puede imputarse por encima del saldo del ítem.
--      "Anticipos y saldo a favor" están fuera de alcance v3: un pago mayor al saldo se rechaza.
--      Sin este tope, un error de tecleo dejaba saldo negativo en v_saldo_item y nadie lo veía.
--
-- COMPATIBILIDAD
--   No toca tablas del v2 ni del paso 3. Solo agrega una tabla append-only y dos triggers.
-- ============================================================
SET search_path TO dentrix;

-- ------------------------------------------------------------
-- A. Historial del diagnóstico principal (append-only para la aplicación)
-- ------------------------------------------------------------
-- No hay UPDATE ni DELETE desde la app: solo se inserta. El CASCADE existe para que
-- borrar una ficha (acción administrativa heredada del v2, no expuesta en la interfaz)
-- no quede bloqueada por su propio historial.
CREATE TABLE historial_diagnostico_principal (
    id              BIGSERIAL PRIMARY KEY,
    ficha_id        BIGINT NOT NULL REFERENCES ficha_clinica(id) ON DELETE CASCADE,
    cie10_anterior  VARCHAR(7) NOT NULL REFERENCES diagnostico_cie10(codigo),
    cie10_nuevo     VARCHAR(7) NOT NULL REFERENCES diagnostico_cie10(codigo),
    motivo          VARCHAR(200) NOT NULL CHECK (btrim(motivo) <> ''),
    usuario_id      INT REFERENCES usuario(id),
    fecha           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT historial_principal_cambia CHECK (cie10_anterior <> cie10_nuevo)
);
CREATE INDEX ix_historial_principal_ficha ON historial_diagnostico_principal (ficha_id, fecha);

COMMENT ON TABLE historial_diagnostico_principal
    IS 'RF-06 · constancia de cada cambio de diagnóstico principal (quién, cuándo, de cuál a cuál y por qué)';

-- ------------------------------------------------------------
-- B. Tope de imputación: nunca por encima del saldo del ítem
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION imputacion_valida() RETURNS TRIGGER AS $$
DECLARE
    v_subtotal NUMERIC(10,2);
    v_estado   estado_item;
    v_ya       NUMERIC(10,2);
    v_saldo    NUMERIC(10,2);
BEGIN
    SELECT subtotal, estado INTO v_subtotal, v_estado
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

    v_saldo := v_subtotal - v_ya;
    IF NEW.monto > v_saldo THEN
        RAISE EXCEPTION 'P5 §2.1: el ítem % solo tiene S/ % pendiente; no se aceptan anticipos (imputó S/ %)',
            NEW.presupuesto_item_id, v_saldo, NEW.monto;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql SET search_path = dentrix;

CREATE TRIGGER tr_imputacion_valida BEFORE INSERT OR UPDATE ON imputacion
FOR EACH ROW EXECUTE FUNCTION imputacion_valida();

COMMENT ON FUNCTION imputacion_valida() IS
    'R6 · impide imputar más que el saldo del ítem (anticipos y saldo a favor fuera de alcance v3)';
