package org.example.demofx;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.time.LocalDate;

public class AnchorPaneController {

    private static final LocalDate FECHA_BASE = LocalDate.of(2026, 9, 28);

    @FXML private DatePicker dpFiltroFecha;
    @FXML private ComboBox<String> cbFiltroConsultorio;
    @FXML private Button btnNuevaCita;
    @FXML private TextField txtDni;
    @FXML private Button btnValidar;
    @FXML private Label lblPaciente;
    @FXML private ComboBox<String> cbOdontologo;
    @FXML private ComboBox<String> cbTratamiento;
    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<String> cbHorario;
    @FXML private Button btnGuardar;
    @FXML private Button btnEliminar;
    @FXML private Button btnCargar;
    @FXML private Button btnCambiarVista;
    @FXML private Button btnCerrarSesion;
    @FXML private TableView<Cita> tblTurnos;
    @FXML private TableColumn<Cita, String> colHorario;
    @FXML private TableColumn<Cita, String> colConsultorio;
    @FXML private TableColumn<Cita, String> colPaciente;
    @FXML private TableColumn<Cita, String> colTratamiento;
    @FXML private TableColumn<Cita, String> colDoctor;

    @FXML
    private void initialize() {
        cbFiltroConsultorio.setItems(FXCollections.observableArrayList(
                "Todos los Consultorios",
                "Consultorio 01 (Dr. Juan Vallejos)",
                "Consultorio 02 (Dra. Jesica Perez)"));
        cbFiltroConsultorio.getSelectionModel().selectFirst();

        cbOdontologo.setItems(FXCollections.observableArrayList(
                "Consultorio 01 — Dr. Juan Vallejos (Ortodoncia / Cirugía)",
                "Consultorio 02 — Dra. Jesica Perez (Endodoncia / Operatoria)"));
        cbOdontologo.getSelectionModel().selectFirst();

        cbTratamiento.setItems(FXCollections.observableArrayList(
                "Control de Ortodoncia (Ajuste de Brackets)",
                "TRAT-01: Curación con Resina Compuesta",
                "TRAT-02: Profilaxis y Destartraje Simple",
                "TRAT-04: Exodoncia / Cirugía Simple"));
        cbTratamiento.getSelectionModel().selectFirst();

        cbHorario.setItems(FXCollections.observableArrayList(
                "09:00 AM - 09:45 AM",
                "10:00 AM - 10:45 AM",
                "11:15 AM - 12:00 PM",
                "02:00 PM - 02:45 PM",
                "03:30 PM - 04:15 PM"));
        cbHorario.getSelectionModel().selectFirst();

        dpFiltroFecha.setValue(FECHA_BASE);
        dpFecha.setValue(FECHA_BASE);

        colHorario.setCellValueFactory(new PropertyValueFactory<>("horario"));
        colConsultorio.setCellValueFactory(new PropertyValueFactory<>("consultorio"));
        colPaciente.setCellValueFactory(new PropertyValueFactory<>("paciente"));
        colTratamiento.setCellValueFactory(new PropertyValueFactory<>("tratamiento"));
        colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctor"));

        tblTurnos.setItems(FXCollections.observableArrayList(
                new Cita("09:00 - 09:45", "Consultorio 01", "Juan Pérez", "Control Ortodoncia", "Dr. Juan V."),
                new Cita("10:00 - 10:45", "Consultorio 02", "María Gómez", "Curación Resina", "Dra. Jesica P."),
                new Cita("11:15 - 12:00", "Consultorio 01", "Pedro Díaz", "Exodoncia Simple", "Dr. Juan V."),
                new Cita("01:00 - 02:00", "—", "ALMUERZO (Bloqueado)", "Pausa personal", "—"),
                new Cita("03:30 - 04:15", "Consultorio 02", "Ana Huamán", "Evaluación Inicial", "Dra. Jesica P.")));

        tblTurnos.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                cargarSeleccion();
            }
        });
    }

    @FXML
    private void onValidarClick() {
        lblPaciente.setText("Paciente: Juan Pérez Alania");
    }

    @FXML
    private void onGuardarClick() {
        String odon = String.valueOf(cbOdontologo.getValue());
        String consultorio = odon.contains("02") ? "Consultorio 02" : "Consultorio 01";
        String doctor = odon.contains("02") ? "Dra. Jesica P." : "Dr. Juan V.";
        String paciente = lblPaciente.getText().replace("Paciente:", "").trim();
        if (paciente.isEmpty() || paciente.equals("—")) {
            paciente = "DNI " + txtDni.getText().trim();
        }
        tblTurnos.getItems().add(new Cita(
                String.valueOf(cbHorario.getValue()),
                consultorio,
                paciente,
                String.valueOf(cbTratamiento.getValue()),
                doctor));
        tblTurnos.getSelectionModel().selectLast();
    }

    @FXML
    private void onEliminarClick() {
        Cita sel = tblTurnos.getSelectionModel().getSelectedItem();
        if (sel != null) {
            tblTurnos.getItems().remove(sel);
        }
    }

    @FXML
    private void onNuevaCitaClick() {
        txtDni.clear();
        lblPaciente.setText("Paciente: —");
        cbOdontologo.getSelectionModel().selectFirst();
        cbTratamiento.getSelectionModel().selectFirst();
        cbHorario.getSelectionModel().selectFirst();
        dpFecha.setValue(FECHA_BASE);
        tblTurnos.getSelectionModel().clearSelection();
    }

    @FXML
    private void onCargarClick() {
        cargarSeleccion();
    }

    private void cargarSeleccion() {
        Cita sel = tblTurnos.getSelectionModel().getSelectedItem();
        if (sel == null) {
            return;
        }
        lblPaciente.setText("Paciente: " + sel.getPaciente());
        if (cbHorario.getItems().contains(sel.getHorario())) {
            cbHorario.setValue(sel.getHorario());
        }
        if (cbTratamiento.getItems().contains(sel.getTratamiento())) {
            cbTratamiento.setValue(sel.getTratamiento());
        }
    }

    @FXML
    private void onCerrarSesion() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Cerrar Sesión");
        alert.setHeaderText(null);
        alert.setContentText("Sesión cerrada (demo).");
        alert.showAndWait();
    }

    @FXML
    private void onCambiarVista() throws IOException {
        btnCambiarVista.getScene().setRoot(FXMLLoader.load(App.class.getResource("primary.fxml")));
    }
}
