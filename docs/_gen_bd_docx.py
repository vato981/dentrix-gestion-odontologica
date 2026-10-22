# -*- coding: utf-8 -*-
"""Genera el documento Word 'Documentacion de la Base de Datos - Dentrix'.

Flujo del proyecto Generar_word: render_document (MCP word) + fix_hierarquia.py.
Este script llama directamente a la funcion render_document del paquete word_mcp.
Uso:  py _gen_bd_docx.py
"""
import json
import os
import sys

sys.path.insert(0, r"C:\Users\maick\word-mcp")
from word_mcp.server import render_document  # noqa: E402

BASE = r"C:\Users\maick\Documents\opencode_trabajos\UTP\Curso Integrador I - Sistemas Software\Proyecto Odontología Grupo"
DOCX = os.path.join(BASE, "Documentacion_BD_Dentrix.docx")
PNG_DER = os.path.join(BASE, "Programa", "docs", "der_dentrix.png")

W_DICT = [3.3, 3.1, 1.1, 4.6, 3.8]


def set_cell_margins(path, twips=45):
    """Reduce los margenes internos (izq/der) de todas las tablas via XML."""
    import re as _re
    import zipfile
    mar = ('<w:tblCellMar><w:top w:w="0" w:type="dxa"/>'
           '<w:left w:w="%d" w:type="dxa"/><w:bottom w:w="0" w:type="dxa"/>'
           '<w:right w:w="%d" w:type="dxa"/></w:tblCellMar>' % (twips, twips))
    tmp = path + ".tmp"
    n = 0
    with zipfile.ZipFile(path, "r") as zin, \
            zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            data = zin.read(item.filename)
            if item.filename == "word/document.xml":
                xml = data.decode("utf-8")
                xml, n = _re.subn(r'<w:tblLayout[^>]*/>',
                                  lambda m: m.group(0) + mar, xml)
                data = xml.encode("utf-8")
            zout.writestr(item, data)
    os.replace(tmp, path)
    return n

BLOCKS = []


def para(t, indent=True):
    BLOCKS.append({"type": "paragraph", "text": t, "align": "justify",
                   "indent_first_line": indent})


def head(t, level, page_break=False):
    b = {"type": "heading", "text": t, "level": level}
    if page_break:
        b["page_break_before"] = True
    BLOCKS.append(b)


def bullets(items):
    BLOCKS.append({"type": "bullets", "items": items})


def spacer(n=1):
    BLOCKS.append({"type": "spacer", "lines": n})


def tcap(n, desc):
    BLOCKS.append({"type": "caption", "text": "Tabla %d" % n, "kind": "table", "bold": True})
    BLOCKS.append({"type": "caption", "text": desc, "kind": "note", "bold": False})


def fcap(text):
    BLOCKS.append({"type": "caption", "text": text, "kind": "figure", "bold": False})


def note(text):
    BLOCKS.append({"type": "caption", "text": text, "kind": "note", "bold": False})


def table(rows, widths, header=True):
    BLOCKS.append({
        "type": "table", "data": rows, "header_row": header,
        "header_shading": "D9E2F3", "header_align": "center",
        "cell_align": "left", "col_widths_cm": widths, "align": "center",
    })


def dict_table(cols):
    rows = [["Columna", "Tipo de dato", "Nulo", "Restricciones", "Descripción"]]
    rows += [list(c) for c in cols]
    table(rows, W_DICT)


# =====================================================================
# 1. INTRODUCCIÓN
# =====================================================================
head("Introducción", 1)
para("El presente documento describe la estructura física y lógica de la base de datos del "
     "Sistema de Gestión de Atención Odontológica para el Centro Odontológico Dentrix. La "
     "documentación se elaboró a partir del script de creación 01_schema_v2.sql, que implementa "
     "el modelo de datos v2 aprobado por la docente del curso (13 entidades más 1 tabla de unión "
     "= 14 tablas), ejecutado y verificado sobre PostgreSQL 18 el 13 de octubre de 2026.")
para("El documento se organiza en ocho secciones: (1) esta introducción; (2) la descripción "
     "general del modelo de datos (tipos enumerados, tablas y convenciones); (3) el diagrama "
     "entidad-relación con sus cardinalidades; (4) el diccionario de datos completo, columna por "
     "columna, de las 14 tablas; (5) las vistas de reportes del Módulo 8; (6) los triggers y "
     "reglas de negocio embebidas; (7) los índices de apoyo; y (8) la verificación del modelo "
     "mediante casos de prueba duros y la coherencia con la semilla de datos.")

# =====================================================================
# 2. DESCRIPCIÓN GENERAL
# =====================================================================
head("Descripción general del modelo de datos", 1)
para("La base de datos dentrix está normalizada en tercera forma normal (3FN) y se compone de "
     "14 tablas relacionales, 11 tipos enumerados, 6 vistas de lectura para los indicadores del "
     "Módulo 8, 6 funciones de trigger con sus respectivos disparadores y 9 índices de apoyo. La "
     "clave primaria de cada entidad es un identificador sustituto (SERIAL o BIGSERIAL) o un "
     "código natural, según corresponda (por ejemplo, codigo en diagnostico_cie10). Las claves "
     "foráneas garantizan la integridad referencial y las reglas de negocio críticas (RF-06, "
     "RF-07, RF-08 y RF-09) se validan directamente en la base mediante triggers y restricciones "
     "CHECK, de modo que ningún cliente del sistema pueda dejarlas sin cumplir.")
para("Los montos se almacenan en columnas NUMERIC(10,2) o NUMERIC(12,2) y se interpretan con "
     "IGV incluido; las marcas de auditoría usan TIMESTAMP con valor por defecto "
     "CURRENT_TIMESTAMP; y los estados se controlan con tipos enumerados (ENUM) para impedir "
     "valores fuera de catálogo.")

head("Tipos enumerados", 2)
para("El esquema define once tipos enumerados que restringen el dominio de los atributos de "
     "estado y de selección, evitando valores inconsistentes en la operación del centro:", indent=True)
tcap(1, "Tipos enumerados definidos en el esquema dentrix.")
table([
    ["Tipo (ENUM)", "Valores permitidos", "Uso principal"],
    ["estado_usuario", "ACTIVO, INACTIVO, BLOQUEADO", "usuario.estado"],
    ["estado_cita", "PENDIENTE, CONFIRMADA, EN_SALA, EN_ATENCION, ATENDIDA, FALTO, CANCELADA, BLOQUEADA", "cita.estado"],
    ["tipo_bloque", "CITA, ALMUERZO, CIERRE, BLOQUEADO", "cita.tipo (bloques de agenda)"],
    ["estado_derivacion", "PENDIENTE, ACEPTADA, RECHAZADA", "ficha_clinica (RF-07)"],
    ["estado_linea", "PLANIFICADO, EJECUTADO, COBRADO", "plan_tratamiento.estado"],
    ["estado_pago", "PENDIENTE, PARCIAL, PAGADO", "pago.estado_pago (RF-08)"],
    ["modalidad_pago", "EFECTIVO, YAPE, PLIN, TARJETA", "pago.modalidad (RF-08)"],
    ["tipo_documento", "BOLETA, NOTA_CREDITO (reservado, fuera de alcance v3)", "pago.tipo"],
    ["estado_documento", "EMITIDO, ANULADO", "pago.estado"],
    ["estado_cierre", "ABIERTO, CONSOLIDADO", "cierre_caja.estado (M7.1)"],
    ["estado_pieza", "SANO, CARIES, OBTURADO, AUSENTE, CORONA, IMPLANTE, ENDODONCIA, PROTESIS", "hallazgo_dental.estado"],
], [3.3, 7.8, 4.6])
note("Nota. Elaboración propia a partir del script 01_schema_v2.sql. Los ENUM se crean antes de "
     "las tablas y se referencian en las columnas indicadas; cualquier valor distinto del "
     "catálogo es rechazado por el motor.")

