# -*- coding: utf-8 -*-
"""Genera el anexo Word 'Anexo v3 - Plan, presupuesto y consentimiento'.

Complementa a Documentacion_BD_Dentrix_v2.docx sin modificarlo.
Flujo del proyecto Generar_word: render_document (MCP word) + fix_hierarquia.py.
Uso:  py _gen_anexo_v3.py
"""
import json
import os
import sys

sys.path.insert(0, r"C:\Users\maick\word-mcp")
from word_mcp.server import render_document  # noqa: E402

BASE = r"C:\Users\maick\Documents\opencode_trabajos\UTP\Curso Integrador I - Sistemas Software\Proyecto Odontología Grupo"
DOCX = os.path.join(BASE, "Anexo_v3_plan_presupuesto.docx")

W_DICT = [3.7, 3.1, 1.2, 4.5, 3.4]

BLOCKS = []


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


def tcap(n, desc):
    BLOCKS.append({"type": "caption", "text": "Tabla %d" % n, "kind": "table", "bold": True})
    BLOCKS.append({"type": "caption", "text": desc, "kind": "note", "bold": False})


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
# 1. PRESENTACIÓN
# =====================================================================
head("Anexo v3. Plan, presupuesto y consentimiento", 1)
para("Este anexo documenta el paso 3 de la migración del modelo de datos de Dentrix: la "
     "incorporación de las tablas que sostienen la decisión económica del paciente. Se genera "
     "como documento complementario de Documentacion_BD_Dentrix_v2.docx, que describe el modelo "
     "aprobado de 14 tablas y que no se modifica. El script es sql/02_schema_v3_plan_presupuesto.sql, "
     "ejecutado y verificado sobre PostgreSQL 18 el 21 de octubre de 2026, después de "
     "01_schema_v2.sql.")
para("El modelo v2 resolvía la aceptación del paciente con un indicador de flujo "
     "(ficha_clinica.derivado_a_caja) y un estado (ficha_clinica.estado_derivacion). Esa solución "
     "no puede registrar qué se propuso, por cuánto, quién aceptó, con qué riesgos informados ni "
     "qué parte se rechazó, y deja la regla R7 apoyada en un valor que no es un acto jurídico. El "
     "anexo agrega cinco tablas que sí pueden sostener ese acto: presupuesto, presupuesto_item, "
     "aceptacion, consentimiento e imputacion.")

head("Brechas que corrige", 2)
bullets([
    "No existía la entidad presupuesto: la aceptación vivía en un flag de la ficha clínica.",
    "No existía la entidad consentimiento, por lo que las reglas R7 y R12 no tenían soporte.",
    "plan_tratamiento mezclaba tres ejes en un solo estado (PLANIFICADO, EJECUTADO, COBRADO) y "
    "ataba la línea clínica al comprobante mediante pago_id.",
    "No había versionado ni vigencia del presupuesto, contra la regla R22 (nada se elimina).",
    "El ítem no tenía fase, prioridad ni secuencia, por lo que no podía proponerse primero la "
    "urgencia ni advertirse la rehabilitación sobre enfermedad activa.",
    "El descuento no exigía autorizador identificado (regla R20).",
])

head("Qué no cambia", 2)
para("La migración es aditiva. No se altera ni se elimina ninguna de las 14 tablas del v2: "
     "plan_tratamiento y su lógica se conservan como referencia y la vista v_plan_tratamiento "
     "reproduce su forma para que el código JavaFX siga leyendo sin cambios. Quedan fuera de este "
     "paso el tarifario con vigencia (el precio se sigue tomando de tratamiento.precio, aunque el "
     "congelamiento al aceptar sí se implementa), la tabla cuota (se modela con caja en el paso 6) "
     "y la nota de crédito (fuera de alcance de esta versión).")

# =====================================================================
# 2. TIPOS ENUMERADOS
# =====================================================================
head("Tipos enumerados nuevos", 1)
para("Cinco tipos enumerados describen los estados del plan, del presupuesto, del consentimiento y "
     "del ítem. Se definieron con nombres nuevos para no afectar los tipos del v2:")
