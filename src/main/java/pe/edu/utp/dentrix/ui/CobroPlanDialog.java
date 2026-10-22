package pe.edu.utp.dentrix.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.model.Paciente;
import pe.edu.utp.dentrix.model.Pago;
import pe.edu.utp.dentrix.model.Presupuesto;
import pe.edu.utp.dentrix.model.PresupuestoItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class CobroPlanDialog {

    private static final String CSS = "/pe/edu/utp/dentrix/estilos.css";
    private static final List<String> MODALIDADES = List.of("EFECTIVO", "YAPE", "PLIN", "TARJETA");

    private final Stage stage = new Stage();
    private final Label lblPaciente = new Label("");
    private final ComboBox<Presupuesto> comboPresupuesto = new ComboBox<>();
    private final TableView<PresupuestoItem> tabla = new TableView<>();
    private final ObservableList<PresupuestoItem> filas = FXCollections.observableArrayList();
    private final Label lblVacio = new Label("No hay items por cobrar de este presupuesto");
    private final Label lblTotal = new Label("S/ 0.00");
    private final Label lblError = new Label("");
    private final ComboBox<String> comboModalidad = new ComboBox<>();
    private final TextField campoOperacion = new TextField();
    private final Button btnTodos = new Button("Seleccionar todos");
    private final Button btnBorrador = new Button("Guardar borrador");
    private final Button btnConfirmar = new Button("Confirmar y emitir");
    private final Button btnCerrar = new Button("Cerrar");

    private final Paciente paciente;
    private final Consumer<Pago> alEmitir;
    private Long borradorId;
    private final List<Long> itemsEnBorrador = new ArrayList<>();

    private CobroPlanDialog(Paciente paciente, Consumer<Pago> alEmitir) {
        this.paciente = paciente;
        this.alEmitir = alEmitir;
    }

    public static void mostrar(Window duenio, Paciente paciente, Consumer<Pago> alEmitir) {
        CobroPlanDialog dialogo = new CobroPlanDialog(paciente, alEmitir);
        dialogo.construir(duenio);
        dialogo.cargarPresupuestos();
        dialogo.stage.showAndWait();
    }

    private void construir(Window duenio) {
        stage.initOwner(duenio);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Cobrar plan aceptado");
        stage.setWidth(760);
        stage.setHeight(540);

        lblPaciente.setText("Paciente: " + paciente.getApellidos() + ", " + paciente.getNombres()
                + " (DNI " + paciente.getDni() + ")");
        lblPaciente.getStyleClass().add("tarjeta-titulo");

        comboPresupuesto.setPrefWidth(360);
        comboPresupuesto.setConverter(new StringConverter<Presupuesto>() {
            @Override
            public String toString(Presupuesto p) {
                return p == null ? "" : "v" + p.getVersion() + " · " + p.getEstado() + " · total S/ "
                        + formatear(p.getTotal());
            }

            @Override
            public Presupuesto fromString(String s) {
                return null;
            }
        });
        comboPresupuesto.valueProperty().addListener((obs, antes, ahora) -> cargarItems(ahora));

        lblVacio.getStyleClass().add("etiqueta-estado");
        lblVacio.setVisible(false);

        configurarTabla();
        StackPane contenedorTabla = new StackPane(tabla, lblVacio);
        VBox.setVgrow(contenedorTabla, Priority.ALWAYS);

        lblTotal.setStyle("-fx-font-family: Consolas; -fx-font-size: 15px; -fx-font-weight: bold;");

        comboModalidad.setItems(FXCollections.observableArrayList(MODALIDADES));
        comboModalidad.setValue("EFECTIVO");
        comboModalidad.setPrefWidth(130);
        campoOperacion.setPromptText("N° de operación");
        campoOperacion.setPrefWidth(190);
        campoOperacion.getStyleClass().add("campo-formulario");

        lblError.getStyleClass().add("etiqueta-error");
        lblError.setWrapText(true);
        lblError.setMinHeight(18);

        btnTodos.setOnAction(e -> tabla.getSelectionModel().selectAll());
        btnBorrador.setOnAction(e -> guardarBorrador());
        btnConfirmar.getStyleClass().add("boton-primario");
        btnConfirmar.setDefaultButton(true);
        btnConfirmar.setOnAction(e -> confirmarYEmitir());
        btnCerrar.setOnAction(e -> stage.close());

        HBox lineaPresupuesto = new HBox(8, new Label("Presupuesto:"), comboPresupuesto, btnTodos);
        lineaPresupuesto.setAlignment(Pos.CENTER_LEFT);

        HBox lineaPago = new HBox(8, new Label("Modalidad:"), comboModalidad,
                new Label("N° operación:"), campoOperacion);
        lineaPago.setAlignment(Pos.CENTER_LEFT);

        Label relleno = new Label(" ");
        HBox.setHgrow(relleno, Priority.ALWAYS);
        HBox lineaTotal = new HBox(10, new Label("Total seleccionado:"), lblTotal, relleno,
                btnBorrador, btnConfirmar, btnCerrar);
        lineaTotal.setAlignment(Pos.CENTER_LEFT);

        VBox raiz = new VBox(10, lblPaciente, lineaPresupuesto, contenedorTabla, lineaPago,
                lblError, lineaTotal);
        raiz.getStyleClass().add("tarjeta");

        Scene escena = new Scene(raiz);
        URL hoja = CobroPlanDialog.class.getResource(CSS);
        if (hoja != null) {
            escena.getStylesheets().add(hoja.toExternalForm());
        }
        stage.setScene(escena);
    }

    private void configurarTabla() {
        tabla.setItems(filas);
        tabla.getStyleClass().add("tabla-datos");
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<PresupuestoItem, String> colPieza = new TableColumn<>("PIEZA");
        colPieza.setPrefWidth(80);
        colPieza.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getPiezaFdi() == null ? "" : String.valueOf(d.getValue().getPiezaFdi())));

        TableColumn<PresupuestoItem, String> colPrestacion = new TableColumn<>("PRESTACIÓN");
        colPrestacion.setPrefWidth(300);
        colPrestacion.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getTratamientoNombre() == null ? "" : d.getValue().getTratamientoNombre()));

        TableColumn<PresupuestoItem, String> colMonto = new TableColumn<>("MONTO NETO");
        colMonto.setPrefWidth(130);
        colMonto.setCellValueFactory(d -> new ReadOnlyStringWrapper(formatear(monto(d.getValue()))));

        TableColumn<PresupuestoItem, String> colEstado = new TableColumn<>("ESTADO");
        colEstado.setPrefWidth(130);
        colEstado.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getEstado() == null ? "" : d.getValue().getEstado()));

        tabla.getColumns().setAll(colPieza, colPrestacion, colMonto, colEstado);
        tabla.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<PresupuestoItem>) c -> actualizarTotal());
    }

    private void cargarPresupuestos() {
        Integer pacienteId = paciente.getId();
        TareasFX.ejecutar(() -> {
            List<Presupuesto> aceptados = new ArrayList<>();
            for (Presupuesto p : AppContext.get().presupuestos().listarPorPaciente(pacienteId)) {
                if ("ACEPTADO".equals(p.getEstado())) {
                    aceptados.add(p);
                }
            }
            return aceptados;
        }, lista -> {
            comboPresupuesto.setItems(FXCollections.observableArrayList(lista));
            if (lista.isEmpty()) {
                lblError.setText("El paciente no tiene presupuestos ACEPTADO.");
            } else {
                comboPresupuesto.setValue(lista.get(0));
            }
        }, error -> lblError.setText(TareasFX.mensaje(error)));
    }

    private void cargarItems(Presupuesto presupuesto) {
        filas.clear();
        lblVacio.setVisible(false);
        actualizarTotal();
        if (presupuesto == null) {
            return;
        }
        Long presupuestoId = presupuesto.getId();
        TareasFX.ejecutar(() -> AppContext.get().presupuestos().itemsCobrables(presupuestoId),
                items -> {
                    filas.setAll(items);
                    tabla.refresh();
                    lblVacio.setVisible(items.isEmpty());
                },
                error -> lblError.setText(TareasFX.mensaje(error)));
    }

    private void actualizarTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (PresupuestoItem item : tabla.getSelectionModel().getSelectedItems()) {
            total = total.add(monto(item));
        }
        lblTotal.setText("S/ " + total.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    private void guardarBorrador() {
        lblError.setText("");
        List<PresupuestoItem> seleccionados = new ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        if (seleccionados.isEmpty()) {
            lblError.setText("Selecciona al menos un item del presupuesto.");
            return;
        }
        Integer pacienteId = paciente.getId();
        Integer usuarioId = usuarioActual();
        Long existente = borradorId;
        List<Long> yaAgregados = new ArrayList<>(itemsEnBorrador);
        TareasFX.ejecutar(() -> {
            var caja = AppContext.get().caja();
            Long id = existente;
            if (id == null) {
                id = caja.crearBorrador(pacienteId, null, usuarioId, null).getId();
            }
            for (PresupuestoItem item : seleccionados) {
                if (!yaAgregados.contains(item.getId())) {
                    caja.agregarItem(id, item.getId(), null, BigDecimal.ONE, null);
                }
            }
            return id;
        }, id -> {
            borradorId = id;
            for (PresupuestoItem item : seleccionados) {
                if (!itemsEnBorrador.contains(item.getId())) {
                    itemsEnBorrador.add(item.getId());
                }
            }
            mostrarMensaje("Borrador #" + id + " guardado", false);
        }, error -> lblError.setText(TareasFX.mensaje(error)));
    }

    private void confirmarYEmitir() {
        lblError.setText("");
        List<PresupuestoItem> seleccionados = new ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        if (seleccionados.isEmpty() && borradorId == null) {
            lblError.setText("Selecciona al menos un item del presupuesto.");
            return;
        }
        String modalidad = comboModalidad.getValue() == null ? "EFECTIVO" : comboModalidad.getValue();
        String texto = campoOperacion.getText() == null ? "" : campoOperacion.getText().trim();
        String referencia = texto.isEmpty() ? null : texto;
        if (!"EFECTIVO".equals(modalidad) && referencia == null) {
            lblError.setText("RF-08: Yape/Plin/Tarjeta requieren N° de operación");
            return;
        }
        Integer pacienteId = paciente.getId();
        Integer usuarioId = usuarioActual();
        Long existente = borradorId;
        List<Long> yaAgregados = new ArrayList<>(itemsEnBorrador);
        TareasFX.ejecutar(() -> {
            var caja = AppContext.get().caja();
            Long id = existente;
            if (id == null) {
                id = caja.crearBorrador(pacienteId, null, usuarioId, null).getId();
            }
            for (PresupuestoItem item : seleccionados) {
                if (!yaAgregados.contains(item.getId())) {
                    caja.agregarItem(id, item.getId(), null, BigDecimal.ONE, null);
                }
            }
            return caja.confirmar(id, modalidad, referencia);
        }, boleta -> {
            borradorId = null;
            itemsEnBorrador.clear();
            alEmitir.accept(boleta);
            stage.close();
        }, error -> lblError.setText(TareasFX.mensaje(error)));
    }

    private Integer usuarioActual() {
        return AppContext.get().sesion().getUsuario() == null
                ? null : AppContext.get().sesion().getUsuario().getId();
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        lblError.setText(mensaje);
        lblError.getStyleClass().removeAll("etiqueta-error", "etiqueta-ok");
        lblError.getStyleClass().add(error ? "etiqueta-error" : "etiqueta-ok");
    }

    private BigDecimal monto(PresupuestoItem item) {
        BigDecimal monto = item.getMontoNeto() == null ? item.getSubtotal() : item.getMontoNeto();
        return monto == null ? BigDecimal.ZERO : monto;
    }

    private String formatear(BigDecimal monto) {
        return monto == null ? "—" : "S/ " + monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
