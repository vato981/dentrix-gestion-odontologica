-- ============================================================
-- DENTRIX DENTAL · Datos de prueba (semilla)
-- Reproduce el "día operativo" de las láminas: lunes 28/09/2026
-- Coherencia verificada con M2 (5 citas / saldos S/650),
-- M7 (boleta B001-000458 = S/150) y M7.1 (gaveta S/1,450 = 200+1,250;
-- billeteras S/980 en 4 transferencias; total sistema S/2,230).
-- Ejecutar DESPUÉS de 01_schema.sql
-- ============================================================
SET search_path TO dentrix;

-- M9.3 · Clínica y consultorios ------------------------------------
INSERT INTO clinica_config (id, razon_social, ruc, direccion, telefono, email,
        representante_legal, medico_director, licencia_sanitaria, licencia_municipal)
VALUES (1, 'Dentrix Dental — Centro Odontológico', '20608945123',
        'Av. Huancavelica N° 450, Chilca - Huancayo', '064 215880', 'contacto@dentrixdental.pe',
        'Dr. Juan Vallejos', 'Dr. Juan Vallejos', 'MINSAL-2024-4412', 'MUNI-HYO-8890');

INSERT INTO consultorio (codigo, nombre, especialidad) VALUES
    ('C01', 'Consultorio 01', 'Ortodoncia / Cirugía'),
    ('C02', 'Consultorio 02', 'Endodoncia / Operatoria');

-- M9.1 · Roles (matriz RBAC) y usuarios ----------------------------
INSERT INTO rol (codigo, nombre, descripcion) VALUES
    ('ADMINISTRADOR', 'Administrador', 'Acceso total: reportes, configuración, caja y clínica'),
    ('ODONTOLOGO',    'Odontólogo',    'Ficha clínica, agenda y triaje; sin caja ni configuración'),
    ('RECEPCION',     'Recepción / Caja', 'Pacientes, agenda, triaje y caja; sin reportes ni configuración');

-- password_hash: en producción bcrypt real (RNF-01); semilla con placeholder
INSERT INTO usuario (username, password_hash, nombres, apellidos, dni, rol_id) VALUES
    ('jvallejos', '$2a$10$DEMO.hash.admin0000000000000000000000000000000000000', 'Juan',    'Vallejos', '45123698', (SELECT id FROM rol WHERE codigo='ADMINISTRADOR')),
    ('jperez',    '$2a$10$DEMO.hash.jperez00000000000000000000000000000000000', 'Jesica',  'Perez',    '47896321', (SELECT id FROM rol WHERE codigo='ODONTOLOGO')),
    ('ccampos',   '$2a$10$DEMO.hash.ccampos0000000000000000000000000000000000', 'Cristina','Campos',   '46215879', (SELECT id FROM rol WHERE codigo='RECEPCION'));

-- M9.1/M8 · Odontólogos (Juan también es usuario Administrador) ----
INSERT INTO odontologo (usuario_id, dni, nombres, apellidos, colegiatura, especialidad, consultorio_id) VALUES
    ((SELECT id FROM usuario WHERE username='jvallejos'), '45123698', 'Juan',   'Vallejos', 'COP 45872-CP', 'Ortodoncia / Cirugía',   (SELECT id FROM consultorio WHERE codigo='C01')),
    ((SELECT id FROM usuario WHERE username='jperez'),    '47896321', 'Jesica', 'Perez',    'COP 51203-CP', 'Endodoncia / Operatoria',(SELECT id FROM consultorio WHERE codigo='C02'));

-- M9.2.2 · Catálogo CIE-10 (capítulo K, norma MINSA) ---------------
INSERT INTO cie10 (codigo, descripcion, capitulo, es_frecuente) VALUES
    ('K02.0', 'Caries del esmalte',                     'K02 Caries dental',     TRUE),
    ('K02.1', 'Caries de la dentina',                   'K02 Caries dental',     TRUE),
    ('K04.0', 'Pulpitis aguda / crónica',               'K04 Pulpar / Periapical', TRUE),
    ('K05.1', 'Gingivitis marginal crónica',            'K05 Periodontal',       TRUE),
    ('K05.3', 'Periodontitis crónica',                  'K05 Periodontal',       FALSE),
    ('Z01.2', 'Examen odontológico especial',         'Z01 Exámenes',          FALSE);