head("Resumen de tablas", 2)
para("Las 14 tablas del modelo y su propósito dentro del proceso de Gestión de Atención "
     "Odontológica se resumen a continuación; la trazabilidad indica el módulo prototipado y los "
     "requerimientos funcionales (RF) del SRS que cada tabla hace verificable:")
tcap(2, "Tablas de la base de datos relacional dentrix (modelo v2).")
table([
    ["#", "Tabla", "Propósito", "Llave primaria (PK)", "Módulo / RF"],
    ["1", "rol", "Catálogo de roles del sistema para el control de acceso (RBAC).", "id", "M1, M9.1 / RF-01, RF-11"],
    ["2", "configuracion", "Parámetros del centro (RUC, IGV, serie de boleta, horarios); singleton.", "id (= 1)", "M9.3 / RF-14"],
    ["3", "consultorio", "Consultorios físicos del centro (C01, C02).", "id", "M5, M9.3 / RF-05, RF-14"],
    ["4", "usuario", "Usuarios del sistema (administrador, odontólogos, recepción); absorbe al odontólogo.", "id", "M1, M2, M5, M8, M9.1 / RF-01, RF-05, RF-11, RNF-01"],
    ["5", "paciente", "Pacientes y su expediente clínico (1:1): alertas, antecedentes, código HC.", "id", "M4, M6 / RF-04, RF-06"],
    ["6", "diagnostico_cie10", "Catálogo de diagnósticos CIE-10 (OMS/MINSA).", "codigo", "M6, M9.2.2 / RF-06, RF-13"],
    ["7", "tratamiento", "Tarifario maestro de tratamientos y precios.", "id", "M9.2.1 / RF-12"],
    ["8", "cita", "Agenda centralizada: citas y bloques de horario (almuerzo, cierre).", "id", "M2, M3, M5 / RF-03, RF-05"],
    ["9", "ficha_clinica", "Sesión clínica (una fila por atención) con derivación a caja (RF-07).", "id", "M6 / RF-06, RF-07"],
    ["10", "ficha_diagnostico", "Tabla de unión N:N entre ficha clínica y CIE-10 (14.ª tabla).", "ficha_id + cie10_codigo", "M6, M9.2.2 / RF-06, RF-13"],
    ["11", "hallazgo_dental", "Odontograma: estado de cada pieza dental por paciente.", "paciente_id + pieza_fdi", "M6 / RF-06"],
    ["12", "cierre_caja", "Cierre del turno de caja; auditoría del fondo en JSONB (RF-09).", "id", "M7.1 / RF-09"],
    ["13", "pago", "Boletas y abonos; control de saldos y estado de pago.", "id", "M2, M6, M7 / RF-08, RF-09"],
    ["14", "plan_tratamiento", "Línea del plan que viaja PLANIFICADO → EJECUTADO → COBRADO.", "id", "M6, M7, M8 / RF-06, RF-08, RF-10, RF-12"],
], [0.8, 3.6, 5.8, 3.0, 2.5])
note("Nota. Elaboración propia con base en el esquema 01_schema_v2.sql y en la Tabla 10 de la "
     "sección 13 del informe. ficha_diagnostico es la única tabla de unión; el resto son "
     "entidades.")

head("Convenciones de lectura", 2)
para("En el diccionario de datos se emplean las siguientes convenciones:", indent=True)
bullets([
    "PK: llave primaria (sustituta SERIAL/BIGSERIAL o natural, como codigo en los catálogos).",
    "FK: llave foránea con integridad referencial declarada hacia la tabla indicada.",
    "UNIQUE: índice único que impide duplicados (documentos, DNI, códigos).",
    "CHECK: restricción de dominio o regla de negocio validada por el motor.",
    "DEFAULT: valor por defecto aplicado cuando la aplicación no envía el dato.",
    "GENERATED: columna calculada automáticamente por PostgreSQL y almacenada (subtotal, diferencia).",
    "Columna «Nulo»: «No» equivale a NOT NULL (obligatorio) y «Sí» admite NULL (opcional).",
])

# =====================================================================
# 3. DIAGRAMA ENTIDAD-RELACIÓN
# =====================================================================
head("Diagrama entidad-relación", 1, page_break=True)
para("El modelo entidad-relación de la base de datos dentrix se presenta en la Figura 1 con "
     "notación pata de gallo. La estructura gira en torno al paciente: sus citas (cita), sus "
     "sesiones clínicas (ficha_clinica) con los diagnósticos CIE-10 asociados "
     "(ficha_diagnostico), su odontograma (hallazgo_dental), su plan de tratamientos "
     "(plan_tratamiento) y su situación económica (pago, con boletas y abonos). La tabla de "
     "unión ficha_diagnostico resuelve la relación de muchos a muchos entre la ficha clínica y "
     "el catálogo de diagnósticos, y la autorreferencia pago → pago permite registrar abonos "
     "que apuntan siempre a la boleta de origen.")
BLOCKS.append({"type": "spacer", "lines": 1})
BLOCKS.append({"type": "image", "path": PNG_DER, "width_cm": 15.5})
fcap("Figura 1. Diagrama entidad-relación de la base de datos dentrix (modelo v2).")

head("Relaciones y cardinalidades", 2)
para("La Tabla 3 detalla las relaciones del diagrama, su cardinalidad y la regla de negocio o "
     "integridad que imponen:")
