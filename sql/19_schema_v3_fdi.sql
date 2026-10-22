SET search_path TO dentrix;

ALTER TABLE plan_tratamiento DROP CONSTRAINT IF EXISTS plan_fdi_chk;
ALTER TABLE plan_tratamiento ADD CONSTRAINT plan_fdi_chk CHECK (
    pieza_fdi IS NULL
    OR (pieza_fdi BETWEEN 11 AND 48 AND pieza_fdi % 10 BETWEEN 1 AND 8)
    OR (pieza_fdi BETWEEN 51 AND 85 AND pieza_fdi % 10 BETWEEN 1 AND 5)
);
