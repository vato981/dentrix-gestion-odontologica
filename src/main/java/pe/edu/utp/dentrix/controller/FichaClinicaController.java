package pe.edu.utp.dentrix.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;

import javafx.scene.layout.Pane;
import javafx.util.StringConverter;

import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.dto.CambioPrincipal;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.DiagnosticoCie10;
import pe.edu.utp.dentrix.model.Evolucion;
import pe.edu.utp.dentrix.model.FichaClinica;
import pe.edu.utp.dentrix.model.FichaDiagnostico;
import pe.edu.utp.dentrix.model.HallazgoDental;
import pe.edu.utp.dentrix.model.Paciente;
import pe.edu.utp.dentrix.model.PlanTratamiento;
import pe.edu.utp.dentrix.model.Tratamiento;
import pe.edu.utp.dentrix.ui.OdontogramaView;
import pe.edu.utp.dentrix.ui.TareasFX;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FichaClinicaController {

    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final List<String> ESTADOS_PIEZA = List.of("SANO", "CARIES", "OBTURADO", "AUSENTE",
            "CORONA", "IMPLANTE", "ENDODONCIA", "PROTESIS");
    private static final List<String> CARAS = List.of("oclusal", "mesial", "distal", "vestibular", "lingual");

    @FXML
    private ComboBox<Paciente> comboPaciente;

    @FXML
    private ComboBox<FichaClinica> comboFichas;

    @FXML
    private Label etiquetaFicha;

    @FXML
    private Label avatarPaciente;

    @FXML
    private Label nombrePaciente;

    @FXML
    private Label etiquetaHc;

    @FXML
    private Label etiquetaDni;

    @FXML
    private Label etiquetaEdad;

    @FXML
    private Label etiquetaEstadoFicha;

    @FXML
    private Label etiquetaAlergias;

    @FXML
    private TextArea areaMotivo;

    @FXML
    private TextArea areaExamen;

    @FXML
    private TextArea areaPrescripcion;

    @FXML
    private TextArea areaPlan;

    @FXML
    private Label etiquetaContenido;

    @FXML
    private ComboBox<DiagnosticoCie10> comboCie10;

    @FXML
    private ComboBox<Integer> comboPiezaDiag;

    @FXML
    private CheckBox checkPrincipal;

    @FXML
    private TextField campoObsDiag;

    @FXML
    private TableView<FichaDiagnostico> tablaDiagnosticos;

    @FXML
    private TableColumn<FichaDiagnostico, String> colDiagCodigo;

    @FXML
    private TableColumn<FichaDiagnostico, String> colDiagDescripcion;

    @FXML
    private TableColumn<FichaDiagnostico, String> colDiagPieza;

    @FXML
    private TableColumn<FichaDiagnostico, String> colDiagPrincipal;

    @FXML
    private TableColumn<FichaDiagnostico, String> colDiagObs;

    @FXML
    private TableView<CambioPrincipal> tablaHistorialPrincipal;

    @FXML
    private TableColumn<CambioPrincipal, String> colHistFecha;

    @FXML
    private TableColumn<CambioPrincipal, String> colHistAnterior;

    @FXML
    private TableColumn<CambioPrincipal, String> colHistNuevo;

    @FXML
    private TableColumn<CambioPrincipal, String> colHistMotivo;

    @FXML
    private ComboBox<Integer> comboPiezaOdonto;

    @FXML
    private ComboBox<String> comboEstadoPieza;

    @FXML
    private ComboBox<String> comboCara;

    @FXML
    private TextField campoObsPieza;

    @FXML
    private TableView<HallazgoDental> tablaOdontograma;

    @FXML
    private TableColumn<HallazgoDental, String> colOdoPieza;

    @FXML
    private TableColumn<HallazgoDental, String> colOdoEstado;

    @FXML
    private TableColumn<HallazgoDental, String> colOdoCara;

    @FXML
    private TableColumn<HallazgoDental, String> colOdoObs;

    @FXML
    private TableColumn<HallazgoDental, String> colOdoFecha;

    @FXML
    private ComboBox<Tratamiento> comboTratamiento;

    @FXML
    private ComboBox<Integer> comboPiezaPlan;

    @FXML
    private TextField campoCantidadPlan;

    @FXML
    private TableView<PlanTratamiento> tablaPlan;

    @FXML
    private TableColumn<PlanTratamiento, String> colPlanTrat;

    @FXML
    private TableColumn<PlanTratamiento, String> colPlanPieza;

    @FXML
    private TableColumn<PlanTratamiento, String> colPlanCant;

    @FXML
    private TableColumn<PlanTratamiento, String> colPlanPrecio;

    @FXML
    private TableColumn<PlanTratamiento, String> colPlanSubtotal;

    @FXML
    private TableColumn<PlanTratamiento, String> colPlanEstado;

    @FXML
    private Label etiquetaEstadoPlan;

    @FXML
    private Label etiquetaTotalPlan;

    @FXML
    private Label etiquetaDerivacion;

    @FXML
    private Label etiquetaDerivResultado;

    @FXML
    private TableView<Evolucion> tablaEvoluciones;

    @FXML
    private TableColumn<Evolucion, String> colEvoFecha;

    @FXML
    private TableColumn<Evolucion, String> colEvoEstado;

    @FXML
    private TableColumn<Evolucion, String> colEvoTexto;

    @FXML
    private TableColumn<Evolucion, String> colEvoEnmienda;

    @FXML
    private ToggleButton herrSano;

    @FXML
    private ToggleButton herrCaries;

    @FXML
    private ToggleButton herrObturado;

    @FXML
    private ToggleButton herrAusente;

    @FXML
    private Pane panelOdontograma;

    @FXML
    private Label etiquetaOdontograma;

    private final Map<String, String> descripcionesCie10 = new HashMap<>();
    private final Map<Integer, String> nombresTratamientos = new HashMap<>();
    private OdontogramaView odontograma;
    private FichaClinica fichaActual;
    private Integer odontologoSesion;

    @FXML
    private void initialize() {
        comboPaciente.setConverter(new StringConverter<Paciente>() {
            @Override
            public String toString(Paciente p) {
                return p == null ? "" : p.getApellidos() + ", " + p.getNombres() + " (HC "
                        + p.getCodigoHc() + ")";
            }

            @Override
            public Paciente fromString(String s) {
                return null;
            }
        });
        comboFichas.setConverter(new StringConverter<FichaClinica>() {
            @Override
            public String toString(FichaClinica f) {
                if (f == null) {
                    return "";
                }
                String fecha = f.getFecha() == null ? "" : FECHA_CORTA.format(f.getFecha());
                return "Ficha #" + f.getId() + " — " + fecha + " — deriv. " + f.getEstadoDerivacion();
            }

            @Override
            public FichaClinica fromString(String s) {
                return null;
            }
        });
        comboCie10.setConverter(new StringConverter<DiagnosticoCie10>() {
            @Override
            public String toString(DiagnosticoCie10 d) {
                return d == null ? "" : d.getCodigo() + " — " + d.getDescripcion();
            }

            @Override
            public DiagnosticoCie10 fromString(String s) {
                return null;
            }
        });
        comboTratamiento.setConverter(new StringConverter<Tratamiento>() {
            @Override
            public String toString(Tratamiento t) {
                return t == null ? "" : t.getCodigo() + " - " + t.getNombre() + " S/ " + t.getPrecio().toPlainString();
            }

            @Override
            public Tratamiento fromString(String s) {
                return null;
            }
        });

        List<Integer> piezas = generarPiezas();
        comboPiezaDiag.setItems(FXCollections.observableArrayList(piezas));
        comboPiezaDiag.setConverter(piezaConverter());
        comboPiezaOdonto.setItems(FXCollections.observableArrayList(piezas));
        comboPiezaPlan.setItems(FXCollections.observableArrayList(piezas));
        comboEstadoPieza.setItems(FXCollections.observableArrayList(ESTADOS_PIEZA));
        comboEstadoPieza.setValue("SANO");
        comboCara.setItems(FXCollections.observableArrayList(CARAS));

        ToggleGroup herramientas = new ToggleGroup();
        herrSano.setToggleGroup(herramientas);
        herrCaries.setToggleGroup(herramientas);
        herrObturado.setToggleGroup(herramientas);
        herrAusente.setToggleGroup(herramientas);
        odontograma = new OdontogramaView();
        odontograma.prefWidthProperty().bind(panelOdontograma.widthProperty());
        odontograma.prefHeightProperty().bind(panelOdontograma.heightProperty());
        panelOdontograma.getChildren().setAll(odontograma);

        // La vista conserva la selección; el controlador aplica el estado clínico.
        odontograma.setOnPiezaSeleccionada(this::aplicarHerramientaPieza);
        odontograma.reiniciar();


        colDiagCodigo.setCellValueFactory(new PropertyValueFactory<>("cie10Codigo"));
        colDiagDescripcion.setCellValueFactory(d -> new SimpleStringProperty(
                descripcionesCie10.getOrDefault(d.getValue().getCie10Codigo(), "")));
        colDiagPieza.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPiezaFdi() == null ? "" : String.valueOf(d.getValue().getPiezaFdi())));
        colDiagPrincipal.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().isEsPrincipal() ? "Sí" : "No"));
        colDiagObs.setCellValueFactory(new PropertyValueFactory<>("observacion"));

        colOdoPieza.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getPiezaFdi())));
        colOdoEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colOdoCara.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCara() == null ? "" : d.getValue().getCara()));
        colOdoObs.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getObservacion() == null ? "" : d.getValue().getObservacion()));
        colOdoFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaRegistro() == null ? "" : FECHA_CORTA.format(d.getValue().getFechaRegistro())));

        colPlanTrat.setCellValueFactory(d -> new SimpleStringProperty(
                nombresTratamientos.getOrDefault(d.getValue().getTratamientoId(),
                        "#" + d.getValue().getTratamientoId())));
        colPlanPieza.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPiezaFdi() == null ? "" : String.valueOf(d.getValue().getPiezaFdi())));
        colPlanCant.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getCantidad())));
        colPlanPrecio.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPrecioUnitario() == null ? "" : d.getValue().getPrecioUnitario().toPlainString()));
        colPlanSubtotal.setCellValueFactory(d -> new SimpleStringProperty(subtotal(d.getValue())));
        colPlanEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        colHistFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().fecha() == null ? "" : FECHA_CORTA.format(d.getValue().fecha())));
        colHistAnterior.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().cie10Anterior() == null ? "" : d.getValue().cie10Anterior()));
        colHistNuevo.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().cie10Nuevo() == null ? "" : d.getValue().cie10Nuevo()));
        colHistMotivo.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().motivo() == null ? "" : d.getValue().motivo()));

        colEvoFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() == null ? "" : FECHA_CORTA.format(d.getValue().getFecha())));
        colEvoEstado.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEstado() == null ? "" : d.getValue().getEstado()));
        colEvoTexto.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTexto() == null ? "" : d.getValue().getTexto()));
        colEvoEnmienda.setCellValueFactory(d -> new SimpleStringProperty(enmiendaEvolucion(d.getValue())));

        comboPaciente.valueProperty().addListener((obs, antes, ahora) -> pacienteCambiado());
        comboFichas.valueProperty().addListener((obs, antes, ahora) -> fichaCambiada(ahora));

        AppContext contexto = AppContext.get();
        if (!contexto.sesion().activa()) {
            return;
        }
        odontologoSesion = contexto.sesion().getUsuario().getId();
        TareasFX.ejecutar(this::cargarCombos,
                this::aplicarCombos,
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private record DatosCombos(List<Paciente> pacientes, List<DiagnosticoCie10> diagnosticos,
                               List<Tratamiento> tratamientos, List<Tratamiento> activos) {
    }

    private DatosCombos cargarCombos() throws DentrixException {
        AppContext contexto = AppContext.get();
        List<Paciente> activos = new ArrayList<>();
        for (Paciente p : contexto.pacientes().listar()) {
            if ("ACTIVO".equals(p.getEstado())) {
                activos.add(p);
            }
        }
        return new DatosCombos(activos, contexto.catalogo().listarDiagnosticos(),
                contexto.catalogo().listarTratamientos(), contexto.catalogo().listarTratamientosActivos());
    }

    private void aplicarCombos(DatosCombos datos) {
        comboPaciente.setItems(FXCollections.observableArrayList(datos.pacientes()));
        Paciente enAtencion = AppContext.get().sesion().getPacienteEnAtencion();
        if (enAtencion != null) {
            for (Paciente p : datos.pacientes()) {
                if (p.getId() != null && p.getId().equals(enAtencion.getId())) {
                    comboPaciente.setValue(p);
                    break;
                }
            }
        }
        comboCie10.setItems(FXCollections.observableArrayList(datos.diagnosticos()));
        descripcionesCie10.clear();
        for (DiagnosticoCie10 d : datos.diagnosticos()) {
            descripcionesCie10.put(d.getCodigo(), d.getDescripcion());
        }
        comboTratamiento.setItems(FXCollections.observableArrayList(datos.activos()));
        nombresTratamientos.clear();
        for (Tratamiento t : datos.tratamientos()) {
            nombresTratamientos.put(t.getId(), t.getNombre());
        }
    }

    private StringConverter<Integer> piezaConverter() {
        return new StringConverter<Integer>() {
            @Override
            public String toString(Integer pieza) {
                if (pieza == null) {
                    return "(sin pieza)";
                }
                return String.valueOf(pieza);
            }

            @Override
            public Integer fromString(String s) {
                return null;
            }
        };
    }

    private List<Integer> generarPiezas() {
        List<Integer> piezas = new ArrayList<>();
        for (int cuadrante = 1; cuadrante <= 4; cuadrante++) {
            for (int diente = 1; diente <= 8; diente++) {
                piezas.add(cuadrante * 10 + diente);
            }
        }
        return piezas;
    }

    private String subtotal(PlanTratamiento linea) {
        if (linea.getPrecioUnitario() == null || linea.getCantidad() == null) {
            return "";
        }
        return linea.getPrecioUnitario().multiply(BigDecimal.valueOf(linea.getCantidad())).toPlainString();
    }

    private void pacienteCambiado() {
        fichaActual = null;
        comboFichas.setValue(null);
        comboFichas.getItems().clear();
        limpiarFormularios();
        odontograma.reiniciar();
        Paciente paciente = comboPaciente.getValue();
        actualizarPaciente(paciente);
        if (paciente == null) {
            etiquetaFicha.setText("Selecciona un paciente para ver sus fichas.");
            return;
        }
        try {
            List<FichaClinica> fichas = AppContext.get().fichas().listarPorPaciente(paciente.getId());
            comboFichas.setItems(FXCollections.observableArrayList(fichas));
            List<HallazgoDental> hallazgos = AppContext.get().fichas().odontograma(paciente.getId());
            tablaOdontograma.setItems(FXCollections.observableArrayList(hallazgos));
            refrescarColoresOdontograma(hallazgos);
            etiquetaFicha.setText(fichas.size() + " ficha(s) para " + paciente.getNombres() + " "
                    + paciente.getApellidos());
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void fichaCambiada(FichaClinica ficha) {
        fichaActual = ficha;
        limpiarFormularios();
        if (ficha == null) {
            return;
        }
        pintarEstadoFicha(ficha.getEstadoDerivacion());
        areaMotivo.setText(ficha.getMotivoConsulta());
        areaExamen.setText(ficha.getExamenClinico());
        areaPrescripcion.setText(ficha.getPrescripcion());
        areaPlan.setText(ficha.getPlanIndicaciones());
        etiquetaFicha.setText("Ficha #" + ficha.getId() + " del " + FECHA_CORTA.format(ficha.getFecha())
                + " — derivación " + ficha.getEstadoDerivacion()
                + (ficha.isDerivadoACaja() ? " (derivada a caja)" : ""));
        etiquetaDerivacion.setText("Estado de derivación: " + ficha.getEstadoDerivacion()
                + (ficha.getFechaDerivacion() == null ? "" : " — derivada el "
                        + FECHA_CORTA.format(ficha.getFechaDerivacion())));
        recargarDiagnosticos();
        recargarPlan();
        cargarEvoluciones();
    }

    private void limpiarFormularios() {
        areaMotivo.clear();
        areaExamen.clear();
        areaPrescripcion.clear();
        areaPlan.clear();
        etiquetaContenido.setText("");
        etiquetaDerivResultado.setText("");
        etiquetaDerivacion.setText("Sin ficha seleccionada.");
        tablaDiagnosticos.setItems(FXCollections.observableArrayList());
        tablaHistorialPrincipal.setItems(FXCollections.observableArrayList());
        tablaPlan.setItems(FXCollections.observableArrayList());
        tablaEvoluciones.setItems(FXCollections.observableArrayList());
        etiquetaTotalPlan.setText("Total plan: S/ 0");
        pintarBadge(etiquetaTotalPlan, "badge-estado-gris");
        pintarBadge(etiquetaEstadoPlan, "badge-estado-gris");
        etiquetaEstadoPlan.setText("Sin plan");
        pintarEstadoFicha(null);
    }

    private void recargarDiagnosticos() {
        try {
            tablaDiagnosticos.setItems(FXCollections.observableArrayList(
                    AppContext.get().fichas().listarDiagnosticos(fichaActual.getId())));
            cargarHistorialPrincipal();
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void recargarPlan() {
        try {
            tablaPlan.setItems(FXCollections.observableArrayList(
                    AppContext.get().fichas().planPorFicha(fichaActual.getId())));
            actualizarResumenPlan();
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void recargarOdontograma() {
        try {
            Paciente paciente = comboPaciente.getValue();
            List<HallazgoDental> hallazgos = AppContext.get().fichas().odontograma(paciente.getId());
            tablaOdontograma.setItems(FXCollections.observableArrayList(hallazgos));
            refrescarColoresOdontograma(hallazgos);
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void cargarHistorialPrincipal() {
        if (fichaActual == null) {
            tablaHistorialPrincipal.setItems(FXCollections.observableArrayList());
            return;
        }
        Long fichaId = fichaActual.getId();
        TareasFX.ejecutar(() -> AppContext.get().fichas().historialPrincipal(fichaId),
                cambios -> {
                    if (fichaActual != null && fichaId.equals(fichaActual.getId())) {
                        tablaHistorialPrincipal.setItems(FXCollections.observableArrayList(cambios));
                    }
                },
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void cargarEvoluciones() {
        if (fichaActual == null) {
            tablaEvoluciones.setItems(FXCollections.observableArrayList());
            return;
        }
        Long fichaId = fichaActual.getId();
        TareasFX.ejecutar(() -> AppContext.get().ejecucion().evolucionesDeFicha(fichaId),
                evoluciones -> {
                    if (fichaActual != null && fichaId.equals(fichaActual.getId())) {
                        tablaEvoluciones.setItems(FXCollections.observableArrayList(evoluciones));
                    }
                },
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void actualizarPaciente(Paciente paciente) {
        if (paciente == null) {
            avatarPaciente.setText("--");
            nombrePaciente.setText("Paciente sin seleccionar");
            etiquetaHc.setText("HC --");
            etiquetaDni.setText("--");
            etiquetaEdad.setText("--");
            etiquetaAlergias.setText("Sin datos clinicos del paciente.");
            return;
        }
        avatarPaciente.setText(iniciales(paciente));
        nombrePaciente.setText(paciente.getApellidos() + ", " + paciente.getNombres());
        etiquetaHc.setText("HC " + (paciente.getCodigoHc() == null ? "--" : paciente.getCodigoHc()));
        etiquetaDni.setText(paciente.getDni() == null ? "--" : paciente.getDni());
        etiquetaEdad.setText(paciente.getFechaNac() == null ? "--"
                : Period.between(paciente.getFechaNac(), LocalDate.now()).getYears() + " años");
        StringBuilder alerta = new StringBuilder();
        if (paciente.getAlergias() != null && !paciente.getAlergias().isBlank()) {
            alerta.append("Alergias: ").append(paciente.getAlergias().trim());
        }
        if (paciente.getAntecedentes() != null && !paciente.getAntecedentes().isBlank()) {
            if (!alerta.isEmpty()) {
                alerta.append(" · ");
            }
            alerta.append("Antecedentes: ").append(paciente.getAntecedentes().trim());
        }
        etiquetaAlergias.setText(alerta.isEmpty()
                ? "Sin alergias ni antecedentes registrados." : alerta.toString());
    }

    private String iniciales(Paciente paciente) {
        StringBuilder iniciales = new StringBuilder();
        if (paciente.getNombres() != null && !paciente.getNombres().isBlank()) {
            iniciales.append(Character.toUpperCase(paciente.getNombres().trim().charAt(0)));
        }
        if (paciente.getApellidos() != null && !paciente.getApellidos().isBlank()) {
            iniciales.append(Character.toUpperCase(paciente.getApellidos().trim().charAt(0)));
        }
        return iniciales.isEmpty() ? "--" : iniciales.toString();
    }

    private void pintarEstadoFicha(String estado) {
        if ("ACEPTADA".equals(estado)) {
            etiquetaEstadoFicha.setText("Derivacion aceptada");
            pintarBadge(etiquetaEstadoFicha, "badge-estado-atendido");
        } else if ("RECHAZADA".equals(estado)) {
            etiquetaEstadoFicha.setText("Derivacion rechazada");
            pintarBadge(etiquetaEstadoFicha, "badge-estado-falto");
        } else if ("PENDIENTE".equals(estado)) {
            etiquetaEstadoFicha.setText("Derivacion pendiente");
            pintarBadge(etiquetaEstadoFicha, "badge-estado-sala");
        } else {
            etiquetaEstadoFicha.setText("Sin ficha");
            pintarBadge(etiquetaEstadoFicha, "badge-estado-gris");
        }
    }

    private void pintarBadge(Label etiqueta, String clase) {
        etiqueta.getStyleClass().removeAll("badge-estado-gris", "badge-estado-sala",
                "badge-estado-atendido", "badge-estado-falto", "badge-estado-info");
        etiqueta.getStyleClass().add(clase);
    }

    private void actualizarResumenPlan() {
        List<PlanTratamiento> lineas = tablaPlan.getItems();
        BigDecimal total = BigDecimal.ZERO;
        boolean hayLineas = false;
        boolean todasEjecutadas = true;
        boolean algunaEjecutada = false;
        if (lineas != null) {
            for (PlanTratamiento linea : lineas) {
                if (linea.getPrecioUnitario() != null && linea.getCantidad() != null) {
                    total = total.add(linea.getPrecioUnitario()
                            .multiply(BigDecimal.valueOf(linea.getCantidad())));
                }
                boolean ejecutada = "EJECUTADO".equals(linea.getEstado());
                hayLineas = true;
                algunaEjecutada = algunaEjecutada || ejecutada;
                todasEjecutadas = todasEjecutadas && ejecutada;
            }
        }
        etiquetaTotalPlan.setText("Total plan: S/ " + total.toPlainString());
        if (!hayLineas) {
            etiquetaEstadoPlan.setText("Sin plan");
            pintarBadge(etiquetaEstadoPlan, "badge-estado-gris");
        } else if (todasEjecutadas) {
            etiquetaEstadoPlan.setText("Plan ejecutado");
            pintarBadge(etiquetaEstadoPlan, "badge-estado-atendido");
        } else if (algunaEjecutada) {
            etiquetaEstadoPlan.setText("En ejecucion");
            pintarBadge(etiquetaEstadoPlan, "badge-estado-info");
        } else {
            etiquetaEstadoPlan.setText("Plan sin ejecutar");
            pintarBadge(etiquetaEstadoPlan, "badge-estado-sala");
        }
    }

    private String enmiendaEvolucion(Evolucion evolucion) {
        if (evolucion.getEnmiendaDe() == null) {
            return "";
        }
        String base = "Enmienda de #" + evolucion.getEnmiendaDe();
        if (evolucion.getMotivoEnmienda() != null && !evolucion.getMotivoEnmienda().isBlank()) {
            base += ": " + evolucion.getMotivoEnmienda().trim();
        }
        return base;
    }

    private void refrescarColoresOdontograma(List<HallazgoDental> hallazgos) {
        Map<Integer, HallazgoDental> ultimoHallazgo = new HashMap<>();
        for (HallazgoDental h : hallazgos) {
            ultimoHallazgo.put(h.getPiezaFdi(), h);
        }
        odontograma.limpiarEstados();
        for (HallazgoDental hallazgo : ultimoHallazgo.values()) {
            odontograma.setEstado(hallazgo.getPiezaFdi(), hallazgo.getEstado(), hallazgo.getCara());
        }
    }

    private void aplicarHerramientaPieza(int pieza) {
        Paciente paciente = comboPaciente.getValue();
        if (paciente == null) {
            mostrarInfo("Selecciona primero un paciente.");
            return;
        }
        String estado = herramientaSeleccionada();
        Integer pacienteId = paciente.getId();
        TareasFX.ejecutar(() -> {
            AppContext.get().fichas().actualizarEstadoPieza(pacienteId, pieza, estado, null, null);
            return null;
        }, ok -> {
            comboPiezaOdonto.setValue(pieza);
            etiquetaOdontograma.setText("Pieza " + pieza + " · " + OdontogramaView.tipoPieza(pieza)
                    + " → " + estado + " (RF-06 registrado)");
            etiquetaOdontograma.getStyleClass().removeAll("etiqueta-error");
            etiquetaOdontograma.getStyleClass().add("etiqueta-ok");
            recargarOdontograma();
        }, error -> mostrarError(TareasFX.mensaje(error)));
    }

    private String herramientaSeleccionada() {
        if (herrCaries.isSelected()) {
            return "CARIES";
        }
        if (herrObturado.isSelected()) {
            return "OBTURADO";
        }
        if (herrAusente.isSelected()) {
            return "AUSENTE";
        }
        return "SANO";
    }

    @FXML
    private void onNuevaFichaClick() {
        Paciente paciente = comboPaciente.getValue();
        if (paciente == null) {
            mostrarInfo("Selecciona el paciente de la nueva ficha.");
            return;
        }
        FichaClinica ficha = new FichaClinica();
        ficha.setPacienteId(paciente.getId());
        ficha.setOdontologoId(odontologoSesion);
        TareasFX.ejecutar(() -> AppContext.get().fichas().crear(ficha),
                creada -> {
                    pacienteCambiado();
                    comboFichas.setValue(creada);
                    comboFichas.getSelectionModel().select(creada);
                },
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onGuardarContenidoClick() {
        try {
            exigirFicha();
            Long fichaId = fichaActual.getId();
            String motivo = texto(areaMotivo);
            String examen = texto(areaExamen);
            String prescripcion = texto(areaPrescripcion);
            String plan = texto(areaPlan);
            TareasFX.ejecutar(() -> {
                AppContext.get().fichas().actualizarContenido(fichaId, motivo, examen, prescripcion, plan);
                return null;
            }, ok -> {
                etiquetaContenido.setText("Contenido guardado");
                etiquetaContenido.getStyleClass().removeAll("etiqueta-error", "etiqueta-ok");
                etiquetaContenido.getStyleClass().add("etiqueta-ok");
            }, error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onAgregarDiagnosticoClick() {
        try {
            exigirFicha();
            if (comboCie10.getValue() == null) {
                throw new DentrixException("Selecciona el diagnóstico CIE-10");
            }
            Long fichaId = fichaActual.getId();
            String cie10 = comboCie10.getValue().getCodigo();
            Integer pieza = comboPiezaDiag.getValue();
            boolean principal = checkPrincipal.isSelected();
            String observacion = texto(campoObsDiag);
            String motivoCambio = null;
            if (principal && hayOtroPrincipal(cie10)) {
                motivoCambio = pedirMotivoCambioPrincipal(cie10);
                if (motivoCambio == null) {
                    return;
                }
            }
            String motivoFinal = motivoCambio;
            TareasFX.ejecutar(() -> AppContext.get().fichas().registrarDiagnostico(fichaId, cie10, pieza,
                            principal, observacion, motivoFinal),
                    diag -> {
                        campoObsDiag.clear();
                        comboCie10.setValue(null);
                        comboPiezaDiag.setValue(null);
                        checkPrincipal.setSelected(false);
                        recargarDiagnosticos();
                        tablaDiagnosticos.getSelectionModel().select(diag);
                    },
                    error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private boolean hayOtroPrincipal(String cie10) {
        return tablaDiagnosticos.getItems().stream()
                .anyMatch(d -> d.isEsPrincipal() && !d.getCie10Codigo().equals(cie10));
    }

    private String pedirMotivoCambioPrincipal(String cie10) {
        String anterior = tablaDiagnosticos.getItems().stream()
                .filter(FichaDiagnostico::isEsPrincipal)
                .map(FichaDiagnostico::getCie10Codigo)
                .findFirst()
                .orElse("");
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Cambio de diagnóstico principal");
        dialogo.setHeaderText("El diagnóstico principal pasa de " + anterior + " a " + cie10);
        dialogo.setContentText("Motivo del cambio (queda registrado):");
        Optional<String> respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty() || respuesta.get().isBlank()) {
            mostrarError("El cambio de diagnóstico principal necesita un motivo (RF-06).");
            return null;
        }
        return respuesta.get().trim();
    }

    @FXML
    private void onQuitarDiagnosticoClick() {
        FichaDiagnostico seleccionado = tablaDiagnosticos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarInfo("Selecciona el diagnóstico a quitar.");
            return;
        }
        Long fichaId;
        try {
            exigirFicha();
            fichaId = fichaActual.getId();
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
            return;
        }
        String cie10 = seleccionado.getCie10Codigo();
        TareasFX.ejecutar(() -> {
            AppContext.get().fichas().eliminarDiagnostico(fichaId, cie10);
            return null;
        }, ok -> recargarDiagnosticos(), error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onQuitarTodosDiagnosticosClick() {
        try {
            exigirFicha();
            Long fichaId = fichaActual.getId();
            TareasFX.ejecutar(() -> {
                AppContext.get().fichas().eliminarTodosLosDiagnosticos(fichaId);
                return null;
            }, ok -> recargarDiagnosticos(), error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onGuardarPiezaClick() {
        try {
            Paciente paciente = comboPaciente.getValue();
            if (paciente == null) {
                throw new DentrixException("Selecciona el paciente del odontograma");
            }
            if (comboPiezaOdonto.getValue() == null) {
                throw new DentrixException("Selecciona la pieza dental");
            }
            Integer pacienteId = paciente.getId();
            Integer pieza = comboPiezaOdonto.getValue();
            String estado = comboEstadoPieza.getValue();
            String cara = comboCara.getValue();
            String observacion = texto(campoObsPieza);
            TareasFX.ejecutar(() -> {
                AppContext.get().fichas().actualizarEstadoPieza(pacienteId, pieza, estado, cara, observacion);
                return null;
            }, ok -> {
                campoObsPieza.clear();
                recargarOdontograma();
            }, error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onQuitarPiezaClick() {
        HallazgoDental seleccionado = tablaOdontograma.getSelectionModel().getSelectedItem();
        Paciente paciente = comboPaciente.getValue();
        if (seleccionado == null || paciente == null) {
            mostrarInfo("Selecciona la pieza del odontograma a quitar.");
            return;
        }
        Integer pacienteId = paciente.getId();
        Integer pieza = seleccionado.getPiezaFdi();
        TareasFX.ejecutar(() -> {
            AppContext.get().fichas().eliminarHallazgo(pacienteId, pieza);
            return null;
        }, ok -> recargarOdontograma(), error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onAgregarLineaPlanClick() {
        try {
            exigirFicha();
            if (comboTratamiento.getValue() == null) {
                throw new DentrixException("Selecciona el tratamiento del tarifario");
            }
            int cantidad;
            try {
                cantidad = Integer.parseInt(texto(campoCantidadPlan) == null ? "1" : texto(campoCantidadPlan));
            } catch (NumberFormatException e) {
                throw new DentrixException("La cantidad debe ser un número entero");
            }
            PlanTratamiento linea = new PlanTratamiento(fichaActual.getPacienteId(),
                    comboTratamiento.getValue().getId(), comboPiezaPlan.getValue(), cantidad, null, null);
            linea.setFichaId(fichaActual.getId());
            TareasFX.ejecutar(() -> {
                AppContext.get().fichas().agregarLinea(linea);
                return null;
            }, ok -> {
                campoCantidadPlan.setText("1");
                comboPiezaPlan.setValue(null);
                recargarPlan();
            }, error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onMarcarEjecutadaClick() {
        PlanTratamiento seleccionada = tablaPlan.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarInfo("Selecciona la línea del plan a marcar como ejecutada.");
            return;
        }
        Long lineaId = seleccionada.getId();
        TareasFX.ejecutar(() -> {
            AppContext.get().fichas().marcarEjecutada(lineaId);
            return null;
        }, ok -> recargarPlan(), error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onEliminarLineaClick() {
        PlanTratamiento seleccionada = tablaPlan.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarInfo("Selecciona la línea del plan a eliminar.");
            return;
        }
        Long lineaId = seleccionada.getId();
        TareasFX.ejecutar(() -> {
            AppContext.get().fichas().eliminarLinea(lineaId);
            return null;
        }, ok -> recargarPlan(), error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onGenerarPlanClick() {
        Paciente paciente = comboPaciente.getValue();
        if (fichaActual == null && paciente == null) {
            mostrarInfo("Selecciona o crea una ficha clinica antes de generar el plan.");
            return;
        }
        try {
            Integer pacienteId = fichaActual == null ? paciente.getId() : fichaActual.getPacienteId();
            Paciente deLaFicha = AppContext.get().pacientes().buscarPorId(pacienteId);
            AppContext.get().sesion().setPacienteEnAtencion(deLaFicha);
            pe.edu.utp.dentrix.ui.Navegador.ir(pe.edu.utp.dentrix.ui.Navegador.PRESUPUESTOS);
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onDerivarClick() {
        try {
            exigirFicha();
            Long fichaId = fichaActual.getId();
            TareasFX.ejecutar(() -> {
                AppContext.get().fichas().derivarACaja(fichaId);
                return null;
            }, ok -> {
                refrescarFichaActual();
                etiquetaDerivResultado.setText("Ficha derivada a caja con estado ACEPTADA; ya puede facturarse (RF-07).");
            }, error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onRechazarClick() {
        try {
            exigirFicha();
            Long fichaId = fichaActual.getId();
            TareasFX.ejecutar(() -> {
                AppContext.get().fichas().rechazarDerivacion(fichaId);
                return null;
            }, ok -> {
                refrescarFichaActual();
                etiquetaDerivResultado.setText("Derivación marcada como RECHAZADA.");
            }, error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onVerificarCobrableClick() {
        try {
            exigirFicha();
            boolean cobrable = AppContext.get().fichas().esCobrable(fichaActual.getId());
            etiquetaDerivResultado.setText(cobrable
                    ? "La ficha ES cobrable: su derivación está ACEPTADA."
                    : "La ficha NO es cobrable todavía (derivación " + fichaActual.getEstadoDerivacion() + ").");
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void refrescarFichaActual() {
        try {
            FichaClinica recargada = AppContext.get().fichas().buscarPorId(fichaActual.getId());
            Paciente paciente = comboPaciente.getValue();
            fichaActual = recargada;
            comboFichas.setItems(FXCollections.observableArrayList(
                    AppContext.get().fichas().listarPorPaciente(paciente.getId())));
            comboFichas.setValue(recargada);
            pintarEstadoFicha(recargada.getEstadoDerivacion());
            etiquetaDerivacion.setText("Estado de derivación: " + recargada.getEstadoDerivacion()
                    + (recargada.getFechaDerivacion() == null ? "" : " — derivada el "
                            + FECHA_CORTA.format(recargada.getFechaDerivacion())));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void exigirFicha() throws DentrixException {
        if (fichaActual == null) {
            throw new DentrixException("Selecciona o crea una ficha clínica primero");
        }
    }

    private String texto(TextArea area) {
        String valor = area.getText() == null ? "" : area.getText().trim();
        return valor.isEmpty() ? null : valor;
    }

    private String texto(TextField campo) {
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        return valor.isEmpty() ? null : valor;
    }

    @FXML
    private void onRefrescarClick() {
        Paciente paciente = comboPaciente.getValue();
        if (paciente != null) {
            pacienteCambiado();
        }
    }

    @FXML
    private void onVolverClick() {
        pe.edu.utp.dentrix.ui.Navegador.ir(pe.edu.utp.dentrix.ui.Navegador.INICIO);
    }

    private void mostrarInfo(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Ficha clínica");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Ficha clínica");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
