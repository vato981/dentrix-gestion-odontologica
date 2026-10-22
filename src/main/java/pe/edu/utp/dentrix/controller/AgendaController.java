package pe.edu.utp.dentrix.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Cita;
import pe.edu.utp.dentrix.model.CitaHistorial;
import pe.edu.utp.dentrix.model.Consultorio;
import pe.edu.utp.dentrix.model.ListaEspera;
import pe.edu.utp.dentrix.model.Paciente;
import pe.edu.utp.dentrix.model.Recordatorio;
import pe.edu.utp.dentrix.model.Tratamiento;
import pe.edu.utp.dentrix.model.Usuario;
import pe.edu.utp.dentrix.ui.Badges;
import pe.edu.utp.dentrix.ui.TareasFX;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AgendaController {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final List<String> TIPOS = List.of("CITA", "ALMUERZO", "CIERRE", "BLOQUEADO");
    private static final List<String> CANALES = List.of("WHATSAPP", "SMS", "LLAMADA", "EMAIL");
    private static final List<String> PRIORIDADES = List.of("1", "2", "3", "4", "5");
    private static final List<String> ESTADOS_FILTRO = List.of("Todos", "PENDIENTE", "CONFIRMADA",
            "EN_SALA", "EN_ATENCION", "ATENDIDA", "FALTO", "CANCELADA", "BLOQUEADA");
    private static final String TODOS = "Todos";
    private static final int DURACION_CONSULTA = 30;
    private static final int DURACION_BLOQUE = 60;
    private static final String FXML_FICHA = "/pe/edu/utp/dentrix/fxml/ficha-clinica.fxml";

    @FXML
    private DatePicker campoFecha;

    @FXML
    private Label etiquetaHoy;

    @FXML
    private Label etiquetaConteo;

    @FXML
    private ComboBox<String> filtroOdontologo;

    @FXML
    private ComboBox<String> filtroConsultorio;

    @FXML
    private ComboBox<String> filtroEstado;

    @FXML
    private Label etiquetaModo;

    @FXML
    private Label etiquetaTipo;

    @FXML
    private ComboBox<String> comboTipo;

    @FXML
    private TextField campoDniPaciente;

    @FXML
    private HBox cajaBusquedaPaciente;

    @FXML
    private Label etiquetaAvatarPaciente;

    @FXML
    private Label etiquetaPacienteTarjeta;

    @FXML
    private Label etiquetaDatosPaciente;

    @FXML
    private VBox cajaPaciente;

    @FXML
    private TextField campoInicio;

    @FXML
    private TextField campoFin;

    @FXML
    private Label etiquetaPaciente;

    @FXML
    private ComboBox<Paciente> comboPaciente;

    @FXML
    private CheckBox checkPrimeraVez;

    @FXML
    private ComboBox<Usuario> comboOdontologo;

    @FXML
    private ComboBox<Consultorio> comboConsultorio;

    @FXML
    private Label etiquetaTratamiento;

    @FXML
    private ComboBox<Tratamiento> comboTratamiento;

    @FXML
    private Label etiquetaDuracion;

    @FXML
    private Label etiquetaFinCalculo;

    @FXML
    private Label etiquetaMotivo;

    @FXML
    private TextField campoMotivo;

    @FXML
    private Label etiquetaDisponibilidad;

    @FXML
    private Label etiquetaError;

    @FXML
    private TableView<Cita> tablaAgenda;

    @FXML
    private TableColumn<Cita, String> colHora;

    @FXML
    private TableColumn<Cita, String> colPaciente;

    @FXML
    private TableColumn<Cita, String> colOdontologo;

    @FXML
    private TableColumn<Cita, String> colConsultorio;

    @FXML
    private TableColumn<Cita, String> colEstado;

    @FXML
    private TableColumn<Cita, String> colTratamiento;

    @FXML
    private TableColumn<Cita, String> colAccion;

    @FXML
    private Label etiquetaAviso;

    @FXML
    private Button btnReprogramar;

    @FXML
    private Button btnHistorial;

    @FXML
    private Label etiquetaEstadoBadge;

    @FXML
    private Label etiquetaDetalleAvatar;

    @FXML
    private Label etiquetaDetallePaciente;

    @FXML
    private Label etiquetaDetalleDocumento;

    @FXML
    private Label etiquetaDetalleHorario;

    @FXML
    private Label etiquetaDetalleTratamiento;

    @FXML
    private Label etiquetaDetalleOdontologo;

    @FXML
    private Label etiquetaDetalleConsultorio;

    @FXML
    private Label etiquetaDetalleObservacion;

    @FXML
    private Label etiquetaOperacion;

    @FXML
    private TableView<ListaEspera> tablaEspera;

    @FXML
    private TableColumn<ListaEspera, String> colEsperaPaciente;

    @FXML
    private TableColumn<ListaEspera, String> colEsperaTratamiento;

    @FXML
    private TableColumn<ListaEspera, String> colEsperaPrioridad;

    @FXML
    private TableColumn<ListaEspera, String> colEsperaDesde;

    @FXML
    private Button btnAgregarEspera;

    @FXML
    private Button btnRetirarEspera;

    @FXML
    private Button btnAsignarCita;

    @FXML
    private Label etiquetaEspera;

    @FXML
    private TableView<Recordatorio> tablaRecordatorios;

    @FXML
    private TableColumn<Recordatorio, String> colRecCanal;

    @FXML
    private TableColumn<Recordatorio, String> colRecProgramado;

    @FXML
    private TableColumn<Recordatorio, String> colRecEstado;

    @FXML
    private Label etiquetaRecCita;

    @FXML
    private Button btnProgramarRecordatorio;

    @FXML
    private Button btnMarcarEnviado;

    @FXML
    private Label etiquetaRecordatorio;

    private final Map<Integer, String> nombresPacientes = new HashMap<>();
    private final Map<Integer, String> nombresOdontologos = new HashMap<>();
    private final Map<Integer, String> consultoriosPorCodigo = new HashMap<>();
    private final Map<Integer, String> nombresTratamientos = new HashMap<>();

    private List<Paciente> pacientesActivos = new ArrayList<>();
    private List<Tratamiento> tratamientosActivos = new ArrayList<>();
    private List<Consultorio> consultoriosCombo = new ArrayList<>();
    private List<Cita> citasDelDia = new ArrayList<>();
    private boolean puedeAccionesClinicas;

    @FXML
    private void initialize() {
        colHora.setCellValueFactory(d -> new SimpleStringProperty(hora(d.getValue())));
        colPaciente.setCellValueFactory(d -> new SimpleStringProperty(
                "CITA".equals(d.getValue().getTipo())
                        ? nombresPacientes.getOrDefault(d.getValue().getPacienteId(), "—") : "—"));
        colOdontologo.setCellValueFactory(d -> new SimpleStringProperty(
                nombresOdontologos.getOrDefault(d.getValue().getUsuarioId(), "—")));
        colConsultorio.setCellValueFactory(d -> new SimpleStringProperty(
                consultoriosPorCodigo.getOrDefault(d.getValue().getConsultorioId(), "—")));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(estadoTabla(d.getValue())));
        colTratamiento.setCellValueFactory(d -> new SimpleStringProperty(tratamientoODespacho(d.getValue())));
        Badges.aplicarEn(colEstado);
        colAccion.setCellFactory(col -> new TableCell<>() {
            private final Button btnPrincipal = new Button();
            private final Button btnMas = new Button("▾");
            private final ContextMenu menu = new ContextMenu();
            private final HBox caja = new HBox(6.0, btnPrincipal, btnMas);

            {
                btnPrincipal.getStyleClass().addAll("boton-mini", "boton-primario");
                btnMas.getStyleClass().addAll("boton-mini", "boton-primario");
                btnMas.setTooltip(new Tooltip("Mas acciones de la cita"));
                btnMas.setOnAction(e -> menu.show(btnMas, javafx.geometry.Side.BOTTOM, 0, 0));
            }

            @Override
            protected void updateItem(String item, boolean vacio) {
                super.updateItem(item, vacio);
                int indice = getIndex();
                Cita cita = vacio || indice < 0 || indice >= getTableView().getItems().size()
                        ? null : getTableView().getItems().get(indice);
                if (cita == null || !"CITA".equals(cita.getTipo())) {
                    setGraphic(null);
                    return;
                }
                menu.getItems().clear();
                boolean principal = false;
                boolean mas = false;
                String estado = cita.getEstado();
                switch (estado) {
                    case "PENDIENTE" -> {
                        prepararPrincipal(btnPrincipal, "Confirmar",
                                () -> aplicarTransicion(cita, AppContext.get().agenda()::confirmar, null));
                        agregarAccion(menu, "En sala",
                                () -> aplicarTransicion(cita, AppContext.get().agenda()::marcarLlegada, null));
                        agregarAccion(menu, "Falto",
                                () -> aplicarTransicion(cita, AppContext.get().agenda()::marcarFalto, null));
                        agregarAccion(menu, "Cancelar", () -> cancelar(cita));
                        principal = true;
                        mas = true;
                    }
                    case "CONFIRMADA" -> {
                        prepararPrincipal(btnPrincipal, "En sala",
                                () -> aplicarTransicion(cita, AppContext.get().agenda()::marcarLlegada, null));
                        agregarAccion(menu, "Falto",
                                () -> aplicarTransicion(cita, AppContext.get().agenda()::marcarFalto, null));
                        agregarAccion(menu, "Cancelar", () -> cancelar(cita));
                        principal = true;
                        mas = true;
                    }
                    case "EN_SALA" -> {
                        if (puedeAccionesClinicas) {
                            prepararPrincipal(btnPrincipal, "Iniciar", () -> iniciar(cita));
                            principal = true;
                        }
                        agregarAccion(menu, "Cancelar", () -> cancelar(cita));
                        mas = true;
                    }
                    case "EN_ATENCION" -> {
                        if (puedeAccionesClinicas) {
                            prepararPrincipal(btnPrincipal, "Finalizar",
                                    () -> aplicarTransicion(cita, AppContext.get().agenda()::finalizar, null));
                            principal = true;
                        }
                    }
                    default -> {
                    }
                }
                btnPrincipal.setVisible(principal);
                btnPrincipal.setManaged(principal);
                btnMas.setVisible(mas);
                btnMas.setManaged(mas);
                setGraphic(principal || mas ? caja : null);
            }
        });
        tablaAgenda.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        campoFecha.valueProperty().addListener((obs, antes, ahora) -> {
            actualizarIndicadorHoy();
            recargar();
        });
        comboTipo.valueProperty().addListener((obs, antes, ahora) -> aplicarVisibilidadTipo());
        checkPrimeraVez.selectedProperty().addListener((obs, antes, ahora) -> actualizarTratamiento());
        comboTratamiento.valueProperty().addListener((obs, antes, ahora) -> recalcularFin());
        campoInicio.textProperty().addListener((obs, antes, ahora) -> recalcularFin());
        comboPaciente.valueProperty().addListener((obs, antes, ahora) -> actualizarTarjetaPaciente());
        filtroOdontologo.valueProperty().addListener((obs, antes, ahora) -> aplicarFiltros());
        filtroConsultorio.valueProperty().addListener((obs, antes, ahora) -> aplicarFiltros());
        filtroEstado.valueProperty().addListener((obs, antes, ahora) -> aplicarFiltros());

        colEsperaPaciente.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPacienteNombre() == null ? "—" : d.getValue().getPacienteNombre()));
        colEsperaTratamiento.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTratamientoNombre() == null ? "—" : d.getValue().getTratamientoNombre()));
        colEsperaPrioridad.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPrioridad() == null ? "" : String.valueOf(d.getValue().getPrioridad())));
        colEsperaDesde.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaAlta() == null ? "" : d.getValue().getFechaAlta().format(FECHA_HORA)));

        colRecCanal.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCanal()));
        colRecProgramado.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getProgramado() == null ? "" : d.getValue().getProgramado().format(FECHA_HORA)));
        colRecEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado()));
        Badges.aplicarEn(colRecEstado);

        tablaAgenda.getSelectionModel().selectedItemProperty()
                .addListener((obs, antes, cita) -> alSeleccionarCita(cita));
        tablaEspera.getSelectionModel().selectedItemProperty()
                .addListener((obs, antes, espera) -> actualizarBotonesEspera());
        tablaRecordatorios.getSelectionModel().selectedItemProperty()
                .addListener((obs, antes, recordatorio) -> btnMarcarEnviado.setDisable(recordatorio == null));

        btnReprogramar.setDisable(true);
        btnHistorial.setDisable(true);
        btnProgramarRecordatorio.setDisable(true);
        btnMarcarEnviado.setDisable(true);
        btnRetirarEspera.setDisable(true);
        btnAsignarCita.setDisable(true);
        etiquetaAviso.setVisible(false);
        etiquetaAviso.setManaged(false);
        filtroOdontologo.setItems(FXCollections.observableArrayList(TODOS));
        filtroConsultorio.setItems(FXCollections.observableArrayList(TODOS));
        filtroEstado.setItems(FXCollections.observableArrayList(ESTADOS_FILTRO));
        filtroOdontologo.setValue(TODOS);
        filtroConsultorio.setValue(TODOS);
        filtroEstado.setValue(TODOS);

        AppContext contexto = AppContext.get();
        if (!contexto.sesion().activa()) {
            return;
        }
        puedeAccionesClinicas = "ADMINISTRADOR".equals(contexto.sesion().getCodigoRol())
                || "ODONTOLOGO".equals(contexto.sesion().getCodigoRol());
        campoFecha.setValue(LocalDate.now());
        actualizarIndicadorHoy();
        comboTipo.setItems(FXCollections.observableArrayList(TIPOS));
        comboTipo.setValue("CITA");
        comboPaciente.setConverter(pacienteConverter());
        comboTratamiento.setConverter(tratamientoConverter());
        comboOdontologo.setConverter(odontologoConverter());
        comboConsultorio.setConverter(consultorioConverter());
        limpiarForma();
        TareasFX.ejecutar(this::cargarDatosIniciales,
                this::aplicarDatosIniciales,
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void prepararPrincipal(Button boton, String texto, Runnable accion) {        boton.setText(texto);
        boton.setOnAction(e -> accion.run());
    }

    private void agregarAccion(ContextMenu boton, String texto, Runnable accion) {
        MenuItem item = new MenuItem(texto);
        item.setOnAction(e -> accion.run());
        boton.getItems().add(item);
    }

    private void actualizarIndicadorHoy() {
        boolean hoy = LocalDate.now().equals(campoFecha.getValue());
        etiquetaHoy.setVisible(hoy);
        etiquetaHoy.setManaged(hoy);
    }

    private String estadoTabla(Cita cita) {
        if (!"CITA".equals(cita.getTipo())) {
            return "BLOQUEADA";
        }
        return cita.getEstado();
    }

    private record DatosIniciales(Map<Integer, String> pacientes, Map<Integer, String> odontologos,
                                  Map<Integer, String> consultorios, Map<Integer, String> tratamientos,
                                  List<Paciente> pacientesActivos, List<Tratamiento> tratamientosActivos,
                                  List<Usuario> odontologosCombo, List<Consultorio> consultoriosCombo) {
    }

    private DatosIniciales cargarDatosIniciales() throws DentrixException {
        AppContext contexto = AppContext.get();

        Map<Integer, String> pacientes = new HashMap<>();
        List<Paciente> pacientesActivos = new ArrayList<>();
        for (Paciente p : contexto.pacientes().listar()) {
            pacientes.put(p.getId(), p.getApellidos() + ", " + p.getNombres());
            if ("ACTIVO".equals(p.getEstado())) {
                pacientesActivos.add(p);
            }
        }

        Integer rolOdontologo = contexto.roles().buscarPorCodigo("ODONTOLOGO")
                .orElseThrow(() -> new DentrixException("Rol ODONTOLOGO no configurado")).getId();
        Integer rolAdmin = contexto.roles().buscarPorCodigo("ADMINISTRADOR")
                .orElseThrow(() -> new DentrixException("Rol ADMINISTRADOR no configurado")).getId();
        Map<Integer, String> odontologos = new HashMap<>();
        List<Usuario> odontologosCombo = new ArrayList<>();
        for (Usuario u : contexto.usuarios().listarTodos()) {
            odontologos.put(u.getId(), "Dr. " + u.getNombres() + " " + u.getApellidos());
            if ("ACTIVO".equals(u.getEstado())
                    && (rolOdontologo.equals(u.getRolId()) || rolAdmin.equals(u.getRolId()))) {
                odontologosCombo.add(u);
            }
        }

        Map<Integer, String> consultorios = new HashMap<>();
        List<Consultorio> listaConsultorios = contexto.catalogo().listarConsultorios();
        for (Consultorio c : listaConsultorios) {
            consultorios.put(c.getId(), c.getCodigo());
        }

        Map<Integer, String> tratamientos = new HashMap<>();
        for (Tratamiento t : contexto.catalogo().listarTratamientos()) {
            tratamientos.put(t.getId(), t.getNombre());
        }

        return new DatosIniciales(pacientes, odontologos, consultorios, tratamientos, pacientesActivos,
                contexto.catalogo().listarTratamientosActivos(), odontologosCombo, listaConsultorios);
    }

    private void aplicarDatosIniciales(DatosIniciales datos) {
        nombresPacientes.clear();
        nombresPacientes.putAll(datos.pacientes());
        nombresOdontologos.clear();
        nombresOdontologos.putAll(datos.odontologos());
        consultoriosPorCodigo.clear();
        consultoriosPorCodigo.putAll(datos.consultorios());
        nombresTratamientos.clear();
        nombresTratamientos.putAll(datos.tratamientos());
        pacientesActivos = new ArrayList<>(datos.pacientesActivos());
        tratamientosActivos = new ArrayList<>(datos.tratamientosActivos());
        consultoriosCombo = new ArrayList<>(datos.consultoriosCombo());
        comboPaciente.setItems(FXCollections.observableArrayList(datos.pacientesActivos()));
        comboTratamiento.setItems(FXCollections.observableArrayList(datos.tratamientosActivos()));
        comboOdontologo.setItems(FXCollections.observableArrayList(datos.odontologosCombo()));
        comboConsultorio.setItems(FXCollections.observableArrayList(datos.consultoriosCombo()));
        List<String> nombres = new ArrayList<>();
        nombres.add(TODOS);
        nombres.addAll(new LinkedHashSet<>(datos.odontologos().values()));
        filtroOdontologo.setItems(FXCollections.observableArrayList(nombres));
        filtroOdontologo.setValue(TODOS);
        List<String> codigos = new ArrayList<>();
        codigos.add(TODOS);
        codigos.addAll(new LinkedHashSet<>(datos.consultorios().values()));
        filtroConsultorio.setItems(FXCollections.observableArrayList(codigos));
        filtroConsultorio.setValue(TODOS);
        recargar();
    }

    private StringConverter<Paciente> pacienteConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Paciente p) {
                return p == null ? "" : p.getApellidos() + ", " + p.getNombres() + " (DNI " + p.getDni() + ")";
            }

            @Override
            public Paciente fromString(String s) {
                return null;
            }
        };
    }

    private StringConverter<Usuario> odontologoConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Usuario u) {
                return u == null ? "" : "Dr. " + u.getNombres() + " " + u.getApellidos()
                        + (u.getColegiatura() == null ? "" : " (" + u.getColegiatura() + ")");
            }

            @Override
            public Usuario fromString(String s) {
                return null;
            }
        };
    }

    private StringConverter<Consultorio> consultorioConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Consultorio c) {
                return c == null ? "" : c.getCodigo() + " - " + c.getNombre();
            }

            @Override
            public Consultorio fromString(String s) {
                return null;
            }
        };
    }

    private StringConverter<Tratamiento> tratamientoConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Tratamiento t) {
                return t == null ? ""
                        : t.getCodigo() + " - " + t.getNombre() + " (" + duracion(t) + " min)";
            }

            @Override
            public Tratamiento fromString(String s) {
                return null;
            }
        };
    }

    private int duracion(Tratamiento tratamiento) {
        return tratamiento == null || tratamiento.getDuracionMinutos() == null
                ? DURACION_CONSULTA : tratamiento.getDuracionMinutos();
    }

    private boolean esModoBloque() {
        return comboTipo.getValue() != null && !"CITA".equals(comboTipo.getValue());
    }

    private void aplicarVisibilidadTipo() {
        boolean bloque = esModoBloque();
        etiquetaPaciente.setVisible(!bloque);
        etiquetaPaciente.setManaged(!bloque);
        comboPaciente.setVisible(!bloque);
        comboPaciente.setManaged(!bloque);
        cajaBusquedaPaciente.setVisible(!bloque);
        cajaBusquedaPaciente.setManaged(!bloque);
        cajaPaciente.setVisible(!bloque);
        cajaPaciente.setManaged(!bloque);
        checkPrimeraVez.setVisible(!bloque);
        checkPrimeraVez.setManaged(!bloque);
        etiquetaTratamiento.setVisible(!bloque);
        etiquetaTratamiento.setManaged(!bloque);
        comboTratamiento.setVisible(!bloque);
        comboTratamiento.setManaged(!bloque);
        etiquetaDuracion.setVisible(!bloque);
        etiquetaDuracion.setManaged(!bloque);
        etiquetaMotivo.setText(bloque ? "Motivo del bloque:" : "Motivo:");
        etiquetaModo.setText(bloque ? "BLOQUE" : "NUEVA");
        if (bloque) {
            etiquetaDisponibilidad.setText("El bloque reserva el horario y el consultorio "
                    + "(almuerzo, cierre o bloqueo).");
        } else {
            etiquetaDisponibilidad.setText("El fin se calcula con la duracion del catalogo (R1 y R2: "
                    + "sin cruce de sillon ni de odontologo).");
        }
        etiquetaError.setText("");
        recalcularFin();
    }

    private void actualizarTratamiento() {
        boolean primera = checkPrimeraVez.isSelected();
        comboTratamiento.setDisable(primera);
        etiquetaTratamiento.setText(primera ? "Tratamiento (primera vez):" : "Tratamiento:");
        if (primera) {
            comboTratamiento.setValue(null);
            etiquetaDuracion.setText("Consulta / evaluacion de primera vez · " + DURACION_CONSULTA + " min");
            if (texto(campoMotivo) == null) {
                campoMotivo.setText("Consulta / evaluacion");
            }
        } else {
            etiquetaDuracion.setText("Duracion: la del tratamiento elegido en el catalogo");
        }
        recalcularFin();
    }

    private int duracionSeleccionada() {
        if (esModoBloque()) {
            return DURACION_BLOQUE;
        }
        if (checkPrimeraVez.isSelected()) {
            return DURACION_CONSULTA;
        }
        return duracion(comboTratamiento.getValue());
    }

    private void recalcularFin() {
        int minutos = duracionSeleccionada();
        String origen;
        if (esModoBloque()) {
            origen = "bloque";
        } else if (checkPrimeraVez.isSelected()) {
            origen = "consulta de primera vez";
        } else if (comboTratamiento.getValue() == null) {
            origen = "sin tratamiento, consulta";
        } else {
            origen = comboTratamiento.getValue().getNombre();
        }
        LocalTime inicio = leerHora(campoInicio.getText());
        if (inicio == null) {
            campoFin.clear();
            etiquetaFinCalculo.setText("Fin calculado: --:-- · " + minutos + " min (" + origen + ")");
            return;
        }
        LocalTime fin = inicio.plusMinutes(minutos);
        campoFin.setText(HORA.format(fin));
        etiquetaFinCalculo.setText("Fin calculado: " + HORA.format(fin) + " · " + minutos
                + " min (" + origen + ")");
    }

    private LocalTime leerHora(String texto) {
        try {
            return LocalTime.parse(texto == null ? "" : texto.trim(), HORA);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String hora(Cita cita) {
        if (cita.getHoraInicio() == null) {
            return "";
        }
        String inicio = cita.getHoraInicio().toString();
        String fin = cita.getHoraFin() == null ? "" : cita.getHoraFin().toString();
        return inicio + " - " + fin;
    }

    private String tratamientoODespacho(Cita cita) {
        if (!"CITA".equals(cita.getTipo())) {
            String motivo = cita.getMotivo();
            return motivo == null || motivo.isBlank() ? "Bloque" : motivo;
        }
        if (cita.getTratamientoId() != null) {
            return nombresTratamientos.getOrDefault(cita.getTratamientoId(), "—");
        }
        String motivo = cita.getMotivo();
        return motivo == null || motivo.isBlank() ? "—" : motivo;
    }

    private void recargar() {
        LocalDate fecha = campoFecha.getValue();
        if (fecha == null) {
            return;
        }
        TareasFX.ejecutar(() -> AppContext.get().agenda().agendaDelDia(fecha),
                this::aplicarAgenda,
                error -> mostrarError(TareasFX.mensaje(error)));
        recargarEspera();
    }

    private void aplicarAgenda(List<Cita> citas) {
        citasDelDia = new ArrayList<>(citas);
        aplicarFiltros();
    }

    private void aplicarFiltros() {
        String odontologo = filtroOdontologo.getValue();
        String consultorio = filtroConsultorio.getValue();
        String estado = filtroEstado.getValue();
        List<Cita> filtradas = citasDelDia.stream()
                .filter(c -> odontologo == null || TODOS.equals(odontologo)
                        || odontologo.equals(nombresOdontologos.get(c.getUsuarioId())))
                .filter(c -> consultorio == null || TODOS.equals(consultorio)
                        || consultorio.equals(consultoriosPorCodigo.get(c.getConsultorioId())))
                .filter(c -> estado == null || TODOS.equals(estado)
                        || estado.equals(estadoTabla(c)))
                .toList();
        Long mantener = tablaAgenda.getSelectionModel().getSelectedItem() == null
                ? null : tablaAgenda.getSelectionModel().getSelectedItem().getId();
        tablaAgenda.setItems(FXCollections.observableArrayList(filtradas));
        if (mantener != null) {
            for (Cita c : filtradas) {
                if (mantener.equals(c.getId())) {
                    tablaAgenda.getSelectionModel().select(c);
                    break;
                }
            }
        }
        long bloques = filtradas.stream().filter(c -> !"CITA".equals(c.getTipo())).count();
        etiquetaConteo.setText((filtradas.size() - bloques) + " cita(s) · " + bloques + " bloque(s)");
    }

    @FXML
    private void onNuevaCitaClick() {
        limpiarForma();
        comboTipo.setValue("CITA");
        campoDniPaciente.requestFocus();
    }

    @FXML
    private void onBloquearClick() {
        limpiarForma();
        comboTipo.setValue("ALMUERZO");
        campoInicio.requestFocus();
    }

    @FXML
    private void onRefrescarClick() {
        recargar();
    }

    @FXML
    private void onCancelarClick() {
        limpiarForma();
    }

    @FXML
    private void onBuscarPacienteClick() {
        String dni = texto(campoDniPaciente);
        if (dni == null) {
            etiquetaError.setText("Ingresa el DNI del paciente para buscarlo.");
            return;
        }
        TareasFX.ejecutar(() -> AppContext.get().pacientes().buscarPorDni(dni),
                encontrado -> {
                    if (encontrado.isEmpty()) {
                        etiquetaError.setText("No existe un paciente con DNI " + dni + ".");
                        return;
                    }
                    Paciente paciente = encontrado.get();
                    Optional<Paciente> enCombo = comboPaciente.getItems().stream()
                            .filter(p -> p.getId().equals(paciente.getId()))
                            .findFirst();
                    if (enCombo.isEmpty()) {
                        etiquetaError.setText("El paciente esta INACTIVO: no se puede agendar una cita.");
                        return;
                    }
                    comboPaciente.setValue(enCombo.get());
                    actualizarTarjetaPaciente();
                    etiquetaError.setText("");
                },
                error -> etiquetaError.setText(TareasFX.mensaje(error)));
    }

    private void actualizarTarjetaPaciente() {
        Paciente paciente = comboPaciente.getValue();
        if (paciente == null) {
            etiquetaAvatarPaciente.setText("--");
            etiquetaPacienteTarjeta.setText("— sin paciente —");
            etiquetaDatosPaciente.setText("Busca por DNI o elige en la lista");
            return;
        }
        etiquetaAvatarPaciente.setText(iniciales(paciente));
        etiquetaPacienteTarjeta.setText(paciente.getApellidos() + ", " + paciente.getNombres());
        String edad = paciente.getFechaNac() == null ? "—"
                : String.valueOf(Period.between(paciente.getFechaNac(), LocalDate.now()).getYears());
        String telefono = paciente.getCelular() == null ? "sin telefono" : paciente.getCelular();
        etiquetaDatosPaciente.setText("DNI: " + paciente.getDni() + " · " + edad + " años · "
                + telefono);
    }

    private String iniciales(Paciente paciente) {
        if (paciente == null) {
            return "--";
        }
        String nombres = paciente.getNombres() == null ? "" : paciente.getNombres().trim();
        String apellidos = paciente.getApellidos() == null ? "" : paciente.getApellidos().trim();
        String inicial = "";
        if (!nombres.isEmpty()) {
            inicial += nombres.substring(0, 1).toUpperCase();
        }
        if (!apellidos.isEmpty()) {
            inicial += apellidos.substring(0, 1).toUpperCase();
        }
        return inicial.isEmpty() ? "--" : inicial;
    }

    @FXML
    private void onGuardarClick() {
        etiquetaError.setText("");
        try {
            Cita cita = new Cita();
            cita.setFechaCita(campoFecha.getValue());
            LocalTime inicio = parseHora(campoInicio.getText(), "inicio");
            cita.setHoraInicio(inicio);
            cita.setHoraFin(inicio.plusMinutes(duracionSeleccionada()));
            if (comboOdontologo.getValue() == null) {
                throw new DentrixException("Selecciona el odontologo responsable");
            }
            cita.setUsuarioId(comboOdontologo.getValue().getId());
            if (comboConsultorio.getValue() == null) {
                throw new DentrixException("Selecciona el consultorio");
            }
            cita.setConsultorioId(comboConsultorio.getValue().getId());
            boolean modoBloque = esModoBloque();
            if (modoBloque) {
                cita.setTipo(comboTipo.getValue());
                String motivo = texto(campoMotivo);
                if (motivo == null) {
                    throw new DentrixException("El motivo del bloque es obligatorio");
                }
                cita.setMotivo(motivo);
            } else {
                cita.setTipo("CITA");
                if (comboPaciente.getValue() == null) {
                    throw new DentrixException("Selecciona el paciente de la cita");
                }
                cita.setPacienteId(comboPaciente.getValue().getId());
                if (!checkPrimeraVez.isSelected() && comboTratamiento.getValue() != null) {
                    cita.setTratamientoId(comboTratamiento.getValue().getId());
                }
                if (checkPrimeraVez.isSelected() && texto(campoMotivo) == null) {
                    cita.setMotivo("Consulta / evaluacion");
                } else {
                    cita.setMotivo(texto(campoMotivo));
                }
            }
            TareasFX.ejecutar(() -> {
                if (modoBloque) {
                    AppContext.get().agenda().bloquear(cita);
                } else {
                    AppContext.get().agenda().agendar(cita);
                }
                return null;
            }, ok -> {
                limpiarForma();
                recargar();
                indicar("Cita registrada con fin calculado.", true);
            }, error -> etiquetaError.setText(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            etiquetaError.setText(e.getMessage());
        }
    }

    @FXML
    private void onVolverClick() {
        pe.edu.utp.dentrix.ui.Navegador.ir(pe.edu.utp.dentrix.ui.Navegador.INICIO);
    }

    private void iniciar(Cita cita) {
        aplicarTransicion(cita, AppContext.get().agenda()::iniciarAtencion, () -> abrirFicha(cita));
    }

    private void abrirFicha(Cita cita) {
        if (cita == null || cita.getPacienteId() == null) {
            return;
        }
        try {
            Paciente paciente = AppContext.get().pacientes().buscarPorId(cita.getPacienteId());
            AppContext.get().sesion().setPacienteEnAtencion(paciente);
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
            return;
        }
        pe.edu.utp.dentrix.ui.Navegador.ir(FXML_FICHA);
    }

    private void aplicarTransicion(Cita cita, TransicionAccion accion, Runnable alExito) {
        if (cita == null || !"CITA".equals(cita.getTipo())) {
            return;
        }
        Long id = cita.getId();
        TareasFX.ejecutar(() -> {
            accion.aplicar(id);
            return null;
        }, ok -> {
            recargar();
            if (alExito != null) {
                alExito.run();
            }
        }, error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void cancelar(Cita cita) {
        if (cita == null || !"CITA".equals(cita.getTipo())) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cancelar cita");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("Cancelar la cita #" + cita.getId()
                + "? Queda registrada como CANCELADA y libera el horario.");
        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta == ButtonType.OK) {
                Long id = cita.getId();
                TareasFX.ejecutar(() -> {
                    AppContext.get().agenda().cancelar(id);
                    return null;
                }, ok -> {
                    recargar();
                    indicar("Cita cancelada.", true);
                }, error -> mostrarError(TareasFX.mensaje(error)));
            }
        });
    }

    private record Horario(LocalDate fecha, LocalTime hora, Consultorio consultorio, String motivo) {
    }

    private record NuevaEspera(Integer pacienteId, Integer tratamientoId, Integer prioridad, String nota) {
    }

    private record NuevoRecordatorio(String canal, LocalDateTime cuando) {
    }

    private void recargarEspera() {
        TareasFX.ejecutar(() -> AppContext.get().agenda().listaEspera(),
                lista -> {
                    tablaEspera.setItems(FXCollections.observableArrayList(lista));
                    actualizarBotonesEspera();
                },
                error -> etiquetaEspera.setText(TareasFX.mensaje(error)));
    }

    private void recargarRecordatorios(Long citaId) {
        if (citaId == null) {
            tablaRecordatorios.getItems().clear();
            return;
        }
        TareasFX.ejecutar(() -> AppContext.get().agenda().recordatorios(citaId),
                lista -> {
                    tablaRecordatorios.setItems(FXCollections.observableArrayList(lista));
                    btnMarcarEnviado.setDisable(true);
                },
                error -> etiquetaRecordatorio.setText(TareasFX.mensaje(error)));
    }

    private void actualizarBotonesEspera() {
        boolean hay = tablaEspera.getSelectionModel().getSelectedItem() != null;
        btnRetirarEspera.setDisable(!hay);
        btnAsignarCita.setDisable(!hay);
    }

    private void alSeleccionarCita(Cita cita) {
        boolean esCita = cita != null && "CITA".equals(cita.getTipo());
        boolean reprogramable = esCita
                && ("PENDIENTE".equals(cita.getEstado()) || "CONFIRMADA".equals(cita.getEstado()));
        btnReprogramar.setDisable(!reprogramable);
        btnHistorial.setDisable(!esCita);
        btnProgramarRecordatorio.setDisable(!esCita);
        etiquetaRecCita.setText(esCita ? "Cita #" + cita.getId() : "Sin cita");
        etiquetaRecordatorio.setText("");
        etiquetaAviso.setVisible(false);
        etiquetaAviso.setManaged(false);
        mostrarDetalle(cita);
        if (!esCita) {
            tablaRecordatorios.getItems().clear();
            return;
        }
        recargarRecordatorios(cita.getId());
        mostrarAvisoInasistencias(cita.getPacienteId());
    }

    private void mostrarDetalle(Cita cita) {
        if (cita == null) {
            etiquetaEstadoBadge.setText("—");
            aplicarBadgeEstado(null);
            etiquetaDetalleAvatar.setText("--");
            etiquetaDetallePaciente.setText("— selecciona una cita —");
            etiquetaDetalleDocumento.setText("");
            etiquetaDetalleHorario.setText("—");
            etiquetaDetalleTratamiento.setText("—");
            etiquetaDetalleOdontologo.setText("—");
            etiquetaDetalleConsultorio.setText("—");
            etiquetaDetalleObservacion.setText("—");
            return;
        }
        aplicarBadgeEstado(cita.getEstado());
        etiquetaDetalleHorario.setText(horarioDetalle(cita));
        etiquetaDetalleTratamiento.setText(tratamientoODespacho(cita));
        etiquetaDetalleOdontologo.setText(nombresOdontologos.getOrDefault(cita.getUsuarioId(), "—"));
        etiquetaDetalleConsultorio.setText(
                consultoriosPorCodigo.getOrDefault(cita.getConsultorioId(), "—"));
        etiquetaDetalleObservacion.setText(cita.getMotivo() == null || cita.getMotivo().isBlank()
                ? "—" : cita.getMotivo());
        if (!"CITA".equals(cita.getTipo())) {
            etiquetaDetalleAvatar.setText("--");
            etiquetaDetallePaciente.setText("Bloqueo de agenda");
            etiquetaDetalleDocumento.setText("Horario reservado");
            return;
        }
        Paciente paciente = buscarPaciente(cita.getPacienteId());
        etiquetaDetalleAvatar.setText(iniciales(paciente));
        etiquetaDetallePaciente.setText(paciente == null
                ? "Paciente #" + cita.getPacienteId()
                : paciente.getApellidos() + ", " + paciente.getNombres());
        etiquetaDetalleDocumento.setText(paciente == null || paciente.getDni() == null
                ? "DNI: —" : "DNI: " + paciente.getDni());
    }

    private String horarioDetalle(Cita cita) {
        if (cita.getHoraInicio() == null) {
            return "—";
        }
        String fin = cita.getHoraFin() == null ? "" : HORA.format(cita.getHoraFin());
        String rango = HORA.format(cita.getHoraInicio()) + (fin.isEmpty() ? "" : " - " + fin);
        if (cita.getHoraFin() == null) {
            return rango;
        }
        long minutos = java.time.Duration.between(cita.getHoraInicio(), cita.getHoraFin()).toMinutes();
        return rango + " · " + minutos + " min";
    }

    private void aplicarBadgeEstado(String estado) {
        etiquetaEstadoBadge.getStyleClass().removeAll("badge-estado-gris", "badge-estado-info",
                "badge-estado-sala", "badge-estado-atendido", "badge-estado-falto");
        String clase = "badge-estado-gris";
        String texto = "—";
        if (estado != null) {
            switch (estado) {
                case "EN_SALA" -> {
                    clase = "badge-estado-sala";
                    texto = "En sala";
                }
                case "EN_ATENCION" -> {
                    clase = "badge-estado-info";
                    texto = "En atencion";
                }
                case "ATENDIDA" -> {
                    clase = "badge-estado-atendido";
                    texto = "Atendida";
                }
                case "FALTO" -> {
                    clase = "badge-estado-falto";
                    texto = "Falto";
                }
                case "CONFIRMADA" -> {
                    clase = "badge-estado-info";
                    texto = "Confirmada";
                }
                case "CANCELADA" -> {
                    clase = "badge-estado-falto";
                    texto = "Cancelada";
                }
                case "BLOQUEADA" -> {
                    clase = "badge-estado-gris";
                    texto = "Bloqueada";
                }
                default -> {
                    clase = "badge-estado-gris";
                    texto = "Pendiente";
                }
            }
        }
        etiquetaEstadoBadge.setText(texto);
        etiquetaEstadoBadge.getStyleClass().add(clase);
    }

    private Paciente buscarPaciente(Integer pacienteId) {
        if (pacienteId == null) {
            return null;
        }
        try {
            return AppContext.get().pacientes().buscarPorId(pacienteId);
        } catch (DentrixException e) {
            return null;
        }
    }

    private void mostrarAvisoInasistencias(Integer pacienteId) {
        if (pacienteId == null) {
            return;
        }
        TareasFX.ejecutar(() -> AppContext.get().agenda().inasistencias(pacienteId),
                total -> {
                    if (total >= 2) {
                        etiquetaAviso.setText("Paciente con " + total
                                + " inasistencias: confirmar antes de agendar (R29)");
                        etiquetaAviso.setVisible(true);
                        etiquetaAviso.setManaged(true);
                    }
                },
                error -> {
                });
    }

    @FXML
    private void onReprogramarClick() {
        Cita cita = tablaAgenda.getSelectionModel().getSelectedItem();
        if (cita == null || !"CITA".equals(cita.getTipo())) {
            mostrarError("Selecciona una cita de la agenda");
            return;
        }
        Long id = cita.getId();
        pedirHorario("Reprogramar cita", cita.getMotivo()).ifPresent(horario ->
                TareasFX.ejecutar(() -> AppContext.get().agenda().reprogramar(id,
                                horario.fecha(), horario.hora(),
                                horario.consultorio() == null ? null : horario.consultorio().getId(),
                                horario.motivo(), usuarioId()),
                        nueva -> {
                            recargar();
                            mostrarInfo("Cita reprogramada: nueva cita #" + nueva.getId());
                        },
                        error -> mostrarError(TareasFX.mensaje(error))));
    }

    @FXML
    private void onHistorialClick() {
        Cita cita = tablaAgenda.getSelectionModel().getSelectedItem();
        if (cita == null || !"CITA".equals(cita.getTipo())) {
            mostrarError("Selecciona una cita de la agenda");
            return;
        }
        TareasFX.ejecutar(() -> AppContext.get().agenda().historial(cita.getId()),
                this::mostrarHistorial,
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onAgregarEsperaClick() {
        pedirNuevaEspera().ifPresent(datos ->
                TareasFX.ejecutar(() -> {
                            AppContext.get().agenda().agregarEspera(datos.pacienteId(),
                                    datos.tratamientoId(), datos.prioridad(), datos.nota(), usuarioId());
                            return null;
                        },
                        ok -> recargarEspera(),
                        error -> etiquetaEspera.setText(TareasFX.mensaje(error))));
    }

    @FXML
    private void onRetirarEsperaClick() {
        ListaEspera espera = tablaEspera.getSelectionModel().getSelectedItem();
        if (espera == null) {
            return;
        }
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Retirar de la lista de espera");
        dialogo.setHeaderText(espera.getPacienteNombre());
        dialogo.setContentText("Motivo del retiro:");
        dialogo.showAndWait().ifPresent(motivo ->
                TareasFX.ejecutar(() -> {
                            AppContext.get().agenda().retirarEspera(espera.getId(), motivo);
                            return null;
                        },
                        ok -> recargarEspera(),
                        error -> etiquetaEspera.setText(TareasFX.mensaje(error))));
    }

    @FXML
    private void onAsignarCitaClick() {
        ListaEspera espera = tablaEspera.getSelectionModel().getSelectedItem();
        if (espera == null) {
            return;
        }
        pedirHorario("Asignar cita desde la lista de espera", "Lista de espera").ifPresent(horario ->
                TareasFX.ejecutar(() -> {
                            int minutos = duracionTratamientoDeEspera(espera.getTratamientoId());
                            Cita cita = new Cita();
                            cita.setTipo("CITA");
                            cita.setPacienteId(espera.getPacienteId());
                            cita.setTratamientoId(espera.getTratamientoId());
                            cita.setUsuarioId(usuarioId());
                            cita.setConsultorioId(
                                    horario.consultorio() == null ? null : horario.consultorio().getId());
                            cita.setFechaCita(horario.fecha());
                            cita.setHoraInicio(horario.hora());
                            cita.setHoraFin(horario.hora().plusMinutes(minutos));
                            cita.setMotivo(horario.motivo() == null ? "Lista de espera" : horario.motivo());
                            Cita agendada = AppContext.get().agenda().agendar(cita);
                            AppContext.get().agenda().marcarEsperaAgendada(espera.getId(), agendada.getId());
                            return agendada;
                        },
                        agendada -> {
                            recargar();
                            mostrarInfo("Cita #" + agendada.getId() + " asignada desde la lista de espera");
                        },
                        error -> etiquetaEspera.setText(TareasFX.mensaje(error))));
    }

    private int duracionTratamientoDeEspera(Integer tratamientoId) {
        if (tratamientoId == null) {
            return DURACION_CONSULTA;
        }
        for (Tratamiento t : tratamientosActivos) {
            if (tratamientoId.equals(t.getId())) {
                return duracion(t);
            }
        }
        return DURACION_CONSULTA;
    }

    @FXML
    private void onProgramarRecordatorioClick() {
        Cita cita = tablaAgenda.getSelectionModel().getSelectedItem();
        if (cita == null || !"CITA".equals(cita.getTipo())) {
            mostrarError("Selecciona una cita de la agenda");
            return;
        }
        Long id = cita.getId();
        pedirRecordatorio().ifPresent(datos ->
                TareasFX.ejecutar(() -> {
                            AppContext.get().agenda().programarRecordatorio(id, datos.canal(),
                                    datos.cuando(), usuarioId());
                            return null;
                        },
                        ok -> recargarRecordatorios(id),
                        error -> etiquetaRecordatorio.setText(TareasFX.mensaje(error))));
    }

    @FXML
    private void onMarcarEnviadoClick() {
        Recordatorio recordatorio = tablaRecordatorios.getSelectionModel().getSelectedItem();
        if (recordatorio == null) {
            return;
        }
        TareasFX.ejecutar(() -> {
                    AppContext.get().agenda().marcarRecordatorio(recordatorio.getId(), "ENVIADO", null);
                    return null;
                },
                ok -> recargarRecordatorios(recordatorio.getCitaId()),
                error -> etiquetaRecordatorio.setText(TareasFX.mensaje(error)));
    }

    private Optional<Horario> pedirHorario(String titulo, String motivoSugerido) {
        List<Consultorio> consultorios;
        try {
            consultorios = AppContext.get().catalogo().listarConsultorios();
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
            return Optional.empty();
        }
        Dialog<Horario> dialogo = new Dialog<>();
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(null);
        ButtonType aceptar = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(aceptar, ButtonType.CANCEL);

        DatePicker fecha = new DatePicker(campoFecha.getValue() == null
                ? LocalDate.now() : campoFecha.getValue());
        TextField hora = new TextField("09:00");
        ComboBox<Consultorio> combo = new ComboBox<>(FXCollections.observableArrayList(consultorios));
        combo.setConverter(consultorioConverter());
        combo.setValue(comboConsultorio.getValue() != null
                ? comboConsultorio.getValue() : consultorios.isEmpty() ? null : consultorios.get(0));
        TextField motivo = new TextField(motivoSugerido == null ? "" : motivoSugerido);

        dialogo.getDialogPane().setContent(formulario(
                new Label("Fecha:"), fecha,
                new Label("Hora (HH:mm):"), hora,
                new Label("Consultorio:"), combo,
                new Label("Motivo:"), motivo));
        dialogo.setResultConverter(boton -> {
            if (boton != aceptar) {
                return null;
            }
            try {
                if (fecha.getValue() == null) {
                    throw new DentrixException("La fecha es obligatoria");
                }
                LocalTime inicio = parseHora(hora.getText(), "inicio");
                return new Horario(fecha.getValue(), inicio, combo.getValue(), texto(motivo));
            } catch (DentrixException e) {
                mostrarError(e.getMessage());
                return null;
            }
        });
        return dialogo.showAndWait();
    }

    private Optional<NuevaEspera> pedirNuevaEspera() {
        Dialog<NuevaEspera> dialogo = new Dialog<>();
        dialogo.setTitle("Agregar a la lista de espera");
        dialogo.setHeaderText(null);
        ButtonType aceptar = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(aceptar, ButtonType.CANCEL);

        Cita seleccionada = tablaAgenda.getSelectionModel().getSelectedItem();
        ComboBox<Paciente> paciente = new ComboBox<>(FXCollections.observableArrayList(pacientesActivos));
        paciente.setConverter(pacienteConverter());
        if (seleccionada != null && seleccionada.getPacienteId() != null) {
            paciente.getItems().stream()
                    .filter(p -> seleccionada.getPacienteId().equals(p.getId()))
                    .findFirst()
                    .ifPresent(paciente::setValue);
        }
        ComboBox<Tratamiento> tratamiento =
                new ComboBox<>(FXCollections.observableArrayList(tratamientosActivos));
        tratamiento.setConverter(tratamientoConverter());
        if (seleccionada != null && seleccionada.getTratamientoId() != null) {
            tratamiento.getItems().stream()
                    .filter(t -> seleccionada.getTratamientoId().equals(t.getId()))
                    .findFirst()
                    .ifPresent(tratamiento::setValue);
        }
        ComboBox<String> prioridad = new ComboBox<>(FXCollections.observableArrayList(PRIORIDADES));
        prioridad.setValue("3");
        TextField nota = new TextField();

        dialogo.getDialogPane().setContent(formulario(
                new Label("Paciente:"), paciente,
                new Label("Tratamiento:"), tratamiento,
                new Label("Prioridad (1-5):"), prioridad,
                new Label("Nota:"), nota));
        dialogo.setResultConverter(boton -> {
            if (boton != aceptar) {
                return null;
            }
            if (paciente.getValue() == null) {
                mostrarError("Selecciona el paciente de la espera");
                return null;
            }
            return new NuevaEspera(paciente.getValue().getId(),
                    tratamiento.getValue() == null ? null : tratamiento.getValue().getId(),
                    prioridad.getValue() == null ? 3 : Integer.parseInt(prioridad.getValue()),
                    texto(nota));
        });
        return dialogo.showAndWait();
    }

    private Optional<NuevoRecordatorio> pedirRecordatorio() {
        Dialog<NuevoRecordatorio> dialogo = new Dialog<>();
        dialogo.setTitle("Programar recordatorio");
        dialogo.setHeaderText(null);
        ButtonType aceptar = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(aceptar, ButtonType.CANCEL);

        ComboBox<String> canal = new ComboBox<>(FXCollections.observableArrayList(CANALES));
        canal.setValue("WHATSAPP");
        DatePicker fecha = new DatePicker(LocalDate.now().plusDays(1));
        TextField hora = new TextField("10:00");

        dialogo.getDialogPane().setContent(formulario(
                new Label("Canal:"), canal,
                new Label("Fecha:"), fecha,
                new Label("Hora (HH:mm):"), hora));
        dialogo.setResultConverter(boton -> {
            if (boton != aceptar) {
                return null;
            }
            try {
                if (fecha.getValue() == null) {
                    throw new DentrixException("La fecha del recordatorio es obligatoria");
                }
                return new NuevoRecordatorio(canal.getValue(),
                        LocalDateTime.of(fecha.getValue(), parseHora(hora.getText(), "inicio")));
            } catch (DentrixException e) {
                mostrarError(e.getMessage());
                return null;
            }
        });
        return dialogo.showAndWait();
    }

    private GridPane formulario(Object... filas) {
        GridPane grid = new GridPane();
        grid.setHgap(8.0);
        grid.setVgap(8.0);
        int fila = 0;
        for (int i = 0; i + 1 < filas.length; i += 2) {
            grid.add((javafx.scene.Node) filas[i], 0, fila);
            grid.add((javafx.scene.Node) filas[i + 1], 1, fila);
            fila++;
        }
        return grid;
    }

    private void mostrarHistorial(List<CitaHistorial> filas) {
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.setTitle("Historial de estados");
        dialogo.setHeaderText(null);
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        TableView<CitaHistorial> tabla = new TableView<>(FXCollections.observableArrayList(filas));

        TableColumn<CitaHistorial, String> colFecha = new TableColumn<>("FECHA");
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() == null ? "" : d.getValue().getFecha().format(FECHA_HORA)));
        TableColumn<CitaHistorial, String> colAnterior = new TableColumn<>("ANTERIOR");
        colAnterior.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEstadoAnterior() == null ? "—" : d.getValue().getEstadoAnterior()));
        TableColumn<CitaHistorial, String> colNuevo = new TableColumn<>("NUEVO");
        colNuevo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstadoNuevo()));
        TableColumn<CitaHistorial, String> colMotivo = new TableColumn<>("MOTIVO");
        colMotivo.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getMotivo() == null ? "—" : d.getValue().getMotivo()));
        TableColumn<CitaHistorial, String> colUsuario = new TableColumn<>("USUARIO");
        colUsuario.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getUsuarioId() == null ? "—" : String.valueOf(d.getValue().getUsuarioId())));
        Badges.aplicarEn(colNuevo);

        tabla.getColumns().add(colFecha);
        tabla.getColumns().add(colAnterior);
        tabla.getColumns().add(colNuevo);
        tabla.getColumns().add(colMotivo);
        tabla.getColumns().add(colUsuario);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPrefSize(680.0, 320.0);
        tabla.setPlaceholder(new Label("La cita no tiene cambios de estado registrados"));

        dialogo.getDialogPane().setContent(tabla);
        dialogo.setResizable(true);
        dialogo.showAndWait();
    }

    private Integer usuarioId() {
        Usuario usuario = AppContext.get().sesion().getUsuario();
        return usuario == null ? null : usuario.getId();
    }

    private void mostrarInfo(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Agenda");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void indicar(String mensaje, boolean ok) {
        etiquetaOperacion.setText(mensaje);
        etiquetaOperacion.getStyleClass().removeAll("etiqueta-error", "etiqueta-ok");
        etiquetaOperacion.getStyleClass().add(ok ? "etiqueta-ok" : "etiqueta-error");
    }

    private void limpiarForma() {
        campoInicio.clear();
        campoFin.clear();
        campoMotivo.clear();
        campoDniPaciente.clear();
        comboPaciente.setValue(null);
        comboOdontologo.setValue(null);
        comboConsultorio.setValue(null);
        comboTratamiento.setValue(null);
        checkPrimeraVez.setSelected(true);
        etiquetaError.setText("");
        etiquetaModo.setText(esModoBloque() ? "BLOQUE" : "NUEVA");
        aplicarVisibilidadTipo();
        actualizarTratamiento();
        actualizarTarjetaPaciente();
    }

    private LocalTime parseHora(String texto, String campo) throws DentrixException {
        String valor = texto == null ? "" : texto.trim();
        try {
            return LocalTime.parse(valor, HORA);
        } catch (DateTimeParseException e) {
            throw new DentrixException("La hora de " + campo + " debe tener formato HH:mm (ej. 09:00)");
        }
    }

    private String texto(TextField campo) {
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        return valor.isEmpty() ? null : valor;
    }

    private interface TransicionAccion {
        void aplicar(Long id) throws DentrixException;
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Agenda");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