-- M9.2.1 · Tarifario (precios exactos de la lámina) -----------------
INSERT INTO procedimiento (codigo, nombre, especialidad, calculo, precio) VALUES
    ('TRAT-01', 'Curación Resina Oclusal',        'Operatoria',  'PIEZA',  80.00),
    ('TRAT-02', 'Profilaxis y Destartraje',       'Periodoncia', 'SESION', 70.00),
    ('TRAT-03', 'Control Mensual Ortodoncia',     'Ortodoncia',  'MENSUAL',100.00),
    ('TRAT-04', 'Exodoncia / Extracción Simple',  'Cirugía',     'PIEZA',  90.00),
    ('TRAT-05', 'Endodoncia Unirradicular',       'Endodoncia',  'PIEZA',  120.00);

-- M4 · Los 5 pacientes del padrón ----------------------------------
INSERT INTO paciente (dni, apellidos, nombres, fecha_nac, celular, email, direccion, alergias, usuario_registro_id) VALUES
    ('47895612', 'Pérez Alania',   'Juan Carlos', '1994-05-14', '954128745', 'juan.perez@gmail.com',
     'Av. Los Incas 450, Chilca - Huancayo', 'Hipertensión arterial controlada. Sin alergia a penicilina ni anestésicos.', (SELECT id FROM usuario WHERE username='ccampos')),
    ('70124589', 'Gómez Mendoza',  'María',       '1990-11-02', '987214563', 'maria.gomez@gmail.com',
     'Jr. Puno 120, El Tambo - Huancayo', NULL, (SELECT id FROM usuario WHERE username='ccampos')),
    ('19854236', 'Díaz Rojas',     'Pedro',       '1985-03-27', '987654321', 'pedro.diaz@gmail.com',
     'Psje. Las Begonias 34, Chilca - Huancayo', 'Alergia medicamentosa a sulfonamidas — confirmar en anamnesis (M4/M6).', (SELECT id FROM usuario WHERE username='ccampos')),
    ('45896321', 'Huamán Solís',   'Ana',         '1998-07-19', '990145678', 'ana.huaman@gmail.com',
     'Av. San Carlos 780, Huancayo', NULL, (SELECT id FROM usuario WHERE username='ccampos')),
    ('73658291', 'Quispe Huamán',  'Rosa',        '1979-01-30', '945612378', 'rosa.quispe@gmail.com',
     'Jr. Huallaga 56, El Tambo - Huancayo', NULL, (SELECT id FROM usuario WHERE username='ccampos'));

-- M6 · Historias clínicas (1:1) ------------------------------------
INSERT INTO historia_clinica (paciente_id, observaciones_generales)
SELECT id, NULL FROM paciente;

-- M6 · Odontograma de Juan (piezas FDI) ----------------------------
INSERT INTO odontograma_pieza (historia_id, pieza_fdi, estado, observacion)
SELECT hc.id, v.pieza, v.estado::estado_pieza, v.obs
  FROM historia_clinica hc
  JOIN paciente p ON p.id = hc.paciente_id
  CROSS JOIN (VALUES
      (17, 'CARIES',   'Caries oclusal detectada en control (K02.1)'),
      (27, 'OBTURADO', 'Resina previa en buen estado'),
      (36, 'AUSENTE',  'Exodoncia realizada en 2024'),
      (48, 'CARIES',   'Muela del juicio con caries distal — evaluar extracción')
  ) AS v(pieza, estado, obs)
 WHERE p.dni = '47895612';

-- M5 · Bloques de agenda (almuerzo y cierre, lunes) ----------------
INSERT INTO bloque_agenda (consultorio_id, dia_semana, hora_inicio, hora_fin, tipo, motivo) VALUES
    ((SELECT id FROM consultorio WHERE codigo='C01'), 1, '12:00', '13:00', 'ALMUERZO', 'Almuerzo del personal'),
    ((SELECT id FROM consultorio WHERE codigo='C02'), 1, '12:00', '13:00', 'ALMUERZO', 'Almuerzo del personal'),
    ((SELECT id FROM consultorio WHERE codigo='C01'), 1, '18:00', '20:00', 'CIERRE',   'Cierre de jornada'),
    ((SELECT id FROM consultorio WHERE codigo='C02'), 1, '18:00', '20:00', 'BLOQUEADO','Mantenimiento programado');

