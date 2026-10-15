-- ============================================================
-- DENTRIX DENTAL · Datos de prueba v2 (14 tablas)
-- Reproduce el mismo "día operativo" de la semilla v1:
-- lunes 28/09/2026 · 5 citas · boleta B001-000458 = S/150
-- gaveta S/1,450 (200 + 1,250) · billeteras S/980 · total S/2,230
-- Ejecutar DESPUÉS de sql/01_schema_v2.sql
-- ============================================================
SET search_path TO dentrix;

-- ------------------------------------------------------------
-- M9.3 · Clínica y consultorios
-- ------------------------------------------------------------
INSERT INTO configuracion (id, razon_social, ruc, direccion, telefono, email,
        representante_legal, medico_director, licencia_sanitaria, licencia_municipal)
VALUES (1, 'Dentrix Dental — Centro Odontológico', '20608945123',
        'Av. Huancavelica N° 450, Chilca - Huancayo', '064 215880', 'contacto@dentrixdental.pe',
        'Dr. Juan Vallejos', 'Dr. Juan Vallejos', 'MINSAL-2024-4412', 'MUNI-HYO-8890');

INSERT INTO consultorio (codigo, nombre, especialidad) VALUES
    ('C01', 'Consultorio 01', 'Ortodoncia / Cirugía'),
    ('C02', 'Consultorio 02', 'Endodoncia / Operatoria');

-- ------------------------------------------------------------
-- M9.1 · Roles (RBAC) y usuarios (odontólogos absorbidos)
-- ------------------------------------------------------------
INSERT INTO rol (codigo, nombre, descripcion) VALUES
    ('ADMINISTRADOR', 'Administrador', 'Acceso total: reportes, configuración, caja y clínica'),
    ('ODONTOLOGO',    'Odontólogo',    'Ficha clínica, agenda y triaje; sin caja ni configuración'),
    ('RECEPCION',     'Recepción / Caja', 'Pacientes, agenda, triaje y caja; sin reportes ni configuración');

-- password_hash: bcrypt real generado con PasswordUtil (RNF-01)
-- Claves demo: jvallejos=Admin2026*  jperez=Odonto2026*  ccampos=Caja2026*
INSERT INTO usuario (username, password_hash, nombres, apellidos, dni, rol_id, colegiatura, especialidad, consultorio_id) VALUES
    ('jvallejos', '$2a$10$nQziOS6lBcwPmsgY3lHGuexa.zQjijHikdAI0JVlXKf9wQ9k1/5tm', 'Juan',    'Vallejos', '45123698',
        (SELECT id FROM rol WHERE codigo='ADMINISTRADOR'), 'COP 45872-CP', 'Ortodoncia / Cirugía',
        (SELECT id FROM consultorio WHERE codigo='C01')),
    ('jperez',    '$2a$10$uC9SyJ6kHz8o2nxv.DE4R.Anoijnz8LHgkytppYKPoj.duXziPNvG', 'Jesica',  'Perez',    '47896321',
        (SELECT id FROM rol WHERE codigo='ODONTOLOGO'),    'COP 51203-CP', 'Endodoncia / Operatoria',
        (SELECT id FROM consultorio WHERE codigo='C02')),
    ('ccampos',   '$2a$10$EoZ67m7EOjy47Z4tp5CRYuVdratsg6tvhRGw7FE1LvTKiBVGkRxpq', 'Cristina','Campos',   '46215879',
        (SELECT id FROM rol WHERE codigo='RECEPCION'),     NULL, NULL, NULL);

