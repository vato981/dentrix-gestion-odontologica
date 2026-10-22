package pe.edu.utp.dentrix.controller;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
import pe.edu.utp.dentrix.ui.Badges;
import pe.edu.utp.dentrix.ui.TareasFX;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PacientesController {

    private static final Duration ESPERA_BUSQUEDA = Duration.millis(250);
    private static final String ACTIVO = "ACTIVO";
    private static final String INACTIVO = "INACTIVO";
    private static final List<String> VINCULOS =
            List.of("Padre", "Madre", "Apoderado", "Tutor legal", "Otro");

    private final PauseTransition esperaBusqueda = new PauseTransition(ESPERA_BUSQUEDA);
    private final AtomicInteger generacionBusqueda = new AtomicInteger();

    @FXML
    private TextField campoBusqueda;

    @FXML
    private TableView<Paciente> tablaPacientes;

    @FXML
    private TableColumn<Paciente, String> colCodigo;

    @FXML
    private TableColumn<Paciente, String> colNombre;

    @FXML
    private TableColumn<Paciente, String> colDni;

    @FXML
    private TableColumn<Paciente, String> colEdad;

    @FXML
    private TableColumn<Paciente, String> colCelular;

    @FXML
    private TableColumn<Paciente, String> colEstado;

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

    @FXML
    private TextField campoTutorNombre;

    @FXML
    private TextField campoTutorDocumento;

    @FXML
    private ComboBox<String> comboTutorVinculo;

    @FXML
    private TextField campoPagadorNombre;

    @FXML
    private TextField campoPagadorDocumento;

    @FXML
    private ComboBox<String> comboEstado;

    @FXML
    private Button btnAnular;

    @FXML
    private Button btnReactivar;

    private Paciente pacienteEditando;

    @FXML
    private void initialize() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoHc"));
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getApellidos() + ", " + d.getValue().getNombres()));
        colDni.setCellValueFactory(new PropertyValueFactory<>("dni"));
        colEdad.setCellValueFactory(d -> new SimpleStringProperty(edad(d.getValue())));
        colCelular.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCelular() == null ? "—" : d.getValue().getCelular()));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEstado() == null ? "" : d.getValue().getEstado()));
        Badges.aplicarEn(colEstado);
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnEstado = new Button("Anular");
            private final HBox caja = new HBox(6, btnEditar, btnEstado);

            {
                btnEditar.getStyleClass().add("boton-mini");
                btnEstado.getStyleClass().addAll("boton-mini", "boton-peligro");
                btnEditar.setOnAction(e -> cargarEnForma(getTableRow().getItem()));
                btnEstado.setOnAction(e -> {
                    Paciente paciente = getTableRow().getItem();
                    if (paciente == null) {
                        return;
                    }
                    if (INACTIVO.equals(paciente.getEstado())) {
                        reactivar(paciente);
                    } else {
                        anular(paciente);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean vacio) {
                super.updateItem(item, vacio);
                if (vacio) {
                    setGraphic(null);
                    return;
                }
                int indice = getIndex();
                Paciente paciente = indice < 0 || indice >= getTableView().getItems().size()
                        ? null : getTableView().getItems().get(indice);
                boolean inactivo = paciente != null && INACTIVO.equals(paciente.getEstado());
                btnEstado.setText(inactivo ? "Reactivar" : "Anular");
                btnEstado.getStyleClass().removeAll("boton-peligro", "boton-primario");
                btnEstado.getStyleClass().add(inactivo ? "boton-primario" : "boton-peligro");
                btnEstado.setDisable(paciente == null);
                setGraphic(caja);
            }
        });
        tablaPacientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaPacientes.getSelectionModel().selectedItemProperty()
                .addListener((obs, antes, ahora) -> {
                    if (ahora != null) {
                        cargarEnForma(ahora);
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
        comboEstado.setItems(FXCollections.observableArrayList(ACTIVO, INACTIVO));
        comboTutorVinculo.setItems(FXCollections.observableArrayList(VINCULOS));
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
                    long activos = pacientes.stream().filter(p -> !INACTIVO.equals(p.getEstado())).count();
                    etiquetaConteo.setText(activos + " activo(s) de " + pacientes.size() + " paciente(s)");
                    etiquetaEstado.setText("");
                },
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private String edad(Paciente paciente) {        if (paciente == null || paciente.getFechaNac() == null) {
            return "—";
        }
        return String.valueOf(Period.between(paciente.getFechaNac(), LocalDate.now()).getYears());
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
        tablaPacientes.getSelectionModel().clearSelection();
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
            indicar("El DNI debe tener exactamente 8 digitos.", true);
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
        paciente.setTutorNombre(texto(campoTutorNombre));
        paciente.setTutorDocumento(texto(campoTutorDocumento));
        paciente.setTutorVinculo(comboTutorVinculo.getValue());
        paciente.setPagadorNombre(texto(campoPagadorNombre));
        paciente.setPagadorDocumento(texto(campoPagadorDocumento));
        String estado = comboEstado.getValue();
        paciente.setEstado(estado == null ? ACTIVO : estado);
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
    private void onAnularClick() {
        if (pacienteEditando == null) {
            indicar("Selecciona un paciente para anular.", true);
            return;
        }
        anular(pacienteEditando);
    }

    @FXML
    private void onReactivarClick() {
        if (pacienteEditando == null) {
            indicar("Selecciona un paciente inactivo para reactivar.", true);
            return;
        }
        reactivar(pacienteEditando);
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
        etiquetaCodigo.setText("Codigo HC " + paciente.getCodigoHc() + " · abierto el "
                + paciente.getFechaApertura() + " · R22: la historia no se elimina");
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
        campoTutorNombre.setText(paciente.getTutorNombre());
        campoTutorDocumento.setText(paciente.getTutorDocumento());
        comboTutorVinculo.setValue(paciente.getTutorVinculo());
        campoPagadorNombre.setText(paciente.getPagadorNombre());
        campoPagadorDocumento.setText(paciente.getPagadorDocumento());
        comboEstado.setValue(INACTIVO.equals(paciente.getEstado()) ? INACTIVO : ACTIVO);
        actualizarBotonesEstado();
        indicar("", false);
    }

    private void limpiarForma() {
        pacienteEditando = null;
        etiquetaModo.setText("NUEVO");
        etiquetaCodigo.setText("El codigo HC se genera automaticamente");
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
        campoTutorNombre.clear();
        campoTutorDocumento.clear();
        comboTutorVinculo.setValue(null);
        campoPagadorNombre.clear();
        campoPagadorDocumento.clear();
        comboEstado.setValue(ACTIVO);
        actualizarBotonesEstado();
        indicar("", false);
    }

    private void actualizarBotonesEstado() {
        boolean inactivo = pacienteEditando != null && INACTIVO.equals(pacienteEditando.getEstado());
        btnAnular.setDisable(pacienteEditando == null || inactivo);
        btnReactivar.setDisable(!inactivo);
    }

    private void anular(Paciente paciente) {
        if (paciente == null) {
            return;
        }
        javafx.scene.control.TextInputDialog dialogo = new javafx.scene.control.TextInputDialog();
        dialogo.setTitle("Anular paciente");
        dialogo.setHeaderText("Anular a " + paciente.getNombres() + " " + paciente.getApellidos()
                + " (HC " + paciente.getCodigoHc() + ")");
        dialogo.setContentText("Motivo de la anulacion (R22):");
        java.util.Optional<String> motivo = dialogo.showAndWait();
        if (motivo.isEmpty() || motivo.get().isBlank()) {
            indicar("La anulacion exige un motivo (R22).", true);
            return;
        }
        TareasFX.ejecutar(() -> {
            AppContext.get().pacientes().inactivar(paciente.getId(), motivo.get());
            return null;
        }, ignorado -> {
            limpiarForma();
            recargar();
            indicar("Paciente anulado: queda INACTIVO con su historia intacta.", false);
        }, error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void reactivar(Paciente paciente) {
        if (paciente == null) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Reactivar paciente");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("Reactivar a " + paciente.getNombres() + " " + paciente.getApellidos()
                + " (HC " + paciente.getCodigoHc() + ")?");
        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta == javafx.scene.control.ButtonType.OK) {
                TareasFX.ejecutar(() -> {
                    AppContext.get().pacientes().reactivar(paciente.getId());
                    return null;
                }, ignorado -> {
                    limpiarForma();
                    recargar();
                    indicar("Paciente reactivado.", false);
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
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        return valor.isEmpty() ? null : valor;
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
