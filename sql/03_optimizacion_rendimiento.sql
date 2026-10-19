-- ============================================================
-- 03_optimizacion_rendimiento.sql  (2026-10-18)
-- Indices faltantes + vistas sin cast no-sargable + funciones
-- trigger con referencias calificadas al esquema.
--
-- Ejecutar DESPUES de 01_schema_v2.sql y 02_datos_prueba_v2.sql.
-- Es idempotente: puede aplicarse tambien sobre una BD existente.
--
--   psql -U postgres -d dentrix -f sql/03_optimizacion_rendimiento.sql
-- ============================================================

-- 1) Extension para busquedas "LIKE '%texto%'" (PacienteDAOImpl.listarPorTexto)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 2) Indices faltantes segun los patrones de consulta reales de los DAOs
CREATE INDEX IF NOT EXISTS idx_pago_fecha ON dentrix.pago (fecha);
CREATE INDEX IF NOT EXISTS idx_pago_origen ON dentrix.pago (pago_origen_id);
CREATE INDEX IF NOT EXISTS idx_pago_ficha ON dentrix.pago (ficha_id);
CREATE INDEX IF NOT EXISTS idx_pago_deuda ON dentrix.pago (paciente_id)
    WHERE pago_origen_id IS NULL AND estado = 'EMITIDO' AND saldo_deudor > 0;
