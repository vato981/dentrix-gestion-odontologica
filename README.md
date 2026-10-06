# Clínica Dental — Agenda y Programación de Citas

Proyecto del curso **Integrador I (UTP)**: aplicación JavaFX + Scene Builder
para la gestión de turnos odontológicos (Módulo 5: Agenda y Programación de Citas).

Repo: `https://github.com/maick7747/clinica-dental-agenda` (privado del equipo).

---

## 1. Requisitos (instalar una sola vez)

| Software | Versión | Descarga |
|---|---|---|
| JDK | **25** (Oracle OpenJDK 25.0.1+) | https://www.oracle.com/java/technologies/downloads/ (o Adoptium Temurin 25) |
| IntelliJ IDEA | **Community** (gratis) | https://www.jetbrains.com/idea/download/ |
| Scene Builder | **26.0.0** (Gluon) | https://gluonhq.com/products/scene-builder/ |
| Git | última estable | https://git-scm.com/download/win |

Además necesitas una **cuenta de GitHub** y aceptar la invitación de
colaborador del repo (llega por correo).

> **Nota:** en IntelliJ 2025.3 el plugin oficial `JavaFX` aparece como
> "solo Ultimate" (bug `IDEA-383145` de JetBrains, no es tu culpa).
> No lo necesitas: este proyecto abre los `.fxml` con Scene Builder
> externo (paso 6).

---

## 2. Instalación paso a paso (desde cero)

### Paso 1 — Instalar el JDK 25
1. Instala el JDK 25 con el instalador (ruta típica: `C:\Program Files\Java\jdk-25`).
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

### Paso 4 — Instalar IntelliJ IDEA Community
1. Descarga la edición **Community** e instálala.
2. No toques nada del plugin JavaFX (ver nota arriba).

### Paso 5 — Instalar Scene Builder
1. Instala Scene Builder 26. Ruta típica:
   ```
   C:\Users\TU_USUARIO\AppData\Local\SceneBuilder\SceneBuilder.exe
   ```
2. Ábrelo una vez para comprobar que inicia, y ciérralo.

### Paso 6 — Abrir el proyecto en IntelliJ
1. `File → Open` → selecciona la carpeta clonada `clinica-dental-agenda`.
2. Si pide SDK, elige **25** (el JDK del paso 1).
3. Espera a que IntelliJ importe Maven y descargue JavaFX
   (barra de progreso abajo, 1-2 min la primera vez).
   Si algo falta: panel Maven (derecha) → botón **Reload**.

### Paso 7 — Conectar Scene Builder (External Tool)
1. `Ctrl+Alt+S` → `Tools → External Tools` → `+`.
2. Completa **exacto**:
   - Name: `SceneBuilder`
   - Program: `C:\Users\TU_USUARIO\AppData\Local\SceneBuilder\SceneBuilder.exe`
   - Arguments: `"$FilePath$"` **← ¡CON comillas dobles!**
   - Working directory: `$FileDir$`
3. `Apply → OK`.

> Sin las comillas, las rutas con espacios se rompen en pedazos
> (error `Could not open ... / Failed to open 8 of 8 files!`).

### Paso 8 — Correr el programa
1. Panel Maven → `Plugins → javafx → javafx:run` (doble clic).
2. Debe abrir la ventana **Dentrix Dental — Agenda y Programación de Turnos**.
3. Para diseñar: clic derecho en `primary.fxml` o `anchor.fxml`
   → `External Tools → SceneBuilder` → edita → `Ctrl+S`.

---

## 3. Las dos vistas

| Archivo | Modelo | Uso |
|---|---|---|
| `primary.fxml` | `VBox` (apilado automático) | Reordena desde el **Hierarchy**, ajusta `spacing`/`padding` |
| `anchor.fxml` | `AnchorPane` (posición libre) | Arrastra con el mouse (`layoutX`/`layoutY`) |

Ambas comparten `agenda.css`, el modelo `Cita.java` y la misma lógica.
El botón de la barra superior salta de una vista a la otra.

---

## 4. Flujo de trabajo del equipo (ramas + PR)

Ramas: `main` (estable) ← `dev` (integración) ← `rama/<apellido>` (cada uno).

```bash
# 1. Trae lo último antes de empezar
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
- Nunca subas directo a `main`.
- Haz `pull` en `dev` antes de empezar cada sesión.
- No subir `target/`, `.idea/` ni `*.class` (ya están en `.gitignore`).

---

## 5. Estructura del proyecto

```
clinica-dental-agenda/
├── pom.xml                        # Maven: Java 25 + JavaFX 25.0.1
├── README.md
└── src/main/
    ├── java/org/example/demofx/
    │   ├── App.java               # Arranque (ventana 1120x690)
    │   ├── Cita.java              # Modelo de la tabla de turnos
    │   ├── PrimaryController.java # Lógica vista VBox
    │   └── AnchorPaneController.java  # Lógica vista AnchorPane
    └── resources/org/example/demofx/
        ├── primary.fxml           # Vista VBox
        ├── anchor.fxml            # Vista AnchorPane
        └── agenda.css             # Estilos Dentrix compartidos
```

---

## 6. Problemas comunes

| Síntoma | Causa | Fix |
|---|---|---|
| `Could not open '...' / Failed to open N of N files` | Arguments sin comillas | Paso 7: usa `"$FilePath$"` |
| No aparece `Languages & Frameworks → JavaFX` | Bug JetBrains 2025.3 (plugin marcado Ultimate) | Normal, no se necesita; usa el External Tool |
| `invalid target release: 25` / SDK en rojo | Proyecto sin JDK 25 | `File → Project Structure → SDK → 25` |
| Dependencias en rojo | Maven no descargó | Panel Maven → **Reload** (con internet) |
| Scene Builder no abre el archivo | Ruta del Program incorrecta | Revisa paso 7 (ruta de TU usuario) |
| `Remote origin already exists` | Ya hay origin | `git remote set-url origin <URL>` |
