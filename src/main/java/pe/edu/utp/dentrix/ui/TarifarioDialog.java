package pe.edu.utp.dentrix.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
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
import pe.edu.utp.dentrix.model.Tarifario;
import pe.edu.utp.dentrix.model.TarifarioItem;
import pe.edu.utp.dentrix.model.Tratamiento;
import pe.edu.utp.dentrix.service.TarifarioService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TarifarioDialog {

    private static final String CSS = "/pe/edu/utp/dentrix/estilos.css";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Stage stage = new Stage();
    private final Label cabecera = new Label("Cargando tarifario...");
    private final Label estado = new Label("");
    private final Label error = new Label("");
    private final DatePicker fecha = new DatePicker(LocalDate.now());
    private final TextField motivo = new TextField();
    private final Button btnAbrir = new Button("Abrir versión nueva");
    private final Button btnPublicar = new Button("Publicar");
    private final Button btnCerrar = new Button("Cerrar");
    private final TableView<TarifarioItem> tabla = new TableView<>();
    private final ObservableList<TarifarioItem> filas = FXCollections.observableArrayList();
    private final Map<Integer, BigDecimal> preciosVigentes = new HashMap<>();
    private final TableColumn<TarifarioItem, BigDecimal> colNuevo =
            new TableColumn<>("NUEVO PRECIO");
    private Long borradorId;
    private Integer versionBorrador;

    private TarifarioDialog() {
    }

    public static void mostrar(Window duenio) {
        TarifarioDialog dialogo = new TarifarioDialog();
        dialogo.construir(duenio);
        dialogo.cargar();
        dialogo.stage.showAndWait();
    }

    private void construir(Window duenio) {
        stage.initOwner(duenio);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Tarifario · versión con vigencia");
        stage.setWidth(820);
        stage.setHeight(560);

        cabecera.getStyleClass().add("tarjeta-titulo");
        estado.getStyleClass().add("tarjeta-etiqueta");
        error.getStyleClass().add("etiqueta-error");
        error.setWrapText(true);
        error.setMinHeight(18);

        fecha.setPrefWidth(140);
        motivo.setPromptText("Motivo del cambio (ej. actualización de tarifas)");
        motivo.setPrefWidth(260);
        motivo.getStyleClass().add("campo-formulario");

        btnAbrir.getStyleClass().add("boton-primario");
        btnAbrir.setOnAction(e -> abrirVersion());
        btnPublicar.setOnAction(e -> publicar());
        btnPublicar.setDisable(true);
        btnCerrar.setOnAction(e -> stage.close());

        HBox controles = new HBox(8, new Label("Vigencia desde:"), fecha, motivo, btnAbrir,
                btnPublicar, btnCerrar);
        controles.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(motivo, Priority.ALWAYS);

        configurarTabla();

        VBox raiz = new VBox(10, cabecera, controles, estado, error, tabla);
        raiz.getStyleClass().add("tarjeta");
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Scene escena = new Scene(raiz);
        URL hoja = TarifarioDialog.class.getResource(CSS);
        if (hoja != null) {
            escena.getStylesheets().add(hoja.toExternalForm());
        }
        stage.setScene(escena);
    }

    private void configurarTabla() {
        tabla.setItems(filas);
        tabla.setEditable(true);
        tabla.getStyleClass().add("tabla-datos");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<TarifarioItem, String> colTratamiento = new TableColumn<>("TRATAMIENTO");
        colTratamiento.setPrefWidth(380);
        colTratamiento.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getTratamientoNombre() == null ? "" : d.getValue().getTratamientoNombre()));

        TableColumn<TarifarioItem, String> colVigente = new TableColumn<>("PRECIO VIGENTE");
        colVigente.setPrefWidth(140);
        colVigente.setEditable(false);
        colVigente.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                formatearPrecio(preciosVigentes.get(d.getValue().getTratamientoId()))));

        colNuevo.setPrefWidth(160);
        colNuevo.setCellValueFactory(
                d -> new ReadOnlyObjectWrapper<>(d.getValue().getPrecio()));
        StringConverter<BigDecimal> convertidor = convertidorPrecio();
        colNuevo.setCellFactory(columna -> new TextFieldTableCell<>(convertidor) {
            private boolean escuchaFoco;

            @Override
            public void startEdit() {
                super.startEdit();
                if (!escuchaFoco && getGraphic() instanceof TextField campo) {
                    escuchaFoco = true;
                    campo.focusedProperty().addListener((obs, antes, ahora) -> {
                        if (!ahora && isEditing()) {
                            commitEdit(convertidor.fromString(campo.getText()));
                        }
                    });
                }
            }
        });
        colNuevo.setOnEditCommit(evento -> {
            evento.getRowValue().setPrecio(evento.getNewValue());
            tabla.refresh();
        });
        colNuevo.setEditable(false);

        tabla.getColumns().setAll(colTratamiento, colVigente, colNuevo);
    }

    private void cargar() {
        try {
            preciosVigentes.clear();
            preciosVigentes.putAll(servicio().preciosVigentes());
            cabecera.setText(servicio().versionPublicada()
                    .map(this::describirPublicada)
                    .orElse("No hay versión publicada"));
            cargarFilas(null);
        } catch (DentrixException e) {
            cabecera.setText("No se pudo leer el tarifario");
            error.setText(e.getMessage());
        }
    }

    private void cargarFilas(Map<Integer, BigDecimal> preciosBorrador) throws DentrixException {
        List<TarifarioItem> nuevos = new ArrayList<>();
        for (Tratamiento tratamiento : AppContext.get().catalogo().listarTratamientosActivos()) {
            TarifarioItem item = new TarifarioItem();
            item.setTratamientoId(tratamiento.getId());
            item.setTratamientoNombre(tratamiento.getNombre());
            BigDecimal precio = null;
            if (preciosBorrador != null) {
                precio = preciosBorrador.get(tratamiento.getId());
                if (precio == null) {
                    precio = preciosVigentes.get(tratamiento.getId());
                }
                if (precio == null) {
                    precio = tratamiento.getPrecio();
                }
            }
            item.setPrecio(precio);
            nuevos.add(item);
        }
        filas.setAll(nuevos);
        tabla.refresh();
    }

    private void abrirVersion() {
        error.setText("");
        String textoMotivo = motivo.getText() == null ? "" : motivo.getText().trim();
        if (textoMotivo.isEmpty()) {
            error.setText("Indica el motivo de la nueva versión del tarifario.");
            return;
        }
        if (fecha.getValue() == null) {
            error.setText("Selecciona la fecha de vigencia inicial.");
            return;
        }
        try {
            Tarifario borrador = servicio().abrirVersion(fecha.getValue(), textoMotivo, usuarioId());
            borradorId = borrador.getId();
            versionBorrador = borrador.getVersion();
            Map<Integer, BigDecimal> preciosBorrador = new HashMap<>();
            for (TarifarioItem item : servicio().items(borradorId)) {
                preciosBorrador.put(item.getTratamientoId(), item.getPrecio());
            }
            cargarFilas(preciosBorrador);
            colNuevo.setEditable(true);
            btnPublicar.setDisable(false);
            btnAbrir.setDisable(true);
            estado.setText("Borrador v" + versionBorrador + " abierto");
        } catch (DentrixException e) {
            error.setText(e.getMessage());
        }
    }

    private void publicar() {
        error.setText("");
        if (borradorId == null) {
            error.setText("Abre una versión nueva antes de publicar.");
            return;
        }
        if (filas.isEmpty()) {
            error.setText("No hay tratamientos activos para tarifar.");
            return;
        }
        for (TarifarioItem item : filas) {
            if (item.getPrecio() == null) {
                error.setText("Falta el precio de " + item.getTratamientoNombre());
                return;
            }
            if (item.getPrecio().signum() < 0) {
                error.setText("El precio de " + item.getTratamientoNombre() + " no puede ser negativo.");
                return;
            }
        }
        try {
            for (TarifarioItem item : filas) {
                servicio().fijarPrecio(borradorId, item.getTratamientoId(), item.getPrecio());
            }
            servicio().publicar(borradorId, usuarioId());
            stage.close();
        } catch (DentrixException e) {
            error.setText(e.getMessage());
        }
    }

    private String describirPublicada(Tarifario tarifario) {
        String desde = tarifario.getVigenciaDesde() == null
                ? "—"
                : tarifario.getVigenciaDesde().format(FORMATO_FECHA);
        return "Vigente: v" + tarifario.getVersion() + " desde " + desde;
    }

    private String formatearPrecio(BigDecimal precio) {
        return precio == null
                ? "—"
                : "S/ " + precio.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private StringConverter<BigDecimal> convertidorPrecio() {
        return new StringConverter<>() {
            @Override
            public String toString(BigDecimal valor) {
                return valor == null ? "" : formatearPrecio(valor);
            }

            @Override
            public BigDecimal fromString(String texto) {
                if (texto == null) {
                    return null;
                }
                String limpio = texto.replace("S/", "").replace("s/", "").replace(",", ".").trim();
                if (limpio.isEmpty()) {
                    return null;
                }
                try {
                    return new BigDecimal(limpio);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        };
    }

    private int usuarioId() {
        SesionActual sesion = AppContext.get().sesion();
        if (sesion.activa() && sesion.getUsuario() != null && sesion.getUsuario().getId() != null) {
            return sesion.getUsuario().getId();
        }
        return 1;
    }

    private TarifarioService servicio() {
        return AppContext.get().tarifario();
    }
}