-- M2/M3/M5 · Las 5 citas del lunes 28/09/2026 ----------------------
INSERT INTO cita (paciente_id, odontologo_id, consultorio_id, fecha_cita, hora_inicio, hora_fin, procedimiento_planificado_id, estado) VALUES
    ((SELECT id FROM paciente WHERE dni='47895612'), (SELECT id FROM odontologo WHERE dni='45123698'), (SELECT id FROM consultorio WHERE codigo='C01'), '2026-09-28', '09:00', '09:45', (SELECT id FROM procedimiento WHERE codigo='TRAT-03'), 'EN_ATENCION'),
    ((SELECT id FROM paciente WHERE dni='73658291'), (SELECT id FROM odontologo WHERE dni='47896321'), (SELECT id FROM consultorio WHERE codigo='C02'), '2026-09-28', '09:00', '09:45', (SELECT id FROM procedimiento WHERE codigo='TRAT-02'), 'EN_SALA'),
    ((SELECT id FROM paciente WHERE dni='70124589'), (SELECT id FROM odontologo WHERE dni='47896321'), (SELECT id FROM consultorio WHERE codigo='C02'), '2026-09-28', '10:00', '10:45', (SELECT id FROM procedimiento WHERE codigo='TRAT-01'), 'EN_SALA'),
    ((SELECT id FROM paciente WHERE dni='19854236'), (SELECT id FROM odontologo WHERE dni='45123698'), (SELECT id FROM consultorio WHERE codigo='C01'), '2026-09-28', '11:15', '12:00', (SELECT id FROM procedimiento WHERE codigo='TRAT-04'), 'FALTO'),
    ((SELECT id FROM paciente WHERE dni='45896321'), (SELECT id FROM odontologo WHERE dni='47896321'), (SELECT id FROM consultorio WHERE codigo='C02'), '2026-09-28', '15:30', '16:15', NULL, 'PENDIENTE');

-- M6 · Atención de Juan (control de ortodoncia con derivación RF-07)
INSERT INTO atencion (historia_id, cita_id, odontologo_id, fecha, motivo_consulta, examen_clinico, plan_tratamiento, estado, derivado_a_caja, estado_derivacion, fecha_derivacion)
SELECT hc.id, c.id, o.id, '2026-09-28',
       'Control mensual de ortodoncia; dolor leve zona pieza 17',
       'Caries oclusal pieza 17 (K02.1). Encías inflamadas leves (K05.1). Ortho en curso, alambre inferior suelto.',
       'Curación resina pieza 17 + profilaxis. Control ortho en 30 días.',
       'DERIVADA', TRUE, 'ACEPTADA', TIMESTAMP '2026-09-28 10:15:00'
  FROM historia_clinica hc
  JOIN paciente p ON p.id = hc.paciente_id
  JOIN cita c ON c.paciente_id = p.id AND c.fecha_cita = '2026-09-28' AND c.hora_inicio = '09:00'
  JOIN odontologo o ON o.dni = '45123698'
 WHERE p.dni = '47895612';

INSERT INTO atencion_diagnostico (atencion_id, cie10_codigo, pieza_fdi, es_principal)
SELECT a.id, 'K02.1', 17, TRUE  FROM atencion a
 JOIN historia_clinica hc ON hc.id = a.historia_id
 JOIN paciente p ON p.id = hc.paciente_id WHERE p.dni = '47895612';
INSERT INTO atencion_diagnostico (atencion_id, cie10_codigo, pieza_fdi, es_principal)
SELECT a.id, 'K05.1', NULL, FALSE FROM atencion a
 JOIN historia_clinica hc ON hc.id = a.historia_id
 JOIN paciente p ON p.id = hc.paciente_id WHERE p.dni = '47895612';

-- Procedimientos clínicos realizados a Juan (alimenta M6/M8) --------
INSERT INTO atencion_procedimiento (atencion_id, procedimiento_id, pieza_fdi, cantidad, precio_unitario)
SELECT a.id, pr.id, 17, 1, pr.precio
  FROM atencion a
  JOIN historia_clinica hc ON hc.id = a.historia_id
  JOIN paciente p ON p.id = hc.paciente_id
  JOIN procedimiento pr ON pr.codigo = 'TRAT-01'
 WHERE p.dni = '47895612';
INSERT INTO atencion_procedimiento (atencion_id, procedimiento_id, pieza_fdi, cantidad, precio_unitario)
SELECT a.id, pr.id, NULL, 1, pr.precio
  FROM atencion a
  JOIN historia_clinica hc ON hc.id = a.historia_id
  JOIN paciente p ON p.id = hc.paciente_id
  JOIN procedimiento pr ON pr.codigo = 'TRAT-02'
 WHERE p.dni = '47895612';