tcap(1, "Tipos enumerados incorporados en el paso 3 de la migración.")
table([
    ["Tipo", "Valores", "Uso"],
    ["fase_tratamiento", "URGENTE, BASICA, RECONSTRUCTIVA, QUIRURGICA, REHABILITACION, MANTENIMIENTO", "Fase clínica del ítem; define el orden de ejecución."],
    ["estado_presupuesto", "BORRADOR, PRESENTADO, ACEPTADO, RECHAZADO, VENCIDO, REEMPLAZADO", "Ciclo de vida de la cabecera del presupuesto."],
    ["tipo_aceptacion", "TOTAL, PARCIAL", "Alcance de la aceptación firmada."],
    ["estado_item", "PROPUESTO, ACEPTADO, RECHAZADO, DIFERIDO, ANULADO, EN_EJECUCION, EJECUTADO, CONTROLADO", "Ciclo clínico del ítem; no incluye estados de cobro."],
    ["estado_consentimiento", "PENDIENTE, FIRMADO, RECHAZADO", "Situación del consentimiento informado."],
], [3.3, 6.4, 6.0])

# =====================================================================
# 3. DICCIONARIO DE DATOS
# =====================================================================
head("Diccionario de datos de las tablas nuevas", 1, page_break=True)
para("Se describen las cinco tablas con el mismo formato del documento principal: columna, tipo de "
     "dato, nulidad, restricciones y descripción.")

head("Tabla presupuesto", 2)
para("Cabecera versionada del presupuesto. Su contenido es inmutable una vez presentado: los "
     "cambios se registran como una versión nueva que referencia a la anterior.")
tcap(2, "Diccionario de la tabla presupuesto.")
dict_table([
    ["id", "BIGSERIAL", "No", "Clave primaria", "Identificador del presupuesto."],
    ["paciente_id", "INT", "No", "FK paciente", "Paciente al que se propone el plan."],
    ["ficha_id", "BIGINT", "Sí", "FK ficha_clinica", "Atención de origen (diagnóstico de P2)."],
    ["presupuesto_anterior_id", "BIGINT", "Sí", "FK presupuesto", "Versión que esta reemplaza."],
    ["version", "INT", "No", "> 0; UNIQUE (paciente, ficha, version)", "Número de versión correlativo."],
    ["estado", "estado_presupuesto", "No", "Por defecto BORRADOR", "Etapa del presupuesto."],
    ["moneda", "CHAR(3)", "No", "Por defecto PEN", "Moneda del presupuesto."],
    ["fecha_emision", "TIMESTAMP", "No", "Por defecto la fecha actual", "Fecha de emisión."],
    ["vigencia_dias", "INT", "No", "> 0; por defecto 15", "Días de validez de la propuesta."],
    ["vigencia_hasta", "DATE", "No", "GENERATED (emisión + días)", "Fecha de vencimiento calculada."],
    ["subtotal", "NUMERIC(10,2)", "No", ">= 0; recalculado por trigger", "Suma de los ítems."],
    ["descuento", "NUMERIC(10,2)", "No", ">= 0; <= subtotal; exige autorizador (R20)", "Descuento otorgado."],
    ["total", "NUMERIC(10,2)", "No", "GENERATED (subtotal - descuento)", "Importe a pagar."],
    ["descuento_motivo", "VARCHAR(200)", "Sí", "—", "Justificación del descuento."],
    ["descuento_autorizado_por", "INT", "Sí", "FK usuario; obligatorio si hay descuento", "Quién autorizó (R20)."],
    ["creado_por", "INT", "No", "FK usuario", "Usuario que creó la versión."],
])

head("Tabla presupuesto_item", 2)
para("Ítem del plan: un procedimiento del catálogo sobre una pieza, con su fase y prioridad. "
     "Deliberadamente no contiene ningún estado de cobro: el pago se registra en imputacion y el "
     "saldo se deriva de allí.")