CREATE INDEX IF NOT EXISTS idx_ficha_odontologo ON dentrix.ficha_clinica (odontologo_id, fecha);
CREATE INDEX IF NOT EXISTS idx_plan_paciente ON dentrix.plan_tratamiento (paciente_id, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_plan_cobrado ON dentrix.plan_tratamiento (tratamiento_id)
    WHERE estado = 'COBRADO';

-- 3) Busqueda de pacientes por texto (indices de trigramas sobre las
--    mismas expresiones que usa el DAO: LOWER(col) LIKE '%texto%')
CREATE INDEX IF NOT EXISTS idx_paciente_apellidos_trgm
    ON dentrix.paciente USING gin (lower(apellidos) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_paciente_nombres_trgm
    ON dentrix.paciente USING gin (lower(nombres) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_paciente_nombre_completo_trgm
    ON dentrix.paciente USING gin (lower(apellidos || ' ' || nombres) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_paciente_dni_trgm
    ON dentrix.paciente USING gin (lower(dni) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_paciente_hc_trgm
    ON dentrix.paciente USING gin (lower(codigo_hc) gin_trgm_ops);

-- 4) Indice redundante: repite el prefijo de la PK de hallazgo_dental (paciente_id, pieza_fdi)
DROP INDEX IF EXISTS dentrix.idx_hallazgo_paciente;

-- 5) v_kpis_dashboard sin cast no-sargable (fecha::date -> rango)
CREATE OR REPLACE VIEW dentrix.v_kpis_dashboard AS
SELECT (SELECT COUNT(*) FROM dentrix.cita WHERE fecha_cita = CURRENT_DATE AND tipo = 'CITA') AS citas_hoy,
       (SELECT COUNT(*) FROM dentrix.cita WHERE fecha_cita = CURRENT_DATE AND estado = 'EN_SALA') AS sala_espera,
       (SELECT COALESCE(SUM(monto), 0) FROM dentrix.pago
         WHERE fecha >= CURRENT_DATE AND fecha < CURRENT_DATE + 1
           AND pago_origen_id IS NULL AND estado = 'EMITIDO') AS recaudado_hoy,
       (SELECT COUNT(DISTINCT paciente_id) FROM dentrix.pago
         WHERE pago_origen_id IS NULL AND saldo_deudor > 0 AND estado = 'EMITIDO') AS pacientes_deudores;

-- 6) Funciones trigger con referencias calificadas al esquema.
--    Misma logica de negocio; antes fallaban si la sesion no tenia
--    "dentrix" en su search_path (p. ej. al operar desde psql).
CREATE OR REPLACE FUNCTION dentrix.ficha_diag_un_principal() RETURNS TRIGGER AS $$
DECLARE v_otros INT;
BEGIN
    IF NEW.es_principal THEN
        SELECT COUNT(*) INTO v_otros
          FROM dentrix.ficha_diagnostico
         WHERE ficha_id = NEW.ficha_id AND es_principal AND cie10_codigo <> NEW.cie10_codigo;
        IF v_otros > 0 THEN
            RAISE EXCEPTION 'RF-06: la ficha % ya tiene un diagnostico principal', NEW.ficha_id;
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION dentrix.pago_requiere_derivacion() RETURNS TRIGGER AS $$
DECLARE v_est dentrix.estado_derivacion;
BEGIN
    IF NEW.ficha_id IS NULL THEN RETURN NEW; END IF;
    SELECT estado_derivacion INTO v_est FROM dentrix.ficha_clinica WHERE id = NEW.ficha_id;
    IF v_est IS DISTINCT FROM 'ACEPTADA' THEN
        RAISE EXCEPTION 'RF-07: la ficha % esta en derivacion %; solo se factura con derivacion ACEPTADA',
            NEW.ficha_id, COALESCE(v_est::TEXT, 'INEXISTENTE');
    END IF;
    UPDATE dentrix.ficha_clinica
       SET derivado_a_caja = TRUE,
           fecha_derivacion = COALESCE(fecha_derivacion, CURRENT_TIMESTAMP)
     WHERE id = NEW.ficha_id;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION dentrix.pago_abono_valido() RETURNS TRIGGER AS $$
DECLARE v_origen_de_origen BIGINT;
BEGIN
    SELECT pago_origen_id INTO v_origen_de_origen FROM dentrix.pago WHERE id = NEW.pago_origen_id;
    IF v_origen_de_origen IS NOT NULL THEN
        RAISE EXCEPTION 'RF-08: el abono % apunta a otro abono; debe apuntar a la boleta', NEW.pago_origen_id;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION dentrix.abono_actualiza_saldo() RETURNS TRIGGER AS $$
DECLARE v_monto NUMERIC(10,2); v_abonos NUMERIC(10,2);
BEGIN
    SELECT monto INTO v_monto FROM dentrix.pago WHERE id = NEW.pago_origen_id;
    SELECT COALESCE(SUM(monto), 0) INTO v_abonos FROM dentrix.pago WHERE pago_origen_id = NEW.pago_origen_id;
    UPDATE dentrix.pago
       SET saldo_deudor = GREATEST(v_monto - v_abonos, 0),
           estado_pago = CASE WHEN v_abonos >= v_monto THEN 'PAGADO'::dentrix.estado_pago
                              ELSE 'PARCIAL'::dentrix.estado_pago END
     WHERE id = NEW.pago_origen_id;
    RETURN NULL;
END $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION dentrix.plan_recalcular_pago() RETURNS TRIGGER AS $$
DECLARE
    v_pago_id BIGINT;
    v_monto   NUMERIC(10,2);
    v_abonos  NUMERIC(10,2);
    v_igv     NUMERIC(4,2);
BEGIN
    v_pago_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.pago_id ELSE NEW.pago_id END;
    IF v_pago_id IS NULL THEN RETURN NULL; END IF;
    SELECT COALESCE(SUM(subtotal), 0) INTO v_monto FROM dentrix.plan_tratamiento WHERE pago_id = v_pago_id;
    SELECT COALESCE(SUM(monto), 0) INTO v_abonos FROM dentrix.pago WHERE pago_origen_id = v_pago_id;
    SELECT igv_porcentaje INTO v_igv FROM dentrix.configuracion WHERE id = 1;
    UPDATE dentrix.pago
       SET monto = v_monto,
           subtotal = ROUND(v_monto / (1 + v_igv / 100), 2),
           igv = v_monto - ROUND(v_monto / (1 + v_igv / 100), 2),
           saldo_deudor = GREATEST(v_monto - v_abonos, 0),
           estado_pago = CASE WHEN v_abonos = 0 THEN 'PENDIENTE'::dentrix.estado_pago
                              WHEN v_abonos >= v_monto THEN 'PAGADO'::dentrix.estado_pago
                              ELSE 'PARCIAL'::dentrix.estado_pago END
     WHERE id = v_pago_id;
    RETURN NULL;
END $$ LANGUAGE plpgsql;

-- cierre_historial_fondo() no referencia tablas: no requiere cambios.
