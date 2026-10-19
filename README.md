# Dentrix — Sistema de Gestión Clínica

Proyecto del curso **Integrador I (UTP)**: aplicación **JavaFX + PostgreSQL** para la
gestión integral de un centro odontológico (9 módulos / 13 vistas, ver informe APF2).

Repo: `https://github.com/maick7747/clinica-dental-agenda` (privado del equipo).
IDE del equipo: **Apache NetBeans**.

---

## 1. Identidad del proyecto (decidido por el equipo)

| Campo | Valor |
|---|---|
| groupId | `pe.edu.utp.dentrix` |
| artifactId | `dentrix-dental` |
| Paquete raíz | `pe.edu.utp.dentrix` |
| Clase principal | `pe.edu.utp.dentrix.DentrixApp` |
| Base de datos | PostgreSQL 15+, esquema `dentrix`, BD `dentrix_db` |
| JDK | 25 (cumple RNF-04: JVM 17+) |
| UI | JavaFX 25 + FXML (Scene Builder) |

**JavaFX, no Swing.** Decisión del equipo del 06/10 (ver `Comparativa-Swing/COMPARATIVA.md`
en la carpeta del proyecto): estética actual, separación vista/lógica y tecnología vigente.
El prototipo Swing `AgendaFrame` quedó de referencia en `demos/agenda-swing/`
(fuera de `src`: no se compila con Maven).

---

## 2. Requisitos (instalar una sola vez)

| Software | Versión | Descarga |
|---|---|---|
| JDK | **25** | https://www.oracle.com/java/technologies/downloads/ (o Adoptium Temurin 25) |
| Apache NetBeans | **25 o superior** | https://netbeans.apache.org/download/ |
| Scene Builder | **26.0.0** (Gluon) | https://gluonhq.com/products/scene-builder/ |
| PostgreSQL | **15 o superior** | https://www.postgresql.org/download/windows/ |
| Git | última estable | https://git-scm.com/download/win |

---

## 3. Instalación paso a paso (desde cero)

### Paso 1 — Instalar el JDK 25
1. Instala el JDK 25 (ruta típica: `C:\Program Files\Java\jdk-25`).
2. Verifica en terminal: `java -version` → debe decir `25.x`.

### Paso 2 — Instalar Git y configurarte
1. Instala Git (siguiente, siguiente…).
2. Configura tu identidad (una sola vez):
   ```
   git config --global user.name "Tu Nombre"
   git config --global user.email "tu@correo.com"
   ```

### Paso 3 — Clonar el proyecto
1. Acepta la invitación de colaborador (correo de GitHub).
2. Clona: `git clone https://github.com/maick7747/clinica-dental-agenda.git`

### Paso 4 — Registrar el JDK 25 en NetBeans
`Tools → Java Platforms → Add Platform…` → `Java Standard Edition` → carpeta
`C:\Program Files\Java\jdk-25` → nombre `JDK 25` → `Finish`.

### Paso 5 — Instalar el soporte JavaFX y enlazar Scene Builder
1. `Tools → Plugins → Available Plugins` → busca `JavaFX` → instala **JavaFX Kit**
   → reinicia el IDE.
2. `Tools → Options → Java → JavaFX` → `Scene Builder Home` → el `.exe` de Scene Builder.

### Paso 6 — Maven con JDK 25
`Tools → Options → Java → Maven → Execution` → JDK por defecto = **JDK 25**.
Sin esto falla con `release version 25 not supported`.

### Paso 7 — Base de datos (una sola vez por persona)
1. Crea tu BD local con tus iniciales (ej. `dentrix_db_mvg`) o usa `dentrix`.
2. Ejecuta en orden desde `sql/` (esquema **v2**: 14 tablas, modelo aprobado):
   ```
   psql -U postgres -f sql/01_schema_v2.sql
   psql -U postgres -f sql/02_datos_prueba_v2.sql
   psql -U postgres -f sql/03_optimizacion_rendimiento.sql
   ```
   `03` agrega indices de rendimiento, trigramas para la busqueda de
   pacientes y califica las funciones trigger (idempotente).
   Al final deben imprimirse los avisos `CASO 1 OK` … `CASO 6 OK` y
   `==== LOS 6 CASOS DUROS PASARON ===`.
3. Crea tu usuario de BD y dale acceso al esquema `dentrix`.

### Paso 8 — Configuración local (no se sube al repo)
1. Copia `config.properties.example` a
   `src/main/resources/pe/edu/utp/dentrix/config.properties`.
2. Completa `db.usuario` y `db.clave` con TU usuario local.
3. Ese archivo está en `.gitignore`: **nunca** se sube (RNF-01 / seguridad).

### Paso 9 — Abrir y correr
1. `File → Open Project` → carpeta `dentrix-dental` → `Open`.
2. Clic derecho en `dentrix-dental` → `Set as Main Project`.
3. Pulsa **F6**: `nbactions.xml` ya mapea el Run a `javafx:run`.
4. Para diseñar pantallas: doble clic en los FXML de
   `src/main/resources/pe/edu/utp/dentrix/fxml/` → se abren en Scene Builder.

---

## 4. Estructura del proyecto (arquitectura en capas)

