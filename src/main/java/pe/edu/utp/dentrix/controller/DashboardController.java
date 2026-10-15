package pe.edu.utp.dentrix.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.dto.KpiDia;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Cita;
import pe.edu.utp.dentrix.model.Paciente;
import pe.edu.utp.dentrix.model.Tratamiento;
import pe.edu.utp.dentrix.model.Usuario;
import pe.edu.utp.dentrix.ui.Badges;
import pe.edu.utp.dentrix.ui.Navegador;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardController {

    private static final String FXML_PACIENTES = "/pe/edu/utp/dentrix/fxml/pacientes.fxml";

    @FXML
    private TextField campoDni;

    @FXML
    private Label etiquetaBusqueda;

    @FXML
    private Label kpiCitas;

    @FXML
    private Label kpiSala;

    @FXML
    private Label kpiRecaudado;

    @FXML
    private Label kpiDeudores;

    @FXML
    private Label etiquetaFechaAgenda;

    @FXML
    private TableView<Cita> tablaAgenda;

    @FXML
    private TableColumn<Cita, String> colHora;

    @FXML
    private TableColumn<Cita, String> colPaciente;

    @FXML
    private TableColumn<Cita, String> colTratamiento;

    @FXML
    private TableColumn<Cita, String> colOdontologo;

    @FXML
    private TableColumn<Cita, String> colEstado;

    private final Map<Integer, String> nombresPacientes = new HashMap<>();
    private final Map<Integer, String> nombresOdontologos = new HashMap<>();
    private final Map<Integer, String> nombresTratamientos = new HashMap<>();

    @FXML
    private void initialize() {
        colHora.setCellValueFactory(d -> new SimpleStringProperty(hora(d.getValue())));
        colPaciente.setCellValueFactory(d -> new SimpleStringProperty(
                nombresPacientes.getOrDefault(d.getValue().getPacienteId(), "—")));
        colTratamiento.setCellValueFactory(d -> new SimpleStringProperty(tratamiento(d.getValue())));
        colOdontologo.setCellValueFactory(d -> new SimpleStringProperty(
                nombresOdontologos.getOrDefault(d.getValue().getUsuarioId(), "—")));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado()));
        Badges.aplicarEn(colEstado);
        tablaAgenda.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        AppContext contexto = AppContext.get();
        if (!contexto.sesion().activa()) {
            return;
        }
        LocalDate hoy = LocalDate.now();
        etiquetaFechaAgenda.setText(hoy.format(DateTimeFormatter.ofPattern(
                "EEEE dd 'de' MMMM 'de' yyyy", new Locale("es", "PE"))));
        try {
            cargarDiccionarios(contexto);
            cargarKpis(contexto, hoy);
            cargarAgenda(contexto, hoy);
        } catch (DentrixException e) {
            kpiCitas.setText("—");
        }
    }

    private void cargarDiccionarios(AppContext contexto) throws DentrixException {
        for (Paciente p : contexto.pacientes().listar()) {
            nombresPacientes.put(p.getId(), p.getApellidos() + ", " + p.getNombres());
        }
        for (Usuario u : contexto.usuarios().listarTodos()) {
            nombresOdontologos.put(u.getId(), "Dr. " + u.getNombres() + " " + u.getApellidos());
        }
        for (Tratamiento t : contexto.catalogo().listarTratamientos()) {
            nombresTratamientos.put(t.getId(), t.getNombre());
        }
    }

    private void cargarKpis(AppContext contexto, LocalDate fecha) throws DentrixException {
        KpiDia kpi = contexto.reportes().kpisDeFecha(fecha);
        kpiCitas.setText(String.valueOf(kpi.citas()));
        kpiSala.setText(String.valueOf(kpi.enSala()));
        kpiRecaudado.setText("S/ " + String.format(Locale.US, "%,.2f", kpi.recaudado()));
        kpiDeudores.setText(String.valueOf(kpi.deudores()));
    }

    private void cargarAgenda(AppContext contexto, LocalDate fecha) throws DentrixException {
        List<Cita> citas = contexto.agenda().agendaDelDia(fecha).stream()
                .filter(c -> "CITA".equals(c.getTipo()))
                .toList();
        tablaAgenda.setItems(FXCollections.observableArrayList(citas));
    }

    @FXML
    private void onBuscarClick() {
        String dni = campoDni.getText() == null ? "" : campoDni.getText().trim();
        if (dni.isEmpty()) {
            etiquetaBusqueda.setText("Escribe un DNI para buscar (RF-04).");
            return;
        }
        if (!dni.matches("\\d{8}")) {
            etiquetaBusqueda.setText("El DNI debe tener 8 dígitos.");
            return;
        }
        try {
            List<Paciente> encontrados = AppContext.get().pacientes().buscar(dni).stream()
                    .filter(p -> dni.equals(p.getDni()))
                    .toList();
            if (encontrados.isEmpty()) {
                etiquetaBusqueda.setText("🔍 No se encontró paciente con ese DNI — usa \"+ Registrar Nuevo Paciente\".");
            } else {
                Paciente p = encontrados.get(0);
                etiquetaBusqueda.setText("✔ " + p.getCodigoHc() + " · " + p.getApellidos() + ", "
                        + p.getNombres() + " (" + p.getEstado() + ")");
            }
        } catch (DentrixException e) {
            etiquetaBusqueda.setText("Error al buscar: " + e.getMessage());
        }
    }

    @FXML
    private void onRegistrarPacienteClick() {
        Navegador.ir(FXML_PACIENTES);
    }

    private String hora(Cita cita) {
        if (cita.getHoraInicio() == null) {
            return "";
        }
        String inicio = cita.getHoraInicio().toString();
        String fin = cita.getHoraFin() == null ? "" : cita.getHoraFin().toString();
        return inicio + " - " + fin;
    }

    private String tratamiento(Cita cita) {
        if (cita.getTratamientoId() != null) {
            return nombresTratamientos.getOrDefault(cita.getTratamientoId(), "—");
        }
        String motivo = cita.getMotivo();
        return motivo == null || motivo.isBlank() ? "—" : motivo;
    }
}