UPDATE odontograma_pieza op
   SET ultima_atencion_id = a.id
  FROM atencion a
  JOIN historia_clinica hc ON hc.id = a.historia_id
  JOIN paciente p ON p.id = hc.paciente_id
 WHERE op.historia_id = hc.id AND op.pieza_fdi = 17 AND p.dni = '47895612';

-- M7.1 · Arqueo del día (Turno Abierto, cuadre exacto) -------------
INSERT INTO arqueo_caja (fecha, turno, cajero_id, fondo_inicial, efectivo_sistema, billeteras_sistema, total_sistema, efectivo_contado, diferencia, estado)
VALUES ('2026-09-28', 'DIURNO', (SELECT id FROM usuario WHERE username='ccampos'),
        200.00, 1250.00, 980.00, 2230.00, 1450.00, 0.00, 'ABIERTO');

-- RF-09 · Auditoría del fondo inicial (100 → 200 por RC) ------------
INSERT INTO fondo_inicial_historial (arqueo_id, monto, usuario_id, motivo, fecha) VALUES
    ((SELECT id FROM arqueo_caja WHERE fecha='2026-09-28'), 100.00, (SELECT id FROM usuario WHERE username='ccampos'),
     'Creado: fondo inicial de apertura (Sistema)', TIMESTAMP '2026-09-28 08:30:00'),
    ((SELECT id FROM arqueo_caja WHERE fecha='2026-09-28'), 200.00, (SELECT id FROM usuario WHERE username='ccampos'),
     'Modificado a S/ 200.00 por RC (Motivo: Ajuste sencillo de cambio)', TIMESTAMP '2026-09-28 09:10:00');

-- M7 · Comprobantes -----------------------------------------------
-- Saldos previos (los del KPI M2 "Saldos por Cobrar S/650 (2 pac.)")
INSERT INTO comprobante (numero, fecha, paciente_id, usuario_caja_id, modalidad, subtotal, igv, total)
SELECT 'B001-000440', '2026-09-15', p.id, (SELECT id FROM usuario WHERE username='ccampos'), 'EFECTIVO', 423.73, 76.27, 500.00
  FROM paciente p WHERE p.dni = '47895612';   -- Juan · pendiente S/500

INSERT INTO comprobante_detalle (comprobante_id, procedimiento_id, cantidad, precio_unitario)
SELECT c.id, pr.id, v.cant, pr.precio
  FROM comprobante c
  CROSS JOIN (VALUES ('TRAT-05', 2), ('TRAT-03', 1), ('TRAT-04', 1), ('TRAT-02', 1)) AS v(cod, cant)
  JOIN procedimiento pr ON pr.codigo = v.cod
 WHERE c.numero = 'B001-000440';

INSERT INTO comprobante (numero, fecha, paciente_id, usuario_caja_id, modalidad, subtotal, igv, total)
SELECT 'B001-000452', '2026-09-20', p.id, (SELECT id FROM usuario WHERE username='ccampos'), 'EFECTIVO', 135.59, 24.41, 160.00
  FROM paciente p WHERE p.dni = '19854236';   -- Pedro · total 160 con abono 10 → saldo 150

INSERT INTO comprobante_detalle (comprobante_id, procedimiento_id, cantidad, precio_unitario)
SELECT c.id, pr.id, 1, pr.precio
  FROM comprobante c
  CROSS JOIN (VALUES ('TRAT-02'), ('TRAT-04')) AS v(cod)
  JOIN procedimiento pr ON pr.codigo = v.cod
 WHERE c.numero = 'B001-000452';

INSERT INTO abono (comprobante_id, monto, fecha, modalidad, usuario_id)
SELECT c.id, 10.00, TIMESTAMP '2026-09-20 16:40:00', 'EFECTIVO', (SELECT id FROM usuario WHERE username='ccampos')
  FROM comprobante c WHERE c.numero = 'B001-000452';   -- trigger → estado PARCIAL, saldo 150

-- Billeteras digitales del 28/09 (4 transferencias = S/980, RF-08) --
INSERT INTO comprobante (numero, fecha, paciente_id, usuario_caja_id, arqueo_id, modalidad, operacion_ref, subtotal, igv, total)
SELECT 'B001-000453', '2026-09-28', p.id, (SELECT id FROM usuario WHERE username='ccampos'), (SELECT id FROM arqueo_caja WHERE fecha='2026-09-28'), 'YAPE', 'YP-450123', 161.02, 28.98, 190.00
  FROM paciente p WHERE p.dni = '70124589';