-- ------------------------------------------------------------
-- M4 · Los 5 pacientes del padrón (expediente 1:1)
-- ------------------------------------------------------------
INSERT INTO paciente (codigo_hc, dni, apellidos, nombres, fecha_nac, celular, email, direccion, alergias, usuario_registro_id) VALUES
    ('HC-000001', '47895612', 'Pérez Alania',  'Juan Carlos', '1994-05-14', '954128745', 'juan.perez@gmail.com',
     'Av. Los Incas 450, Chilca - Huancayo', 'Hipertensión arterial controlada. Sin alergia a penicilina ni anestésicos.', (SELECT id FROM usuario WHERE username='ccampos')),
    ('HC-000002', '70124589', 'Gómez Mendoza', 'María',       '1990-11-02', '987214563', 'maria.gomez@gmail.com',
     'Jr. Puno 120, El Tambo - Huancayo', NULL, (SELECT id FROM usuario WHERE username='ccampos')),
    ('HC-000003', '19854236', 'Díaz Rojas',    'Pedro',       '1985-03-27', '987654321', 'pedro.diaz@gmail.com',
     'Psje. Las Begonias 34, Chilca - Huancayo', 'Alergia medicamentosa a sulfonamidas — confirmar en anamnesis (M4/M6).', (SELECT id FROM usuario WHERE username='ccampos')),
    ('HC-000004', '45896321', 'Huamán Solís',  'Ana',         '1998-07-19', '990145678', 'ana.huaman@gmail.com',
     'Av. San Carlos 780, Huancayo', NULL, (SELECT id FROM usuario WHERE username='ccampos')),
    ('HC-000005', '73658291', 'Quispe Huamán', 'Rosa',        '1979-01-30', '945612378', 'rosa.quispe@gmail.com',
     'Jr. Huallaga 56, El Tambo - Huancayo', NULL, (SELECT id FROM usuario WHERE username='ccampos'));

-- ------------------------------------------------------------
-- M9.2.2 · Catálogo CIE-10 (capítulo K, norma MINSA, RF-13)
-- ------------------------------------------------------------
INSERT INTO diagnostico_cie10 (codigo, descripcion, capitulo, es_frecuente) VALUES
    ('K02.0', 'Caries del esmalte',              'K02 Caries dental',     TRUE),
    ('K02.1', 'Caries de la dentina',            'K02 Caries dental',     TRUE),
    ('K04.0', 'Pulpitis aguda / crónica',        'K04 Pulpar / Periapical', TRUE),
    ('K05.1', 'Gingivitis marginal crónica',     'K05 Periodontal',       TRUE),
    ('K05.3', 'Periodontitis crónica',           'K05 Periodontal',       FALSE),
    ('Z01.2', 'Examen odontológico especial',    'Z01 Exámenes',          FALSE);

-- ------------------------------------------------------------
-- M9.2.1 · Tarifario (precios de la lámina, RF-12)
-- ------------------------------------------------------------
INSERT INTO tratamiento (codigo, nombre, especialidad, calculo, precio) VALUES
    ('TRAT-01', 'Curación Resina Oclusal',       'Operatoria',  'PIEZA',  80.00),
    ('TRAT-02', 'Profilaxis y Destartraje',      'Periodoncia', 'SESION', 70.00),
    ('TRAT-03', 'Control Mensual Ortodoncia',    'Ortodoncia',  'MENSUAL',100.00),
    ('TRAT-04', 'Exodoncia / Extracción Simple', 'Cirugía',     'PIEZA',  90.00),
    ('TRAT-05', 'Endodoncia Unirradicular',      'Endodoncia',  'PIEZA',  120.00);

-- ------------------------------------------------------------
-- M5 · Bloques de agenda y las 5 citas del lunes 28/09/2026
--      (bloques como filas de tipo ALMUERZO/CIERRE/BLOQUEADO, RF-05)
-- ------------------------------------------------------------
INSERT INTO cita (tipo, paciente_id, usuario_id, consultorio_id, fecha_cita, hora_inicio, hora_fin, estado, motivo) VALUES
    ('ALMUERZO',  NULL, (SELECT id FROM usuario WHERE username='jvallejos'), (SELECT id FROM consultorio WHERE codigo='C01'), '2026-09-28', '12:00', '13:00', 'BLOQUEADA', 'Almuerzo del personal'),
    ('ALMUERZO',  NULL, (SELECT id FROM usuario WHERE username='jperez'),    (SELECT id FROM consultorio WHERE codigo='C02'), '2026-09-28', '12:00', '13:00', 'BLOQUEADA', 'Almuerzo del personal'),
    ('CIERRE',    NULL, (SELECT id FROM usuario WHERE username='jvallejos'), (SELECT id FROM consultorio WHERE codigo='C01'), '2026-09-28', '18:00', '20:00', 'BLOQUEADA', 'Cierre de jornada'),
    ('BLOQUEADO', NULL, (SELECT id FROM usuario WHERE username='jperez'),    (SELECT id FROM consultorio WHERE codigo='C02'), '2026-09-28', '18:00', '20:00', 'BLOQUEADA', 'Mantenimiento programado');