tcap(3, "Diccionario de la tabla presupuesto_item.")
dict_table([
    ["id", "BIGSERIAL", "No", "Clave primaria", "Identificador del ítem."],
    ["presupuesto_id", "BIGINT", "No", "FK presupuesto; ON DELETE CASCADE", "Presupuesto al que pertenece."],
    ["tratamiento_id", "INT", "No", "FK tratamiento", "Procedimiento del catálogo."],
    ["pieza_fdi", "SMALLINT", "Sí", "FDI válido: 11-48 (1-8) y 51-85 (1-5)", "Pieza; nulo si no aplica."],
    ["fase", "fase_tratamiento", "No", "Por defecto BASICA", "Fase clínica (03 §4.1)."],
    ["prioridad", "SMALLINT", "No", "Entre 1 y 6", "Orden de atención dentro de la fase."],
    ["secuencia", "INT", "No", "> 0", "Orden del ítem en el plan."],
    ["cantidad", "INT", "No", "> 0", "Unidades del procedimiento."],
    ["sesiones_planificadas", "INT", "No", "> 0", "Sesiones previstas para ejecutarlo."],
    ["precio_unitario", "NUMERIC(10,2)", "No", ">= 0; inmutable si el precio está congelado (R21)", "Precio aplicado."],
    ["precio_congelado", "BOOLEAN", "No", "Por defecto falso; se activa al aceptar", "Impide cambios de precio posteriores."],
    ["subtotal", "NUMERIC(10,2)", "No", "GENERATED (cantidad x precio)", "Importe del ítem."],
    ["estado", "estado_item", "No", "Por defecto PROPUESTO", "Situación clínica del ítem."],
    ["requiere_consentimiento", "BOOLEAN", "No", "Por defecto falso", "Exige consentimiento firmado antes de ejecutar (R7)."],
    ["motivo_rechazo", "VARCHAR(200)", "Sí", "Obligatorio si el estado es RECHAZADO", "Razón del rechazo informado."],
])

head("Tabla aceptacion", 2)
para("Firma del paciente, tutor o responsable de pago sobre el presupuesto. Registra también qué "
     "alternativas se ofrecieron y qué riesgos se informaron, que es lo que exige el estándar "
     "clínico y lo que distingue una aceptación real de un checkbox.")
tcap(4, "Diccionario de la tabla aceptacion.")
dict_table([
    ["id", "BIGSERIAL", "No", "Clave primaria", "Identificador de la aceptación."],
    ["presupuesto_id", "BIGINT", "No", "FK presupuesto; UNIQUE", "Presupuesto aceptado."],
    ["tipo", "tipo_aceptacion", "No", "TOTAL o PARCIAL", "Alcance de lo aceptado."],
    ["firmante_nombre", "VARCHAR(120)", "No", "—", "Nombre de quien firma."],
    ["firmante_documento", "VARCHAR(20)", "No", "—", "Documento de quien firma."],
    ["firmante_rol", "VARCHAR(10)", "No", "PACIENTE, TUTOR o PAGADOR", "Vínculo con el paciente."],
    ["fecha", "TIMESTAMP", "No", "Por defecto la fecha actual", "Momento de la firma."],
    ["firma_hash", "CHAR(64)", "No", "—", "Hash de la firma simulada (usuario + fecha)."],
    ["alternativas_ofrecidas", "TEXT", "No", "—", "Alternativas explicadas al paciente."],
    ["riesgos_informados", "TEXT", "No", "—", "Riesgos comunicados antes de aceptar."],
    ["registrado_por", "INT", "No", "FK usuario", "Usuario que registró la aceptación."],
])

head("Tabla consentimiento", 2)
para("Consentimiento informado por ítem de riesgo. Su firma es de tipo distinto a la de la "
     "atención y no se reutiliza (R12). Un ítem no puede tener dos consentimientos firmados; el "
     "rechazo informado se guarda con su motivo y no se elimina.")
tcap(5, "Diccionario de la tabla consentimiento.")
dict_table([
    ["id", "BIGSERIAL", "No", "Clave primaria", "Identificador del consentimiento."],
    ["paciente_id", "INT", "No", "FK paciente", "Paciente que consiente."],
    ["presupuesto_item_id", "BIGINT", "Sí", "FK presupuesto_item; UNIQUE si está firmado", "Ítem al que aplica."],
    ["tratamiento_id", "INT", "Sí", "FK tratamiento", "Procedimiento consentido."],
    ["procedimiento", "VARCHAR(160)", "No", "—", "Descripción del acto clínico."],
    ["riesgos", "TEXT", "No", "—", "Riesgos informados."],
    ["alternativas", "TEXT", "No", "—", "Alternativas ofrecidas."],
    ["estado", "estado_consentimiento", "No", "Por defecto PENDIENTE", "Situación del documento."],
    ["firmante_nombre", "VARCHAR(120)", "Sí", "Obligatorio si está firmado", "Quién firma."],
    ["firmante_documento", "VARCHAR(20)", "Sí", "Obligatorio si está firmado", "Documento del firmante."],
    ["representante_vinculo", "VARCHAR(20)", "Sí", "TUTOR o APODERADO", "Vínculo cuando firma un tercero."],
    ["profesional_id", "INT", "Sí", "FK usuario", "Profesional que informó."],
    ["fecha_firma", "TIMESTAMP", "Sí", "Obligatorio si está firmado", "Momento de la firma."],
    ["firma_hash", "CHAR(64)", "Sí", "Obligatorio si está firmado", "Hash de la firma."],
    ["motivo_rechazo", "VARCHAR(200)", "Sí", "Obligatorio si el estado es RECHAZADO", "Razón del rechazo."],
])

