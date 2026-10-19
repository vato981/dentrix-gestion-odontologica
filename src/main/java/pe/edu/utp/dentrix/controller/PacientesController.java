package pe.edu.utp.dentrix.controller;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.model.Paciente;
import pe.edu.utp.dentrix.ui.TareasFX;

import java.util.concurrent.atomic.AtomicInteger;

public class PacientesController {

    private static final Duration ESPERA_BUSQUEDA = Duration.millis(250);

    private final PauseTransition esperaBusqueda = new PauseTransition(ESPERA_BUSQUEDA);
    private final AtomicInteger generacionBusqueda = new AtomicInteger();

    @FXML
    private TextField campoBusqueda;

    @FXML
    private TableView<Paciente> tablaPacientes;

    @FXML
    private TableColumn<Paciente, String> colCodigo;

    @FXML
    private TableColumn<Paciente, String> colDni;

    @FXML
    private TableColumn<Paciente, String> colNombre;

    @FXML
    private TableColumn<Paciente, String> colCelular;

    @FXML
    private TableColumn<Paciente, String> colAcciones;

    @FXML
    private Label etiquetaEstado;

    @FXML
    private Label etiquetaConteo;

    @FXML
    private Label etiquetaModo;

    @FXML
    private Label etiquetaCodigo;

    @FXML
    private Label etiquetaError;

    @FXML
    private TextField campoDni;

    @FXML
    private TextField campoCodigo;

    @FXML
    private TextField campoApellidos;

    @FXML
    private TextField campoNombres;

    @FXML
    private DatePicker campoFechaNac;

    @FXML
    private TextField campoCelular;

    @FXML
    private TextField campoEmail;

    @FXML
    private TextField campoDireccion;

    @FXML
    private TextArea campoAlergias;

    @FXML
    private TextArea campoAntecedentes;

    private Paciente pacienteEditando;