INSERT INTO cita (tipo, paciente_id, usuario_id, consultorio_id, fecha_cita, hora_inicio, hora_fin, tratamiento_id, estado) VALUES
    ('CITA', (SELECT id FROM paciente WHERE dni='47895612'), (SELECT id FROM usuario WHERE username='jvallejos'), (SELECT id FROM consultorio WHERE codigo='C01'), '2026-09-28', '09:00', '09:45', (SELECT id FROM tratamiento WHERE codigo='TRAT-03'), 'EN_ATENCION'),
    ('CITA', (SELECT id FROM paciente WHERE dni='73658291'), (SELECT id FROM usuario WHERE username='jperez'),    (SELECT id FROM consultorio WHERE codigo='C02'), '2026-09-28', '09:00', '09:45', (SELECT id FROM tratamiento WHERE codigo='TRAT-02'), 'EN_SALA'),
    ('CITA', (SELECT id FROM paciente WHERE dni='70124589'), (SELECT id FROM usuario WHERE username='jperez'),    (SELECT id FROM consultorio WHERE codigo='C02'), '2026-09-28', '10:00', '10:45', (SELECT id FROM tratamiento WHERE codigo='TRAT-01'), 'EN_SALA'),
    ('CITA', (SELECT id FROM paciente WHERE dni='19854236'), (SELECT id FROM usuario WHERE username='jvallejos'), (SELECT id FROM consultorio WHERE codigo='C01'), '2026-09-28', '11:15', '12:00', (SELECT id FROM tratamiento WHERE codigo='TRAT-04'), 'FALTO'),
    ('CITA', (SELECT id FROM paciente WHERE dni='45896321'), (SELECT id FROM usuario WHERE username='jperez'),    (SELECT id FROM consultorio WHERE codigo='C02'), '2026-09-28', '15:30', '16:15', NULL, 'PENDIENTE');

-- ------------------------------------------------------------
-- M6 · Ficha clínica de Juan (derivación ACEPTADA, RF-07)
--      RF-06: 2 diagnósticos, uno principal (K02.1 pieza 17)
-- ------------------------------------------------------------
INSERT INTO ficha_clinica (paciente_id, cita_id, odontologo_id, fecha, motivo_consulta, examen_clinico, plan_indicaciones,
        estado_derivacion, derivado_a_caja, fecha_derivacion)
SELECT p.id, c.id, (SELECT id FROM usuario WHERE username='jvallejos'), TIMESTAMP '2026-09-28 09:05:00',
       'Control mensual de ortodoncia; dolor leve zona pieza 17',
       'Caries oclusal pieza 17 (K02.1). Encías inflamadas leves (K05.1). Ortho en curso, alambre inferior suelto.',
       'Curación resina pieza 17 + profilaxis. Control ortho en 30 días.',
       'ACEPTADA', TRUE, TIMESTAMP '2026-09-28 10:15:00'
  FROM paciente p
  JOIN cita c ON c.paciente_id = p.id AND c.fecha_cita = '2026-09-28' AND c.hora_inicio = '09:00'
 WHERE p.dni = '47895612';

INSERT INTO ficha_diagnostico (ficha_id, cie10_codigo, pieza_fdi, es_principal, observacion)
SELECT f.id, 'K02.1', 17, TRUE, 'Caries oclusal confirmada'
  FROM ficha_clinica f JOIN paciente p ON p.id = f.paciente_id WHERE p.dni = '47895612';

INSERT INTO ficha_diagnostico (ficha_id, cie10_codigo, pieza_fdi, es_principal, observacion)
SELECT f.id, 'K05.1', NULL, FALSE, 'Gingivitis generalizada'
  FROM ficha_clinica f JOIN paciente p ON p.id = f.paciente_id WHERE p.dni = '47895612';