INSERT INTO comprobante_detalle (comprobante_id, procedimiento_id, cantidad, precio_unitario)
SELECT c.id, pr.id, 1, pr.precio FROM comprobante c
  CROSS JOIN (VALUES ('TRAT-05'), ('TRAT-02')) AS v(cod)
  JOIN procedimiento pr ON pr.codigo = v.cod WHERE c.numero = 'B001-000453';

INSERT INTO comprobante (numero, fecha, paciente_id, usuario_caja_id, arqueo_id, modalidad, operacion_ref, subtotal, igv, total)
SELECT 'B001-000454', '2026-09-28', p.id, (SELECT id FROM usuario WHERE username='ccampos'), (SELECT id FROM arqueo_caja WHERE fecha='2026-09-28'), 'PLIN', 'PL-88045', 254.24, 45.76, 300.00
  FROM paciente p WHERE p.dni = '73658291';
INSERT INTO comprobante_detalle (comprobante_id, procedimiento_id, cantidad, precio_unitario)
SELECT c.id, pr.id, 3, pr.precio FROM comprobante c
  JOIN procedimiento pr ON pr.codigo = 'TRAT-03' WHERE c.numero = 'B001-000454';

INSERT INTO comprobante (numero, fecha, paciente_id, usuario_caja_id, arqueo_id, modalidad, operacion_ref, subtotal, igv, total)
SELECT 'B001-000455', '2026-09-28', p.id, (SELECT id FROM usuario WHERE username='ccampos'), (SELECT id FROM arqueo_caja WHERE fecha='2026-09-28'), 'YAPE', 'YP-450199', 211.86, 38.14, 250.00
  FROM paciente p WHERE p.dni = '19854236';
INSERT INTO comprobante_detalle (comprobante_id, procedimiento_id, cantidad, precio_unitario)
SELECT c.id, pr.id, 1, pr.precio FROM comprobante c
  CROSS JOIN (VALUES ('TRAT-03'), ('TRAT-01'), ('TRAT-02')) AS v(cod)
  JOIN procedimiento pr ON pr.codigo = v.cod WHERE c.numero = 'B001-000455';

INSERT INTO comprobante (numero, fecha, paciente_id, usuario_caja_id, arqueo_id, modalidad, operacion_ref, subtotal, igv, total)
SELECT 'B001-000456', '2026-09-28', p.id, (SELECT id FROM usuario WHERE username='ccampos'), (SELECT id FROM arqueo_caja WHERE fecha='2026-09-28'), 'YAPE', 'YP-450201', 203.39, 36.61, 240.00
  FROM paciente p WHERE p.dni = '70124589';
INSERT INTO comprobante_detalle (comprobante_id, procedimiento_id, cantidad, precio_unitario)
SELECT c.id, pr.id, 2, pr.precio FROM comprobante c
  JOIN procedimiento pr ON pr.codigo = 'TRAT-05' WHERE c.numero = 'B001-000456';

-- Efectivo del 28/09: 457 (Rosa) + 458 (Juan, la boleta de M6/M7) + mostrador 459-468 = S/1,250
INSERT INTO comprobante (numero, fecha, paciente_id, usuario_caja_id, arqueo_id, modalidad, subtotal, igv, total)
SELECT 'B001-000457', '2026-09-28', p.id, (SELECT id FROM usuario WHERE username='ccampos'), (SELECT id FROM arqueo_caja WHERE fecha='2026-09-28'), 'EFECTIVO', 59.32, 10.68, 70.00
  FROM paciente p WHERE p.dni = '73658291';
INSERT INTO comprobante_detalle (comprobante_id, procedimiento_id, cantidad, precio_unitario)
SELECT c.id, pr.id, 1, pr.precio FROM comprobante c
  JOIN procedimiento pr ON pr.codigo = 'TRAT-02' WHERE c.numero = 'B001-000457';