tcap(3, "Relaciones, cardinalidades y reglas del modelo relacional.")
table([
    ["Relación (origen → destino)", "Cardinalidad", "Regla de negocio / integridad"],
    ["rol → usuario", "1 : N", "Todo usuario posee un rol; base del control de acceso RBAC (RF-01, RF-11)."],
    ["consultorio → usuario", "1 : N", "Consultorio asignado al odontólogo (RF-05, RF-14)."],
    ["consultorio → cita", "1 : N", "Agenda por consultorio; sin doble reserva de horario (RF-05)."],
    ["usuario → cita", "1 : N", "Odontólogo responsable de la cita."],
    ["usuario → ficha_clinica", "1 : N", "Odontólogo firmante de la sesión clínica."],
    ["usuario → cierre_caja", "1 : N", "Cajero abre y consolida el turno de caja (RF-09)."],
    ["usuario → pago", "1 : N", "Usuario de caja que emite el documento o el abono."],
    ["usuario → paciente", "1 : N", "Usuario que registró al paciente en el sistema."],
    ["paciente → cita", "1 : N", "Citas del paciente; sin doble reserva (RF-05)."],
    ["paciente → ficha_clinica", "1 : N", "Historial de sesiones clínicas del paciente."],
    ["paciente → hallazgo_dental", "1 : N", "Odontograma: una fila por pieza dental FDI (RF-06)."],
    ["paciente → plan_tratamiento", "1 : N", "Plan de tratamiento y sus líneas."],
    ["paciente → pago", "1 : N", "Documentos y abonos del paciente."],
    ["tratamiento → cita", "1 : N", "Tratamiento previsto en la cita."],
    ["tratamiento → plan_tratamiento", "1 : N", "Tarifa aplicada a la línea del plan (RF-12)."],
    ["cita → ficha_clinica", "1 : 0..1", "Una cita genera, a lo más, una ficha clínica."],
    ["ficha_clinica → ficha_diagnostico", "1 : N", "Diagnósticos CIE-10 de la sesión; uno principal (RF-06)."],
    ["diagnostico_cie10 → ficha_diagnostico", "1 : N", "Un código clasifica muchas fichas (RF-13)."],
    ["ficha_clinica → plan_tratamiento", "1 : N", "Líneas planificadas o ejecutadas en la sesión."],
    ["ficha_clinica → pago", "1 : 0..1", "Cobro de la ficha con derivación ACEPTADA (RF-07)."],
    ["cierre_caja → pago", "1 : N", "Pagos agrupados en el turno de caja (RF-09)."],
    ["pago → pago", "1 : N", "Abonos que apuntan a la boleta de origen, nunca a otro abono (RF-08)."],
    ["pago → plan_tratamiento", "1 : N", "Líneas incluidas en la boleta; el trigger recalcula el monto (RF-08)."],
], [4.5, 1.7, 9.5])

# =====================================================================
# 4. DICCIONARIO DE DATOS
# =====================================================================
head("Diccionario de datos", 1, page_break=True)
para("A continuación se documenta cada una de las 14 tablas: su propósito, sus columnas con el "
     "tipo de dato PostgreSQL, la obligatoriedad, las restricciones y la descripción funcional "
     "del atributo, así como las restricciones de tabla y las referencias entrantes. La "
     "trazabilidad módulo/RF proviene del modelado aprobado y de la especificación de "
     "requerimientos del informe (Tablas 4 y 5).")