-- ------------------------------------------------------------
-- M6 · Odontograma de Juan (piezas FDI)
-- ------------------------------------------------------------
INSERT INTO hallazgo_dental (paciente_id, pieza_fdi, estado, observacion)
SELECT p.id, v.pieza, v.estado::estado_pieza, v.obs
  FROM paciente p
  CROSS JOIN (VALUES
      (17, 'CARIES',   'Caries oclusal detectada en control (K02.1)'),
      (27, 'OBTURADO', 'Resina previa en buen estado'),
      (36, 'AUSENTE',  'Exodoncia realizada en 2024'),
      (48, 'CARIES',   'Muela del juicio con caries distal — evaluar extracción')
  ) AS v(pieza, estado, obs)
 WHERE p.dni = '47895612';

-- ------------------------------------------------------------
-- M7.1 · Cierre de caja del día (fondo S/200, RF-09)
-- ------------------------------------------------------------
INSERT INTO cierre_caja (fecha, turno, cajero_id, fondo_inicial, historial_fondos, efectivo_sistema, billeteras_sistema, total_sistema, efectivo_contado, estado)
VALUES ('2026-09-28', 'DIURNO', (SELECT id FROM usuario WHERE username='ccampos'),
        200.00,
        '[{"monto_anterior": 100.00, "monto_nuevo": 200.00, "motivo": "Modificado a S/ 200.00 por RC (Ajuste de cambio)", "fecha": "2026-09-28T09:10:00"}]'::jsonb,
        1250.00, 980.00, 2230.00, 1450.00, 'ABIERTO');

-- ------------------------------------------------------------
-- M7 · Comprobantes del "día operativo" (pago) y sus líneas (plan_tratamiento)
--      El trigger recalcula monto/IGV/saldo a partir de las líneas.
-- ------------------------------------------------------------
-- Saldos previos del KPI M2 "Saldos por Cobrar S/650 (2 pac.)"
INSERT INTO pago (numero, fecha, paciente_id, usuario_caja_id, modalidad)
SELECT 'B001-000440', TIMESTAMP '2026-09-15 11:00:00', p.id, (SELECT id FROM usuario WHERE username='ccampos'), 'EFECTIVO'
  FROM paciente p WHERE p.dni = '47895612';   -- Juan · pendiente S/500

INSERT INTO plan_tratamiento (paciente_id, tratamiento_id, pago_id, pieza_fdi, cantidad, precio_unitario, estado)
SELECT p.id, t.id, pg.id, v.pieza, v.cant, t.precio, 'COBRADO'
  FROM pago pg
  JOIN paciente p ON p.id = pg.paciente_id AND p.dni = '47895612'
  CROSS JOIN (VALUES ('TRAT-05', 1, 36), ('TRAT-05', 1, 48), ('TRAT-03', 1, NULL), ('TRAT-04', 1, NULL), ('TRAT-02', 1, NULL)) AS v(cod, cant, pieza)
  JOIN tratamiento t ON t.codigo = v.cod
 WHERE pg.numero = 'B001-000440';

-- B001-000452: total S/160 con abono de S/10 → saldo S/150 (RF-08)
INSERT INTO pago (numero, fecha, paciente_id, usuario_caja_id, modalidad)
SELECT 'B001-000452', TIMESTAMP '2026-09-20 10:30:00', p.id, (SELECT id FROM usuario WHERE username='ccampos'), 'EFECTIVO'
  FROM paciente p WHERE p.dni = '19854236';

INSERT INTO plan_tratamiento (paciente_id, tratamiento_id, pago_id, cantidad, precio_unitario, estado)
SELECT p.id, t.id, pg.id, 1, t.precio, 'COBRADO'
  FROM pago pg
  JOIN paciente p ON p.id = pg.paciente_id AND p.dni = '19854236'
  CROSS JOIN (VALUES ('TRAT-02'), ('TRAT-04')) AS v(cod)
  JOIN tratamiento t ON t.codigo = v.cod
 WHERE pg.numero = 'B001-000452';

INSERT INTO pago (pago_origen_id, fecha, paciente_id, usuario_caja_id, modalidad, monto)
SELECT pg.id, TIMESTAMP '2026-09-20 16:40:00', pg.paciente_id, (SELECT id FROM usuario WHERE username='ccampos'), 'EFECTIVO', 10.00
  FROM pago pg WHERE pg.numero = 'B001-000452';   -- trigger → estado PARCIAL, saldo 150

