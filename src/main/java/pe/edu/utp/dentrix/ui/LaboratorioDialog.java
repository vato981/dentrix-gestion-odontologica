package pe.edu.utp.dentrix.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.config.SesionActual;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.ConsumoLote;
import pe.edu.utp.dentrix.model.EjecucionItem;
import pe.edu.utp.dentrix.model.LoteInsumo;
import pe.edu.utp.dentrix.model.OrdenLaboratorio;
import pe.edu.utp.dentrix.model.PresupuestoItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class LaboratorioDialog {

    private static final String CSS = "/pe/edu/utp/dentrix/estilos.css";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PresupuestoItem item;
    private final EjecucionItem sesion;
    private final Stage stage = new Stage();
    private final Label lblCabecera = new Label("");
    private final Label lblAyuda = new Label(
            "La corona no se instala sin recepcion y control conformes (R18)");
    private final Label lblMensaje = new Label("");
    private final TableView<OrdenLaboratorio> tablaOrdenes = new TableView<>();
    private final ObservableList<OrdenLaboratorio> ordenes = FXCollections.observableArrayList();
    private final Button btnSolicitar = new Button("Solicitar orden");
    private final Button btnRecibir = new Button("Recibir");
    private final Button btnControlar = new Button("Controlar");
    private final Button btnInstalar = new Button("Instalar");
    private final Button btnAnular = new Button("Anular");
    private final TableView<ConsumoLote> tablaConsumos = new TableView<>();
    private final ObservableList<ConsumoLote> consumos = FXCollections.observableArrayList();
    private final ComboBox<LoteInsumo> comboLote = new ComboBox<>();
    private final TextField campoCantidad = new TextField();
    private final Button btnAgregarConsumo = new Button("Agregar consumo");
    private final Button btnQuitarConsumo = new Button("Quitar consumo");
    private final Map<Long, LocalDate> vencePorLote = new HashMap<>();

    private LaboratorioDialog(PresupuestoItem item, EjecucionItem sesion) {
        this.item = item;
        this.sesion = sesion;
    }

    public static void mostrar(Window duenio, PresupuestoItem item, EjecucionItem sesion, Runnable alCambiar) {
        if (item == null || item.getId() == null) {
            return;
        }
        LaboratorioDialog dialogo = new LaboratorioDialog(item, sesion);
        dialogo.construir(duenio);
        dialogo.cargar();
        dialogo.stage.showAndWait();
        if (alCambiar != null) {
            alCambiar.run();
        }
    }

    private void construir(Window duenio) {
        stage.initOwner(duenio);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Laboratorio e insumos");
        stage.setWidth(900);
        stage.setHeight(600);

        lblCabecera.getStyleClass().add("tarjeta-titulo");
        lblCabecera.setText("Pieza " + (item.getPiezaFdi() == null ? "—" : item.getPiezaFdi())
                + " · " + (item.getTratamientoNombre() == null
                ? "Item " + item.getId() : item.getTratamientoNombre()));

        lblAyuda.setStyle("-fx-font-size: 11px;");
        lblAyuda.setWrapText(true);

        lblMensaje.getStyleClass().add("etiqueta-error");
        lblMensaje.setWrapText(true);
        lblMensaje.setMinHeight(18);

        configurarTablaOrdenes();

        btnSolicitar.getStyleClass().add("boton-primario");
        btnSolicitar.setOnAction(e -> solicitar());
        btnRecibir.setOnAction(e -> recibir());
        btnControlar.setOnAction(e -> controlar());
        btnInstalar.setOnAction(e -> instalar());
        btnAnular.setOnAction(e -> anular());

        HBox lineaBotones = new HBox(8, btnSolicitar, btnRecibir, btnControlar, btnInstalar, btnAnular);
        lineaBotones.setAlignment(Pos.CENTER_LEFT);

        VBox raiz = new VBox(10, lblCabecera, titulo("ORDENES DE LABORATORIO"), tablaOrdenes,
                lineaBotones, lblAyuda, lblMensaje);
        if (sesion != null) {
            raiz.getChildren().add(construirInsumos());
        }
        raiz.getStyleClass().add("tarjeta");
        raiz.setPadding(new Insets(16));
        VBox.setVgrow(tablaOrdenes, Priority.ALWAYS);

        aplicarEstadoOrden();
        aplicarEstadoConsumo();

        Scene escena = new Scene(raiz);
        URL hoja = LaboratorioDialog.class.getResource(CSS);
        if (hoja != null) {
            escena.getStylesheets().add(hoja.toExternalForm());
        }
        stage.setScene(escena);
    }

    private void configurarTablaOrdenes() {
        tablaOrdenes.setItems(ordenes);
        tablaOrdenes.getStyleClass().add("tabla-datos");
        tablaOrdenes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaOrdenes.setPrefHeight(190);

        TableColumn<OrdenLaboratorio, String> colLaboratorio = new TableColumn<>("LABORATORIO");
        colLaboratorio.setPrefWidth(180);
        colLaboratorio.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getLaboratorio() == null ? "" : d.getValue().getLaboratorio()));

        TableColumn<OrdenLaboratorio, String> colTrabajo = new TableColumn<>("TRABAJO");
        colTrabajo.setPrefWidth(260);
        colTrabajo.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getTrabajo() == null ? "" : d.getValue().getTrabajo()));

        TableColumn<OrdenLaboratorio, String> colEstado = new TableColumn<>("ESTADO");
        colEstado.setPrefWidth(120);
        colEstado.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getEstado() == null ? "" : d.getValue().getEstado()));

        TableColumn<OrdenLaboratorio, String> colPrometida = new TableColumn<>("PROMETIDA");
        colPrometida.setPrefWidth(110);
        colPrometida.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getFechaPrometida() == null
                        ? "—" : FECHA.format(d.getValue().getFechaPrometida())));

        TableColumn<OrdenLaboratorio, String> colCosto = new TableColumn<>("COSTO");
        colCosto.setPrefWidth(110);
        colCosto.setCellValueFactory(d -> new ReadOnlyStringWrapper(formatear(d.getValue().getCosto())));

        tablaOrdenes.getColumns().setAll(colLaboratorio, colTrabajo, colEstado, colPrometida, colCosto);
        tablaOrdenes.getSelectionModel().selectedItemProperty()
                .addListener((obs, antes, ahora) -> aplicarEstadoOrden());
    }

    private VBox construirInsumos() {
        VBox seccion = new VBox(8);
        seccion.getChildren().add(titulo("INSUMOS CONSUMIDOS"));
        if (!esBorrador()) {
            Label aviso = new Label("Los insumos se registran antes de firmar la sesion");
            aviso.setStyle("-fx-font-size: 12px;");
            aviso.setWrapText(true);
            seccion.getChildren().add(aviso);
            return seccion;
        }
        configurarTablaConsumos();

        comboLote.setPrefWidth(330);
        comboLote.setConverter(new StringConverter<LoteInsumo>() {
            @Override
            public String toString(LoteInsumo lote) {
                if (lote == null) {
                    return "";
                }
                return (lote.getInsumoNombre() == null ? "Insumo " + lote.getInsumoId()
                        : lote.getInsumoNombre()) + " · lote " + lote.getNumero();
            }

            @Override
            public LoteInsumo fromString(String texto) {
                return null;
            }
        });
        campoCantidad.getStyleClass().add("campo-formulario");
        campoCantidad.setPromptText("Cantidad");
        campoCantidad.setPrefWidth(90);
        btnAgregarConsumo.getStyleClass().add("boton-primario");
        btnAgregarConsumo.setOnAction(e -> agregarConsumo());
        btnQuitarConsumo.setOnAction(e -> quitarConsumo());

        Label relleno = new Label(" ");
        HBox.setHgrow(relleno, Priority.ALWAYS);
        HBox formulario = new HBox(8, new Label("Insumo:"), comboLote, new Label("Cantidad:"),
                campoCantidad, btnAgregarConsumo, relleno, btnQuitarConsumo);
        formulario.setAlignment(Pos.CENTER_LEFT);

        seccion.getChildren().addAll(tablaConsumos, formulario);
        VBox.setVgrow(tablaConsumos, Priority.ALWAYS);
        return seccion;
    }

    private void configurarTablaConsumos() {
        tablaConsumos.setItems(consumos);
        tablaConsumos.getStyleClass().add("tabla-datos");
        tablaConsumos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaConsumos.setPrefHeight(150);

        TableColumn<ConsumoLote, String> colInsumo = new TableColumn<>("INSUMO");
        colInsumo.setPrefWidth(300);
        colInsumo.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getInsumoNombre() == null ? "" : d.getValue().getInsumoNombre()));

        TableColumn<ConsumoLote, String> colLote = new TableColumn<>("LOTE");
        colLote.setPrefWidth(140);
        colLote.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getLote() == null ? "" : d.getValue().getLote()));

        TableColumn<ConsumoLote, String> colVence = new TableColumn<>("VENCE");
        colVence.setPrefWidth(110);
        colVence.setCellValueFactory(d -> {
            LocalDate vence = vencePorLote.get(d.getValue().getLoteInsumoId());
            return new ReadOnlyStringWrapper(vence == null ? "—" : FECHA.format(vence));
        });

        TableColumn<ConsumoLote, String> colCantidad = new TableColumn<>("CANTIDAD");
        colCantidad.setPrefWidth(100);
        colCantidad.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getCantidad() == null
                        ? "" : d.getValue().getCantidad().stripTrailingZeros().toPlainString()));

        tablaConsumos.getColumns().setAll(colInsumo, colLote, colVence, colCantidad);
        tablaConsumos.getSelectionModel().selectedItemProperty()
                .addListener((obs, antes, ahora) -> aplicarEstadoConsumo());
    }

    private void cargar() {
        lblMensaje.setText("");
        try {
            cargarLotes();
            ordenes.setAll(AppContext.get().laboratorio().ordenes(item.getId()));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
        cargarConsumos();
        aplicarEstadoOrden();
        aplicarEstadoConsumo();
    }

    private void cargarLotes() {
        if (!esBorrador()) {
            return;
        }
        try {
            List<LoteInsumo> lotes = AppContext.get().laboratorio().lotes();
            vencePorLote.clear();
            for (LoteInsumo lote : lotes) {
                if (lote.getId() != null) {
                    vencePorLote.put(lote.getId(), lote.getVence());
                }
            }
            comboLote.setItems(FXCollections.observableArrayList(lotes));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void cargarConsumos() {
        if (!esBorrador()) {
            return;
        }
        try {
            consumos.setAll(AppContext.get().laboratorio().consumos(sesion.getId()));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void solicitar() {
        lblMensaje.setText("");
        TextField laboratorio = campo("Nombre del laboratorio");
        TextField trabajo = campo("Trabajo solicitado");
        TextField material = campo("Opcional");
        TextField color = campo("Opcional");
        DatePicker prometida = new DatePicker(LocalDate.now().plusDays(7));
        TextField costo = campo("0.00");

        GridPane grilla = grilla();
        grilla.addRow(0, new Label("Laboratorio:"), laboratorio);
        grilla.addRow(1, new Label("Trabajo:"), trabajo);
        grilla.addRow(2, new Label("Material:"), material);
        grilla.addRow(3, new Label("Color:"), color);
        grilla.addRow(4, new Label("Prometida:"), prometida);
        grilla.addRow(5, new Label("Costo:"), costo);
        if (formulario("Solicitar orden",
                "La orden nace SOLICITADA; la BD impide dos ordenes activas", grilla, "Solicitar").isEmpty()) {
            return;
        }
        String textoLaboratorio = texto(laboratorio);
        if (textoLaboratorio == null) {
            mostrarError("El laboratorio es obligatorio");
            return;
        }
        String textoTrabajo = texto(trabajo);
        if (textoTrabajo == null) {
            mostrarError("El trabajo solicitado al laboratorio es obligatorio");
            return;
        }
        BigDecimal monto;
        try {
            monto = monto(costo);
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
            return;
        }
        Long sesionId = sesion == null ? null : sesion.getId();
        try {
            AppContext.get().laboratorio().solicitar(item.getId(), sesionId, textoLaboratorio,
                    textoTrabajo, texto(material), texto(color), prometida.getValue(), monto, usuarioId());
            cargar();
            indicar("Orden de laboratorio solicitada");
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void recibir() {
        OrdenLaboratorio orden = seleccionada();
        if (orden == null || !orden.esSolicitada()) {
            mostrarError("Solo se recibe una orden SOLICITADA");
            return;
        }
        try {
            AppContext.get().laboratorio().recibir(orden.getId());
            cargar();
            indicar("Orden recibida: pasa a control de recepcion");
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void controlar() {
        OrdenLaboratorio orden = seleccionada();
        if (orden == null || !orden.esRecibida()) {
            mostrarError("Solo se controla una orden RECIBIDA");
            return;
        }
        lblMensaje.setText("");
        TextField ajuste = campo("Ajuste realizado");
        CheckBox colorOk = new CheckBox("Color OK");
        CheckBox contactosOk = new CheckBox("Contactos OK");
        CheckBox conforme = new CheckBox("Conforme");
        conforme.setSelected(true);
        TextArea observaciones = new TextArea();
        observaciones.setPrefHeight(80);
        observaciones.setWrapText(true);
        observaciones.setPromptText("Observaciones del control");
        observaciones.getStyleClass().add("campo-formulario");

        GridPane grilla = grilla();
        grilla.addRow(0, new Label("Ajuste:"), ajuste);
        grilla.addRow(1, new Label("Color OK:"), colorOk);
        grilla.addRow(2, new Label("Contactos OK:"), contactosOk);
        grilla.addRow(3, new Label("Conforme:"), conforme);
        grilla.addRow(4, new Label("Observaciones:"), observaciones);
        if (formulario("Controlar orden", "El rechazo exige observaciones (R18)",
                grilla, "Controlar").isEmpty()) {
            return;
        }
        String textoObservaciones = texto(observaciones);
        if (!conforme.isSelected() && textoObservaciones == null) {
            mostrarError("El rechazo del trabajo exige observaciones");
            return;
        }
        try {
            AppContext.get().laboratorio().controlar(orden.getId(), texto(ajuste),
                    colorOk.isSelected(), contactosOk.isSelected(), conforme.isSelected(),
                    textoObservaciones, usuarioId());
            cargar();
            indicar(conforme.isSelected()
                    ? "Control conforme: la orden puede instalarse"
                    : "Control rechazado: la orden vuelve a reproceso");
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void instalar() {
        OrdenLaboratorio orden = seleccionada();
        if (orden == null || !orden.esConforme()) {
            mostrarError("Solo se instala una orden CONFORME");
            return;
        }
        try {
            AppContext.get().laboratorio().instalar(orden.getId());
            cargar();
            indicar("Orden instalada");
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void anular() {
        OrdenLaboratorio orden = seleccionada();
        if (orden == null) {
            mostrarError("Selecciona la orden a anular");
            return;
        }
        if (orden.esInstalada()) {
            mostrarError("Una orden INSTALADA no se anula");
            return;
        }
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.initOwner(stage);
        dialogo.setTitle("Anular orden");
        dialogo.setHeaderText("La orden anulada se conserva como evidencia");
        dialogo.setContentText("Motivo de la anulacion:");
        Optional<String> respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty() || respuesta.get().isBlank()) {
            return;
        }
        String motivo = respuesta.get().trim();
        try {
            AppContext.get().laboratorio().anular(orden.getId(), motivo);
            cargar();
            indicar("Orden anulada: " + motivo);
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void agregarConsumo() {
        if (!esBorrador()) {
            mostrarError("Los insumos se registran antes de firmar la sesion");
            return;
        }
        lblMensaje.setText("");
        LoteInsumo lote = comboLote.getValue();
        if (lote == null || lote.getId() == null) {
            mostrarError("Selecciona el lote del insumo");
            return;
        }
        BigDecimal cantidad;
        try {
            cantidad = cantidad(campoCantidad);
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
            return;
        }
        try {
            AppContext.get().laboratorio().consumir(sesion.getId(), lote.getId(), cantidad, usuarioId());
            campoCantidad.clear();
            cargarConsumos();
            aplicarEstadoConsumo();
            indicar("Consumo registrado");
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void quitarConsumo() {
        ConsumoLote consumo = tablaConsumos.getSelectionModel().getSelectedItem();
        if (consumo == null || consumo.getId() == null) {
            mostrarError("Selecciona el consumo a quitar");
            return;
        }
        try {
            AppContext.get().laboratorio().quitarConsumo(consumo.getId());
            cargarConsumos();
            aplicarEstadoConsumo();
            indicar("Consumo quitado");
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void aplicarEstadoOrden() {
        OrdenLaboratorio orden = tablaOrdenes.getSelectionModel().getSelectedItem();
        btnRecibir.setDisable(orden == null || !orden.esSolicitada());
        btnControlar.setDisable(orden == null || !orden.esRecibida());
        btnInstalar.setDisable(orden == null || !orden.esConforme());
        btnAnular.setDisable(orden == null || orden.esInstalada());
    }

    private void aplicarEstadoConsumo() {
        ConsumoLote consumo = tablaConsumos.getSelectionModel().getSelectedItem();
        btnQuitarConsumo.setDisable(consumo == null);
    }

    private boolean esBorrador() {
        return sesion != null && "BORRADOR".equals(sesion.getEstado());
    }

    private OrdenLaboratorio seleccionada() {
        return tablaOrdenes.getSelectionModel().getSelectedItem();
    }

    private Label titulo(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("tarjeta-etiqueta");
        return etiqueta;
    }

    private TextField campo(String prompt) {
        TextField campo = new TextField();
        campo.setPromptText(prompt);
        campo.getStyleClass().add("campo-formulario");
        return campo;
    }

    private GridPane grilla() {
        GridPane grilla = new GridPane();
        grilla.setHgap(8.0);
        grilla.setVgap(8.0);
        grilla.setPadding(new Insets(10.0));
        return grilla;
    }

    private Optional<ButtonType> formulario(String titulo, String cabecera, Node contenido,
                                            String textoAccion) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.initOwner(stage);
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(cabecera);
        ButtonType accion = new ButtonType(textoAccion, ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().setAll(accion, ButtonType.CANCEL);
        dialogo.getDialogPane().setContent(contenido);
        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != accion) {
            return Optional.empty();
        }
        return respuesta;
    }

    private Integer usuarioId() {
        SesionActual actual = AppContext.get().sesion();
        if (actual.activa() && actual.getUsuario() != null && actual.getUsuario().getId() != null) {
            return actual.getUsuario().getId();
        }
        return 1;
    }

    private BigDecimal monto(TextField campo) throws DentrixException {
        String valor = texto(campo);
        if (valor == null) {
            return null;
        }
        try {
            return new BigDecimal(valor.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new DentrixException("El costo del laboratorio debe ser un numero (ej. 95.00)");
        }
    }

    private BigDecimal cantidad(TextField campo) throws DentrixException {
        String valor = texto(campo);
        if (valor == null) {
            throw new DentrixException("La cantidad consumida es obligatoria");
        }
        try {
            return new BigDecimal(valor.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new DentrixException("La cantidad debe ser un numero (ej. 1.5)");
        }
    }

    private String texto(TextField campo) {
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        return valor.isEmpty() ? null : valor;
    }

    private String texto(TextArea area) {
        String valor = area.getText() == null ? "" : area.getText().trim();
        return valor.isEmpty() ? null : valor;
    }

    private String formatear(BigDecimal valor) {
        if (valor == null) {
            return "—";
        }
        return "S/ " + valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void indicar(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.getStyleClass().removeAll("etiqueta-error", "etiqueta-ok");
        lblMensaje.getStyleClass().add("etiqueta-ok");
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.getStyleClass().removeAll("etiqueta-error", "etiqueta-ok");
        lblMensaje.getStyleClass().add("etiqueta-error");
    }
}