head("Tabla imputacion", 2)
para("Relación de muchos a muchos entre pagos e ítems del plan. Sustituye a la columna "
     "plan_tratamiento.pago_id, que solo permitía un pago por ítem cuando la realidad es la "
     "contraria: un pago cubre varios ítems y un ítem se paga en cuotas.")
tcap(6, "Diccionario de la tabla imputacion.")
dict_table([
    ["id", "BIGSERIAL", "No", "Clave primaria", "Identificador de la imputación."],
    ["pago_id", "BIGINT", "No", "FK pago; UNIQUE (pago, ítem)", "Pago o abono aplicado."],
    ["presupuesto_item_id", "BIGINT", "No", "FK presupuesto_item", "Ítem que recibe el pago."],
    ["monto", "NUMERIC(10,2)", "No", "> 0", "Importe imputado."],
    ["fecha", "TIMESTAMP", "No", "Por defecto la fecha actual", "Momento de la imputación."],
    ["usuario_id", "INT", "No", "FK usuario", "Usuario de caja que la registró."],
])

# =====================================================================
# 4. VISTAS
# =====================================================================
head("Vistas", 1)
para("Dos vistas completan el paso. La primera deriva el saldo, en cumplimiento de la regla R6 "
     "(el saldo se calcula, nunca se edita). La segunda mantiene la compatibilidad con el código "
     "actual: reproduce la forma de plan_tratamiento, pero con el estado COBRADO derivado de las "
     "imputaciones en lugar de almacenado.")
tcap(7, "Vistas incorporadas en el paso 3 de la migración.")
table([
    ["Vista", "Función", "Regla"],
    ["v_saldo_item", "Devuelve subtotal, imputado y saldo de cada ítem sumando la tabla imputacion.", "R6"],
    ["v_plan_tratamiento", "Reproduce la forma de plan_tratamiento para el código JavaFX; deduce PLANIFICADO, EJECUTADO o COBRADO.", "R6, compatibilidad"],
], [3.6, 9.4, 2.7])

# =====================================================================
# 5. TRIGGERS
# =====================================================================
head("Triggers y reglas embebidas", 1)
para("Las reglas críticas se validan en la base, no solo en Java. Once disparadores protegen la "
     "inmutabilidad, el congelamiento de precios y la compuerta de ejecución:")
tcap(8, "Triggers incorporados y regla que implementan.")
table([
    ["Trigger", "Qué impide o provoca", "Regla"],
    ["tr_presupuesto_no_delete", "Rechaza DELETE sobre la cabecera del presupuesto.", "R22"],
    ["tr_item_no_delete", "Rechaza DELETE sobre ítems del plan.", "R22"],
    ["tr_aceptacion_no_delete", "Rechaza DELETE sobre aceptaciones firmadas.", "R22"],
    ["tr_consent_no_delete", "Rechaza DELETE sobre consentimientos.", "R22"],
    ["tr_imputacion_no_delete", "Rechaza DELETE sobre imputaciones de pago.", "R22"],
    ["tr_presupuesto_inmutable", "Impide cambiar importes o vigencia de un presupuesto no borrador.", "R22"],
    ["tr_item_inmutable", "Impide editar ítems de un presupuesto presentado y cambiar un precio congelado.", "R22, R21"],
    ["tr_item_recalcula", "Recalcula el subtotal del presupuesto desde sus ítems.", "Integridad"],
    ["tr_item_r7", "Impide pasar un ítem a ejecución sin presupuesto ACEPTADO y, si lo exige, sin consentimiento firmado.", "R7, R12"],
    ["tr_presupuesto_acepta", "Al aceptar, congela precios y pasa los ítems propuestos a ACEPTADO.", "R21"],
    ["tr_presupuesto_versiona", "Marca como REEMPLAZADO el presupuesto anterior cuando se crea una versión nueva.", "R22"],
], [4.5, 8.6, 2.6])