-- Billeteras digitales del 28/09 (4 transferencias = S/980, RF-08)
INSERT INTO pago (numero, fecha, paciente_id, usuario_caja_id, cierre_id, modalidad, operacion_ref)
SELECT v.num, TIMESTAMP '2026-09-28 10:00:00', p.id, (SELECT id FROM usuario WHERE username='ccampos'),
       (SELECT id FROM cierre_caja WHERE fecha='2026-09-28'), v.mod::modalidad_pago, v.op
  FROM (VALUES
      ('B001-000453', '70124589', 'YAPE', 'YP-450123'),
      ('B001-000454', '73658291', 'PLIN', 'PL-88045'),
      ('B001-000455', '19854236', 'YAPE', 'YP-450199'),
      ('B001-000456', '70124589', 'YAPE', 'YP-450201')
  ) AS v(num, dni, mod, op)
  JOIN paciente p ON p.dni = v.dni;

INSERT INTO plan_tratamiento (paciente_id, tratamiento_id, pago_id, pieza_fdi, cantidad, precio_unitario, estado)
SELECT p.id, t.id, pg.id, v.pieza, 1, t.precio, 'COBRADO'
  FROM pago pg
  JOIN (VALUES
      ('B001-000453', 'TRAT-05', 36), ('B001-000453', 'TRAT-02', NULL),
      ('B001-000454', 'TRAT-03', NULL),
      ('B001-000455', 'TRAT-03', NULL), ('B001-000455', 'TRAT-01', 26), ('B001-000455', 'TRAT-02', NULL),
      ('B001-000456', 'TRAT-05', 16), ('B001-000456', 'TRAT-05', 26)
  ) AS v(num, cod, pieza) ON v.num = pg.numero
  JOIN paciente p ON p.id = pg.paciente_id
  JOIN tratamiento t ON t.codigo = v.cod;

-- B001-000457: efectivo (Rosa)
INSERT INTO pago (numero, fecha, paciente_id, usuario_caja_id, cierre_id, modalidad)
SELECT 'B001-000457', TIMESTAMP '2026-09-28 11:40:00', p.id, (SELECT id FROM usuario WHERE username='ccampos'),
       (SELECT id FROM cierre_caja WHERE fecha='2026-09-28'), 'EFECTIVO'
  FROM paciente p WHERE p.dni = '73658291';

INSERT INTO plan_tratamiento (paciente_id, tratamiento_id, pago_id, cantidad, precio_unitario, estado)
SELECT p.id, t.id, pg.id, 1, t.precio, 'COBRADO'
  FROM pago pg JOIN paciente p ON p.id = pg.paciente_id AND p.dni = '73658291'
  JOIN tratamiento t ON t.codigo = 'TRAT-02'
 WHERE pg.numero = 'B001-000457';

-- B001-000458: la boleta de M6/M7 (atención de Juan, RF-07 ya ACEPTADA)
INSERT INTO pago (numero, fecha, paciente_id, ficha_id, odontologo_id, usuario_caja_id, cierre_id, modalidad)
SELECT 'B001-000458', TIMESTAMP '2026-09-28 10:30:00', p.id, f.id,
       (SELECT id FROM usuario WHERE username='jvallejos'), (SELECT id FROM usuario WHERE username='ccampos'),
       (SELECT id FROM cierre_caja WHERE fecha='2026-09-28'), 'EFECTIVO'
  FROM paciente p
  JOIN ficha_clinica f ON f.paciente_id = p.id
 WHERE p.dni = '47895612';

INSERT INTO plan_tratamiento (paciente_id, tratamiento_id, ficha_id, pago_id, pieza_fdi, cantidad, precio_unitario, estado)
SELECT p.id, t.id, f.id, pg.id, v.pieza, 1, t.precio, 'COBRADO'
  FROM pago pg
  JOIN paciente p ON p.id = pg.paciente_id AND p.dni = '47895612'
  JOIN ficha_clinica f ON f.paciente_id = p.id
  CROSS JOIN (VALUES ('TRAT-01', 17), ('TRAT-02', NULL)) AS v(cod, pieza)
  JOIN tratamiento t ON t.codigo = v.cod
 WHERE pg.numero = 'B001-000458';