TABLES_DATA = [
    {
        "name": "rol", "num": 4, "title": "Tabla rol",
        "desc": "Catálogo de roles del sistema para el control de acceso basado en roles (RBAC). "
                "Almacena los tres roles vigentes: ADMINISTRADOR, ODONTOLOGO y RECEPCION. "
                "Módulos M1 y M9.1; RF-01 y RF-11.",
        "cols": [
            ["id", "SERIAL", "No", "PK", "Identificador interno autogenerado del rol."],
            ["codigo", "VARCHAR(20)", "No", "UNIQUE", "Código del rol (ADMINISTRADOR, ODONTOLOGO, RECEPCION); base del control de acceso."],
            ["nombre", "VARCHAR(40)", "No", "—", "Nombre descriptivo del rol mostrado en la interfaz."],
            ["descripcion", "VARCHAR(120)", "Sí", "—", "Detalle del alcance y funciones del rol."],
        ],
        "note": "Nota. Sin llaves foráneas. Referenciada por usuario.rol_id. Cada usuario posee "
                "un único rol (matriz RBAC del M9.1).",
    },
    {
        "name": "configuracion", "num": 5, "title": "Tabla configuracion",
        "desc": "Parámetros institucionales del centro odontológico: identificación fiscal, "
                "licencias, IGV y horarios de atención. Es una tabla singleton: admite una sola "
                "fila (id = 1). Módulo M9.3; RF-14.",
        "cols": [
            ["id", "INT", "No", "PK, CHECK (id = 1)", "Clave fija; la tabla admite un único registro."],
            ["razon_social", "VARCHAR(120)", "No", "—", "Nombre legal del centro odontológico."],
            ["ruc", "CHAR(11)", "No", "—", "RUC del centro (11 dígitos) para los comprobantes."],
            ["direccion", "VARCHAR(160)", "No", "—", "Dirección fiscal del establecimiento."],
            ["telefono", "VARCHAR(20)", "Sí", "—", "Teléfono de contacto del centro."],
            ["email", "VARCHAR(80)", "Sí", "—", "Correo electrónico institucional."],
            ["representante_legal", "VARCHAR(80)", "Sí", "—", "Representante legal para documentos impresos."],
            ["medico_director", "VARCHAR(80)", "Sí", "—", "Médico director consignado en las licencias."],
            ["licencia_sanitaria", "VARCHAR(40)", "Sí", "—", "Número de licencia sanitaria."],
            ["licencia_municipal", "VARCHAR(40)", "Sí", "—", "Número de licencia municipal."],
            ["hora_apertura", "TIME", "No", "DEFAULT '08:00'", "Hora de apertura del turno de atención."],
            ["hora_cierre", "TIME", "No", "DEFAULT '20:00'", "Hora de cierre del turno de atención."],
            ["almuerzo_inicio", "TIME", "No", "DEFAULT '12:00'", "Inicio del bloque de almuerzo de la agenda."],
            ["almuerzo_fin", "TIME", "No", "DEFAULT '13:00'", "Fin del bloque de almuerzo de la agenda."],
            ["igv_porcentaje", "NUMERIC(4,2)", "No", "DEFAULT 18.00", "Porcentaje de IGV aplicado en las boletas."],
            ["serie_boleta", "VARCHAR(10)", "No", "DEFAULT 'B001'", "Serie base de la numeración de boletas."],
        ],
        "note": "Nota. Sin llaves foráneas. El IGV y la serie de esta tabla alimentan el cálculo "
                "de las boletas (trigger tr_plan_recalcular_pago).",
    },
    {
        "name": "consultorio", "num": 6, "title": "Tabla consultorio",
        "desc": "Consultorios físicos donde se atiende a los pacientes (C01, C02). Módulos M5 y "
                "M9.3; RF-05 y RF-14.",
        "cols": [
            ["id", "SERIAL", "No", "PK", "Identificador interno del consultorio."],
            ["codigo", "VARCHAR(4)", "No", "UNIQUE", "Código corto del consultorio (C01, C02)."],
            ["nombre", "VARCHAR(60)", "No", "—", "Nombre del consultorio."],
            ["especialidad", "VARCHAR(60)", "Sí", "—", "Especialidad asociada al consultorio."],
            ["estado", "VARCHAR(20)", "No", "DEFAULT 'ACTIVO'", "Estado operativo (ACTIVO o INACTIVO)."],
        ],
        "note": "Nota. Referenciada por usuario.consultorio_id y cita.consultorio_id.",
    },
    {
        "name": "usuario", "num": 7, "title": "Tabla usuario",
        "desc": "Usuarios del sistema: administrador, odontólogos y personal de recepción. La "
                "tabla absorbe al odontólogo mediante las columnas colegiatura, especialidad y "
                "consultorio_id (solo aplican al rol ODONTOLOGO). Módulos M1, M2, M5, M8 y M9.1; "
                "RF-01, RF-05, RF-11 y RNF-01.",
        "cols": [
            ["id", "SERIAL", "No", "PK", "Identificador interno del usuario."],
            ["username", "VARCHAR(30)", "No", "UNIQUE", "Nombre de usuario para el inicio de sesión."],
            ["password_hash", "VARCHAR(80)", "No", "—", "Contraseña cifrada con bcrypt; nunca se almacena en texto plano (RNF-01)."],
            ["nombres", "VARCHAR(60)", "No", "—", "Nombres del usuario."],
            ["apellidos", "VARCHAR(60)", "No", "—", "Apellidos del usuario."],
            ["dni", "CHAR(8)", "No", "UNIQUE", "Documento Nacional de Identidad (8 dígitos)."],
            ["rol_id", "INT", "No", "FK → rol(id)", "Rol del usuario; define sus permisos en el sistema."],
            ["colegiatura", "VARCHAR(20)", "Sí", "—", "Colegiatura COP/RNE; solo odontólogos (RF-14)."],
            ["especialidad", "VARCHAR(60)", "Sí", "—", "Especialidad del odontólogo."],
            ["consultorio_id", "INT", "Sí", "FK → consultorio(id)", "Consultorio asignado al odontólogo."],
            ["estado", "estado_usuario", "No", "DEFAULT 'ACTIVO'", "ACTIVO, INACTIVO o BLOQUEADO."],
            ["intentos_fallidos", "INT", "No", "DEFAULT 0", "Contador de intentos fallidos de inicio de sesión (M1)."],
            ["bloqueado_hasta", "TIMESTAMP", "Sí", "—", "Fecha y hora de desbloqueo tras exceder los intentos."],
            ["ultimo_acceso", "TIMESTAMP", "Sí", "—", "Última fecha y hora de inicio de sesión."],
            ["fecha_creacion", "TIMESTAMP", "No", "DEFAULT CURRENT_TIMESTAMP", "Fecha de alta del usuario en el sistema."],
        ],
        "note": "Nota. UNIQUE (username) y UNIQUE (dni). Referenciada por paciente."
                "usuario_registro_id, cita.usuario_id, ficha_clinica.odontologo_id, "
                "cierre_caja.cajero_id y consolidado_por, y pago.odontologo_id y "
                "usuario_caja_id. El rol ODONTOLOGO se valida en la capa de servicio a partir de "
                "colegiatura y especialidad (RF-14).",
    },
    {
        "name": "paciente", "num": 8, "title": "Tabla paciente",
        "desc": "Pacientes del centro odontológico. Absorbe el expediente clínico en una relación "
                "1:1 (alertas de alergias, antecedentes y fecha de apertura). Módulos M4 y M6; "
                "RF-04 y RF-06.",
        "cols": [
            ["id", "SERIAL", "No", "PK", "Identificador interno del paciente."],
            ["codigo_hc", "VARCHAR(15)", "No", "UNIQUE", "Código de historia clínica (HC-000001); identifica el expediente."],
            ["dni", "CHAR(8)", "No", "UNIQUE", "DNI del paciente; evita duplicados (RF-04)."],
            ["apellidos", "VARCHAR(60)", "No", "—", "Apellidos del paciente."],
            ["nombres", "VARCHAR(60)", "No", "—", "Nombres del paciente."],
            ["fecha_nac", "DATE", "No", "—", "Fecha de nacimiento."],
            ["celular", "VARCHAR(15)", "Sí", "—", "Teléfono celular de contacto."],
            ["email", "VARCHAR(80)", "Sí", "—", "Correo electrónico."],
            ["direccion", "VARCHAR(120)", "Sí", "—", "Dirección domiciliaria."],
            ["alergias", "VARCHAR(200)", "Sí", "—", "Alertas farmacológicas; se muestran de forma destacada en la ficha (M4, M6)."],
            ["antecedentes", "TEXT", "Sí", "—", "Antecedentes médicos generales del expediente."],
            ["fecha_apertura", "DATE", "No", "DEFAULT CURRENT_DATE", "Fecha de apertura del expediente clínico."],
            ["estado", "VARCHAR(15)", "No", "DEFAULT 'ACTIVO'", "Estado del paciente (ACTIVO o INACTIVO)."],
            ["usuario_registro_id", "INT", "Sí", "FK → usuario(id)", "Usuario que registró al paciente."],
            ["fecha_registro", "TIMESTAMP", "No", "DEFAULT CURRENT_TIMESTAMP", "Fecha y hora del registro."],
        ],
        "note": "Nota. UNIQUE (codigo_hc) y UNIQUE (dni). Referenciada por cita, ficha_clinica, "
                "hallazgo_dental, plan_tratamiento y pago.",
    },
    {
        "name": "diagnostico_cie10", "num": 9, "title": "Tabla diagnostico_cie10",
        "desc": "Catálogo de diagnósticos odontológicos codificados según CIE-10 (OMS/MINSA). "
                "Evita el texto libre en los diagnósticos y permite estadísticas. Módulos M6 y "
                "M9.2.2; RF-06 y RF-13.",
        "cols": [
            ["codigo", "VARCHAR(7)", "No", "PK", "Código CIE-10 del diagnóstico (por ejemplo, K02.1)."],
            ["descripcion", "VARCHAR(160)", "No", "—", "Descripción oficial del diagnóstico."],
            ["capitulo", "VARCHAR(80)", "Sí", "—", "Capítulo del catálogo al que pertenece el código."],
            ["es_frecuente", "BOOLEAN", "No", "DEFAULT FALSE", "Marca los diagnósticos de uso frecuente para acceso rápido."],
        ],
        "note": "Nota. Clave primaria natural (codigo). Referenciada por "
                "ficha_diagnostico.cie10_codigo.",
    },
    {
        "name": "tratamiento", "num": 10, "title": "Tabla tratamiento",
        "desc": "Tarifario maestro de tratamientos del centro: catálogo de servicios con su forma "
                "de cálculo (por pieza, por sesión o mensual) y su precio de mostrador. Módulo "
                "M9.2.1; RF-12.",
        "cols": [
            ["id", "SERIAL", "No", "PK", "Identificador interno del tratamiento."],
            ["codigo", "VARCHAR(10)", "No", "UNIQUE", "Código del tarifario (TRAT-01 … TRAT-05)."],
            ["nombre", "VARCHAR(80)", "No", "—", "Nombre del tratamiento."],
            ["especialidad", "VARCHAR(60)", "No", "—", "Especialidad que ejecuta el tratamiento."],
            ["calculo", "VARCHAR(20)", "No", "DEFAULT 'PIEZA'", "Forma de cálculo: PIEZA, SESION o MENSUAL."],
            ["precio", "NUMERIC(10,2)", "No", "CHECK (precio >= 0)", "Precio de mostrador con IGV incluido."],
            ["estado", "VARCHAR(15)", "No", "DEFAULT 'ACTIVO'", "Estado del servicio (ACTIVO o INACTIVO)."],
        ],
        "note": "Nota. UNIQUE (codigo). Referenciada por cita.tratamiento_id y "
                "plan_tratamiento.tratamiento_id.",
    },
    {
        "name": "cita", "num": 11, "title": "Tabla cita",
        "desc": "Agenda centralizada del centro: citas de pacientes y bloques de horario "
                "(almuerzo, cierre y bloqueos) como filas con tipo distinto de CITA. Las "
                "restricciones de unicidad impiden la doble reserva de consultorio o de "
                "paciente. Módulos M2, M3 y M5; RF-03 y RF-05.",
        "cols": [
            ["id", "BIGSERIAL", "No", "PK", "Identificador interno de la cita o bloque."],
            ["tipo", "tipo_bloque", "No", "DEFAULT 'CITA'", "CITA, ALMUERZO, CIERRE o BLOQUEADO (bloques de la agenda)."],
            ["paciente_id", "INT", "Sí", "FK → paciente(id)", "Paciente citado; obligatorio cuando el tipo es CITA."],
            ["usuario_id", "INT", "No", "FK → usuario(id)", "Odontólogo responsable del horario."],
            ["consultorio_id", "INT", "No", "FK → consultorio(id)", "Consultorio reservado para la atención."],
            ["fecha_cita", "DATE", "No", "—", "Fecha de la cita."],
            ["hora_inicio", "TIME", "No", "—", "Hora de inicio del bloque."],
            ["hora_fin", "TIME", "No", "CHECK (hora_fin > hora_inicio)", "Hora de fin del bloque."],
            ["tratamiento_id", "INT", "Sí", "FK → tratamiento(id)", "Tratamiento previsto para la cita."],
            ["estado", "estado_cita", "No", "DEFAULT 'PENDIENTE'", "PENDIENTE, CONFIRMADA, EN_SALA, EN_ATENCION, ATENDIDA, FALTO, CANCELADA o BLOQUEADA."],
            ["motivo", "VARCHAR(160)", "Sí", "—", "Motivo o nota de la cita."],
        ],
        "note": "Nota. Restricciones de tabla: UNIQUE (consultorio_id, fecha_cita, hora_inicio) y "
                "UNIQUE (paciente_id, fecha_cita, hora_inicio) — sin doble reserva (RF-05); CHECK "
                "(hora_fin > hora_inicio); CHECK (tipo <> 'CITA' OR paciente_id IS NOT NULL). "
                "Índices: idx_cita_fecha e idx_cita_paciente. Referenciada por "
                "ficha_clinica.cita_id.",
    },
    {
        "name": "ficha_clinica", "num": 12, "title": "Tabla ficha_clinica",
        "desc": "Sesión clínica: una fila por cada atención odontológica, con el motivo de "
                "consulta, el examen clínico, la prescripción y el plan de indicaciones. La "
                "columna estado_derivacion implementa el flujo de derivación a caja del RF-07. "
                "Módulo M6; RF-06 y RF-07.",
        "cols": [
            ["id", "BIGSERIAL", "No", "PK", "Identificador interno de la sesión clínica."],
            ["paciente_id", "INT", "No", "FK → paciente(id)", "Paciente atendido."],
            ["cita_id", "BIGINT", "Sí", "FK → cita(id)", "Cita que originó la sesión."],
            ["odontologo_id", "INT", "No", "FK → usuario(id)", "Odontólogo firmante de la ficha."],
            ["fecha", "TIMESTAMP", "No", "DEFAULT CURRENT_TIMESTAMP", "Fecha y hora de la atención."],
            ["motivo_consulta", "VARCHAR(200)", "Sí", "—", "Motivo de consulta registrado."],
            ["examen_clinico", "TEXT", "Sí", "—", "Hallazgos del examen clínico."],
            ["prescripcion", "TEXT", "Sí", "—", "Prescripción o receta emitida."],
            ["plan_indicaciones", "TEXT", "Sí", "—", "Plan de tratamiento e indicaciones al paciente."],
            ["estado_derivacion", "estado_derivacion", "No", "DEFAULT 'PENDIENTE'", "PENDIENTE, ACEPTADA o RECHAZADA; ACEPTADA habilita la facturación (RF-07)."],
            ["derivado_a_caja", "BOOLEAN", "No", "DEFAULT FALSE", "Marca de derivación efectiva a caja."],
            ["fecha_derivacion", "TIMESTAMP", "Sí", "—", "Fecha y hora de la derivación a caja."],
        ],
        "note": "Nota. CHECK (derivado_a_caja = FALSE OR estado_derivacion = 'ACEPTADA') y CHECK "
                "(derivado_a_caja = FALSE OR fecha_derivacion IS NOT NULL) — RF-07. Índice: "
                "idx_ficha_paciente. Referenciada por ficha_diagnostico (ON DELETE CASCADE), "
                "hallazgo_dental.ultima_ficha_id, pago.ficha_id y plan_tratamiento.ficha_id.",
    },
    {
        "name": "ficha_diagnostico", "num": 13, "title": "Tabla ficha_diagnostico",
        "desc": "Tabla de unión que resuelve la relación de muchos a muchos entre la ficha "
                "clínica y el catálogo CIE-10 (14.ª tabla del modelo). Un trigger garantiza como "
                "máximo un diagnóstico principal por ficha. Módulos M6 y M9.2.2; RF-06 y RF-13.",
        "cols": [
            ["ficha_id", "BIGINT", "No", "PK, FK → ficha_clinica(id)", "Sesión clínica diagnosticada; se elimina en cascada con su ficha."],
            ["cie10_codigo", "VARCHAR(7)", "No", "PK, FK → diagnostico_cie10", "Diagnóstico CIE-10 asociado a la sesión."],
            ["pieza_fdi", "SMALLINT", "Sí", "CHECK (FDI 11–48)", "Pieza dental afectada en numeración FDI; opcional."],
            ["es_principal", "BOOLEAN", "No", "DEFAULT FALSE", "Marca el diagnóstico principal; máximo uno por ficha (trigger)."],
            ["observacion", "VARCHAR(120)", "Sí", "—", "Observación del diagnóstico en la sesión."],
        ],
        "note": "Nota. Clave primaria compuesta (ficha_id, cie10_codigo). Trigger: "
                "tr_ficha_diag_principal (un solo principal, RF-06). Índice: "
                "idx_ficha_diag_codigo.",
    },
    {
        "name": "hallazgo_dental", "num": 14, "title": "Tabla hallazgo_dental",
        "desc": "Odontograma del paciente: una fila por cada pieza dental (FDI) con su estado "
                "clínico, la cara afectada y la última sesión que lo actualizó. Módulo M6; RF-06.",
        "cols": [
            ["paciente_id", "INT", "No", "PK, FK → paciente(id)", "Paciente al que pertenece el odontograma."],
            ["pieza_fdi", "SMALLINT", "No", "PK, CHECK (FDI 11–48)", "Número de pieza dental en notación FDI."],
            ["estado", "estado_pieza", "No", "DEFAULT 'SANO'", "SANO, CARIES, OBTURADO, AUSENTE, CORONA, IMPLANTE, ENDODONCIA o PROTESIS."],
            ["cara", "VARCHAR(20)", "Sí", "—", "Cara de la pieza (oclusal, mesial, distal, vestibular o lingual)."],
            ["observacion", "VARCHAR(120)", "Sí", "—", "Observación del hallazgo clínico."],
            ["ultima_ficha_id", "BIGINT", "Sí", "FK → ficha_clinica(id)", "Última sesión que actualizó el hallazgo."],
            ["fecha_registro", "TIMESTAMP", "No", "DEFAULT CURRENT_TIMESTAMP", "Fecha de la última actualización del hallazgo."],
        ],
        "note": "Nota. Clave primaria compuesta (paciente_id, pieza_fdi): una fila por pieza del "
                "odontograma. CHECK de pieza FDI (11–48). Índice: idx_hallazgo_paciente.",
    },
    {
        "name": "cierre_caja", "num": 15, "title": "Tabla cierre_caja",
        "desc": "Cierre de caja del turno diario: fondo inicial, montos calculados por el sistema, "
                "efectivo contado y diferencia. Absorbe la auditoría de cambios del fondo en la "
                "columna JSONB historial_fondos (RF-09). Módulo M7.1; RF-09.",
        "cols": [
            ["id", "BIGSERIAL", "No", "PK", "Identificador interno del cierre de caja."],
            ["fecha", "DATE", "No", "—", "Fecha del turno de caja."],
            ["turno", "VARCHAR(10)", "No", "DEFAULT 'DIURNO'", "Turno del día (DIURNO)."],
            ["cajero_id", "INT", "No", "FK → usuario(id)", "Cajero responsable del cierre."],
            ["fondo_inicial", "NUMERIC(10,2)", "No", "DEFAULT 0, CHECK (>= 0)", "Fondo de caja al iniciar el turno (S/ 100 en la semilla)."],
            ["historial_fondos", "JSONB", "No", "DEFAULT '[]'", "Auditoría de cambios del fondo: monto anterior, monto nuevo y fecha (RF-09)."],
            ["efectivo_sistema", "NUMERIC(12,2)", "No", "DEFAULT 0", "Efectivo del turno calculado por el sistema."],
            ["billeteras_sistema", "NUMERIC(12,2)", "No", "DEFAULT 0", "Total Yape/Plin/Tarjeta calculado por el sistema."],
            ["total_sistema", "NUMERIC(12,2)", "No", "DEFAULT 0", "Total recaudado según el sistema (efectivo + billeteras)."],
            ["efectivo_contado", "NUMERIC(12,2)", "Sí", "—", "Efectivo contado físicamente en la gaveta."],
            ["diferencia", "NUMERIC(12,2)", "Sí", "GENERATED (contado − fondo − sistema)", "Descuadre del turno; calculado por PostgreSQL y almacenado."],
            ["estado", "estado_cierre", "No", "DEFAULT 'ABIERTO'", "ABIERTO o CONSOLIDADO."],
            ["consolidado_por", "INT", "Sí", "FK → usuario(id)", "Usuario que consolida el cierre en el M9.3."],
            ["consolidado_en", "TIMESTAMP", "Sí", "—", "Fecha y hora de la consolidación."],
            ["acta_generada", "BOOLEAN", "No", "DEFAULT FALSE", "Marca de generación del acta de cierre."],
        ],
        "note": "Nota. UNIQUE (fecha, turno): un solo cierre por turno. Trigger: "
                "tr_cierre_historial_fondo (auditoría del fondo en JSONB, RF-09). Referenciada "
                "por pago.cierre_id.",
    },
    {
        "name": "pago", "num": 16, "title": "Tabla pago",
        "desc": "Documentos de cobro del centro: boletas (con número y líneas en "
                "plan_tratamiento) y abonos (filas sin número que apuntan a la boleta de origen "
                "mediante pago_origen_id). Los triggers recalculan el saldo y el estado de pago, "
                "y exigen número de operación en billeteras digitales. Módulos M2, M6 y M7; "
                "RF-08 y RF-09.",
        "cols": [
            ["id", "BIGSERIAL", "No", "PK", "Identificador interno del documento o abono."],
            ["numero", "VARCHAR(20)", "Sí", "UNIQUE", "Número de boleta (B001-000458); NULL cuando la fila es un abono."],
            ["tipo", "tipo_documento", "No", "DEFAULT 'BOLETA'", "BOLETA o NOTA_CREDITO (reservado, fuera de alcance v3)."],
            ["estado", "estado_documento", "No", "DEFAULT 'EMITIDO'", "EMITIDO o ANULADO."],
            ["pago_origen_id", "BIGINT", "Sí", "FK → pago(id)", "Boleta a la que abona esta fila; NULL si es documento."],
            ["fecha", "TIMESTAMP", "No", "DEFAULT CURRENT_TIMESTAMP", "Fecha y hora de emisión del documento o del abono."],
            ["paciente_id", "INT", "No", "FK → paciente(id)", "Paciente que efectúa el pago."],
            ["ficha_id", "BIGINT", "Sí", "FK → ficha_clinica(id)", "Ficha clínica facturada; su trigger exige derivación ACEPTADA (RF-07)."],
            ["odontologo_id", "INT", "Sí", "FK → usuario(id)", "Odontólogo que brindó la atención cobrada."],
            ["usuario_caja_id", "INT", "No", "FK → usuario(id)", "Usuario de caja que emite el documento."],
            ["cierre_id", "BIGINT", "Sí", "FK → cierre_caja(id)", "Cierre de caja del turno al que pertenece el pago."],
            ["modalidad", "modalidad_pago", "No", "DEFAULT 'EFECTIVO'", "EFECTIVO, YAPE, PLIN o TARJETA."],
            ["operacion_ref", "VARCHAR(40)", "Sí", "—", "Número de operación de la billetera o tarjeta; obligatorio en pagos digitales (RF-08)."],
            ["subtotal", "NUMERIC(10,2)", "No", "DEFAULT 0", "Base imponible descompuesta por el trigger (monto dividido entre 1 + IGV)."],
            ["igv", "NUMERIC(10,2)", "No", "DEFAULT 0", "IGV descompuesto del monto por el trigger."],
            ["monto", "NUMERIC(10,2)", "No", "DEFAULT 0, CHECK (>= 0)", "Importe total del documento o del abono (con IGV incluido)."],
            ["saldo_deudor", "NUMERIC(10,2)", "No", "DEFAULT 0, CHECK (>= 0)", "Saldo pendiente; lo recalculan los triggers de líneas y abonos."],
            ["estado_pago", "estado_pago", "No", "DEFAULT 'PENDIENTE'", "PENDIENTE, PARCIAL o PAGADO; lo actualizan los triggers."],
        ],
        "note": "Nota. UNIQUE (numero). Restricciones de tabla: CHECK pago_documento_o_abono (toda "
                "boleta lleva número y todo abono apunta a una boleta); CHECK "
                "pago_billetera_requiere_operacion (Yape/Plin/Tarjeta exigen operacion_ref, "
                "RF-08). Triggers: tr_pago_requiere_derivacion, tr_pago_abono_valido y "
                "tr_abono_actualiza_saldo. Índices: idx_pago_paciente e idx_pago_cierre. La "
                "autorreferencia pago_origen_id vincula cada abono con su boleta.",
    },
    {
        "name": "plan_tratamiento", "num": 17, "title": "Tabla plan_tratamiento",
        "desc": "Línea del plan de tratamiento que viaja por todo el flujo: se planifica en la "
                "ficha (PLANIFICADO), se ejecuta en la sesión (EJECUTADO) y se cobra en la "
                "boleta (COBRADO). El subtotal es calculado por el motor y el trigger "
                "tr_plan_recalcular_pago mantiene cuadrada la boleta. Módulos M6, M7 y M8; "
                "RF-06, RF-08, RF-10 y RF-12.",
        "cols": [
            ["id", "BIGSERIAL", "No", "PK", "Identificador interno de la línea del plan."],
            ["paciente_id", "INT", "No", "FK → paciente(id)", "Paciente del plan de tratamiento."],
            ["tratamiento_id", "INT", "No", "FK → tratamiento(id)", "Tratamiento del tarifario aplicado."],
            ["ficha_id", "BIGINT", "Sí", "FK → ficha_clinica(id)", "Sesión clínica donde se planifica o ejecuta la línea."],
            ["pago_id", "BIGINT", "Sí", "FK → pago(id)", "Boleta que cobra la línea; obligatorio cuando el estado es COBRADO."],
            ["pieza_fdi", "SMALLINT", "Sí", "CHECK (FDI 11–48)", "Pieza dental sobre la que se aplica el tratamiento."],
            ["cantidad", "INT", "No", "DEFAULT 1, CHECK (> 0)", "Número de unidades facturadas de la línea."],
            ["precio_unitario", "NUMERIC(10,2)", "No", "CHECK (>= 0)", "Precio unitario del tratamiento al momento de la línea."],
            ["subtotal", "NUMERIC(10,2)", "No", "GENERATED (cantidad × precio)", "Importe de la línea; calculado y almacenado por PostgreSQL."],
            ["estado", "estado_linea", "No", "DEFAULT 'PLANIFICADO'", "PLANIFICADO, EJECUTADO o COBRADO."],
            ["fecha", "TIMESTAMP", "No", "DEFAULT CURRENT_TIMESTAMP", "Fecha y hora de la línea."],
        ],
        "note": "Nota. CHECK (estado <> 'COBRADO' OR pago_id IS NOT NULL) y CHECK de pieza FDI "
                "(11–48). Trigger: tr_plan_recalcular_pago (recalcula monto, subtotal, IGV y "
                "saldo de la boleta desde sus líneas). Índices: idx_plan_ficha e idx_plan_pago.",
    },
]

