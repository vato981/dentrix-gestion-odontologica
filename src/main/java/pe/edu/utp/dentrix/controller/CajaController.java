package pe.edu.utp.dentrix.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.dto.LineaBoleta;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.CierreCaja;
import pe.edu.utp.dentrix.model.Paciente;
import pe.edu.utp.dentrix.model.Pago;
import pe.edu.utp.dentrix.model.Tratamiento;
import pe.edu.utp.dentrix.ui.CobroPlanDialog;
import pe.edu.utp.dentrix.ui.TareasFX;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CajaController {

    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final List<String> MODALIDADES = List.of("EFECTIVO", "YAPE", "PLIN", "TARJETA");
    private static final List<String> TURNOS = List.of("DIURNO", "NOCTURNO");

    public record LineaGrilla(Integer tratamientoId, String codigo, String nombre, Integer pieza,
                              Integer cantidad, BigDecimal precio, BigDecimal subtotal) {
    }

    @FXML
    private Label etiquetaCajero;

    @FXML
    private ComboBox<Paciente> comboPaciente;

    @FXML
    private Label etiquetaPaciente;

    @FXML
    private Label lblTotal;

    @FXML
    private ComboBox<Tratamiento> comboTratamiento;

    @FXML
    private TextField campoPieza;

    @FXML
    private TextField campoCantidad;

    @FXML
    private TableView<LineaGrilla> tablaLineas;

    @FXML
    private TableColumn<LineaGrilla, String> colTratamiento;

    @FXML
    private TableColumn<LineaGrilla, String> colPieza;

    @FXML
    private TableColumn<LineaGrilla, String> colCantidad;

    @FXML
    private TableColumn<LineaGrilla, String> colPrecio;

    @FXML
    private TableColumn<LineaGrilla, String> colSubtotal;

    @FXML
    private ComboBox<String> comboModalidad;

    @FXML
    private TextField campoOperacion;

    @FXML
    private Label etiquetaResultado;

    @FXML
    private TableView<Pago> tablaBoletas;

    @FXML
    private TableColumn<Pago, String> colBNumero;

    @FXML
    private TableColumn<Pago, String> colBPaciente;

    @FXML
    private TableColumn<Pago, String> colBFecha;

    @FXML
    private TableColumn<Pago, String> colBMonto;

    @FXML
    private TableColumn<Pago, String> colBModalidad;

    @FXML
    private TableColumn<Pago, String> colBEstado;

    @FXML
    private TableColumn<Pago, String> colBSaldo;

    @FXML
    private Button btnAnularBoleta;

    @FXML
    private Button btnVerBoleta;

    @FXML
    private Label etiquetaFechaHoy;

    @FXML
    private Button btnCobrarPlan;

    @FXML
    private TextField campoNumeroBoleta;

    @FXML
    private Label etiquetaDocumento;

    @FXML
    private TableView<Pago> tablaAbonos;

    @FXML
    private TableColumn<Pago, String> colAFecha;

    @FXML
    private TableColumn<Pago, String> colAMonto;

    @FXML
    private TableColumn<Pago, String> colAModalidad;

    @FXML
    private TableColumn<Pago, String> colAOp;

    @FXML
    private TextField campoMontoAbono;

    @FXML
    private ComboBox<String> comboModalidadAbono;

    @FXML
    private TextField campoOpAbono;

    @FXML
    private Label etiquetaAbonoResultado;

    @FXML
    private ComboBox<String> comboTurno;

    @FXML
    private TextField campoFondo;

    @FXML
    private Label etiquetaSinCierre;

    @FXML
    private Label etiquetaCierreInfo;

    @FXML
    private TextField campoNuevoFondo;

    @FXML
    private TextField campoContado;

    @FXML
    private Label etiquetaCierreResultado;

    @FXML
    private Label lblEstadoTurno;

    @FXML
    private Label lblCierreFecha;

    @FXML
    private Label lblFondoInicial;

    @FXML
    private Label lblEfectivoSistema;

    @FXML
    private Label lblBilleterasSistema;

    @FXML
    private Label lblTotalSistema;

    @FXML
    private Label lblEfectivoContado;

    @FXML
    private Label lblBilleterasContado;

    @FXML
    private Label lblDiferencia;

    @FXML
    private Label lblDiferenciaDetalle;

    @FXML
    private Label lblCierreNumero;

    @FXML
    private Label lblActaEstado;

    @FXML
    private HBox panelCaja;

    @FXML
    private ScrollPane panelCierre;

    private final ObservableList<LineaGrilla> lineas = FXCollections.observableArrayList();
    private final List<Paciente> pacientesActivos = new ArrayList<>();
    private final List<Tratamiento> tratamientosActivos = new ArrayList<>();
    private final Map<Integer, String> nombresPacientes = new HashMap<>();
    private Pago documentoAbonado;
    private CierreCaja cierreActual;
    private Integer usuarioCajaId;

    @FXML
    private void initialize() {
        colTratamiento.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().codigo() + " " + d.getValue().nombre()));
        colPieza.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().pieza() == null ? "" : String.valueOf(d.getValue().pieza())));
        colCantidad.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().cantidad())));
        colPrecio.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().precio().toPlainString()));
        colSubtotal.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().subtotal().toPlainString()));
        tablaLineas.setItems(lineas);

        colBNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colBPaciente.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPacienteId() == null ? ""
                        : nombresPacientes.getOrDefault(d.getValue().getPacienteId(),
                                "Paciente " + d.getValue().getPacienteId())));
        colBFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() == null ? "" : FECHA_CORTA.format(d.getValue().getFecha())));
        colBMonto.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getMonto() == null ? "" : d.getValue().getMonto().toPlainString()));
        colBModalidad.setCellValueFactory(new PropertyValueFactory<>("modalidad"));
        colBEstado.setCellValueFactory(new PropertyValueFactory<>("estadoPago"));
        pe.edu.utp.dentrix.ui.Badges.aplicarEn(colBEstado);
        colBSaldo.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getSaldoDeudor() == null ? "" : d.getValue().getSaldoDeudor().toPlainString()));

        btnAnularBoleta.setDisable(true);
        btnVerBoleta.setDisable(true);
        tablaBoletas.getSelectionModel().selectedItemProperty().addListener((obs, antes, ahora) -> {
            btnAnularBoleta.setDisable(ahora == null || !"EMITIDO".equals(ahora.getEstado()));
            btnVerBoleta.setDisable(ahora == null);
        });

        colAFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() == null ? "" : FECHA_CORTA.format(d.getValue().getFecha())));
        colAMonto.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getMonto() == null ? "" : d.getValue().getMonto().toPlainString()));
        colAModalidad.setCellValueFactory(new PropertyValueFactory<>("modalidad"));
        colAOp.setCellValueFactory(new PropertyValueFactory<>("operacionRef"));

        comboModalidad.setItems(FXCollections.observableArrayList(MODALIDADES));
        comboModalidadAbono.setItems(FXCollections.observableArrayList(MODALIDADES));
        comboTurno.setItems(FXCollections.observableArrayList(TURNOS));

        comboPaciente.setConverter(new StringConverter<Paciente>() {
            @Override
            public String toString(Paciente p) {
                return p == null ? "" : p.getApellidos() + ", " + p.getNombres() + " (DNI " + p.getDni() + ")";
            }

            @Override
            public Paciente fromString(String s) {
                return null;
            }
        });
        comboPaciente.valueProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null) {
                etiquetaPaciente.setText("Selecciona el paciente del cobro");
            } else {
                etiquetaPaciente.setText(ahora.getApellidos() + ", " + ahora.getNombres()
                        + " - DNI " + ahora.getDni() + " - HC " + ahora.getCodigoHc());
            }
        });
        comboTratamiento.setConverter(new StringConverter<Tratamiento>() {
            @Override
            public String toString(Tratamiento t) {
                return t == null ? "" : t.getCodigo() + " - " + t.getNombre() + " S/ " + t.getPrecio().toPlainString();
            }

            @Override
            public Tratamiento fromString(String s) {
                return null;
            }
        });

        lineas.addListener((javafx.collections.ListChangeListener<LineaGrilla>) c -> actualizarTotal());

        AppContext contexto = AppContext.get();
        if (!contexto.sesion().activa()) {
            return;
        }
        usuarioCajaId = contexto.sesion().getUsuario().getId();
        etiquetaCajero.setText("Cajero: " + contexto.sesion().getNombreCompleto());
        comboModalidad.setValue("EFECTIVO");
        comboModalidadAbono.setValue("EFECTIVO");
        comboTurno.setValue("DIURNO");
        etiquetaFechaHoy.setText("Hoy: " + LocalDate.now());
        TareasFX.ejecutar(this::cargarCombos,
                this::aplicarCombos,
                error -> mostrarError(TareasFX.mensaje(error)));
        recargarCierre();
    }

    private record DatosCombos(List<Paciente> pacientesActivos, List<Tratamiento> tratamientosActivos,
                               Map<Integer, String> nombresPaciente) {
    }

    private DatosCombos cargarCombos() throws DentrixException {
        AppContext contexto = AppContext.get();
        List<Paciente> activos = new ArrayList<>();
        Map<Integer, String> nombres = new HashMap<>();
        for (Paciente p : contexto.pacientes().listar()) {
            nombres.put(p.getId(), p.getCodigoHc() + " - " + p.getApellidos() + ", " + p.getNombres());
            if ("ACTIVO".equals(p.getEstado())) {
                activos.add(p);
            }
        }
        return new DatosCombos(activos, contexto.catalogo().listarTratamientosActivos(), nombres);
    }

    private void aplicarCombos(DatosCombos datos) {
        pacientesActivos.clear();
        pacientesActivos.addAll(datos.pacientesActivos());
        tratamientosActivos.clear();
        tratamientosActivos.addAll(datos.tratamientosActivos());
        nombresPacientes.clear();
        nombresPacientes.putAll(datos.nombresPaciente());
        comboPaciente.setItems(FXCollections.observableArrayList(pacientesActivos));
        comboTratamiento.setItems(FXCollections.observableArrayList(tratamientosActivos));
        recargarBoletas();
    }

    private void actualizarTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (LineaGrilla linea : lineas) {
            total = total.add(linea.subtotal());
        }
        lblTotal.setText("S/ " + total.toPlainString());
    }

    @FXML
    private void onAgregarLineaClick() {
        try {
            Tratamiento tratamiento = comboTratamiento.getValue();
            if (tratamiento == null) {
                throw new DentrixException("Selecciona un tratamiento del tarifario");
            }
            String textoCantidad = campoCantidad.getText() == null ? "" : campoCantidad.getText().trim();
            int cantidad = textoCantidad.isEmpty() ? 1 : Integer.parseInt(textoCantidad);
            if (cantidad < 1) {
                throw new DentrixException("La cantidad debe ser al menos 1");
            }
            Integer pieza = null;
            String textoPieza = campoPieza.getText() == null ? "" : campoPieza.getText().trim();
            if (!textoPieza.isEmpty()) {
                if (!textoPieza.matches("\\d{1,2}")) {
                    throw new DentrixException("La pieza FDI debe ser un número de 1 o 2 dígitos (ej. 21, 48)");
                }
                pieza = Integer.parseInt(textoPieza);
            }
            BigDecimal precio = tratamiento.getPrecio();
            lineas.add(new LineaGrilla(tratamiento.getId(), tratamiento.getCodigo(), tratamiento.getNombre(),
                    pieza, cantidad, precio, precio.multiply(BigDecimal.valueOf(cantidad))));
            campoPieza.clear();
            campoCantidad.setText("1");
            etiquetaResultado.setText("");
        } catch (NumberFormatException e) {
            mostrarError("La cantidad debe ser un número entero");
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onQuitarLineaClick() {
        LineaGrilla seleccionada = tablaLineas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarInfo("Selecciona una línea de la tabla para quitarla.");
            return;
        }
        lineas.remove(seleccionada);
    }

    @FXML
    private void onEmitirClick() {
        try {
            Paciente paciente = comboPaciente.getValue();
            if (paciente == null) {
                throw new DentrixException("Selecciona el paciente de la boleta");
            }
            if (lineas.isEmpty()) {
                throw new DentrixException("Agrega al menos un tratamiento antes de emitir");
            }
            String modalidad = comboModalidad.getValue();
            String operacion = texto(campoOperacion);
            if (!"EFECTIVO".equals(modalidad) && operacion == null) {
                throw new DentrixException("RF-08: Yape/Plin/Tarjeta requieren N° de operación");
            }
            List<LineaBoleta> detalle = new ArrayList<>();
            for (LineaGrilla g : lineas) {
                detalle.add(LineaBoleta.directa(g.tratamientoId(), g.pieza(), g.cantidad()));
            }
            Integer pacienteId = paciente.getId();
            TareasFX.ejecutar(() -> AppContext.get().caja().emitirBoleta(pacienteId, modalidad, operacion,
                            null, null, usuarioCajaId, null, detalle),
                    boleta -> {
                        lineas.clear();
                        aplicarBoletaEmitida(boleta);
                    },
                    error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onCobrarPlanClick() {
        try {
            Paciente paciente = comboPaciente.getValue();
            if (paciente == null) {
                throw new DentrixException("Selecciona el paciente de la boleta");
            }
            CobroPlanDialog.mostrar(ventana(), paciente, this::aplicarBoletaEmitida);
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void aplicarBoletaEmitida(Pago boleta) {
        indicar(etiquetaResultado, "Boleta " + boleta.getNumero() + " emitida: S/ "
                + boleta.getMonto().toPlainString()
                + " (saldo S/ " + boleta.getSaldoDeudor().toPlainString() + ")", false);
        campoOperacion.clear();
        recargarBoletas();
    }

    private Window ventana() {
        return comboPaciente.getScene() == null ? null : comboPaciente.getScene().getWindow();
    }

    @FXML
    private void onRefrescarBoletasClick() {
        recargarBoletas();
    }

    @FXML
    private void onVerBoletaClick() {
        Pago seleccionada = tablaBoletas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarInfo("Selecciona un comprobante para ver sus abonos.");
            return;
        }
        campoNumeroBoleta.setText(seleccionada.getNumero());
        onCargarBoletaClick();
    }

    @FXML
    private void onAnularBoletaClick() {
        Pago seleccionada = tablaBoletas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarInfo("Selecciona una boleta para anular.");
            return;
        }
        if (!"EMITIDO".equals(seleccionada.getEstado())) {
            mostrarInfo("Solo se pueden anular boletas en estado EMITIDO.");
            return;
        }
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Anular boleta");
        dialogo.setHeaderText("Anular la boleta " + seleccionada.getNumero());
        dialogo.setContentText("Motivo:");
        Optional<String> motivo = dialogo.showAndWait();
        if (motivo.isEmpty() || motivo.get().isBlank()) {
            return;
        }
        Long id = seleccionada.getId();
        String numero = seleccionada.getNumero();
        TareasFX.ejecutar(() -> {
            AppContext.get().caja().anular(id, motivo.get().trim(), usuarioCajaId);
            return null;
        }, ignorado -> {
            indicar(etiquetaResultado, "Boleta " + numero + " anulada", false);
            recargarBoletas();
        }, error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void recargarBoletas() {
        TareasFX.ejecutar(() -> AppContext.get().caja().boletasDelDia(LocalDate.now()),
                boletas -> tablaBoletas.setItems(FXCollections.observableArrayList(boletas)),
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onCargarBoletaClick() {
        String numero;
        try {
            numero = texto(campoNumeroBoleta);
            if (numero == null) {
                throw new DentrixException("Ingresa el número de la boleta (ej. B001-000440)");
            }
        } catch (DentrixException e) {
            documentoAbonado = null;
            etiquetaDocumento.setText(e.getMessage());
            tablaAbonos.setItems(FXCollections.observableArrayList());
            return;
        }
        TareasFX.ejecutar(() -> {
            var caja = AppContext.get().caja();
            Pago boleta = caja.buscarBoleta(numero);
            return new DocumentoAbonado(boleta, caja.abonosDe(boleta.getId()));
        }, datos -> {
            documentoAbonado = datos.boleta();
            mostrarDocumentoAbonado();
            tablaAbonos.setItems(FXCollections.observableArrayList(datos.abonos()));
            etiquetaAbonoResultado.setText("");
        }, error -> {
            documentoAbonado = null;
            etiquetaDocumento.setText(TareasFX.mensaje(error));
            tablaAbonos.setItems(FXCollections.observableArrayList());
        });
    }

    private record DocumentoAbonado(Pago boleta, List<Pago> abonos) {
    }

    private void mostrarDocumentoAbonado() {
        etiquetaDocumento.setText("Boleta " + documentoAbonado.getNumero()
                + "  |  Monto S/ " + documentoAbonado.getMonto().toPlainString()
                + "  |  Saldo S/ " + documentoAbonado.getSaldoDeudor().toPlainString()
                + "  |  Estado pago: " + documentoAbonado.getEstadoPago());
    }

    @FXML
    private void onRegistrarAbonoClick() {
        try {
            if (documentoAbonado == null) {
                throw new DentrixException("Carga primero la boleta a la que abonar");
            }
            BigDecimal monto;
            try {
                monto = new BigDecimal(texto(campoMontoAbono) == null ? "" : texto(campoMontoAbono));
            } catch (NumberFormatException e) {
                throw new DentrixException("El monto del abono debe ser un número (ej. 150.00)");
            }
            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                throw new DentrixException("El monto del abono debe ser mayor a 0");
            }
            String modalidad = comboModalidadAbono.getValue();
            String operacion = texto(campoOpAbono);
            if (!"EFECTIVO".equals(modalidad) && operacion == null) {
                throw new DentrixException("RF-08: Yape/Plin/Tarjeta requieren N° de operación");
            }
            Long documentoId = documentoAbonado.getId();
            String numeroBoleta = documentoAbonado.getNumero();
            TareasFX.ejecutar(() -> {
                var caja = AppContext.get().caja();
                caja.registrarAbono(documentoId, monto, modalidad, operacion, usuarioCajaId);
                Pago actualizado = caja.buscarBoleta(numeroBoleta);
                return new DocumentoAbonado(actualizado, caja.abonosDe(actualizado.getId()));
            }, datos -> {
                documentoAbonado = datos.boleta();
                mostrarDocumentoAbonado();
                tablaAbonos.setItems(FXCollections.observableArrayList(datos.abonos()));
                campoMontoAbono.clear();
                campoOpAbono.clear();
                indicar(etiquetaAbonoResultado, "Abono registrado correctamente", false);
                recargarBoletas();
            }, error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onAbrirCierreClick() {
        try {
            BigDecimal fondo;
            try {
                fondo = new BigDecimal(texto(campoFondo) == null ? "" : texto(campoFondo));
            } catch (NumberFormatException e) {
                throw new DentrixException("El fondo inicial debe ser un número (ej. 100.00)");
            }
            if (fondo.compareTo(BigDecimal.ZERO) < 0) {
                throw new DentrixException("El fondo inicial no puede ser negativo");
            }
            LocalDate hoy = LocalDate.now();
            String turno = comboTurno.getValue();
            TareasFX.ejecutar(() -> AppContext.get().caja().abrirCierre(hoy, turno, usuarioCajaId, fondo),
                    cierre -> {
                        cierreActual = cierre;
                        campoFondo.clear();
                        indicar(etiquetaCierreResultado, "Cierre de turno " + cierre.getTurno() + " abierto", false);
                        recargarCierre();
                    },
                    error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onCambiarFondoClick() {
        try {
            if (cierreActual == null || !"ABIERTO".equals(cierreActual.getEstado())) {
                throw new DentrixException("Solo se cambia el fondo de un cierre en estado ABIERTO");
            }
            BigDecimal fondo;
            try {
                fondo = new BigDecimal(texto(campoNuevoFondo) == null ? "" : texto(campoNuevoFondo));
            } catch (NumberFormatException e) {
                throw new DentrixException("El nuevo fondo debe ser un número (ej. 150.00)");
            }
            if (fondo.compareTo(BigDecimal.ZERO) < 0) {
                throw new DentrixException("El fondo inicial no puede ser negativo");
            }
            Long cierreId = cierreActual.getId();
            TareasFX.ejecutar(() -> AppContext.get().caja().cambiarFondo(cierreId, fondo),
                    cierre -> {
                        cierreActual = cierre;
                        campoNuevoFondo.clear();
                        indicar(etiquetaCierreResultado, "Fondo inicial actualizado a S/ " + fondo.toPlainString(), false);
                        recargarCierre();
                    },
                    error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onConsolidarClick() {
        try {
            if (cierreActual == null || !"ABIERTO".equals(cierreActual.getEstado())) {
                throw new DentrixException("Solo se consolida un cierre en estado ABIERTO");
            }
            BigDecimal contado;
            try {
                contado = new BigDecimal(texto(campoContado) == null ? "" : texto(campoContado));
            } catch (NumberFormatException e) {
                throw new DentrixException("El efectivo contado debe ser un número (ej. 350.00)");
            }
            Long cierreId = cierreActual.getId();
            TareasFX.ejecutar(() -> AppContext.get().caja().consolidar(cierreId, contado, usuarioCajaId),
                    cierre -> {
                        cierreActual = cierre;
                        campoContado.clear();
                        indicar(etiquetaCierreResultado, "Cierre consolidado con diferencia S/ "
                                + cierre.getDiferencia().toPlainString(), false);
                        recargarCierre();
                    },
                    error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onActaClick() {
        try {
            if (cierreActual == null) {
                throw new DentrixException("No hay cierre del día para marcar el acta");
            }
            Long cierreId = cierreActual.getId();
            LocalDate hoy = LocalDate.now();
            TareasFX.ejecutar(() -> {
                var caja = AppContext.get().caja();
                caja.marcarActaGenerada(cierreId);
                return caja.buscarCierre(hoy);
            }, cierre -> {
                cierreActual = cierre;
                indicar(etiquetaCierreResultado, "Acta de arqueo marcada como generada", false);
                recargarCierre();
            }, error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void recargarCierre() {
        TareasFX.ejecutar(this::buscarCierreDelDia,
                this::aplicarCierre,
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private CierreCaja buscarCierreDelDia() {
        try {
            return AppContext.get().caja().buscarCierre(LocalDate.now());
        } catch (DentrixException e) {
            return null;
        }
    }

    private void aplicarCierre(CierreCaja cierre) {
        cierreActual = cierre;
        if (cierreActual == null) {
            etiquetaSinCierre.setText("Todavía no se abrió el cierre del día. Abre el turno con su fondo inicial.");
            etiquetaCierreInfo.setText("");
            aplicarBadge(lblEstadoTurno, "SIN CIERRE", "badge-gris");
            lblCierreFecha.setText("");
            lblFondoInicial.setText("S/ 0.00");
            lblEfectivoSistema.setText("S/ 0.00");
            lblBilleterasSistema.setText("S/ 0.00");
            lblTotalSistema.setText("S/ 0.00");
            lblEfectivoContado.setText("—");
            lblBilleterasContado.setText("—");
            ponerDiferencia(lblDiferencia, null);
            ponerDiferencia(lblDiferenciaDetalle, null);
            lblCierreNumero.setText("Sin cierre");
            aplicarBadge(lblActaEstado, "PENDIENTE", "badge-gris");
            return;
        }
        etiquetaSinCierre.setText("");
        aplicarBadge(lblEstadoTurno, cierreActual.getEstado(),
                "CONSOLIDADO".equals(cierreActual.getEstado()) ? "badge-verde" : "badge-azul");
        lblCierreFecha.setText("Fecha: " + cierreActual.getFecha() + " - Turno: " + cierreActual.getTurno());
        lblFondoInicial.setText(moneda(cierreActual.getFondoInicial()));
        lblEfectivoSistema.setText(moneda(cierreActual.getEfectivoSistema()));
        lblBilleterasSistema.setText(moneda(cierreActual.getBilleterasSistema()));
        lblTotalSistema.setText(moneda(cierreActual.getTotalSistema()));
        lblEfectivoContado.setText(moneda(cierreActual.getEfectivoContado()));
        lblBilleterasContado.setText(moneda(cierreActual.getBilleterasSistema()));
        ponerDiferencia(lblDiferencia, cierreActual.getDiferencia());
        ponerDiferencia(lblDiferenciaDetalle, cierreActual.getDiferencia());
        lblCierreNumero.setText(cierreActual.getId() == null ? "Sin cierre" : "N° " + cierreActual.getId());
        aplicarBadge(lblActaEstado, cierreActual.isActaGenerada() ? "GENERADA" : "PENDIENTE",
                cierreActual.isActaGenerada() ? "badge-verde" : "badge-gris");
        StringBuilder info = new StringBuilder();
        info.append("Fecha: ").append(cierreActual.getFecha())
                .append("   Turno: ").append(cierreActual.getTurno())
                .append("   Estado: ").append(cierreActual.getEstado()).append('\n');
        info.append("Fondo inicial: S/ ").append(cierreActual.getFondoInicial().toPlainString()).append('\n');
        info.append("Historial de fondos: ").append(cierreActual.getHistorialFondos()).append('\n');
        info.append("Efectivo en sistema: S/ ").append(cierreActual.getEfectivoSistema().toPlainString()).append('\n');
        info.append("Billeteras digitales: S/ ").append(cierreActual.getBilleterasSistema().toPlainString()).append('\n');
        info.append("Total sistema: S/ ").append(cierreActual.getTotalSistema().toPlainString());
        if (cierreActual.getEfectivoContado() != null) {
            info.append('\n');
            info.append("Efectivo contado: S/ ").append(cierreActual.getEfectivoContado().toPlainString()).append('\n');
            info.append("Diferencia: S/ ").append(cierreActual.getDiferencia().toPlainString());
        }
        if (cierreActual.getConsolidadoEn() != null) {
            info.append('\n');
            info.append("Consolidado el ").append(FECHA_CORTA.format(cierreActual.getConsolidadoEn()));
        }
        info.append('\n');
        info.append("Acta generada: ").append(cierreActual.isActaGenerada() ? "Sí" : "No");
        etiquetaCierreInfo.setText(info.toString());
    }

    private String moneda(BigDecimal valor) {
        return valor == null ? "—" : "S/ " + valor.toPlainString();
    }

    private void ponerDiferencia(Label etiqueta, BigDecimal diferencia) {
        if (diferencia == null) {
            etiqueta.setText("—");
            etiqueta.setStyle("");
        } else if (diferencia.signum() == 0) {
            etiqueta.setText(moneda(diferencia) + " (conforme)");
            etiqueta.setStyle("-fx-text-fill: #059669;");
        } else {
            etiqueta.setText(moneda(diferencia));
            etiqueta.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
        }
    }

    private void aplicarBadge(Label etiqueta, String texto, String clase) {
        etiqueta.setText(texto);
        etiqueta.getStyleClass().removeAll("badge-verde", "badge-azul", "badge-rojo",
                "badge-amarillo", "badge-gris", "badge-naranja");
        etiqueta.getStyleClass().add(clase);
    }

    private String texto(TextField campo) {
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        return valor.isEmpty() ? null : valor;
    }

    private void indicar(Label etiqueta, String mensaje, boolean error) {
        etiqueta.setText(mensaje);
        etiqueta.getStyleClass().removeAll("etiqueta-error", "etiqueta-ok");
        etiqueta.getStyleClass().add(error ? "etiqueta-error" : "etiqueta-ok");
    }

    @FXML
    private void onVolverClick() {
        pe.edu.utp.dentrix.ui.Navegador.ir(pe.edu.utp.dentrix.ui.Navegador.INICIO);
    }

    @FXML
    private void onCierreClick() {
        panelCaja.setVisible(false);
        panelCaja.setManaged(false);
        panelCierre.setVisible(true);
        panelCierre.setManaged(true);
    }

    @FXML
    private void onCajaClick() {
        panelCaja.setVisible(true);
        panelCaja.setManaged(true);
        panelCierre.setVisible(false);
        panelCierre.setManaged(false);
    }

    private void mostrarInfo(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Caja");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Caja");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