```
dentrix-dental/
├── pom.xml
├── nbactions.xml                     # F6 → javafx:run
├── config.properties.example         # plantilla de conexión (sin secretos)
├── sql/                              # 01_schema_v2.sql (14 tablas) + 02_datos + 03_optimizacion_rendimiento.sql
├── docs/modelo_er.md                 # modelo ER con trazabilidad módulo → RF
├── demos/agenda-swing/               # prototipo Swing de referencia (no se compila)
└── src/
    ├── main/java/pe/edu/utp/dentrix/
    │   ├── DentrixApp.java           # arranque de la aplicación
    │   ├── config/                   # AppConfig + ConexionBD (pool HikariCP)
    │   ├── model/                    # 1 clase por tabla (14)
    │   ├── dao/                      # interfaces DAO (1 por tabla)
    │   ├── dao/impl/                 # implementaciones JDBC (únicos con SQL)
    │   ├── service/                  # reglas de negocio
    │   ├── controller/               # controllers FXML
    │   ├── dto/                      # objetos para reportes (M8)
    │   ├── util/                     # PasswordUtil (BCrypt), Constantes, Validador
    │   └── excepcion/                # DentrixException
    ├── main/resources/pe/edu/utp/dentrix/
    │   ├── fxml/                     # vistas (.fxml)
    │   └── config.properties         # LOCAL, no se sube
    └── test/java/pe/edu/utp/dentrix/ # pruebas JUnit 5
```

**Regla de dependencias (SOLID):**
`controller → service → dao (interfaz) → dao.impl`. La vista nunca toca SQL ni JDBC;
`dao.impl` es la única capa que conoce PostgreSQL.

**Sin `module-info.java`** a propósito: el proyecto corre en modo classpath para no
tropezar con módulos automáticos de POI, Logback y jbcrypt. JavaFX lo resuelve el
plugin Maven solo.

---

## 5. Librerías del proyecto (Semana 10)

| Librería | Uso en el proyecto |
|---|---|
| PostgreSQL JDBC | conexión a la BD |
| HikariCP | pool de conexiones (reúso de conexiones en lugar de reconectar por consulta) |
| Logback | logging (base del plan de monitoreo, Semana 16) |
| Guava | utilidades del dominio |
| Apache Commons Lang3 | validaciones y utilidades de texto |
| Apache POI | reportes en Excel (M8, RNF-04) |
| jbcrypt | hash de contraseñas (RNF-01) |
| JUnit 5 | pruebas (Semana 13) |

---

## 6. Convenciones del equipo (para no pisarse)

**Ramas** (ya usadas): `main` (estable) ← `dev` (integración) ← `rama/<apellido>`.

```bash
git checkout dev && git pull
git checkout -b rama/tu-apellido
git add -A
git commit -m "M6: odontograma FDI con estados por pieza"
git push -u origin rama/tu-apellido
# En GitHub: Pull Request hacia dev; otro compañero revisa y hace Merge.
```

**Dueños por módulo** (nadie toca el código de otro sin avisar por el chat):

| Rol | Persona | Módulos |
|---|---|---|
| Frontend | Maick Vicente | FXML + controllers de todos los módulos |
| Backend | Yair Salamán | `model`, `dao`, `dao.impl`, `service`, SQL |
| Analista/Tester | Yarol Bello | casos de prueba, validación, reportes de pruebas |
| PM | Marc Anthony Berrocal | documentos, cronograma, sustentación |

**Nombres:** clases `PascalCase`, métodos/variables `camelCase`, tablas y columnas
`snake_case`, constantes en `MAYUSCULA_SNAKE`.

**Commits:** `<Módulo>: <qué hiciste>` — ej. `M7: abonos parciales con saldo por trigger`.

**Prohibido:**
- SQL o JDBC dentro de `controller` o FXML.
- Subir `config.properties`, `.env` o credenciales.
- Editar archivos de otro módulo sin avisar.
- Clases de más de 400 líneas (partirlas: señal de diseño incorrecto).

---

## 7. Modo de trabajo del equipo (ramas + PR)

Ramas: `main` (estable) ← `dev` (integración) ← `rama/<apellido>` (cada uno).

```bash
# 1. Trae lo último antes de empezar (o Team → Pull en NetBeans)
git checkout dev
git pull

# 2. Crea tu rama (una sola vez por tarea)
git checkout -b rama/tu-apellido

# 3. Trabaja, guarda y sube
git add -A
git commit -m "Describe tu cambio"
git push -u origin rama/tu-apellido

# 4. En GitHub: Pull requests → New → tu rama hacia dev → pide revisión.
#    Otro compañero revisa y hace Merge.
```

Reglas:
- Nunca subir directo a `main`.
- `pull` en `dev` antes de empezar cada sesión.
- No subir `target/`, `.idea/`, `*.class`, `.env` ni `config.properties`
  (ya están en `.gitignore`).

---

## 8. Las vistas base

| Archivo | Modelo | Uso |
|---|---|---|
| `fxml/menu-principal.fxml` | `VBox` (responsivo) | base de las pantallas nuevas |
| `fxml/panel-principal.fxml` | `AnchorPane` | experimentación / maquetación libre |

Decisión del equipo: las pantallas del sistema se maquetan sobre **VBox** (responsivo),
salvo pantallas con posición libre (odontograma, dashboard).

---

## 9. Problemas comunes

| Síntoma | Causa | Fix |
|---|---|---|
| `release version 25 not supported` | Maven corre con otro JDK | Paso 6: Maven Execution JDK = 25 |
| No aparece `JavaFX` en Options | Falta el plugin | Paso 5: instalar JavaFX Kit |
| Doble clic al FXML no abre diseñador | Falta Scene Builder Home | Paso 5: registrar el `.exe` |
| `No se encontro .../config.properties` | Falta tu config local | Paso 8: copiar el `.example` |
| `Connection refused` en PostgreSQL | BD caída o mal puerto | Paso 7/8: puerto 5432 y tu usuario |
| `Remote origin already exists` | Ya hay origin | `git remote set-url origin <URL>` |
| El IDE no ve cambios externos | Caché de NetBeans | Clic derecho al proyecto → `Reload POM` |