for t in TABLES_DATA:
    head(t["title"], 2)
    para(t["desc"])
    tcap(t["num"], "Diccionario de datos de la tabla %s." % t["name"])
    dict_table(t["cols"])
    note(t["note"])

# =====================================================================
# 5. VISTAS
# =====================================================================
head("Vistas de reportes (Módulo 8)", 1, page_break=True)
para("El Módulo 8 (Reportes y KPIs) no persiste datos propios: se alimenta de seis vistas de "
     "solo lectura definidas sobre el esquema dentrix, que resumen la operación del día, la "
     "situación de deuda de los pacientes y los indicadores de gestión del centro:")
tcap(18, "Vistas de reportes del Módulo 8 y su función.")
table([
    ["Vista", "Contenido", "Alimenta"],
    ["v_kpis_dashboard", "Citas de hoy, pacientes en sala de espera, recaudado hoy y pacientes deudores.", "Dashboard del Módulo 2"],
    ["v_saldos_paciente", "Saldo deudor total y documentos pendientes por paciente.", "Saldo del paciente (M2, M4 y M6)"],
    ["v_ranking_tratamientos", "Unidades cobradas y monto cobrado por tratamiento.", "Ranking de tratamientos (M8)"],
    ["v_productividad_odontologo", "Atenciones y pacientes atendidos por odontólogo.", "Productividad (M8)"],
    ["v_ingresos_mes", "Ingresos por mes y modalidad (documentos y abonos).", "Reporte de ingresos (M8)"],
    ["v_no_show", "Citas programadas, ausentes y tasa de ausentismo (%).", "Reporte de ausentismo (M8)"],
], [4.9, 6.0, 5.0])