-- Mostrador en efectivo del día (S/1,030 adicionales)
INSERT INTO pago (numero, fecha, paciente_id, usuario_caja_id, cierre_id, modalidad)
SELECT v.num, TIMESTAMP '2026-09-28 12:30:00', p.id, (SELECT id FROM usuario WHERE username='ccampos'),
       (SELECT id FROM cierre_caja WHERE fecha='2026-09-28'), 'EFECTIVO'
  FROM (VALUES
      ('B001-000459', '70124589'), ('B001-000460', '73658291'), ('B001-000461', '19854236'),
      ('B001-000462', '70124589'), ('B001-000463', '73658291'), ('B001-000464', '19854236'),
      ('B001-000465', '70124589'), ('B001-000466', '73658291'), ('B001-000467', '19854236'),
      ('B001-000468', '70124589')
  ) AS v(num, dni)
  JOIN paciente p ON p.dni = v.dni;

INSERT INTO plan_tratamiento (paciente_id, tratamiento_id, pago_id, pieza_fdi, cantidad, precio_unitario, estado)
SELECT p.id, t.id, pg.id, v.pieza, 1, t.precio, 'COBRADO'
  FROM pago pg
  JOIN (VALUES
      ('B001-000459', 'TRAT-05', 26), ('B001-000460', 'TRAT-03', NULL), ('B001-000461', 'TRAT-04', 18),
      ('B001-000462', 'TRAT-01', 27), ('B001-000462', 'TRAT-01', 37),
      ('B001-000463', 'TRAT-02', NULL), ('B001-000464', 'TRAT-03', NULL), ('B001-000465', 'TRAT-05', 46),
      ('B001-000466', 'TRAT-04', 38), ('B001-000467', 'TRAT-01', 47), ('B001-000468', 'TRAT-03', NULL)
  ) AS v(num, cod, pieza) ON v.num = pg.numero
  JOIN paciente p ON p.id = pg.paciente_id
  JOIN tratamiento t ON t.codigo = v.cod;

-- Los comprobantes cobrados el 28/09 se liquidan con un abono total
INSERT INTO pago (pago_origen_id, fecha, paciente_id, usuario_caja_id, modalidad, monto, operacion_ref)
SELECT pg.id, pg.fecha + interval '5 minutes', pg.paciente_id, pg.usuario_caja_id, pg.modalidad, pg.monto, pg.operacion_ref
  FROM pago pg
 WHERE pg.numero IN ('B001-000453','B001-000454','B001-000455','B001-000456','B001-000457',
                     'B001-000458','B001-000459','B001-000460','B001-000461','B001-000462',
                     'B001-000463','B001-000464','B001-000465','B001-000466','B001-000467','B001-000468');

