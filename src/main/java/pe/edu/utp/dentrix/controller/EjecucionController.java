package pe.edu.utp.dentrix.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
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
import javafx.util.StringConverter;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.CierrePlan;
import pe.edu.utp.dentrix.model.ControlPost;
import pe.edu.utp.dentrix.model.EjecucionItem;
import pe.edu.utp.dentrix.model.Evolucion;
import pe.edu.utp.dentrix.model.Paciente;
import pe.edu.utp.dentrix.model.Presupuesto;
import pe.edu.utp.dentrix.model.PresupuestoItem;
import pe.edu.utp.dentrix.model.Usuario;
import pe.edu.utp.dentrix.ui.Badges;
import pe.edu.utp.dentrix.ui.LaboratorioDialog;
import pe.edu.utp.dentrix.ui.Navegador;
import pe.edu.utp.dentrix.ui.TareasFX;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EjecucionController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final List<String> ESTADOS_CIERRE = List.of("TERMINADO", "ABANDONADO", "DERIVADO");

    @FXML
    private Label etiquetaMensaje;

    @FXML
    private Label etiquetaItem;

    @FXML
    private Label etiquetaError;

    @FXML
    private ComboBox<Paciente> comboPaciente;

    @FXML
    private ComboBox<Presupuesto> comboPresupuesto;

    @FXML
    private Button btnAbrirSesion;

    @FXML
    private Button btnFirmarSesion;

    @FXML
    private Button btnDescartarSesion;

    @FXML
    private Button btnAnularSesion;

    @FXML
    private Button btnProgramarControl;

    @FXML
    private Button btnRegistrarControl;

    @FXML
    private Button btnCerrarPlan;

    @FXML
    private Button btnCerrarPlanTarjeta;

    @FXML
    private Button btnLaboratorio;

    @FXML
    private TableView<PresupuestoItem> tablaItems;

    @FXML
    private TableColumn<PresupuestoItem, String> colPieza;

    @FXML
    private TableColumn<PresupuestoItem, String> colPrestacion;

    @FXML
    private TableColumn<PresupuestoItem, String> colFase;

    @FXML
    private TableColumn<PresupuestoItem, String> colSesiones;

    @FXML
    private TableColumn<PresupuestoItem, String> colEstado;

    @FXML
    private TableColumn<PresupuestoItem, String> colControl;

    @FXML
    private TableView<EjecucionItem> tablaSesiones;

    @FXML
    private TableColumn<EjecucionItem, String> colSesNumero;

    @FXML
    private TableColumn<EjecucionItem, String> colSesFecha;

    @FXML
    private TableColumn<EjecucionItem, String> colSesProfesional;

    @FXML
    private TableColumn<EjecucionItem, String> colSesEstado;

    @FXML
    private TextArea areaEvolucion;

    @FXML
    private Label lblControlFecha;

    @FXML
    private Label lblControlHallazgo;

    @FXML
    private Label lblPlanCierre;

    private final Map<Long, Integer> sesionesFirmadas = new HashMap<>();
    private final Map<Long, String> controlesPorItem = new HashMap<>();
    private List<ControlPost> controlesItem = new ArrayList<>();
    private ControlPost controlPendiente;
    private Presupuesto presupuestoActual;
    private PresupuestoItem itemActual;
    private Integer usuarioSesion;
    private boolean recargando;

    private record DatosPlan(List<PresupuestoItem> items, Map<Long, Integer> sesiones,
                             Map<Long, String> controles, CierrePlan cierre) {
    }

    private record DatosItem(List<EjecucionItem> sesiones, List<ControlPost> controles) {
    }

    @FXML
    private void initialize() {
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
        comboPresupuesto.setConverter(new StringConverter<Presupuesto>() {
            @Override
            public String toString(Presupuesto p) {
                return p == null ? "" : "v" + p.getVersion() + " · " + dinero(p.getTotal());
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
        colSesiones.setCellValueFactory(d -> new SimpleStringProperty(textoSesiones(d.getValue())));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEstado() == null ? "" : d.getValue().getEstado()));
        Badges.aplicarEn(colEstado);
        colControl.setCellValueFactory(d -> new SimpleStringProperty(textoControl(d.getValue().getId())));

        colSesNumero.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getNumeroSesion() == null ? "" : String.valueOf(d.getValue().getNumeroSesion())));
        colSesFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() == null ? "" : FECHA_HORA.format(d.getValue().getFecha())));
        colSesProfesional.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getProfesionalNombre() == null ? "" : d.getValue().getProfesionalNombre()));
        colSesEstado.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEstado() == null ? "" : d.getValue().getEstado()));
        Badges.aplicarEn(colSesEstado);

        tablaItems.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaSesiones.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        comboPaciente.valueProperty().addListener((obs, antes, ahora) -> pacienteCambiado(ahora));
        comboPresupuesto.valueProperty().addListener((obs, antes, ahora) -> mostrarPresupuesto(ahora));
        tablaItems.getSelectionModel().selectedItemProperty()
                .addListener((obs, antes, ahora) -> itemSeleccionado(ahora));
        tablaSesiones.getSelectionModel().selectedItemProperty()
                .addListener((obs, antes, ahora) -> sesionSeleccionada(ahora));

        aplicarEstadoBotones();
        AppContext contexto = AppContext.get();
        if (!contexto.sesion().activa()) {
            return;
        }
        usuarioSesion = contexto.sesion().getUsuario() == null
                ? null : contexto.sesion().getUsuario().getId();
        TareasFX.ejecutar(() -> contexto.pacientes().listar(), this::aplicarPacientes,
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void aplicarPacientes(List<Paciente> pacientes) {
        comboPaciente.setItems(FXCollections.observableArrayList(pacientes));
        Paciente enAtencion = AppContext.get().sesion().getPacienteEnAtencion();
        if (enAtencion == null) {
            return;
        }
        for (Paciente p : pacientes) {
            if (p.getId() != null && p.getId().equals(enAtencion.getId())) {
                comboPaciente.setValue(p);
                return;
            }
        }
    }

    private void pacienteCambiado(Paciente paciente) {
        if (recargando) {
            return;
        }
        presupuestoActual = null;
        etiquetaError.setText("");
        if (paciente == null) {
            comboPresupuesto.getItems().clear();
            comboPresupuesto.setValue(null);
            limpiarPlan();
            etiquetaMensaje.setText("");
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
        TareasFX.ejecutar(() -> {
            List<Presupuesto> aceptados = new ArrayList<>();
            for (Presupuesto p : AppContext.get().presupuestos().listarPorPaciente(pacienteId)) {
                if (p.estaAceptado()) {
                    aceptados.add(p);
                }
            }
            return aceptados;
        }, lista -> aplicarListaPresupuestos(lista, idSeleccionado),
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
        if (elegido == null && !lista.isEmpty()) {
            elegido = lista.get(0);
        }
        if (elegido == null) {
            comboPresupuesto.setValue(null);
            limpiarPlan();
            etiquetaMensaje.setText("Este paciente no tiene un plan aceptado");
            return;
        }
        etiquetaMensaje.setText("");
        comboPresupuesto.setValue(elegido);
    }

    private void mostrarPresupuesto(Presupuesto presupuesto) {
        presupuestoActual = presupuesto;
        if (presupuesto == null) {
            limpiarPlan();
            return;
        }
        etiquetaError.setText("");
        Long presupuestoId = presupuesto.getId();
        TareasFX.ejecutar(() -> cargarPlan(presupuestoId),
                datos -> aplicarPlan(datos, null),
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private DatosPlan cargarPlan(Long presupuestoId) throws DentrixException {
        var servicio = AppContext.get().ejecucion();
        List<PresupuestoItem> items = new ArrayList<>(servicio.itemsEjecutables(presupuestoId));
        Map<Long, String> estados = new HashMap<>();
        for (PresupuestoItem item : items) {
            estados.put(item.getId(), item.getEstado());
        }
        for (PresupuestoItem item : AppContext.get().presupuestos().items(presupuestoId)) {
            if (!estados.containsKey(item.getId())
                    && ("EJECUTADO".equals(item.getEstado()) || "CONTROLADO".equals(item.getEstado()))) {
                items.add(item);
            }
        }
        Map<Long, Integer> sesiones = new HashMap<>();
        Map<Long, String> controles = new HashMap<>();
        for (PresupuestoItem item : items) {
            int firmadas = 0;
            for (EjecucionItem sesion : servicio.sesiones(item.getId())) {
                if (sesion.esFirmada()) {
                    firmadas++;
                }
            }
            sesiones.put(item.getId(), firmadas);
            List<ControlPost> lista = servicio.controles(item.getId());
            controles.put(item.getId(), lista.isEmpty()
                    ? "—" : lista.get(lista.size() - 1).getEstado());
        }
        CierrePlan cierre = servicio.cierre(presupuestoId).orElse(null);
        return new DatosPlan(items, sesiones, controles, cierre);
    }

    private void aplicarPlan(DatosPlan datos, Long idSeleccionado) {
        sesionesFirmadas.clear();
        sesionesFirmadas.putAll(datos.sesiones());
        controlesPorItem.clear();
        controlesPorItem.putAll(datos.controles());
        tablaItems.setItems(FXCollections.observableArrayList(datos.items()));
        tablaItems.refresh();
        aplicarCierre(datos.cierre());
        if (datos.items().isEmpty()) {
            limpiarItem();
            etiquetaMensaje.setText("El plan aceptado no tiene items ejecutables");
            return;
        }
        int indice = 0;
        if (idSeleccionado != null) {
            for (int i = 0; i < datos.items().size(); i++) {
                if (idSeleccionado.equals(datos.items().get(i).getId())) {
                    indice = i;
                    break;
                }
            }
        }
        tablaItems.getSelectionModel().select(indice);
    }

    private void recargarItemSeleccionado() {
        PresupuestoItem item = itemActual;
        if (item == null) {
            return;
        }
        itemSeleccionado(item);
    }

    private void recargarPlan() {
        if (presupuestoActual == null) {
            return;
        }
        Long presupuestoId = presupuestoActual.getId();
        Long itemId = itemActual == null ? null : itemActual.getId();
        TareasFX.ejecutar(() -> cargarPlan(presupuestoId),
                datos -> aplicarPlan(datos, itemId),
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void itemSeleccionado(PresupuestoItem item) {
        itemActual = item;
        controlesItem = new ArrayList<>();
        controlPendiente = null;
        if (item == null) {
            limpiarItem();
            return;
        }
        etiquetaItem.setText("Pieza " + (item.getPiezaFdi() == null ? "—" : item.getPiezaFdi())
                + " · " + (item.getTratamientoNombre() == null ? "Item " + item.getId()
                : item.getTratamientoNombre()));
        Long itemId = item.getId();
        TareasFX.ejecutar(() -> new DatosItem(
                        AppContext.get().ejecucion().sesiones(itemId),
                        AppContext.get().ejecucion().controles(itemId)),
                datos -> aplicarItem(itemId, datos),
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void aplicarItem(Long itemId, DatosItem datos) {
        if (itemActual == null || !itemId.equals(itemActual.getId())) {
            return;
        }
        tablaSesiones.setItems(FXCollections.observableArrayList(datos.sesiones()));
        controlesItem = new ArrayList<>(datos.controles());
        controlPendiente = null;
        for (ControlPost control : controlesItem) {
            if (control.estaPendiente()) {
                controlPendiente = control;
                break;
            }
        }
        if (!datos.sesiones().isEmpty()) {
            tablaSesiones.getSelectionModel().select(datos.sesiones().size() - 1);
        } else {
            tablaSesiones.getSelectionModel().clearSelection();
            areaEvolucion.clear();
        }
        pintarControl();
        aplicarEstadoBotones();
    }

    private void sesionSeleccionada(EjecucionItem sesion) {
        aplicarEstadoBotones();
        if (sesion == null) {
            areaEvolucion.clear();
            return;
        }
        Long sesionId = sesion.getId();
        TareasFX.ejecutar(() -> AppContext.get().ejecucion().evolucionDeSesion(sesionId),
                evolucion -> {
                    EjecucionItem actual = tablaSesiones.getSelectionModel().getSelectedItem();
                    if (actual == null || !sesionId.equals(actual.getId())) {
                        return;
                    }
                    areaEvolucion.setText(textoEvolucion(sesion, evolucion));
                },
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private String textoEvolucion(EjecucionItem sesion, Optional<Evolucion> evolucion) {
        if (evolucion.isEmpty()) {
            return "Sesion " + sesion.getNumeroSesion() + " en " + sesion.getEstado()
                    + ": sin evolucion firmada.";
        }
        Evolucion firmada = evolucion.get();
        StringBuilder texto = new StringBuilder(firmada.getTexto() == null ? "" : firmada.getTexto());
        texto.append("\n\nFirmada: ").append(firmada.esFirmada() ? "si" : "no");
        if (firmada.getFechaFirma() != null) {
            texto.append(" · ").append(FECHA_HORA.format(firmada.getFechaFirma()));
        }
        if (firmada.getFirmaHash() != null) {
            texto.append(" · firma ").append(firmada.getFirmaHash());
        }
        return texto.toString();
    }

    private void pintarControl() {
        if (controlPendiente != null) {
            lblControlFecha.setText("Control programado para "
                    + FECHA.format(controlPendiente.getFechaProgramada()) + " (PENDIENTE)");
            lblControlHallazgo.setText("");
            return;
        }
        if (controlesItem.isEmpty()) {
            lblControlFecha.setText("Sin control programado");
            lblControlHallazgo.setText("");
            return;
        }
        ControlPost ultimo = controlesItem.get(controlesItem.size() - 1);
        StringBuilder texto = new StringBuilder("Ultimo control: ").append(ultimo.getEstado());
        LocalDate fecha = ultimo.getFechaRealizada() == null
                ? ultimo.getFechaProgramada() : ultimo.getFechaRealizada();
        if (fecha != null) {
            texto.append(" · ").append(FECHA.format(fecha));
        }
        lblControlFecha.setText(texto.toString());
        lblControlHallazgo.setText(ultimo.getHallazgo() == null ? "" : "Hallazgo: " + ultimo.getHallazgo());
    }

    private void aplicarCierre(CierrePlan cierre) {
        if (cierre == null) {
            lblPlanCierre.setText("Plan sin cierre registrado");
            return;
        }
        StringBuilder texto = new StringBuilder("Cierre ").append(cierre.getEstado());
        if (cierre.getFecha() != null) {
            texto.append(" · ").append(FECHA_HORA.format(cierre.getFecha()));
        }
        if (cierre.getMotivo() != null) {
            texto.append(" · motivo: ").append(cierre.getMotivo());
        }
        if (cierre.getFechaRecall() != null) {
            texto.append(" · recall: ").append(FECHA.format(cierre.getFechaRecall()));
        }
        lblPlanCierre.setText(texto.toString());
    }

    private void limpiarPlan() {
        sesionesFirmadas.clear();
        controlesPorItem.clear();
        itemActual = null;
        controlesItem = new ArrayList<>();
        controlPendiente = null;
        tablaItems.setItems(FXCollections.observableArrayList());
        tablaSesiones.setItems(FXCollections.observableArrayList());
        areaEvolucion.clear();
        etiquetaItem.setText("Selecciona un item");
        lblControlFecha.setText("Sin control programado");
        lblControlHallazgo.setText("");
        lblPlanCierre.setText("Plan sin cierre registrado");
        aplicarEstadoBotones();
    }

    private void limpiarItem() {
        tablaSesiones.setItems(FXCollections.observableArrayList());
        areaEvolucion.clear();
        etiquetaItem.setText("Selecciona un item");
        controlesItem = new ArrayList<>();
        controlPendiente = null;
        lblControlFecha.setText("Sin control programado");
        lblControlHallazgo.setText("");
        aplicarEstadoBotones();
    }

    private void aplicarEstadoBotones() {
        PresupuestoItem item = itemActual;
        EjecucionItem sesion = tablaSesiones.getSelectionModel().getSelectedItem();
        boolean ejecutable = item != null
                && ("ACEPTADO".equals(item.getEstado()) || "EN_EJECUCION".equals(item.getEstado()));
        boolean borrador = sesion != null && sesion.esBorrador();
        boolean firmada = sesion != null && sesion.esFirmada();
        boolean itemEjecutado = item != null && "EJECUTADO".equals(item.getEstado());
        btnAbrirSesion.setDisable(!ejecutable);
        btnFirmarSesion.setDisable(!borrador);
        btnDescartarSesion.setDisable(!borrador);
        btnAnularSesion.setDisable(!firmada);
        btnProgramarControl.setDisable(!itemEjecutado || controlPendiente != null);
        btnRegistrarControl.setDisable(controlPendiente == null);
        btnCerrarPlan.setDisable(presupuestoActual == null);
        btnCerrarPlanTarjeta.setDisable(presupuestoActual == null);
        btnLaboratorio.setDisable(item == null);
    }

    private String textoSesiones(PresupuestoItem item) {
        int firmadas = sesionesFirmadas.getOrDefault(item.getId(), 0);
        String planificadas = item.getSesionesPlanificadas() == null
                ? "—" : String.valueOf(item.getSesionesPlanificadas());
        return firmadas + " de " + planificadas;
    }

    private String textoControl(Long itemId) {
        return controlesPorItem.getOrDefault(itemId, "—");
    }

    private String dinero(BigDecimal valor) {
        BigDecimal monto = valor == null ? BigDecimal.ZERO : valor;
        return "S/ " + monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    @FXML
    private void onRecargarClick() {
        Paciente paciente = comboPaciente.getValue();
        Long presupuestoId = presupuestoActual == null ? null : presupuestoActual.getId();
        TareasFX.ejecutar(() -> AppContext.get().pacientes().listar(), pacientes -> {
            Paciente elegido;
            boolean anterior = recargando;
            recargando = true;
            try {
                comboPaciente.setItems(FXCollections.observableArrayList(pacientes));
                if (paciente == null) {
                    Paciente enAtencion = AppContext.get().sesion().getPacienteEnAtencion();
                    elegido = enAtencion == null ? null : buscarPaciente(pacientes, enAtencion.getId());
                } else {
                    elegido = buscarPaciente(pacientes, paciente.getId());
                }
                comboPaciente.setValue(elegido);
            } finally {
                recargando = anterior;
            }
            if (elegido == null) {
                presupuestoActual = null;
                comboPresupuesto.getItems().clear();
                comboPresupuesto.setValue(null);
                limpiarPlan();
                etiquetaMensaje.setText("");
                return;
            }
            recargarPresupuestos(presupuestoId);
        }, error -> mostrarError(TareasFX.mensaje(error)));
    }

    private Paciente buscarPaciente(List<Paciente> pacientes, Integer pacienteId) {
        if (pacienteId == null) {
            return null;
        }
        for (Paciente p : pacientes) {
            if (pacienteId.equals(p.getId())) {
                return p;
            }
        }
        return null;
    }

    @FXML
    private void onAbrirSesionClick() {
        PresupuestoItem item = itemActual;
        if (item == null) {
            mostrarError("Selecciona un item del plan aceptado");
            return;
        }
        if (!"ACEPTADO".equals(item.getEstado()) && !"EN_EJECUCION".equals(item.getEstado())) {
            mostrarError("Solo se abre sesion de un item ACEPTADO o EN_EJECUCION (R7)");
            return;
        }
        TextField procedimiento = new TextField(item.getTratamientoNombre() == null
                ? "" : item.getTratamientoNombre());
        procedimiento.getStyleClass().add("campo-formulario");
        TextField pieza = new TextField(item.getPiezaFdi() == null ? "" : String.valueOf(item.getPiezaFdi()));
        pieza.getStyleClass().add("campo-formulario");
        TextField superficies = new TextField();
        superficies.setPromptText("Ej. O, M, D");
        superficies.getStyleClass().add("campo-formulario");
        GridPane grilla = grilla();
        grilla.addRow(0, new Label("Procedimiento:"), procedimiento);
        grilla.addRow(1, new Label("Pieza FDI:"), pieza);
        grilla.addRow(2, new Label("Superficies:"), superficies);
        if (mostrarDialogo("Abrir sesion", "La sesion se abre en BORRADOR (R9)", grilla, "Abrir").isEmpty()) {
            return;
        }
        String textoProcedimiento = texto(procedimiento);
        if (textoProcedimiento == null) {
            mostrarError("El procedimiento de la sesion es obligatorio");
            return;
        }
        Integer piezaFdi;
        try {
            piezaFdi = numero(pieza);
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
            return;
        }
        Long itemId = item.getId();
        String textoSuperficies = texto(superficies);
        TareasFX.ejecutar(() -> AppContext.get().ejecucion()
                        .abrirSesion(itemId, textoProcedimiento, piezaFdi, textoSuperficies, usuarioId()),
                sesion -> {
                    indicar("Sesion " + sesion.getNumeroSesion() + " abierta en BORRADOR");
                    recargarPlan();
                },
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onFirmarSesionClick() {
        EjecucionItem sesion = tablaSesiones.getSelectionModel().getSelectedItem();
        if (sesion == null || !sesion.esBorrador()) {
            mostrarError("Selecciona una sesion BORRADOR para firmar");
            return;
        }
        TextArea evolucion = new TextArea();
        evolucion.setPrefHeight(120.0);
        evolucion.setWrapText(true);
        evolucion.setPromptText("Evolucion clinica de la sesion");
        evolucion.getStyleClass().add("campo-formulario");
        GridPane grilla = grilla();
        grilla.addRow(0, new Label("Evolucion:"), evolucion);
        if (mostrarDialogo("Firmar sesion", "La evolucion firmada no se edita (R9)", grilla, "Firmar").isEmpty()) {
            return;
        }
        String textoEvolucion = texto(evolucion);
        if (textoEvolucion == null) {
            mostrarError("El texto de la evolucion es obligatorio");
            return;
        }
        Long sesionId = sesion.getId();
        TareasFX.ejecutar(() -> AppContext.get().ejecucion()
                        .firmarSesion(sesionId, textoEvolucion, usuarioId()),
                firmada -> {
                    indicar("Sesion firmada; la evolucion queda inmutable (R9)");
                    recargarPlan();
                },
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onDescartarSesionClick() {
        EjecucionItem sesion = tablaSesiones.getSelectionModel().getSelectedItem();
        if (sesion == null || !sesion.esBorrador()) {
            mostrarError("Solo se descarta una sesion BORRADOR");
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Descartar sesion");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("Descartar la sesion " + sesion.getNumeroSesion() + " del item?");
        Long sesionId = sesion.getId();
        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta == ButtonType.OK) {
                TareasFX.ejecutar(() -> {
                    AppContext.get().ejecucion().descartarSesion(sesionId);
                    return null;
                }, ok -> {
                    indicar("Sesion descartada");
                    recargarPlan();
                }, error -> mostrarError(TareasFX.mensaje(error)));
            }
        });
    }

    @FXML
    private void onAnularSesionClick() {
        EjecucionItem sesion = tablaSesiones.getSelectionModel().getSelectedItem();
        if (sesion == null || !sesion.esFirmada()) {
            mostrarError("Solo se anula una sesion FIRMADA");
            return;
        }
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Anular sesion");
        dialogo.setHeaderText("La sesion anulada se conserva como evidencia");
        dialogo.setContentText("Motivo de la anulacion:");
        Optional<String> respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty() || respuesta.get().isBlank()) {
            return;
        }
        Long sesionId = sesion.getId();
        String motivo = respuesta.get().trim();
        TareasFX.ejecutar(() -> {
            AppContext.get().ejecucion().anularSesion(sesionId, motivo);
            return null;
        }, ok -> {
            indicar("Sesion anulada: " + motivo);
            recargarPlan();
        }, error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onProgramarControlClick() {
        PresupuestoItem item = itemActual;
        if (item == null) {
            mostrarError("Selecciona un item del plan aceptado");
            return;
        }
        DatePicker fecha = new DatePicker(LocalDate.now().plusDays(7));
        GridPane grilla = grilla();
        grilla.addRow(0, new Label("Fecha programada:"), fecha);
        if (mostrarDialogo("Programar control", "El control post exige un item EJECUTADO (R26)",
                grilla, "Programar").isEmpty()) {
            return;
        }
        LocalDate programada = fecha.getValue();
        if (programada == null) {
            mostrarError("La fecha programada del control es obligatoria");
            return;
        }
        Long itemId = item.getId();
        TareasFX.ejecutar(() -> AppContext.get().ejecucion()
                        .programarControl(itemId, programada, usuarioId()),
                control -> {
                    indicar("Control programado para " + FECHA.format(control.getFechaProgramada()));
                    recargarPlan();
                },
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onRegistrarControlClick() {
        if (controlPendiente == null) {
            mostrarError("El item no tiene un control PENDIENTE");
            return;
        }
        DatePicker fecha = new DatePicker(LocalDate.now());
        TextField hallazgo = new TextField();
        hallazgo.getStyleClass().add("campo-formulario");
        GridPane grilla = grilla();
        grilla.addRow(0, new Label("Fecha realizada:"), fecha);
        grilla.addRow(1, new Label("Hallazgo:"), hallazgo);
        if (mostrarDialogo("Registrar control", "Al registrarlo el item queda CONTROLADO",
                grilla, "Registrar").isEmpty()) {
            return;
        }
        LocalDate realizada = fecha.getValue();
        String textoHallazgo = texto(hallazgo);
        if (realizada == null) {
            mostrarError("La fecha del control realizado es obligatoria");
            return;
        }
        if (textoHallazgo == null) {
            mostrarError("El hallazgo del control es obligatorio");
            return;
        }
        Long controlId = controlPendiente.getId();
        TareasFX.ejecutar(() -> {
            AppContext.get().ejecucion().realizarControl(controlId, realizada, textoHallazgo, usuarioId());
            return null;
        }, ok -> {
            indicar("Control registrado; el item queda CONTROLADO");
            recargarPlan();
        }, error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onCerrarPlanClick() {
        Presupuesto presupuesto = presupuestoActual;
        if (presupuesto == null) {
            mostrarError("Selecciona un plan aceptado");
            return;
        }
        ComboBox<String> estado = new ComboBox<>(FXCollections.observableArrayList(ESTADOS_CIERRE));
        estado.setValue("TERMINADO");
        TextField motivo = new TextField();
        motivo.getStyleClass().add("campo-formulario");
        DatePicker recall = new DatePicker(LocalDate.now().plusMonths(6));
        recall.disableProperty().bind(estado.valueProperty().isNotEqualTo("TERMINADO"));
        GridPane grilla = grilla();
        grilla.addRow(0, new Label("Estado del cierre:"), estado);
        grilla.addRow(1, new Label("Motivo:"), motivo);
        grilla.addRow(2, new Label("Fecha de recall:"), recall);
        if (mostrarDialogo("Cerrar plan", "Cerrar el plan deja constancia del motivo",
                grilla, "Cerrar").isEmpty()) {
            return;
        }
        String estadoCierre = estado.getValue();
        String textoMotivo = texto(motivo);
        if (estadoCierre == null) {
            mostrarError("Selecciona el estado del cierre");
            return;
        }
        if (textoMotivo == null) {
            mostrarError("El motivo del cierre es obligatorio");
            return;
        }
        LocalDate fechaRecall = "TERMINADO".equals(estadoCierre) ? recall.getValue() : null;
        if ("TERMINADO".equals(estadoCierre) && fechaRecall == null) {
            mostrarError("El cierre TERMINADO exige una fecha de recall");
            return;
        }
        Long presupuestoId = presupuesto.getId();
        TareasFX.ejecutar(() -> AppContext.get().ejecucion()
                        .cerrarPlan(presupuestoId, estadoCierre, textoMotivo, fechaRecall, usuarioId()),
                cierre -> {
                    aplicarCierre(cierre);
                    indicar("Plan cerrado: " + cierre.getEstado());
                },
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onLaboratorioClick() {
        PresupuestoItem item = itemActual;
        if (item == null) {
            mostrarError("Selecciona un item del plan aceptado");
            return;
        }
        EjecucionItem sesion = tablaSesiones.getSelectionModel().getSelectedItem();
        LaboratorioDialog.mostrar(ventana(), item, sesion, this::recargarItemSeleccionado);
    }

    @FXML
    private void onVolverClick() {
        Navegador.ir(Navegador.INICIO);
    }

    private GridPane grilla() {
        GridPane grilla = new GridPane();
        grilla.setHgap(8.0);
        grilla.setVgap(8.0);
        grilla.setPadding(new Insets(10.0));
        return grilla;
    }

    private Optional<ButtonType> mostrarDialogo(String titulo, String cabecera, Node contenido,
                                                String textoAccion) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(cabecera);
        dialogo.initOwner(ventana());
        ButtonType accion = new ButtonType(textoAccion, ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().setAll(accion, ButtonType.CANCEL);
        dialogo.getDialogPane().setContent(contenido);
        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != accion) {
            return Optional.empty();
        }
        return respuesta;
    }

    private Integer numero(TextField campo) throws DentrixException {
        String valor = texto(campo);
        if (valor == null) {
            return null;
        }
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException e) {
            throw new DentrixException("La pieza FDI debe ser un numero (ej. 21, 48)");
        }
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
        etiquetaError.setText(mensaje);
        etiquetaError.getStyleClass().removeAll("etiqueta-error", "etiqueta-ok");
        etiquetaError.getStyleClass().add("etiqueta-ok");
    }

    private void mostrarError(String mensaje) {
        etiquetaError.setText(mensaje);
        etiquetaError.getStyleClass().removeAll("etiqueta-error", "etiqueta-ok");
        etiquetaError.getStyleClass().add("etiqueta-error");
    }
}
