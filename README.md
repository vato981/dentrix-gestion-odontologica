# Clínica Dental — Agenda y Programación de Citas

Proyecto del curso **Integrador I (UTP)**: aplicación JavaFX + Scene Builder
para la gestión de turnos odontológicos (Módulo 5: Agenda y Programación de Citas).

Repo: `https://github.com/maick7747/clinica-dental-agenda` (privado del equipo).
IDE del equipo: **Apache NetBeans**.

---

## 1. Requisitos (instalar una sola vez)

| Software | Versión | Descarga |
|---|---|---|
| JDK | **25** (Oracle OpenJDK 25.0.1+) | https://www.oracle.com/java/technologies/downloads/ (o Adoptium Temurin 25) |
| Apache NetBeans | **25 o superior** | https://netbeans.apache.org/download/ |
| Scene Builder | **26.0.0** (Gluon) | https://gluonhq.com/products/scene-builder/ |
| Git | última estable | https://git-scm.com/download/win |

Además necesitas una **cuenta de GitHub** y aceptar la invitación de
colaborador del repo (llega por correo).

---

## 2. Instalación paso a paso (desde cero)

### Paso 1 — Instalar el JDK 25
1. Instala el JDK 25 (ruta típica: `C:\Program Files\Java\jdk-25`).
2. Verifica en terminal:
   ```
   java -version
   ```
   Debe decir `25.x`.

### Paso 2 — Instalar Git y configurarte
1. Instala Git (siguiente, siguiente…).
2. Configura tu identidad (una sola vez):
   ```
   git config --global user.name "Tu Nombre"
   git config --global user.email "tu@correo.com"
   ```

### Paso 3 — Clonar el proyecto
1. Acepta la invitación de colaborador (correo de GitHub).
2. Clona:
   ```
   git clone https://github.com/maick7747/clinica-dental-agenda.git
   ```

### Paso 4 — Registrar el JDK 25 en NetBeans
1. `Tools → Java Platforms → Add Platform…` → `Java Standard Edition`
   → carpeta `C:\Program Files\Java\jdk-25` → nombre `JDK 25` → `Finish`.

### Paso 5 — Instalar el soporte JavaFX y enlazar Scene Builder
1. `Tools → Plugins → Available Plugins` → busca `JavaFX` → instala
   **JavaFX Kit** (acepta la licencia) → reinicia el IDE.
2. `Tools → Options → Java → JavaFX` → `Scene Builder Home` → `Browse` →
   `C:\Users\TU_USUARIO\AppData\Local\SceneBuilder\SceneBuilder.exe`
   → `Apply → OK`. (Normalmente NetBeans lo detecta solo.)

### Paso 6 — Maven con JDK 25
1. `Tools → Options → Java → Maven → Execution` → JDK por defecto = **JDK 25**.
2. Sin esto, la compilación falla con `release version 25 not supported`.

### Paso 7 — Abrir y correr
1. `File → Open Project` → carpeta `clinica-dental-agenda` → `Open`.
2. Clic derecho en `demofx` → `Set as Main Project` (si no lo es).
3. Pulsa **F6** o el ▶ verde: el archivo `nbactions.xml` ya mapea el Run
   a `javafx:run` y abre la ventana del programa.
4. Para diseñar: doble clic en `primary.fxml` o `anchor.fxml`
   (en `Other Sources → src/main/resources → org.example.demofx`) → se abren
   en Scene Builder dentro del IDE → edita → `Ctrl+S`.

---

## 3. Las dos vistas base

| Archivo | Modelo | Uso |
|---|---|---|
| `primary.fxml` | `VBox` (apilado automático, responsivo) | Reordena desde el **Hierarchy**, ajusta `spacing`/`padding` |
| `anchor.fxml` | `AnchorPane` (posición libre) | Arrastra con el mouse (`layoutX`/`layoutY`) |

Decisión del equipo: desarrollar sobre el modelo **responsivo (VBox)**;
AnchorPane queda como base de experimentación.

---

## 4. Modo de trabajo del equipo (ramas + PR)

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
- No subir `target/`, `.idea/`, `*.class` ni `.env` (ya están en `.gitignore`).

### Variables de entorno (`.env`)
- Las claves y conexiones (ej. base de datos) **nunca** van al repo.
- Copia `.env.example` como `.env` y completa TUS datos locales.
- Si agregas una variable nueva, agrégala también a `.env.example`
  (con valor de ejemplo, sin secretos) para que el equipo la conozca.

---

## 5. Estructura del proyecto

```
clinica-dental-agenda/
├── pom.xml                        # Maven: Java 25 + JavaFX 25.0.1
├── nbactions.xml                  # F6 → javafx:run en NetBeans
├── README.md
├── .env.example                   # Plantilla de variables (sin secretos)
└── src/main/
    ├── java/org/example/demofx/
    │   ├── App.java               # Arranque (ventana 480x320)
    │   ├── PrimaryController.java # Lógica vista VBox
    │   └── AnchorPaneController.java  # Lógica vista AnchorPane
    └── resources/org/example/demofx/
        ├── primary.fxml           # Vista VBox
        └── anchor.fxml            # Vista AnchorPane
```

---

## 6. Problemas comunes

| Síntoma | Causa | Fix |
|---|---|---|
| `release version 25 not supported` | Maven corre con otro JDK | Paso 6: Maven Execution JDK = 25 |
| No aparece `JavaFX` en Options | Falta el plugin | Paso 5: instalar JavaFX Kit |
| Doble clic al FXML no abre diseñador | Falta Scene Builder Home | Paso 5: registrar el `.exe` |
| `Remote origin already exists` | Ya hay origin | `git remote set-url origin <URL>` |
| El IDE no ve cambios externos | Caché de NetBeans | Clic derecho al proyecto → `Reload POM` / reabrir archivo |