# =====================================================================
# 6. VERIFICACIÓN
# =====================================================================
head("Verificación ejecutada", 1, page_break=True)
para("El script 01_schema_v2.sql y, sobre esa base, 02_schema_v3_plan_presupuesto.sql se ejecutaron "
     "en PostgreSQL 18 el 21 de octubre de 2026. Ocho casos de prueba se corrieron con datos "
     "reales de prueba: un presupuesto de dos ítems (endodoncia de la pieza 36 y resina de la 16), "
     "S/ 630 de subtotal y S/ 30 de descuento autorizado. Los ocho finalizaron con el resultado "
     "esperado:")
tcap(9, "Casos de prueba ejecutados sobre las tablas nuevas.")
table([
    ["Caso", "Qué se probó", "Resultado"],
    ["1. Alta de dos ítems", "El subtotal se deriva por trigger y el total descuenta S/ 30.", "Correcto: S/ 630.00 y S/ 600.00"],
    ["2. Editar precio tras presentar", "El ítem de un presupuesto presentado no se edita.", "Correcto: rechazado"],
    ["3. Aceptar el presupuesto", "Se congelan precios y los ítems pasan a ACEPTADO.", "Correcto: 2 ítems"],
    ["4. Ejecutar sin consentimiento", "El ítem de riesgo no avanza sin consentimiento firmado.", "Correcto: rechazado"],
    ["5. Firmar el consentimiento", "Con la firma registrada, el ítem pasa a EN_EJECUCION.", "Correcto"],
    ["6. Borrar un presupuesto", "El DELETE se rechaza en la base.", "Correcto: rechazado"],
    ["7. Crear una versión nueva", "No se toca un presupuesto ya aceptado.", "Correcto: sigue ACEPTADO"],
    ["8. Imputar un pago parcial", "El saldo se deriva de las imputaciones.", "Correcto: saldo S/ 150.00"],
], [4.6, 7.4, 3.7])
note("Nota. Los casos reproducen la secuencia del mapa P3 (procesos/P3_plan_presupuesto_consentimiento.md): "
     "propuesta, presentación, aceptación, consentimiento y ejecución. La prueba se ejecutó en una "
     "base temporal creada solo para la verificación, sin tocar la base de trabajo dentrix.")

# =====================================================================
# 7. APLICACIÓN
# =====================================================================
head("Cómo aplicar la migración", 1)
para("La migración no destruye datos: se ejecuta después del esquema v2 y agrega objetos nuevos. "
     "Para aplicarla en un entorno local:")
bullets([
    "psql -U postgres -f sql/01_schema_v2.sql (solo en una base nueva; en la base de trabajo no se repite).",
    "psql -U postgres -d dentrix -f sql/02_schema_v3_plan_presupuesto.sql",
    "Verificar con \\dt que existan las cinco tablas y con \\dv las dos vistas.",
    "El código JavaFX sigue funcionando sin cambios: leer de v_plan_tratamiento en lugar de plan_tratamiento "
    "cuando se requiera el estado derivado.",
])
para("El orden de la migración es por riesgo y no por módulo: primero comprobante, pago e imputación "
     "(paso 1), luego hallazgos y odontograma (paso 2), después plan y consentimiento (este paso), y "
     "al final exclusión de agenda, tarifario y auditoría. Cada paso es aditivo, de modo que un error "
     "se corrige sin deshacer el anterior.")

# =====================================================================
# SPEC
# =====================================================================
spec = {
    "title": "",
    "author": "Marc Anthony Berrocal Bernaola y Maick Fernando Vicente Guadalupe",
    "affiliation": "Facultad de Ingeniería de Sistemas e Informática - Universidad Tecnológica del Perú",
    "course": "Curso Integrador I: Sistemas Software",
    "instructor": "Ing. María Elena Tasa Catanzaro",
    "date": "Octubre de 2026",
    "apa": {
        "font": "Times New Roman",
        "font_size": 12,
        "line_spacing": 2,
        "page_size": "a4",
        "title_page": False,
        "table_font_size": 9,
        "heading_num": {"fmt": "decimal", "sep": ".", "levels": 5},
    },
    "blocks": BLOCKS,
}

if os.path.exists(DOCX):
    os.remove(DOCX)
res = render_document(DOCX, json.dumps(spec, ensure_ascii=False))
nt = set_cell_margins(DOCX, 45)
print("Margenes de celda reducidos en %d tablas." % nt)
print(json.dumps(res, ensure_ascii=False, indent=2))
