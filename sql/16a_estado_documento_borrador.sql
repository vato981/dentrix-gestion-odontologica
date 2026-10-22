-- ============================================================
-- DENTRIX DENTAL · Esquema v3 · COMPROBANTE EN BORRADOR (paso 1)
-- Archivo ADITIVO: se ejecuta DESPUÉS de 15_schema_v3_agenda.sql
--   psql -U postgres -d dentrix -f sql/16a_estado_documento_borrador.sql
--
-- Se separa del resto porque PostgreSQL no permite usar un valor nuevo de enum
-- en la misma transacción en que se agrega. Este archivo solo agrega el valor.
-- ============================================================
SET search_path TO dentrix;

ALTER TYPE estado_documento ADD VALUE IF NOT EXISTS 'BORRADOR';
