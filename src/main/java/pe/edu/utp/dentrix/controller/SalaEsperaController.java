package pe.edu.utp.dentrix.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Cita;
import pe.edu.utp.dentrix.model.Paciente;
import pe.edu.utp.dentrix.model.Tratamiento;
import pe.edu.utp.dentrix.model.Usuario;
import pe.edu.utp.dentrix.ui.Badges;
import pe.edu.utp.dentrix.ui.Navegador;
import pe.edu.utp.dentrix.ui.TareasFX;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SalaEsperaController {

    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", new java.util.Locale("es"));
    private static final String FXML_AGENDA = "/pe/edu/utp/dentrix/fxml/agenda.fxml";

    @FXML
    private Label etiquetaFecha;

    @FXML
    private Label etiquetaAsistieron;

    @FXML
    private Label etiquetaFaltantes;

    @FXML
    private Label etiquetaPendientes;

    @FXML
    private Label etiquetaSelPaciente;

    @FXML
    private Label etiquetaSelDetalle;

    @FXML
    private Button btnAsistio;

    @FXML
    private Button btnFalto;

    @FXML
    private Label etiquetaOperacion;

    @FXML
    private TableView<Cita> tablaCola;

    @FXML
    private TableColumn<Cita, String> colHora;

    @FXML
    private TableColumn<Cita, String> colDoctor;

    @FXML
    private TableColumn<Cita, String> colPaciente;

    @FXML
    private TableColumn<Cita, String> colTratamiento;

    @FXML
    private TableColumn<Cita, String> colEstado;

    @FXML
    private TableColumn<Cita, String> colAccion;

    private final Map<Integer, String> nombresPacientes = new HashMap<>();
    private final Map<Integer, String> nombresOdontologos = new HashMap<>();
    private final Map<Integer, String> consultoriosPorId = new HashMap<>();
    private final Map<Integer, String> nombresTratamientos = new HashMap<>();
    private Cita citaSeleccionada;

    @FXML
    private void initialize() {
        colHora.setCellValueFactory(d -> new SimpleStringProperty(hora(d.getValue())));
        colDoctor.setCellValueFactory(d -> new SimpleStringProperty(
                consultoriosPorId.getOrDefault(d.getValue().getConsultorioId(), "—") + " · "
                        + nombresOdontologos.getOrDefault(d.getValue().getUsuarioId(), "—")));
        colPaciente.setCellValueFactory(d -> new SimpleStringProperty(
                nombresPacientes.getOrDefault(d.getValue().getPacienteId(), "—")));
        colTratamiento.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTratamientoId() == null
                        ? (d.getValue().getMotivo() == null ? "—" : d.getValue().getMotivo())
                        : nombresTratamientos.getOrDefault(d.getValue().getTratamientoId(), "—")));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado()));
        Badges.aplicarEn(colEstado);
        colAccion.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Seleccionar");

            {
                btn.getStyleClass().add("boton-mini");
                btn.setOnAction(e -> {
                    Cita fila = getTableRow().getItem();
                    if (fila != null) {
                        tablaCola.getSelectionModel().select(fila);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean vacio) {
                super.updateItem(item, vacio);
                setGraphic(vacio ? null : btn);
            }
        });
        tablaCola.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaCola.getSelectionModel().selectedItemProperty().addListener((obs, antes, ahora) -> seleccionar(ahora));

        AppContext contexto = AppContext.get();
        if (!contexto.sesion().activa()) {
            return;
        }
        etiquetaFecha.setText("📅 Control Diario • " + FECHA_LARGA.format(LocalDate.now()));
        TareasFX.ejecutar(this::cargarInicial,
                this::aplicarInicial,
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private record DatosSala(Map<Integer, String> pacientes, Map<Integer, String> odontologos,
                             Map<Integer, String> consultorios, Map<Integer, String> tratamientos,
                             List<Cita> citas) {
    }

    private DatosSala cargarInicial() throws DentrixException {
        AppContext contexto = AppContext.get();
        Map<Integer, String> pacientes = new HashMap<>();
        for (Paciente p : contexto.pacientes().listar()) {
            pacientes.put(p.getId(), p.getApellidos() + ", " + p.getNombres());
        }
        Map<Integer, String> odontologos = new HashMap<>();
        for (Usuario u : contexto.usuarios().listarTodos()) {
            odontologos.put(u.getId(), "Dr. " + u.getNombres() + " " + u.getApellidos());
        }
        Map<Integer, String> consultorios = new HashMap<>();
        for (var c : contexto.catalogo().listarConsultorios()) {
            consultorios.put(c.getId(), c.getNombre());
        }
        Map<Integer, String> tratamientos = new HashMap<>();
        for (Tratamiento t : contexto.catalogo().listarTratamientos()) {
            tratamientos.put(t.getId(), t.getNombre());
        }
        return new DatosSala(pacientes, odontologos, consultorios, tratamientos,
                contexto.agenda().agendaDelDia(LocalDate.now()));
    }

    private void aplicarInicial(DatosSala datos) {
        nombresPacientes.clear();
        nombresPacientes.putAll(datos.pacientes());
        nombresOdontologos.clear();
        nombresOdontologos.putAll(datos.odontologos());
        consultoriosPorId.clear();
        consultoriosPorId.putAll(datos.consultorios());
        nombresTratamientos.clear();
        nombresTratamientos.putAll(datos.tratamientos());
        aplicarCitas(datos.citas());
    }

    private String hora(Cita cita) {
        if (cita.getHoraInicio() == null) {
            return "";
        }
        String fin = cita.getHoraFin() == null ? "" : cita.getHoraFin().toString();
        return cita.getHoraInicio() + " - " + fin;
    }

    private void recargar() {
        TareasFX.ejecutar(() -> AppContext.get().agenda().agendaDelDia(LocalDate.now()),
                this::aplicarCitas,
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void aplicarCitas(List<Cita> citas) {
        tablaCola.setItems(FXCollections.observableArrayList(citas));
        int asistieron = 0;
        int faltaron = 0;
        int pendientes = 0;
        for (Cita c : citas) {
            String estado = c.getEstado();
            if ("EN_SALA".equals(estado) || "EN_ATENCION".equals(estado) || "ATENDIDA".equals(estado)) {
                asistieron++;
            } else if ("FALTO".equals(estado)) {
                faltaron++;
            } else if ("PENDIENTE".equals(estado) || "CONFIRMADA".equals(estado)) {
                pendientes++;
            }
        }
        etiquetaAsistieron.setText("✔ Asistieron: " + asistieron);
        etiquetaFaltantes.setText("✖ Faltantes: " + faltaron);
        etiquetaPendientes.setText("⏳ Pendientes: " + pendientes);
    }

    private void seleccionar(Cita cita) {
        citaSeleccionada = cita;
        boolean disponible = cita != null && "CITA".equals(cita.getTipo());
        if (!disponible) {
            etiquetaSelPaciente.setText("— selecciona una fila —");
            etiquetaSelDetalle.setText("");
            btnAsistio.setDisable(true);
            btnFalto.setDisable(true);
            return;
        }
        etiquetaSelPaciente.setText(nombresPacientes.getOrDefault(cita.getPacienteId(), "Paciente #" + cita.getPacienteId())
                + " (" + (cita.getHoraInicio() == null ? "" : cita.getHoraInicio()) + ")");
        etiquetaSelDetalle.setText(consultoriosPorId.getOrDefault(cita.getConsultorioId(), "—")
                + " · " + nombresOdontologos.getOrDefault(cita.getUsuarioId(), "—")
                + " · " + (cita.getTratamientoId() == null
                        ? (cita.getMotivo() == null ? "Sin tratamiento" : cita.getMotivo())
                        : nombresTratamientos.getOrDefault(cita.getTratamientoId(), "—"))
                + " · Estado: " + cita.getEstado());
        String estado = cita.getEstado();
        boolean marcarLlegada = "PENDIENTE".equals(estado) || "CONFIRMADA".equals(estado);
        btnAsistio.setDisable(!marcarLlegada);
        btnFalto.setDisable(!marcarLlegada);
    }

    @FXML
    private void onAsistioClick() {
        aplicar(AppContext.get().agenda()::marcarLlegada, "Paciente en sala de espera (EN_SALA)");
    }

    @FXML
    private void onFaltoClick() {
        aplicar(AppContext.get().agenda()::marcarFalto, "Paciente marcado como FALTO");
    }

    private void aplicar(TransicionAccion accion, String mensajeOk) {
        if (citaSeleccionada == null) {
            return;
        }
        Long id = citaSeleccionada.getId();
        btnAsistio.setDisable(true);
        btnFalto.setDisable(true);
        TareasFX.ejecutar(() -> {
            accion.aplicar(id);
            return mensajeOk;
        }, mensaje -> {
            etiquetaOperacion.setText(mensaje);
            etiquetaOperacion.getStyleClass().removeAll("etiqueta-error");
            etiquetaOperacion.getStyleClass().add("etiqueta-ok");
            recargar();
        }, error -> {
            etiquetaOperacion.setText(TareasFX.mensaje(error));
            etiquetaOperacion.getStyleClass().removeAll("etiqueta-ok");
            etiquetaOperacion.getStyleClass().add("etiqueta-error");
            seleccionar(citaSeleccionada);
        });
    }

    @FXML
    private void onVerAgendaClick() {
        Navegador.ir(FXML_AGENDA);
    }

    @FXML
    private void onRefrescarClick() {
        tablaCola.getSelectionModel().clearSelection();
        citaSeleccionada = null;
        seleccionar(null);
        etiquetaOperacion.setText("");
        recargar();
    }

    @FXML
    private void onVolverClick() {
        Navegador.ir(Navegador.INICIO);
    }

    private interface TransicionAccion {
        void aplicar(Long id) throws DentrixException;
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Sala de espera");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