# =====================================================================
# 6. TRIGGERS Y REGLAS
# =====================================================================
head("Triggers y reglas de negocio", 1, page_break=True)
para("Las reglas de negocio críticas se implementan en la base de datos —y no solo en la "
     "aplicación— para garantizar la integridad aun si otro cliente accede a los datos. Seis "
     "funciones PL/pgSQL mantienen estas invariantes mediante disparadores (triggers):")
head("Funciones y triggers", 2)
tcap(19, "Triggers de la base de datos dentrix y reglas que garantizan.")
table([
    ["Trigger", "Evento / momento", "Qué garantiza"],
    ["tr_ficha_diag_principal", "BEFORE INSERT/UPDATE · ficha_diagnostico", "Un único diagnóstico principal por ficha: rechaza el segundo (RF-06)."],
    ["tr_pago_requiere_derivacion", "BEFORE INSERT · pago", "Solo permite facturar fichas con derivación ACEPTADA; marca derivado_a_caja y fecha_derivacion (RF-07)."],
    ["tr_pago_abono_valido", "BEFORE INSERT · pago con pago_origen_id", "Un abono apunta siempre a la boleta, nunca a otro abono (RF-08)."],
    ["tr_abono_actualiza_saldo", "AFTER INSERT · pago (abonos)", "Recalcula el saldo del documento y lo pasa a PARCIAL o PAGADO (RF-08)."],
    ["tr_plan_recalcular_pago", "AFTER INSERT, UPDATE o DELETE · plan_tratamiento", "Suma las líneas y recalcula monto, subtotal, IGV y saldo de la boleta (RF-08, RF-12)."],
    ["tr_cierre_historial_fondo", "BEFORE UPDATE · cierre_caja", "Registra cada cambio del fondo inicial en historial_fondos (JSONB, RF-09)."],
], [4.8, 4.0, 7.1])