-- B001-000458: la boleta que muestran M6 y M7 (atención de Juan, RF-07 ya aceptada)
INSERT INTO comprobante (numero, fecha, paciente_id, atencion_id, odontologo_id, usuario_caja_id, arqueo_id, modalidad, subtotal, igv, total)
SELECT 'B001-000458', '2026-09-28', p.id, a.id, o.id, (SELECT id FROM usuario WHERE username='ccampos'),
       (SELECT id FROM arqueo_caja WHERE fecha='2026-09-28'), 'EFECTIVO', 127.12, 22.88, 150.00
  FROM paciente p
  JOIN atencion a ON a.historia_id = (SELECT hc.id FROM historia_clinica hc JOIN paciente pp ON pp.id = hc.paciente_id WHERE pp.dni = '47895612')
  JOIN odontologo o ON o.dni = '45123698'
 WHERE p.dni = '47895612';
INSERT INTO comprobante_detalle (comprobante_id, procedimiento_id, cantidad, precio_unitario)
SELECT c.id, pr.id, 1, pr.precio FROM comprobante c
  CROSS JOIN (VALUES ('TRAT-01'), ('TRAT-02')) AS v(cod)
  JOIN procedimiento pr ON pr.codigo = v.cod WHERE c.numero = 'B001-000458';

-- Mostrador en efectivo del día (S/1,030 adicionales) ---------------
INSERT INTO comprobante (numero, fecha, paciente_id, usuario_caja_id, arqueo_id, modalidad, subtotal, igv, total)
SELECT v.num, '2026-09-28', p.id, (SELECT id FROM usuario WHERE username='ccampos'), (SELECT id FROM arqueo_caja WHERE fecha='2026-09-28'), 'EFECTIVO', v.sub, v.igv, v.tot
  FROM (VALUES
      ('B001-000459', '70124589', 101.69, 18.31, 120.00),
      ('B001-000460', '73658291',  84.75, 15.25, 100.00),
      ('B001-000461', '19854236',  76.27, 13.73,  90.00),
      ('B001-000462', '70124589', 135.59, 24.41, 160.00),
      ('B001-000463', '73658291',  59.32, 10.68,  70.00),
      ('B001-000464', '19854236',  84.75, 15.25, 100.00),
      ('B001-000465', '70124589', 101.69, 18.31, 120.00),
      ('B001-000466', '73658291',  76.27, 13.73,  90.00),
      ('B001-000467', '19854236',  67.80, 12.20,  80.00),
      ('B001-000468', '70124589',  84.75, 15.25, 100.00)
  ) AS v(num, dni, sub, igv, tot)
  JOIN paciente p ON p.dni = v.dni;

INSERT INTO comprobante_detalle (comprobante_id, procedimiento_id, cantidad, precio_unitario)
SELECT c.id, pr.id, v.cant, pr.precio
  FROM comprobante c
  JOIN (VALUES
      ('B001-000459', 'TRAT-05', 1), ('B001-000460', 'TRAT-03', 1), ('B001-000461', 'TRAT-04', 1),
      ('B001-000462', 'TRAT-01', 2), ('B001-000463', 'TRAT-02', 1), ('B001-000464', 'TRAT-03', 1),
      ('B001-000465', 'TRAT-05', 1), ('B001-000466', 'TRAT-04', 1), ('B001-000467', 'TRAT-01', 1),
      ('B001-000468', 'TRAT-03', 1)
  ) AS v(num, cod, cant) ON v.num = c.numero
  JOIN procedimiento pr ON pr.codigo = v.cod;

-- Los comprobantes cobrados el 28/09 quedan PAGADOS ----------------
UPDATE comprobante SET estado_pago = 'PAGADO', saldo_pendiente = 0
 WHERE fecha = '2026-09-28' AND estado = 'EMITIDO';

-- M9.3 · Bitácora de backups (estados de la lámina) ----------------
INSERT INTO backup_log (fecha, archivo, tamano_mb, estado, usuario_id, notas) VALUES
    (TIMESTAMP '2026-09-25 19:00:12', 'dentrix_20260925.sql', NULL, 'FALLIDO',  (SELECT id FROM usuario WHERE username='jvallejos'), 'Espacio insuficiente en disco; reintento manual al día siguiente'),
    (TIMESTAMP '2026-09-26 19:02:40', 'dentrix_20260926.sql', 4.10, 'EXITOSO', (SELECT id FROM usuario WHERE username='jvallejos'), 'Backup programado diario 19:00'),
    (TIMESTAMP '2026-09-27 19:01:05', 'dentrix_20260927.sql', 4.20, 'VERIFICADO', (SELECT id FROM usuario WHERE username='jvallejos'), 'Verificación de integridad OK (RNF-05)');

-- Fin de la semilla
