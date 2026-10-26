package pe.edu.utp.dentrix.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Consentimiento;
import pe.edu.utp.dentrix.model.FichaClinica;
import pe.edu.utp.dentrix.model.Paciente;
import pe.edu.utp.dentrix.model.Presupuesto;
import pe.edu.utp.dentrix.model.PresupuestoItem;
import pe.edu.utp.dentrix.model.Rol;
import pe.edu.utp.dentrix.model.Tratamiento;
import pe.edu.utp.dentrix.model.Usuario;
import pe.edu.utp.dentrix.ui.Badges;
import pe.edu.utp.dentrix.ui.ConsentimientoDialog;
import pe.edu.utp.dentrix.ui.EnmiendaDialog;
import pe.edu.utp.dentrix.ui.Navegador;
import pe.edu.utp.dentrix.ui.TareasFX;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PresupuestoController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final List<String> FASES = List.of("URGENTE", "BASICA", "RECONSTRUCTIVA",
            "QUIRURGICA", "REHABILITACION", "MANTENIMIENTO");
    private static final List<String> ROLES_FIRMANTE = List.of("PACIENTE", "TUTOR", "PAGADOR");

    @FXML
    private Label etiquetaCabecera;

    @FXML
    private Label etiquetaMensaje;

    @FXML
    private ComboBox<Paciente> comboPaciente;

    @FXML
    private ComboBox<Presupuesto> comboPresupuesto;

    @FXML
    private Button btnNuevoPresupuesto;

    @FXML
    private Button btnDescartar;

    @FXML
    private Button btnPresentar;

    @FXML
    private Button btnAceptarTodo;

    @FXML
    private Button btnAceptarSeleccionados;

    @FXML
    private Button btnRechazar;

    @FXML
    private Button btnEnmendar;

    @FXML
    private TableView<PresupuestoItem> tablaItems;

    @FXML
    private TableColumn<PresupuestoItem, String> colPieza;

    @FXML
    private TableColumn<PresupuestoItem, String> colPrestacion;

    @FXML
    private TableColumn<PresupuestoItem, String> colFase;

    @FXML
    private TableColumn<PresupuestoItem, String> colPrioridad;

    @FXML
    private TableColumn<PresupuestoItem, String> colSesiones;

    @FXML
    private TableColumn<PresupuestoItem, String> colPrecio;

    @FXML
    private TableColumn<PresupuestoItem, String> colEstado;

    @FXML
    private TableColumn<PresupuestoItem, String> colConsentimiento;

    @FXML
    private VBox panelFormularioItems;

    @FXML
    private ComboBox<Tratamiento> comboTratamiento;

    @FXML
    private TextField campoPieza;

    @FXML
    private ComboBox<String> comboFase;

    @FXML
    private Spinner<Integer> spinnerPrioridad;

    @FXML
    private Spinner<Integer> spinnerCantidad;

    @FXML
    private Spinner<Integer> spinnerSesiones;

    @FXML
    private CheckBox checkConsentimiento;

    @FXML
    private Label lblSubtotal;

    @FXML
    private Label lblDescuento;

    @FXML
    private Label lblTotal;

    @FXML
    private Label lblTotalAceptado;

    @FXML
    private Label lblDescuentoMotivo;

    @FXML
    private TextField campoFirmante;

    @FXML
    private TextField campoDocumento;

    @FXML
    private ComboBox<String> comboRolFirmante;

    @FXML
    private TextArea areaAlternativas;

    @FXML
    private TextArea areaRiesgos;

    @FXML
    private Button btnRegistrarAceptacion;

    @FXML
    private VBox tarjetaConsentimiento;

    @FXML
    private Button btnFirmarConsentimiento;

    private final Set<Long> consentimientosFirmados = new HashSet<>();
    private Presupuesto presupuestoActual;
    private Integer usuarioSesion;

    private record DatosIniciales(List<Paciente> pacientes, List<Tratamiento> tratamientos) {
    }

    private record DatosPresupuesto(List<PresupuestoItem> items, List<Consentimiento> consentimientos,
                                    BigDecimal totalAceptado) {
    }

    @FXML
    private void initialize() {
        comboFase.setItems(FXCollections.observableArrayList(FASES));
        comboFase.setValue("BASICA");
        comboRolFirmante.setItems(FXCollections.observableArrayList(ROLES_FIRMANTE));
        comboRolFirmante.setValue("PACIENTE");
        spinnerPrioridad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, 3));
        spinnerCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
        spinnerSesiones.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 1));

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
        comboTratamiento.setConverter(new StringConverter<Tratamiento>() {
            @Override
            public String toString(Tratamiento t) {
                return t == null ? "" : t.getCodigo() + " - " + t.getNombre();
            }

            @Override
            public Tratamiento fromString(String s) {
                return null;
            }
        });
        comboPresupuesto.setConverter(new StringConverter<Presupuesto>() {
            @Override
            public String toString(Presupuesto p) {
                return p == null ? "" : "v" + p.getVersion() + " · " + p.getEstado() + " · " + dinero(p.getTotal());
            }

            @Override
            public Presupuesto fromString(String s) {
                return null;
            }
        });

        colPieza.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPiezaFdi() == null ? "" : String.valueOf(d.getValue().getPiezaFdi())));
        colPrestacion.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTratamientoNombre() == null ? "" : d.getValue().getTratamientoNombre()));
        colFase.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFase() == null ? "" : d.getValue().getFase()));
        colPrioridad.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPrioridad() == null ? "" : String.valueOf(d.getValue().getPrioridad())));
        colSesiones.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getSesionesPlanificadas() == null ? ""
                        : String.valueOf(d.getValue().getSesionesPlanificadas())));
        colPrecio.setCellValueFactory(d -> new SimpleStringProperty(dinero(d.getValue().getPrecioUnitario())));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEstado() == null ? "" : d.getValue().getEstado()));
        Badges.aplicarEn(colEstado);
        colConsentimiento.setCellValueFactory(d -> new SimpleStringProperty(textoConsentimiento(d.getValue())));
        tablaItems.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaItems.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        comboPaciente.valueProperty().addListener((obs, antes, ahora) -> pacienteCambiado(ahora));
        comboPresupuesto.valueProperty().addListener((obs, antes, ahora) -> mostrarPresupuesto(ahora));

        AppContext contexto = AppContext.get();
        if (!contexto.sesion().activa()) {
            aplicarEstadoBotones(null);
            return;
        }
        usuarioSesion = contexto.sesion().getUsuario().getId();
        aplicarPermisos();
        aplicarEstadoBotones(null);
        TareasFX.ejecutar(this::cargarInicial, this::aplicarInicial,
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private DatosIniciales cargarInicial() throws DentrixException {
        AppContext contexto = AppContext.get();
        return new DatosIniciales(contexto.pacientes().listar(),
                contexto.catalogo().listarTratamientosActivos());
    }

    private void aplicarInicial(DatosIniciales datos) {
        comboTratamiento.setItems(FXCollections.observableArrayList(datos.tratamientos()));
        comboPaciente.setItems(FXCollections.observableArrayList(datos.pacientes()));
        Paciente enAtencion = AppContext.get().sesion().getPacienteEnAtencion();
        if (enAtencion == null) {
            return;
        }
        for (Paciente p : datos.pacientes()) {
            if (p.getId() != null && p.getId().equals(enAtencion.getId())) {
                comboPaciente.setValue(p);
                return;
            }
        }
    }

    private void pacienteCambiado(Paciente paciente) {
        presupuestoActual = null;
        if (paciente == null) {
            comboPresupuesto.getItems().clear();
            comboPresupuesto.setValue(null);
            limpiar();
            return;
        }
        recargarPresupuestos(null);
    }

    private void recargarPresupuestos(Long idSeleccionado) {
        Paciente paciente = comboPaciente.getValue();
        if (paciente == null) {
            return;
        }
        Integer pacienteId = paciente.getId();
        TareasFX.ejecutar(() -> AppContext.get().presupuestos().listarPorPaciente(pacienteId),
                lista -> aplicarListaPresupuestos(lista, idSeleccionado),
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void aplicarListaPresupuestos(List<Presupuesto> lista, Long idSeleccionado) {
        comboPresupuesto.setItems(FXCollections.observableArrayList(lista));
        Presupuesto elegido = null;
        if (idSeleccionado != null) {
            for (Presupuesto p : lista) {
                if (idSeleccionado.equals(p.getId())) {
                    elegido = p;
                    break;
                }
            }
        }
        if (elegido == null) {
            elegido = mayorVersion(lista);
        }
        comboPresupuesto.setValue(elegido);
        if (elegido == null) {
            presupuestoActual = null;
            limpiar();
        }
    }

    private Presupuesto mayorVersion(List<Presupuesto> lista) {
        Presupuesto mayor = null;
        for (Presupuesto p : lista) {
            int version = p.getVersion() == null ? 0 : p.getVersion();
            int versionMayor = mayor == null || mayor.getVersion() == null ? 0 : mayor.getVersion();
            if (mayor == null || version > versionMayor) {
                mayor = p;
            }
        }
        return mayor;
    }

    private void mostrarPresupuesto(Presupuesto presupuesto) {
        presupuestoActual = presupuesto;
        if (presupuesto == null) {
            limpiar();
            return;
        }
        Long id = presupuesto.getId();
        boolean aceptado = presupuesto.estaAceptado();
        TareasFX.ejecutar(() -> {
            var servicio = AppContext.get().presupuestos();
            List<PresupuestoItem> items = servicio.items(id);
            List<Consentimiento> consentimientos = servicio.consentimientos(id);
            BigDecimal totalAceptado = aceptado ? servicio.totalAceptado(id) : BigDecimal.ZERO;
            return new DatosPresupuesto(items, consentimientos, totalAceptado);
        }, datos -> aplicarPresupuesto(presupuesto, datos),
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void aplicarPresupuesto(Presupuesto presupuesto, DatosPresupuesto datos) {
        consentimientosFirmados.clear();
        for (Consentimiento c : datos.consentimientos()) {
            if (c.estaFirmado() && c.getPresupuestoItemId() != null) {
                consentimientosFirmados.add(c.getPresupuestoItemId());
            }
        }
        tablaItems.setItems(FXCollections.observableArrayList(datos.items()));
        tablaItems.refresh();
        etiquetaCabecera.setText(describir(presupuesto));
        lblSubtotal.setText("Subtotal: " + dinero(presupuesto.getSubtotal()));
        lblDescuento.setText("Descuento: " + dinero(presupuesto.getDescuento()));
        lblTotal.setText("Total: " + dinero(presupuesto.getTotal()));
        boolean aceptado = presupuesto.estaAceptado();
        lblTotalAceptado.setText("Total aceptado: " + dinero(datos.totalAceptado()));
        lblTotalAceptado.setVisible(aceptado);
        lblTotalAceptado.setManaged(aceptado);
        String motivo = presupuesto.getDescuentoMotivo();
        lblDescuentoMotivo.setText(motivo == null ? "" : "Motivo del descuento: " + motivo);
        aplicarEstadoBotones(presupuesto.getEstado());

        // --- CORRECCIÓN: AUTOCOMPLETAR CAMPOS DE FIRMA ---
        Paciente paciente = comboPaciente.getValue();
        if (paciente != null) {
            campoFirmante.setText(paciente.getApellidos() + ", " + paciente.getNombres());
            campoDocumento.setText(paciente.getDni());
            comboRolFirmante.setValue("PACIENTE");
        }
        // -------------------------------------------------
    }

    private String describir(Presupuesto presupuesto) {
        StringBuilder texto = new StringBuilder();
        texto.append("Paciente: ")
                .append(presupuesto.getPacienteNombre() == null ? "" : presupuesto.getPacienteNombre());
        texto.append("   Versión: v").append(presupuesto.getVersion());
        texto.append("   Estado: ").append(presupuesto.getEstado());
        texto.append("   Vigencia hasta: ").append(presupuesto.getVigenciaHasta() == null
                ? "—" : FECHA.format(presupuesto.getVigenciaHasta()));
        if (presupuesto.getFechaEmision() != null) {
            texto.append("   Emitido: ").append(FECHA_HORA.format(presupuesto.getFechaEmision()));
        }
        return texto.toString();
    }

    private void limpiar() {
        tablaItems.setItems(FXCollections.observableArrayList());
        consentimientosFirmados.clear();
        etiquetaCabecera.setText("Selecciona un paciente para ver sus presupuestos.");
        lblSubtotal.setText("Subtotal: S/ 0.00");
        lblDescuento.setText("Descuento: S/ 0.00");
        lblTotal.setText("Total: S/ 0.00");
        lblTotalAceptado.setVisible(false);
        lblTotalAceptado.setManaged(false);
        lblDescuentoMotivo.setText("");
        aplicarEstadoBotones(null);
    }

    private void aplicarPermisos() {
        boolean esRecepcion = "RECEPCION".equals(AppContext.get().sesion().getCodigoRol());
        ocultar(panelFormularioItems, esRecepcion);
        ocultar(btnPresentar, esRecepcion);
        ocultar(btnDescartar, esRecepcion);
        ocultar(tarjetaConsentimiento, esRecepcion);
        ocultar(colFase, esRecepcion);
        ocultar(colPrioridad, esRecepcion);
        ocultar(colSesiones, esRecepcion);
    }

    private void ocultar(Node nodo, boolean oculto) {
        nodo.setVisible(!oculto);
        nodo.setManaged(!oculto);
    }

    private void ocultar(TableColumn<?, ?> columna, boolean oculto) {
        columna.setVisible(!oculto);
    }

     private void aplicarEstadoBotones(String estado) {
        boolean borrador = "BORRADOR".equals(estado);
        boolean presentado = "PRESENTADO".equals(estado);
        boolean aceptado = "ACEPTADO".equals(estado);
        
        // El formulario de ítems solo se edita en borrador
        panelFormularioItems.setDisable(!borrador);
        
        // El botón de presentar sigue estando por si quieres hacerlo manual
        btnPresentar.setDisable(!borrador);
        btnDescartar.setDisable(!(borrador || presentado));
        
        // --- MEJORA UX: Habilitamos los botones de aceptar en borrador y presentado ---
        btnAceptarTodo.setDisable(!(borrador || presentado));
        btnAceptarSeleccionados.setDisable(!(borrador || presentado));
        btnRegistrarAceptacion.setDisable(!(borrador || presentado)); 
        // -----------------------------------------------------------------------------

        btnRechazar.setDisable(!presentado);
        
        // El consentimiento solo se firma si ya está aceptado
        btnFirmarConsentimiento.setDisable(!aceptado);
        btnEnmendar.setDisable(!(aceptado && puedeEnmendar()));
    }
    private boolean puedeEnmendar() {
        String rol = AppContext.get().sesion().getCodigoRol();
        return "ADMINISTRADOR".equals(rol) || "ODONTOLOGO".equals(rol);
    }

    private Presupuesto exigirPresupuesto(String estadoRequerido) throws DentrixException {
        if (presupuestoActual == null) {
            throw new DentrixException("Selecciona o crea un presupuesto");
        }
        if (estadoRequerido != null && !estadoRequerido.equals(presupuestoActual.getEstado())) {
            throw new DentrixException("La operacion exige un presupuesto " + estadoRequerido
                    + "; el presupuesto esta en " + presupuestoActual.getEstado());
        }
        return presupuestoActual;
    }

    @FXML
    private void onRecargarClick() {
        Long id = presupuestoActual == null ? null : presupuestoActual.getId();
        TareasFX.ejecutar(() -> AppContext.get().catalogo().listarTratamientosActivos(),
                tratamientos -> {
                    comboTratamiento.setItems(FXCollections.observableArrayList(tratamientos));
                    if (comboPaciente.getValue() != null) {
                        recargarPresupuestos(id);
                    }
                },
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onNuevoPresupuestoClick() {
        Paciente paciente = comboPaciente.getValue();
        if (paciente == null) {
            mostrarError("Selecciona el paciente del presupuesto");
            return;
        }
        Integer pacienteId = paciente.getId();
        Integer creador = usuarioId();
        TareasFX.ejecutar(() -> {
            List<FichaClinica> fichas = AppContext.get().fichas().listarPorPaciente(pacienteId);
            if (fichas.isEmpty()) {
                throw new DentrixException("El paciente no tiene ficha clinica; registrela primero");
            }
            return AppContext.get().presupuestos().crearDesdeFicha(fichas.get(0).getId(), creador);
        }, creado -> {
            indicar("Presupuesto v" + creado.getVersion() + " creado en BORRADOR");
            recargarPresupuestos(creado.getId());
        }, error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onDescartarClick() {
        try {
            Presupuesto presupuesto = exigirPresupuesto(null);
            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacion.setTitle("Presupuesto");
            confirmacion.setHeaderText(null);
            confirmacion.setContentText("¿Descartar el presupuesto v" + presupuesto.getVersion() + "?");
            Long presupuestoId = presupuesto.getId();
            confirmacion.showAndWait().ifPresent(respuesta -> {
                if (respuesta == ButtonType.OK) {
                    TareasFX.ejecutar(() -> {
                        AppContext.get().presupuestos().descartar(presupuestoId);
                        return null;
                    }, ok -> {
                        indicar("Presupuesto descartado");
                        recargarPresupuestos(null);
                    }, error -> mostrarError(TareasFX.mensaje(error)));
                }
            });
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onPresentarClick() {
        try {
            Presupuesto presupuesto = exigirPresupuesto("BORRADOR");
            Long presupuestoId = presupuesto.getId();
            TareasFX.ejecutar(() -> {
                AppContext.get().presupuestos().presentar(presupuestoId);
                return null;
            }, ok -> {
                indicar("Presupuesto presentado al paciente");
                recargarPresupuestos(presupuestoId);
            }, error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onAgregarItemClick() {
        try {
            Presupuesto presupuesto = exigirPresupuesto("BORRADOR");
            Tratamiento tratamiento = comboTratamiento.getValue();
            if (tratamiento == null) {
                throw new DentrixException("Selecciona el tratamiento del item");
            }
            PresupuestoItem item = new PresupuestoItem();
            item.setTratamientoId(tratamiento.getId());
            item.setPiezaFdi(piezaIngresada());
            item.setFase(comboFase.getValue());
            item.setPrioridad(spinnerPrioridad.getValue());
            item.setCantidad(spinnerCantidad.getValue());
            item.setSesionesPlanificadas(spinnerSesiones.getValue());
            item.setRequiereConsentimiento(checkConsentimiento.isSelected());
            Long presupuestoId = presupuesto.getId();
            TareasFX.ejecutar(() -> AppContext.get().presupuestos().agregarItem(presupuestoId, item),
                    creado -> {
                        indicar("Item agregado: " + creado.getTratamientoNombre());
                        recargarPresupuestos(presupuestoId);
                    },
                    error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onQuitarItemClick() {
        PresupuestoItem seleccionado = tablaItems.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarInfo("Selecciona el item que quieres quitar.");
            return;
        }
        try {
            Presupuesto presupuesto = exigirPresupuesto("BORRADOR");
            Long presupuestoId = presupuesto.getId();
            Long itemId = seleccionado.getId();
            TareasFX.ejecutar(() -> {
                AppContext.get().presupuestos().quitarItem(presupuestoId, itemId);
                return null;
            }, ok -> {
                indicar("Item quitado del presupuesto");
                recargarPresupuestos(presupuestoId);
            }, error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onAceptarTodoClick() {
        aceptarItems(itemsPropuestos());
    }

    @FXML
    private void onAceptarSeleccionadosClick() {
        List<PresupuestoItem> seleccionados = itemsSeleccionadosPropuestos();
        if (seleccionados.isEmpty()) {
            mostrarError("Selecciona al menos un item PROPUESTO en la tabla");
            return;
        }
        aceptarItems(seleccionados);
    }

    @FXML
    private void onRegistrarAceptacionClick() {
        List<PresupuestoItem> seleccionados = itemsSeleccionadosPropuestos();
        aceptarItems(seleccionados.isEmpty() ? itemsPropuestos() : seleccionados);
    }

        private void aceptarItems(List<PresupuestoItem> items) {
        try {
            Presupuesto presupuesto = exigirPresupuesto(null); // Ya no exigimos que esté PRESENTADO aquí
            
            // --- MEJORA UX: Si está en borrador, lo presentamos automáticamente ---
            if ("BORRADOR".equals(presupuesto.getEstado())) {
                AppContext.get().presupuestos().presentar(presupuesto.getId());
                presupuesto.setEstado("PRESENTADO");
            }
            // ---------------------------------------------------------------------

            if (!"PRESENTADO".equals(presupuesto.getEstado())) {
                 throw new DentrixException("El presupuesto debe estar PRESENTADO o en BORRADOR para aceptarse. Estado actual: " + presupuesto.getEstado());
            }

            if (items.isEmpty()) {
                throw new DentrixException("La aceptacion debe incluir al menos un item PROPUESTO");
            }
            String firmante = texto(campoFirmante);
            String documento = texto(campoDocumento);
            String rol = comboRolFirmante.getValue();
            if (firmante == null) {
                throw new DentrixException("El nombre del firmante es obligatorio");
            }
            if (documento == null) {
                throw new DentrixException("El documento del firmante es obligatorio");
            }
            if (rol == null) {
                throw new DentrixException("Selecciona el rol del firmante");
            }
            List<Long> ids = new ArrayList<>();
            for (PresupuestoItem item : items) {
                ids.add(item.getId());
            }
            String alternativas = texto(areaAlternativas);
            String riesgos = texto(areaRiesgos);
            Long presupuestoId = presupuesto.getId();
            Integer registradoPor = usuarioId();
            
            // Aquí se hace la aceptación real
            TareasFX.ejecutar(() -> {
                AppContext.get().presupuestos().aceptar(presupuestoId, ids, firmante, documento, rol,
                        alternativas, riesgos, registradoPor);
                return null;
            }, ok -> {
                indicar("Aceptacion registrada exitosamente.");
                recargarPresupuestos(presupuestoId);
            }, error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private List<PresupuestoItem> itemsPropuestos() {
        List<PresupuestoItem> propuestos = new ArrayList<>();
        for (PresupuestoItem item : tablaItems.getItems()) {
            if ("PROPUESTO".equals(item.getEstado())) {
                propuestos.add(item);
            }
        }
        return propuestos;
    }

    private List<PresupuestoItem> itemsSeleccionadosPropuestos() {
        List<PresupuestoItem> propuestos = new ArrayList<>();
        for (PresupuestoItem item : tablaItems.getSelectionModel().getSelectedItems()) {
            if ("PROPUESTO".equals(item.getEstado())) {
                propuestos.add(item);
            }
        }
        return propuestos;
    }

    @FXML
    private void onRechazarClick() {
        try {
            Presupuesto presupuesto = exigirPresupuesto("PRESENTADO");
            TextInputDialog dialogo = new TextInputDialog();
            dialogo.setTitle("Rechazar presupuesto");
            dialogo.setHeaderText("El rechazo se conserva como evidencia (R22)");
            dialogo.setContentText("Motivo del rechazo:");
            Optional<String> respuesta = dialogo.showAndWait();
            if (respuesta.isEmpty() || respuesta.get().isBlank()) {
                return;
            }
            Long presupuestoId = presupuesto.getId();
            String motivo = respuesta.get().trim();
            TareasFX.ejecutar(() -> {
                AppContext.get().presupuestos().rechazar(presupuestoId, motivo);
                return null;
            }, ok -> {
                indicar("Presupuesto rechazado: " + motivo);
                recargarPresupuestos(presupuestoId);
            }, error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onEnmendarClick() {
        try {
            Presupuesto presupuesto = exigirPresupuesto("ACEPTADO");
            if (!puedeEnmendar()) {
                throw new DentrixException(
                        "Solo Administracion u Odontologia enmiendan un plan aceptado");
            }
            if (!EnmiendaDialog.mostrar(ventana(), presupuesto)) {
                return;
            }
            indicar("Enmienda firmada: el plan aceptado se actualizo (R22)");
            recargarPresupuestos(presupuesto.getId());
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

        @FXML
    private void onFirmarConsentimientoClick() {
        try {
            Presupuesto presupuesto = exigirPresupuesto("ACEPTADO");
            List<PresupuestoItem> seleccionados = new ArrayList<>();
            
            // --- CORRECCIÓN: Si no hay nada seleccionado, toma automáticamente todos los pendientes ---
            List<PresupuestoItem> itemsAProcesar = new ArrayList<>(tablaItems.getSelectionModel().getSelectedItems());
            if (itemsAProcesar.isEmpty()) {
                itemsAProcesar = tablaItems.getItems(); // Toma todos los ítems de la tabla
            }

            for (PresupuestoItem item : itemsAProcesar) {
                if (item.isRequiereConsentimiento()
                        && !consentimientosFirmados.contains(item.getId())
                        && !"ANULADO".equals(item.getEstado())
                        && !"RECHAZADO".equals(item.getEstado())) {
                    seleccionados.add(item);
                }
            }
            // --------------------------------------------------------------------------------------

            if (seleccionados.isEmpty()) {
                throw new DentrixException(
                        "No hay items pendientes de consentimiento. Asegúrate de que el item tenga marcado 'Requiere consentimiento'.");
            }
            List<Long> ids = new ArrayList<>();
            StringBuilder procedimiento = new StringBuilder();
            for (PresupuestoItem item : seleccionados) {
                ids.add(item.getId());
                if (procedimiento.length() > 0) {
                    procedimiento.append("; ");
                }
                procedimiento.append(item.getTratamientoNombre() == null
                        ? "Item " + item.getId() : item.getTratamientoNombre());
            }
            // Calcular edad del paciente y pasarla al diálogo
            Paciente paciente = comboPaciente.getValue();
            int edad = 0;
            if (paciente != null && paciente.getFechaNac() != null) {
                edad = Period.between(paciente.getFechaNac(), LocalDate.now()).getYears();
            }
            Optional<ConsentimientoDialog.Datos> datos =
                    ConsentimientoDialog.mostrar(ventana(), procedimiento.toString(), edad);
            if (datos.isEmpty()) {
                return;
            }
            ConsentimientoDialog.Datos firmado = datos.get();
            Integer profesional = usuarioId();
            Long presupuestoId = presupuesto.getId();
            TareasFX.ejecutar(() -> AppContext.get().presupuestos().firmarConsentimiento(ids,
                            firmado.procedimiento(), firmado.riesgos(), firmado.alternativas(),
                            firmado.firmante(), firmado.documento(), firmado.rol(),
                            firmado.vinculo(), profesional),
                    firmados -> {
                        indicar(firmados.size() + " consentimiento(s) firmado(s)");
                        recargarPresupuestos(presupuestoId);
                    },
                    error -> mostrarError(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onVolverClick() {
        Navegador.ir(Navegador.INICIO);
    }

    private Integer piezaIngresada() throws DentrixException {
        String valor = texto(campoPieza);
        if (valor == null) {
            return null;
        }
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException e) {
            throw new DentrixException("La pieza FDI debe ser un numero (ej. 21, 48)");
        }
    }

    private String textoConsentimiento(PresupuestoItem item) {
        if (!item.isRequiereConsentimiento()) {
            return "—";
        }
        return consentimientosFirmados.contains(item.getId()) ? "Firmado" : "Pendiente";
    }

    private String dinero(BigDecimal valor) {
        BigDecimal monto = valor == null ? BigDecimal.ZERO : valor;
        return "S/ " + monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private Integer usuarioId() {
        if (usuarioSesion != null) {
            return usuarioSesion;
        }
        Usuario usuario = AppContext.get().sesion().getUsuario();
        return usuario == null || usuario.getId() == null ? 1 : usuario.getId();
    }

    private javafx.stage.Window ventana() {
        return comboPaciente.getScene() == null ? null : comboPaciente.getScene().getWindow();
    }

    private String texto(TextField campo) {
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        return valor.isEmpty() ? null : valor;
    }

    private String texto(TextArea area) {
        String valor = area.getText() == null ? "" : area.getText().trim();
        return valor.isEmpty() ? null : valor;
    }

    private void indicar(String mensaje) {
        etiquetaMensaje.setText(mensaje);
        etiquetaMensaje.getStyleClass().removeAll("etiqueta-error", "etiqueta-ok");
        etiquetaMensaje.getStyleClass().add("etiqueta-ok");
    }

    private void mostrarInfo(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Presupuesto");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Presupuesto");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}