head("Reglas de negocio embebidas", 2)
para("El resumen de reglas y su mecanismo de implementación en la base de datos es el siguiente:")
tcap(20, "Reglas de negocio embebidas en la base de datos.")
table([
    ["Regla", "Mecanismo en la base", "RF"],
    ["Un solo diagnóstico principal por ficha", "Trigger tr_ficha_diag_principal", "RF-06"],
    ["No se factura una ficha sin derivación ACEPTADA", "Trigger + CHECK ficha_derivada_requiere_aceptada", "RF-07"],
    ["Un abono apunta a la boleta y nunca a otro abono", "Trigger tr_pago_abono_valido", "RF-08"],
    ["Yape, Plin y Tarjeta exigen número de operación", "CHECK pago_billetera_requiere_operacion", "RF-08"],
    ["Los abonos recalculan saldo y estado del documento", "Trigger tr_abono_actualiza_saldo", "RF-08"],
    ["Emitir una boleta recalcula monto, IGV y saldo desde sus líneas", "Trigger tr_plan_recalcular_pago", "RF-08, RF-12"],
    ["Toda boleta lleva número y todo abono apunta a una boleta", "CHECK pago_documento_o_abono + UNIQUE (numero)", "RF-08"],
    ["El cambio del fondo inicial queda auditado", "Trigger tr_cierre_historial_fondo (JSONB)", "RF-09"],
    ["Sin doble reserva de consultorio ni de paciente", "UNIQUE en cita (dos restricciones)", "RF-05"],
    ["La diferencia de caja se calcula sola", "Columna GENERATED diferencia", "RF-09"],
    ["Una línea COBRADO exige boleta asociada", "CHECK plan_cobrado_requiere_boleta", "RF-08"],
    ["Numeración FDI válida en odontograma y planes", "CHECK (pieza_fdi 11–48) en 3 tablas", "RF-06"],
], [7.7, 5.0, 3.0])