    @FXML
    private void initialize() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoHc"));
        colDni.setCellValueFactory(new PropertyValueFactory<>("dni"));
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getApellidos() + ", " + d.getValue().getNombres()));
        colCelular.setCellValueFactory(new PropertyValueFactory<>("celular"));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnDesactivar = new Button("Desactivar");
            private final HBox caja = new HBox(6, btnEditar, btnDesactivar);

            {
                btnEditar.getStyleClass().add("boton-mini");
                btnDesactivar.getStyleClass().addAll("boton-mini", "boton-peligro");
                btnEditar.setOnAction(e -> cargarEnForma(getTableRow().getItem()));
                btnDesactivar.setOnAction(e -> inactivar(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(String item, boolean vacio) {
                super.updateItem(item, vacio);
                if (vacio) {
                    setGraphic(null);
                } else {
                    Paciente p = getTableRow().getItem();
                    btnDesactivar.setDisable(p == null || "INACTIVO".equals(p.getEstado()));
                    setGraphic(caja);
                }
            }
        });
        tablaPacientes.setRowFactory(fila -> {
            TableRow<Paciente> filaTabla = new TableRow<>();
            filaTabla.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !filaTabla.isEmpty()) {
                    cargarEnForma(filaTabla.getItem());
                }
            });
            return filaTabla;
        });
        esperaBusqueda.setOnFinished(evento -> recargar());
        campoBusqueda.textProperty().addListener((obs, antes, ahora) -> esperaBusqueda.playFromStart());
        limpiarForma();
        recargar();
    }

    private void recargar() {
        String texto = campoBusqueda.getText() == null ? "" : campoBusqueda.getText();
        int generacion = generacionBusqueda.incrementAndGet();
        TareasFX.ejecutar(() -> AppContext.get().pacientes().buscar(texto),
                pacientes -> {
                    if (generacion != generacionBusqueda.get()) {
                        return;
                    }
                    tablaPacientes.setItems(FXCollections.observableArrayList(pacientes));
                    long activos = pacientes.stream().filter(p -> !"INACTIVO".equals(p.getEstado())).count();
                    etiquetaConteo.setText(pacientes.size() + " Paciente(s) · " + activos + " activo(s)");
                    etiquetaEstado.setText("");
                },
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onBuscarClick() {
        recargar();
    }

    @FXML
    private void onNuevoClick() {
        limpiarForma();
        campoDni.requestFocus();
    }

    @FXML
    private void onRefrescarClick() {
        campoBusqueda.clear();
        limpiarForma();
        recargar();
    }

    @FXML
    private void onCancelarClick() {
        limpiarForma();
    }

    @FXML
    private void onGuardarClick() {
        indicar("", false);
        String dni = texto(campoDni);
        if (!dni.matches("\\d{8}")) {
            indicar("El DNI debe tener exactamente 8 dígitos.", true);
            return;
        }
        if (estaVacio(campoApellidos) || estaVacio(campoNombres)) {
            indicar("Apellidos y nombres son obligatorios.", true);
            return;
        }
        Paciente paciente = new Paciente();
        paciente.setId(pacienteEditando == null ? null : pacienteEditando.getId());
        paciente.setDni(dni);
        paciente.setApellidos(campoApellidos.getText().trim());
        paciente.setNombres(campoNombres.getText().trim());
        paciente.setFechaNac(campoFechaNac.getValue());
        paciente.setCelular(texto(campoCelular));
        paciente.setEmail(texto(campoEmail));
        paciente.setDireccion(texto(campoDireccion));
        paciente.setAlergias(texto(campoAlergias));
        paciente.setAntecedentes(texto(campoAntecedentes));
        TareasFX.ejecutar(() -> {
            if (pacienteEditando == null) {
                AppContext.get().pacientes().registrar(paciente);
            } else {
                paciente.setCodigoHc(pacienteEditando.getCodigoHc());
                AppContext.get().pacientes().editar(paciente);
            }
            return null;
        }, ignorado -> {
            limpiarForma();
            recargar();
            indicar("Datos del paciente guardados.", false);
        }, error -> indicar(TareasFX.mensaje(error), true));
    }

    @FXML
    private void onVolverClick() {
        pe.edu.utp.dentrix.ui.Navegador.ir(pe.edu.utp.dentrix.ui.Navegador.INICIO);
    }

    private void cargarEnForma(Paciente paciente) {
        if (paciente == null) {
            return;
        }
        pacienteEditando = paciente;
        etiquetaModo.setText("MOD-" + paciente.getCodigoHc());
        etiquetaCodigo.setText("Código HC " + paciente.getCodigoHc() + " · abierto el " + paciente.getFechaApertura());
        campoDni.setText(paciente.getDni());
        campoCodigo.setText(paciente.getCodigoHc());
        campoApellidos.setText(paciente.getApellidos());
        campoNombres.setText(paciente.getNombres());
        campoFechaNac.setValue(paciente.getFechaNac());
        campoCelular.setText(paciente.getCelular());
        campoEmail.setText(paciente.getEmail());
        campoDireccion.setText(paciente.getDireccion());
        campoAlergias.setText(paciente.getAlergias());
        campoAntecedentes.setText(paciente.getAntecedentes());
        indicar("", false);
    }

    private void limpiarForma() {
        pacienteEditando = null;
        etiquetaModo.setText("NUEVO");
        etiquetaCodigo.setText("El código HC se genera automáticamente");
        campoDni.clear();
        campoCodigo.clear();
        campoApellidos.clear();
        campoNombres.clear();
        campoFechaNac.setValue(null);
        campoCelular.clear();
        campoEmail.clear();
        campoDireccion.clear();
        campoAlergias.clear();
        campoAntecedentes.clear();
        indicar("", false);
    }

    private void inactivar(Paciente paciente) {
        if (paciente == null) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Desactivar paciente");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Desactivar a " + paciente.getNombres() + " " + paciente.getApellidos()
                + " (HC " + paciente.getCodigoHc() + ")?");
        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta == javafx.scene.control.ButtonType.OK) {
                TareasFX.ejecutar(() -> {
                    AppContext.get().pacientes().inactivar(paciente.getId());
                    return null;
                }, ignorado -> {
                    if (pacienteEditando != null && pacienteEditando.getId().equals(paciente.getId())) {
                        limpiarForma();
                    }
                    recargar();
                }, error -> mostrarError(TareasFX.mensaje(error)));
            }
        });
    }

    private void indicar(String mensaje, boolean error) {
        etiquetaError.setText(mensaje);
        etiquetaError.getStyleClass().removeAll("etiqueta-error", "etiqueta-ok");
        etiquetaError.getStyleClass().add(error ? "etiqueta-error" : "etiqueta-ok");
    }

    private boolean estaVacio(TextField campo) {
        return campo.getText() == null || campo.getText().isBlank();
    }

    private String texto(TextField campo) {
        return campo.getText() == null ? "" : campo.getText().trim();
    }

    private String texto(TextArea area) {
        if (area.getText() == null || area.getText().isBlank()) {
            return null;
        }
        return area.getText().trim();
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Pacientes");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
