package pe.edu.utp.dentrix.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Enmienda;
import pe.edu.utp.dentrix.model.Presupuesto;
import pe.edu.utp.dentrix.model.PresupuestoItem;
import pe.edu.utp.dentrix.model.Tratamiento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class EnmiendaDialog {

    private static final String CSS = "/pe/edu/utp/dentrix/estilos.css";
    private static final List<String> FASES = List.of("URGENTE", "BASICA", "RECONSTRUCTIVA",
            "QUIRURGICA", "REHABILITACION", "MANTENIMIENTO");
    private static final List<String> ROLES_FIRMANTE = List.of("PACIENTE", "TUTOR", "PAGADOR");

    private record Anulacion(PresupuestoItem item, String motivo) {
    }

    private record Agregado(Tratamiento tratamiento, PresupuestoItem item, BigDecimal precio) {
    }

    private final Presupuesto presupuesto;
    private final Stage stage = new Stage();
    private final Label lblCabecera = new Label("");
    private final Label lblTotalActual = new Label("Total actual: S/ 0.00");
    private final Label lblNuevoTotal = new Label("Nuevo total estimado: S/ 0.00");
    private final TextField campoMotivo = new TextField();
    private final TableView<PresupuestoItem> tablaPlan = new TableView<>();
    private final ObservableList<PresupuestoItem> itemsPlan = FXCollections.observableArrayList();
    private final TextField campoMotivoAnulacion = new TextField();
    private final ComboBox<Tratamiento> comboTratamiento = new ComboBox<>();
    private final TextField campoPieza = new TextField();
    private final ComboBox<String> comboFase = new ComboBox<>();
    private final Spinner<Integer> spinnerPrioridad = new Spinner<>();
    private final Spinner<Integer> spinnerCantidad = new Spinner<>();
    private final Spinner<Integer> spinnerSesiones = new Spinner<>();
    private final CheckBox checkConsentimiento = new CheckBox("Requiere consentimiento");
    private final ListView<String> listaCambios = new ListView<>();
    private final ObservableList<String> cambios = FXCollections.observableArrayList();
    private final TextField campoNombre = new TextField();
    private final TextField campoDocumento = new TextField();
    private final ComboBox<String> comboRol = new ComboBox<>();
    private final Label lblError = new Label("");
    private final List<Anulacion> anulaciones = new ArrayList<>();
    private final List<Agregado> agregados = new ArrayList<>();
    private boolean firmado;

    private EnmiendaDialog(Presupuesto presupuesto) {
        this.presupuesto = presupuesto;
    }

    public static boolean mostrar(Window duenio, Presupuesto presupuesto) {
        if (presupuesto == null || presupuesto.getId() == null) {
            return false;
        }
        EnmiendaDialog dialogo = new EnmiendaDialog(presupuesto);
        dialogo.construir(duenio);
        dialogo.cargarPlan();
        dialogo.stage.showAndWait();
        return dialogo.firmado;
    }

    private void construir(Window duenio) {
        stage.initOwner(duenio);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Enmendar plan aceptado");
        stage.setWidth(900);
        stage.setHeight(620);

        lblCabecera.getStyleClass().add("tarjeta-titulo");
        lblCabecera.setText("Paciente: " + textoPlano(presupuesto.getPacienteNombre())
                + "   Version: v" + presupuesto.getVersion()
                + "   Estado: " + presupuesto.getEstado());
        lblTotalActual.getStyleClass().add("tarjeta-etiqueta");
        lblNuevoTotal.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        campoMotivo.getStyleClass().add("campo-formulario");
        campoMotivo.setPromptText("Motivo de la enmienda (obligatorio)");
        HBox lineaMotivo = new HBox(8, new Label("Motivo de la enmienda:"), campoMotivo);
        lineaMotivo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(campoMotivo, Priority.ALWAYS);

        HBox lineaTotales = new HBox(24, lblTotalActual, lblNuevoTotal);
        lineaTotales.setAlignment(Pos.CENTER_LEFT);

        configurarTablaPlan();

        campoMotivoAnulacion.getStyleClass().add("campo-formulario");
        campoMotivoAnulacion.setPromptText("Motivo de la anulacion");
        Button btnAnular = new Button("Marcar para anular");
        btnAnular.setOnAction(e -> marcarParaAnular());
        HBox lineaAnular = new HBox(8, campoMotivoAnulacion, btnAnular);
        lineaAnular.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(campoMotivoAnulacion, Priority.ALWAYS);

        VBox columnaPlan = new VBox(8, titulo("PLAN ACTUAL"), tablaPlan, lineaAnular);
        VBox.setVgrow(tablaPlan, Priority.ALWAYS);
        HBox.setHgrow(columnaPlan, Priority.ALWAYS);
        columnaPlan.setPrefWidth(450);

        GridPane formulario = formularioAgregar();
        listaCambios.setItems(cambios);
        listaCambios.getStyleClass().add("tabla-datos");
        listaCambios.setPrefHeight(110);

        VBox columnaAgregar = new VBox(8, titulo("AGREGAR TRATAMIENTO"), formulario,
                titulo("CAMBIOS PENDIENTES"), listaCambios);
        VBox.setVgrow(listaCambios, Priority.ALWAYS);
        HBox.setHgrow(columnaAgregar, Priority.ALWAYS);
        columnaAgregar.setPrefWidth(400);

        HBox centro = new HBox(14, columnaPlan, columnaAgregar);
        VBox.setVgrow(centro, Priority.ALWAYS);

        campoNombre.getStyleClass().add("campo-formulario");
        campoNombre.setPromptText("Nombre del firmante");
        campoNombre.setPrefWidth(210);
        campoDocumento.getStyleClass().add("campo-formulario");
        campoDocumento.setPromptText("Documento");
        campoDocumento.setPrefWidth(140);
        comboRol.setItems(FXCollections.observableArrayList(ROLES_FIRMANTE));
        comboRol.setValue("PACIENTE");
        comboRol.setPrefWidth(140);
        HBox lineaFirma = new HBox(8, new Label("Firma:"), campoNombre, campoDocumento, comboRol);
        lineaFirma.setAlignment(Pos.CENTER_LEFT);

        Button btnFirmar = new Button("Firmar enmienda");
        btnFirmar.getStyleClass().add("boton-primario");
        btnFirmar.setOnAction(e -> firmar());
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> stage.close());
        Label relleno = new Label(" ");
        HBox.setHgrow(relleno, Priority.ALWAYS);
        HBox lineaAcciones = new HBox(10, btnFirmar, relleno, btnCancelar);
        lineaAcciones.setAlignment(Pos.CENTER_LEFT);

        lblError.getStyleClass().add("etiqueta-error");
        lblError.setWrapText(true);
        lblError.setMinHeight(18);

        VBox raiz = new VBox(10, lblCabecera, lineaTotales, lineaMotivo, centro, lineaFirma,
                lblError, lineaAcciones);
        raiz.getStyleClass().add("tarjeta");
        raiz.setPadding(new Insets(16));

        actualizarTotales();

        Scene escena = new Scene(raiz);
        URL hoja = EnmiendaDialog.class.getResource(CSS);
        if (hoja != null) {
            escena.getStylesheets().add(hoja.toExternalForm());
        }
        stage.setScene(escena);
    }

    private void configurarTablaPlan() {
        tablaPlan.setItems(itemsPlan);
        tablaPlan.getStyleClass().add("tabla-datos");
        tablaPlan.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaPlan.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tablaPlan.setPrefHeight(190);

        TableColumn<PresupuestoItem, String> colPieza = new TableColumn<>("PIEZA");
        colPieza.setPrefWidth(60);
        colPieza.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getPiezaFdi() == null ? "" : String.valueOf(d.getValue().getPiezaFdi())));

        TableColumn<PresupuestoItem, String> colPrestacion = new TableColumn<>("PRESTACION");
        colPrestacion.setPrefWidth(190);
        colPrestacion.setCellValueFactory(d -> new ReadOnlyStringWrapper(nombre(d.getValue())));

        TableColumn<PresupuestoItem, String> colMonto = new TableColumn<>("MONTO NETO");
        colMonto.setPrefWidth(100);
        colMonto.setCellValueFactory(d -> new ReadOnlyStringWrapper(formatear(monto(d.getValue()))));

        TableColumn<PresupuestoItem, String> colEstado = new TableColumn<>("ESTADO");
        colEstado.setPrefWidth(90);
        colEstado.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getEstado() == null ? "" : d.getValue().getEstado()));

        tablaPlan.getColumns().setAll(colPieza, colPrestacion, colMonto, colEstado);
    }

    private GridPane formularioAgregar() {
        try {
            comboTratamiento.setItems(FXCollections.observableArrayList(
                    AppContext.get().catalogo().listarTratamientosActivos()));
        } catch (DentrixException e) {
            lblError.setText(e.getMessage());
        }
        comboTratamiento.setConverter(new StringConverter<Tratamiento>() {
            @Override
            public String toString(Tratamiento t) {
                return t == null ? "" : etiqueta(t);
            }

            @Override
            public Tratamiento fromString(String s) {
                return null;
            }
        });
        comboTratamiento.setPrefWidth(250);

        campoPieza.getStyleClass().add("campo-formulario");
        campoPieza.setPromptText("Opcional");
        campoPieza.setPrefWidth(70);

        comboFase.setItems(FXCollections.observableArrayList(FASES));
        comboFase.setValue("BASICA");
        comboFase.setPrefWidth(150);

        spinnerPrioridad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, 3));
        spinnerPrioridad.setPrefWidth(70);
        spinnerCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
        spinnerCantidad.setPrefWidth(70);
        spinnerSesiones.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 1));
        spinnerSesiones.setPrefWidth(70);

        Button btnAgregar = new Button("Agregar");
        btnAgregar.getStyleClass().add("boton-primario");
        btnAgregar.setOnAction(e -> agregarTratamiento());

        GridPane grilla = new GridPane();
        grilla.setHgap(8);
        grilla.setVgap(8);
        grilla.add(new Label("Tratamiento:"), 0, 0);
        grilla.add(comboTratamiento, 1, 0, 3, 1);
        grilla.add(new Label("Pieza:"), 0, 1);
        grilla.add(campoPieza, 1, 1);
        grilla.add(new Label("Fase:"), 2, 1);
        grilla.add(comboFase, 3, 1);
        grilla.add(new Label("Prioridad:"), 0, 2);
        grilla.add(spinnerPrioridad, 1, 2);
        grilla.add(new Label("Cantidad:"), 2, 2);
        grilla.add(spinnerCantidad, 3, 2);
        grilla.add(new Label("Sesiones:"), 0, 3);
        grilla.add(spinnerSesiones, 1, 3);
        grilla.add(checkConsentimiento, 2, 3, 2, 1);
        HBox pieBoton = new HBox(btnAgregar);
        pieBoton.setAlignment(Pos.CENTER_RIGHT);
        grilla.add(pieBoton, 0, 4, 4, 1);
        return grilla;
    }

    private Label titulo(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("tarjeta-etiqueta");
        return etiqueta;
    }

    private void cargarPlan() {
        try {
            List<PresupuestoItem> vigentes = new ArrayList<>();
            for (PresupuestoItem item : AppContext.get().presupuestos().items(presupuesto.getId())) {
                if (!"ANULADO".equals(item.getEstado())) {
                    vigentes.add(item);
                }
            }
            itemsPlan.setAll(vigentes);
            actualizarTotales();
        } catch (DentrixException e) {
            lblError.setText(e.getMessage());
        }
    }

    private void agregarTratamiento() {
        lblError.setText("");
        Tratamiento tratamiento = comboTratamiento.getValue();
        if (tratamiento == null) {
            lblError.setText("Selecciona el tratamiento a agregar.");
            return;
        }
        Integer pieza;
        try {
            pieza = piezaIngresada();
        } catch (DentrixException e) {
            lblError.setText(e.getMessage());
            return;
        }
        BigDecimal precio;
        try {
            precio = AppContext.get().tarifario().precioVigente(tratamiento.getId());
        } catch (DentrixException e) {
            lblError.setText(e.getMessage());
            return;
        }
        PresupuestoItem item = new PresupuestoItem();
        item.setTratamientoId(tratamiento.getId());
        item.setTratamientoNombre(tratamiento.getNombre());
        item.setPiezaFdi(pieza);
        item.setFase(comboFase.getValue());
        item.setPrioridad(spinnerPrioridad.getValue());
        item.setCantidad(spinnerCantidad.getValue());
        item.setSesionesPlanificadas(spinnerSesiones.getValue());
        item.setRequiereConsentimiento(checkConsentimiento.isSelected());
        item.setPrecioUnitario(precio);
        agregados.add(new Agregado(tratamiento, item, precio));
        int cantidad = item.getCantidad() == null ? 1 : item.getCantidad();
        cambios.add("Se agregará: " + etiqueta(tratamiento) + " x" + cantidad);
        checkConsentimiento.setSelected(false);
        campoPieza.clear();
        actualizarTotales();
    }

    private void marcarParaAnular() {
        lblError.setText("");
        List<PresupuestoItem> seleccionados = new ArrayList<>(
                tablaPlan.getSelectionModel().getSelectedItems());
        if (seleccionados.isEmpty()) {
            lblError.setText("Selecciona al menos un item del plan actual.");
            return;
        }
        String motivo = texto(campoMotivoAnulacion);
        if (motivo == null) {
            lblError.setText("Indica el motivo de la anulacion.");
            return;
        }
        for (PresupuestoItem item : seleccionados) {
            anulaciones.add(new Anulacion(item, motivo));
            itemsPlan.remove(item);
            cambios.add("Se anulará: " + nombre(item) + " (" + motivo + ")");
        }
        campoMotivoAnulacion.clear();
        nuevaSeleccion();
        actualizarTotales();
    }

    private void nuevaSeleccion() {
        tablaPlan.getSelectionModel().clearSelection();
    }

    private void actualizarTotales() {
        BigDecimal total = presupuesto.getTotal() == null ? BigDecimal.ZERO : presupuesto.getTotal();
        for (Anulacion anulacion : anulaciones) {
            total = total.subtract(monto(anulacion.item()));
        }
        for (Agregado agregado : agregados) {
            int cantidad = agregado.item().getCantidad() == null ? 1 : agregado.item().getCantidad();
            total = total.add(agregado.precio().multiply(BigDecimal.valueOf(cantidad)));
        }
        lblTotalActual.setText("Total actual: " + formatear(presupuesto.getTotal()));
        lblNuevoTotal.setText("Nuevo total estimado: " + formatear(total));
    }

    private void firmar() {
        lblError.setText("");
        String motivo = texto(campoMotivo);
        String nombre = texto(campoNombre);
        String documento = texto(campoDocumento);
        String rol = comboRol.getValue();
        Long enmiendaId = null;
        try {
            if (motivo == null) {
                throw new DentrixException("El motivo de la enmienda es obligatorio.");
            }
            if (anulaciones.isEmpty() && agregados.isEmpty()) {
                throw new DentrixException("La enmienda debe tener al menos un cambio: anular o agregar.");
            }
            if (nombre == null) {
                throw new DentrixException("El nombre del firmante es obligatorio.");
            }
            if (documento == null) {
                throw new DentrixException("El documento del firmante es obligatorio.");
            }
            if (rol == null) {
                throw new DentrixException("Selecciona el rol del firmante.");
            }
            Integer usuario = usuarioId();
            Enmienda enmienda = AppContext.get().enmiendas().abrir(presupuesto.getId(), motivo, usuario);
            enmiendaId = enmienda.getId();
            for (Agregado agregado : agregados) {
                AppContext.get().enmiendas().agregarItem(enmiendaId, agregado.item());
            }
            for (Anulacion anulacion : anulaciones) {
                AppContext.get().enmiendas().marcarAnulacion(enmiendaId, anulacion.item().getId(),
                        anulacion.motivo());
            }
            AppContext.get().enmiendas().firmar(enmiendaId, nombre, documento, rol, usuario);
            firmado = true;
            stage.close();
        } catch (DentrixException e) {
            descartarEnSilencio(enmiendaId);
            lblError.setText(e.getMessage());
        } catch (RuntimeException e) {
            descartarEnSilencio(enmiendaId);
            lblError.setText(TareasFX.mensaje(e));
        }
    }

    private void descartarEnSilencio(Long enmiendaId) {
        if (enmiendaId == null) {
            return;
        }
        try {
            AppContext.get().enmiendas().descartar(enmiendaId);
        } catch (DentrixException ignorada) {
            return;
        }
    }

    private Integer usuarioId() throws DentrixException {
        if (AppContext.get().sesion().getUsuario() == null
                || AppContext.get().sesion().getUsuario().getId() == null) {
            throw new DentrixException("No hay un usuario en sesion para registrar la enmienda.");
        }
        return AppContext.get().sesion().getUsuario().getId();
    }

    private Integer piezaIngresada() throws DentrixException {
        String valor = texto(campoPieza);
        if (valor == null) {
            return null;
        }
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException e) {
            throw new DentrixException("La pieza FDI debe ser un numero (ej. 21, 48).");
        }
    }

    private BigDecimal monto(PresupuestoItem item) {
        BigDecimal monto = item.getMontoNeto() == null ? item.getSubtotal() : item.getMontoNeto();
        return monto == null ? BigDecimal.ZERO : monto;
    }

    private String nombre(PresupuestoItem item) {
        return item.getTratamientoNombre() == null
                ? "Item " + item.getId() : item.getTratamientoNombre();
    }

    private String etiqueta(Tratamiento tratamiento) {
        if (tratamiento == null) {
            return "";
        }
        return tratamiento.getNombre() == null
                ? "Tratamiento " + tratamiento.getId() : tratamiento.getNombre();
    }

    private String textoPlano(String valor) {
        return valor == null ? "" : valor;
    }

    private String formatear(BigDecimal monto) {
        BigDecimal valor = monto == null ? BigDecimal.ZERO : monto;
        return "S/ " + valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String texto(TextField campo) {
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        return valor.isEmpty() ? null : valor;
    }
}