# =====================================================================
# 7. ÍNDICES
# =====================================================================
head("Índices de apoyo", 1)
para("Nueve índices secundarios aceleran los accesos más frecuentes de la operación (agenda, "
     "historial clínico, caja y reportes) sin alterar el modelo lógico:")
tcap(21, "Índices secundarios definidos en el esquema.")
table([
    ["Índice", "Tabla", "Columnas", "Objetivo"],
    ["idx_cita_fecha", "cita", "fecha_cita", "Búsqueda de la agenda por día."],
    ["idx_cita_paciente", "cita", "paciente_id, fecha_cita", "Citas e inasistencias por paciente."],
    ["idx_ficha_paciente", "ficha_clinica", "paciente_id, fecha", "Historial clínico del paciente."],
    ["idx_ficha_diag_codigo", "ficha_diagnostico", "cie10_codigo", "Búsqueda por diagnóstico CIE-10 (RF-13)."],
    ["idx_plan_ficha", "plan_tratamiento", "ficha_id", "Líneas por sesión clínica."],
    ["idx_plan_pago", "plan_tratamiento", "pago_id", "Líneas de cada boleta."],
    ["idx_pago_paciente", "pago", "paciente_id, fecha", "Estado de cuenta del paciente."],
    ["idx_pago_cierre", "pago", "cierre_id", "Arqueo del turno de caja (RF-09)."],
    ["idx_hallazgo_paciente", "hallazgo_dental", "paciente_id", "Odontograma del paciente."],
], [3.9, 3.0, 3.3, 5.7])

# =====================================================================
# 8. VERIFICACIÓN
# =====================================================================
head("Verificación del modelo", 1, page_break=True)
head("Casos de prueba duros", 2)
para("El esquema 01_schema_v2.sql y la semilla 02_datos_prueba_v2.sql se ejecutaron en "
     "PostgreSQL 18 el 13 de octubre de 2026. Los seis casos de prueba dura que sostienen los "
     "requerimientos críticos finalizaron con resultado correcto:")
tcap(22, "Casos de prueba duros verificados contra la base de datos.")
table([
    ["Caso", "Regla probada", "Resultado"],
    ["1. Boleta B001-000458 con 2 líneas (curación y profilaxis)", "El monto S/ 150 se recalcula desde plan_tratamiento (trigger).", "Correcto: monto = 150.00"],
    ["2. B001-000452 (S/ 160) con abono de S/ 10", "Saldo = S/ 150.00 y estado PARCIAL (RF-08).", "Correcto"],
    ["3. Fondo de caja modificado de S/ 100 a S/ 200", "El cambio queda auditado en historial_fondos (JSONB, RF-09).", "Correcto: 1 entrada"],
    ["4. Intento de boletar una ficha PENDIENTE", "El trigger bloquea la emisión (RF-07).", "Correcto: rechazada"],
    ["5. Yape sin número de operación", "El CHECK exige operacion_ref (RF-08).", "Correcto: rechazada"],
    ["6. Segundo diagnóstico principal en una ficha", "El trigger admite uno solo (RF-06).", "Correcto: rechazada"],
], [5.3, 7.4, 3.0])

head("Coherencia con la semilla de datos", 2)
para("La semilla del día 28 de septiembre de 2026 mantiene coherencia con los indicadores del "
     "prototipo (M2 y M7.1):")
tcap(23, "Datos clave de la semilla y su coherencia con los KPIs.")
table([
    ["Dato", "Valor"],
    ["Citas del día", "5 atendibles + 4 bloques de agenda"],
    ["Boleta de Juan", "B001-000458 · S/ 150.00 (base 127.12 + IGV 22.88)"],
    ["Billeteras del turno", "S/ 980.00 (4 transferencias)"],
    ["Efectivo del turno", "S/ 1,250.00"],
    ["Gaveta al cierre", "S/ 200 (fondo) + S/ 1,250 = S/ 1,450.00 · diferencia S/ 0.00"],
    ["Saldos por cobrar", "S/ 650.00 en 2 pacientes (S/ 500 + S/ 150)"],
    ["Recaudación del día", "S/ 2,230.00 (efectivo + billeteras)"],
], [5.2, 10.5])
note("Nota. Para reproducir la verificación, ejecutar en orden: psql -U postgres -f "
     "sql/01_schema_v2.sql y psql -U postgres -f sql/02_datos_prueba_v2.sql; la propia semilla "
     "imprime los resultados esperados (OK) de los seis casos.")

# =====================================================================
# REFERENCIAS (sin numerar; el bloque ya imprime el título "Referencias")
# =====================================================================
spacer(1)
BLOCKS.append({"type": "references", "items": [
    "Elmasri, R., y Navathe, S. B. (2016). Fundamentals of database systems (7.ª ed.). Pearson.",
    "Hoffer, J. A., Ramesh, V., y Topi, H. (2019). Modern database management (13.ª ed.). Pearson.",
    "PostgreSQL Global Development Group. (2026). PostgreSQL 18 documentation. https://www.postgresql.org/docs/18/",
    "Silberschatz, A., Korth, H. F., y Sudarshan, S. (2020). Database system concepts (7.ª ed.). McGraw-Hill.",
]})

# =====================================================================
# SPEC FINAL
# =====================================================================
spec = {
    "title": "Documentación de la Base de Datos — Sistema de Gestión de Atención "
             "Odontológica para el Centro Odontológico Dentrix",
    "author": "Marc Anthony Berrocal Bernaola y Maick Fernando Vicente Guadalupe",
    "affiliation": "Facultad de Ingeniería de Sistemas e Informática — Universidad "
                   "Tecnológica del Perú",
    "course": "Curso Integrador I: Sistemas Software",
    "instructor": "Ing. María Elena Tasa Catanzaro",
    "date": "Octubre de 2026",
    "apa": {
        "font": "Times New Roman",
        "font_size": 12,
        "line_spacing": 2,
        "page_size": "a4",
        "title_page": True,
        "table_font_size": 11,
        "heading_num": {"fmt": "decimal", "sep": ".", "levels": 5},
    },
    "blocks": BLOCKS,
}

if os.path.exists(DOCX):
    os.remove(DOCX)
res = render_document(DOCX, json.dumps(spec, ensure_ascii=False))
nt = set_cell_margins(DOCX, 45)
print("Margenes de celda reducidos a 45 twips en %d tablas." % nt)
print(json.dumps(res, ensure_ascii=False, indent=2))