-- ============================================================
-- VERIFICACIÓN DE LOS 6 CASOS DUROS (deben imprimir OK)
-- ============================================================
DO $$
DECLARE v_monto NUMERIC; v_saldo NUMERIC; v_est estado_pago; v_his JSONB; v_n INT; v_msg TEXT;
BEGIN
    -- Caso 1 · B001-000458 = S/150 en 2 líneas (TRAT-01 + TRAT-02)
    SELECT monto, saldo_deudor, estado_pago INTO v_monto, v_saldo, v_est FROM pago WHERE numero='B001-000458';
    IF v_monto = 150.00 AND v_est = 'PAGADO' AND v_saldo = 0 THEN
        RAISE NOTICE 'CASO 1 OK · B001-000458 = S/150 (2 lineas) y liquidada';
    ELSE
        RAISE EXCEPTION 'CASO 1 FALLA · monto=% saldo=% estado=%', v_monto, v_saldo, v_est;
    END IF;

    -- Caso 2 · B001-000452 con abono S/10 → estado PARCIAL y saldo S/150
    SELECT monto, saldo_deudor, estado_pago INTO v_monto, v_saldo, v_est FROM pago WHERE numero='B001-000452';
    IF v_monto = 160.00 AND v_saldo = 150.00 AND v_est = 'PARCIAL' THEN
        RAISE NOTICE 'CASO 2 OK · abono S/10 → saldo S/150 (PARCIAL)';
    ELSE
        RAISE EXCEPTION 'CASO 2 FALLA · monto=% saldo=% estado=%', v_monto, v_saldo, v_est;
    END IF;

    -- Caso 3 · RF-09: cambio de fondo registrado en historial_fondos
    SELECT historial_fondos INTO v_his FROM cierre_caja WHERE fecha='2026-09-28';
    IF jsonb_array_length(v_his) = 1 AND (v_his->0->>'monto_nuevo')::numeric = 200.00 THEN
        RAISE NOTICE 'CASO 3 OK · historial RF-09 con el cambio 100 → 200';
    ELSE
        RAISE EXCEPTION 'CASO 3 FALLA · historial=%', v_his;
    END IF;

    -- Caso 4 · RF-07: boletar una ficha PENDIENTE debe fallar
    INSERT INTO ficha_clinica (paciente_id, odontologo_id, fecha, motivo_consulta, examen_clinico, estado_derivacion)
    SELECT p.id, (SELECT id FROM usuario WHERE username='jperez'), TIMESTAMP '2026-09-28 10:20:00',
           'Dolor muela inferior', 'Caries profunda pieza 36 (K02.1). Sin derivar a caja todavía.', 'PENDIENTE'
      FROM paciente p WHERE p.dni='70124589';
    BEGIN
        INSERT INTO pago (numero, paciente_id, ficha_id, usuario_caja_id, modalidad)
        SELECT 'TEST-RF07', p.id, f.id, (SELECT id FROM usuario WHERE username='ccampos'), 'EFECTIVO'
          FROM paciente p JOIN ficha_clinica f ON f.paciente_id = p.id
         WHERE p.dni='70124589' AND f.estado_derivacion = 'PENDIENTE';
        RAISE EXCEPTION 'CASO 4 FALLA · dejo facturar una ficha sin derivacion aceptada';
    EXCEPTION WHEN raise_exception THEN
        IF SQLERRM LIKE 'CASO 4%' THEN RAISE; END IF;
        RAISE NOTICE 'CASO 4 OK · RF-07 bloqueo la boleta de una ficha PENDIENTE';
    END;

    -- Caso 5 · RF-08: Yape sin N° de operación debe fallar
    BEGIN
        INSERT INTO pago (numero, paciente_id, usuario_caja_id, modalidad)
        SELECT 'TEST-RF08', p.id, (SELECT id FROM usuario WHERE username='ccampos'), 'YAPE'
          FROM paciente p WHERE p.dni='70124589';
        RAISE EXCEPTION 'CASO 5 FALLA · dejo emitir Yape sin operacion_ref';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'CASO 5 OK · RF-08 exige N° de operación en Yape/Plin';
    END;

    -- Caso 6 · RF-06: dos diagnósticos principales en la misma ficha debe fallar
    BEGIN
        INSERT INTO ficha_diagnostico (ficha_id, cie10_codigo, es_principal)
        SELECT f.id, 'K05.3', TRUE
          FROM ficha_clinica f JOIN paciente p ON p.id=f.paciente_id WHERE p.dni='47895612';
        RAISE EXCEPTION 'CASO 6 FALLA · dejo dos diagnosticos principales';
    EXCEPTION WHEN raise_exception THEN
        IF SQLERRM LIKE 'CASO 6%' THEN RAISE; END IF;
        RAISE NOTICE 'CASO 6 OK · RF-06 admite varios CIE-10 pero solo uno principal';
    END;

    RAISE NOTICE '==== LOS 6 CASOS DUROS PASARON ===';
END $$;

-- ------------------------------------------------------------
-- Resumen final (los KPI que reproduce el M2/M7/M8)
-- ------------------------------------------------------------
SELECT 'Citas del 28/09 (5 reales + 4 bloques)' AS control,
       COUNT(*) FILTER (WHERE tipo='CITA') AS citas,
       COUNT(*) FILTER (WHERE tipo<>'CITA') AS bloques
  FROM cita WHERE fecha_cita='2026-09-28';

SELECT 'Caja 28/09' AS control, efectivo_sistema, billeteras_sistema, total_sistema, efectivo_contado, diferencia
  FROM cierre_caja WHERE fecha='2026-09-28';

SELECT 'Saldos por cobrar (KPI M2 = S/650 en 2 pac.)' AS control, paciente_id, saldo_total
  FROM v_saldos_paciente WHERE saldo_total > 0 ORDER BY paciente_id;

SELECT 'Ranking tratamientos (M8)' AS control, codigo, nombre, unidades_cobradas, monto_cobrado
  FROM v_ranking_tratamientos;

-- Fin de la semilla v2
