package pe.edu.utp.dentrix.controller;

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
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Cita;
import pe.edu.utp.dentrix.model.Consultorio;
import pe.edu.utp.dentrix.model.Paciente;
import pe.edu.utp.dentrix.model.Tratamiento;
import pe.edu.utp.dentrix.model.Usuario;
import pe.edu.utp.dentrix.ui.Badges;
import pe.edu.utp.dentrix.ui.TareasFX;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgendaController {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> TIPOS = List.of("CITA", "ALMUERZO", "CIERRE", "BLOQUEADO");
    private static final String FXML_FICHA = "/pe/edu/utp/dentrix/fxml/ficha-clinica.fxml";

    @FXML
    private DatePicker campoFecha;

    @FXML
    private Label etiquetaConteo;

    @FXML
    private Label etiquetaModo;

    @FXML
    private Label etiquetaTipo;

    @FXML
    private ComboBox<String> comboTipo;

    @FXML
    private TextField campoInicio;

    @FXML
    private TextField campoFin;

    @FXML
    private Label etiquetaPaciente;

    @FXML
    private ComboBox<Paciente> comboPaciente;

    @FXML
    private ComboBox<Usuario> comboOdontologo;

    @FXML
    private ComboBox<Consultorio> comboConsultorio;

    @FXML
    private Label etiquetaTratamiento;

    @FXML
    private ComboBox<Tratamiento> comboTratamiento;

    @FXML
    private Label etiquetaMotivo;

    @FXML
    private TextField campoMotivo;

    @FXML
    private Label etiquetaError;

    @FXML
    private TableView<Cita> tablaAgenda;

    @FXML
    private TableColumn<Cita, String> colHora;

    @FXML
    private TableColumn<Cita, String> colPaciente;

    @FXML
    private TableColumn<Cita, String> colOdontologo;

    @FXML
    private TableColumn<Cita, String> colConsultorio;

    @FXML
    private TableColumn<Cita, String> colEstado;

    @FXML
    private TableColumn<Cita, String> colTratamiento;

    @FXML
    private TableColumn<Cita, String> colAccion;

    private final Map<Integer, String> nombresPacientes = new HashMap<>();
    private final Map<Integer, String> nombresOdontologos = new HashMap<>();
    private final Map<Integer, String> consultoriosPorCodigo = new HashMap<>();
    private final Map<Integer, String> nombresTratamientos = new HashMap<>();

    @FXML
    private void initialize() {
        colHora.setCellValueFactory(d -> new SimpleStringProperty(hora(d.getValue())));
        colPaciente.setCellValueFactory(d -> new SimpleStringProperty(
                "CITA".equals(d.getValue().getTipo())
                        ? nombresPacientes.getOrDefault(d.getValue().getPacienteId(), "—") : "—"));
        colOdontologo.setCellValueFactory(d -> new SimpleStringProperty(
                nombresOdontologos.getOrDefault(d.getValue().getUsuarioId(), "—")));
        colConsultorio.setCellValueFactory(d -> new SimpleStringProperty(
                consultoriosPorCodigo.getOrDefault(d.getValue().getConsultorioId(), "—")));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(estadoTabla(d.getValue())));
        colTratamiento.setCellValueFactory(d -> new SimpleStringProperty(tratamientoODespacho(d.getValue())));
        Badges.aplicarEn(colEstado);
        colAccion.setCellFactory(col -> new TableCell<>() {
            private final Button btnAtender = new Button("Atender");
            private final Button btnFalto = new Button("Faltó");
            private final Button btnEliminar = new Button("✕");
            private final HBox caja = new HBox(6, btnAtender, btnFalto, btnEliminar);

            {
                btnAtender.getStyleClass().addAll("boton-mini", "boton-primario");
                btnFalto.getStyleClass().add("boton-mini");
                btnEliminar.getStyleClass().addAll("boton-mini", "boton-peligro");
                btnAtender.setOnAction(e -> atender(getTableRow().getItem()));
                btnFalto.setOnAction(e -> aplicarTransicion(getTableRow().getItem(), AppContext.get().agenda()::marcarFalto, null));
                btnEliminar.setOnAction(e -> eliminar(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(String item, boolean vacio) {
                super.updateItem(item, vacio);
                Cita cita = vacio ? null : getTableRow().getItem();
                boolean esCita = cita != null && "CITA".equals(cita.getTipo());
                if (!esCita) {
                    setGraphic(null);
                    return;
                }
                String estado = cita.getEstado();
                btnAtender.setDisable(!("PENDIENTE".equals(estado) || "CONFIRMADA".equals(estado)
                        || "EN_SALA".equals(estado) || "EN_ATENCION".equals(estado)));
                btnFalto.setDisable(!("PENDIENTE".equals(estado) || "CONFIRMADA".equals(estado)));
                setGraphic(caja);
            }
        });
        tablaAgenda.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        campoFecha.valueProperty().addListener((obs, antes, ahora) -> recargar());
        comboTipo.valueProperty().addListener((obs, antes, ahora) -> aplicarVisibilidadTipo());

        AppContext contexto = AppContext.get();
        if (!contexto.sesion().activa()) {
            return;
        }
        campoFecha.setValue(java.time.LocalDate.now());
        comboTipo.setItems(FXCollections.observableArrayList(TIPOS));
        comboTipo.setValue("CITA");
        comboPaciente.setConverter(pacienteConverter());
        comboTratamiento.setConverter(tratamientoConverter());
        comboOdontologo.setConverter(odontologoConverter());
        comboConsultorio.setConverter(consultorioConverter());
        limpiarForma();
        TareasFX.ejecutar(this::cargarDatosIniciales,
                this::aplicarDatosIniciales,
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private String estadoTabla(Cita cita) {
        if (!"CITA".equals(cita.getTipo())) {
            return "BLOQUEADA";
        }
        return cita.getEstado();
    }

    private record DatosIniciales(Map<Integer, String> pacientes, Map<Integer, String> odontologos,
                                  Map<Integer, String> consultorios, Map<Integer, String> tratamientos,
                                  List<Paciente> pacientesActivos, List<Tratamiento> tratamientosActivos,
                                  List<Usuario> odontologosCombo, List<Consultorio> consultoriosCombo) {
    }

    private DatosIniciales cargarDatosIniciales() throws DentrixException {
        AppContext contexto = AppContext.get();

        Map<Integer, String> pacientes = new HashMap<>();
        List<Paciente> pacientesActivos = new ArrayList<>();
        for (Paciente p : contexto.pacientes().listar()) {
            pacientes.put(p.getId(), p.getApellidos() + ", " + p.getNombres());
            if ("ACTIVO".equals(p.getEstado())) {
                pacientesActivos.add(p);
            }
        }

        Integer rolOdontologo = contexto.roles().buscarPorCodigo("ODONTOLOGO")
                .orElseThrow(() -> new DentrixException("Rol ODONTOLOGO no configurado")).getId();
        Integer rolAdmin = contexto.roles().buscarPorCodigo("ADMINISTRADOR")
                .orElseThrow(() -> new DentrixException("Rol ADMINISTRADOR no configurado")).getId();
        Map<Integer, String> odontologos = new HashMap<>();
        List<Usuario> odontologosCombo = new ArrayList<>();
        for (Usuario u : contexto.usuarios().listarTodos()) {
            odontologos.put(u.getId(), "Dr. " + u.getNombres() + " " + u.getApellidos());
            if ("ACTIVO".equals(u.getEstado())
                    && (rolOdontologo.equals(u.getRolId()) || rolAdmin.equals(u.getRolId()))) {
                odontologosCombo.add(u);
            }
        }

        Map<Integer, String> consultorios = new HashMap<>();
        List<Consultorio> listaConsultorios = contexto.catalogo().listarConsultorios();
        for (Consultorio c : listaConsultorios) {
            consultorios.put(c.getId(), c.getCodigo());
        }

        Map<Integer, String> tratamientos = new HashMap<>();
        for (Tratamiento t : contexto.catalogo().listarTratamientos()) {
            tratamientos.put(t.getId(), t.getNombre());
        }

        return new DatosIniciales(pacientes, odontologos, consultorios, tratamientos, pacientesActivos,
                contexto.catalogo().listarTratamientosActivos(), odontologosCombo, listaConsultorios);
    }

    private void aplicarDatosIniciales(DatosIniciales datos) {
        nombresPacientes.clear();
        nombresPacientes.putAll(datos.pacientes());
        nombresOdontologos.clear();
        nombresOdontologos.putAll(datos.odontologos());
        consultoriosPorCodigo.clear();
        consultoriosPorCodigo.putAll(datos.consultorios());
        nombresTratamientos.clear();
        nombresTratamientos.putAll(datos.tratamientos());
        comboPaciente.setItems(FXCollections.observableArrayList(datos.pacientesActivos()));
        comboTratamiento.setItems(FXCollections.observableArrayList(datos.tratamientosActivos()));
        comboOdontologo.setItems(FXCollections.observableArrayList(datos.odontologosCombo()));
        comboConsultorio.setItems(FXCollections.observableArrayList(datos.consultoriosCombo()));
        recargar();
    }

    private StringConverter<Paciente> pacienteConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Paciente p) {
                return p == null ? "" : p.getApellidos() + ", " + p.getNombres() + " (DNI " + p.getDni() + ")";
            }

            @Override
            public Paciente fromString(String s) {
                return null;
            }
        };
    }

    private StringConverter<Usuario> odontologoConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Usuario u) {
                return u == null ? "" : "Dr. " + u.getNombres() + " " + u.getApellidos()
                        + (u.getColegiatura() == null ? "" : " (" + u.getColegiatura() + ")");
            }

            @Override
            public Usuario fromString(String s) {
                return null;
            }
        };
    }

    private StringConverter<Consultorio> consultorioConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Consultorio c) {
                return c == null ? "" : c.getCodigo() + " - " + c.getNombre();
            }

            @Override
            public Consultorio fromString(String s) {
                return null;
            }
        };
    }

    private StringConverter<Tratamiento> tratamientoConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Tratamiento t) {
                return t == null ? "" : t.getCodigo() + " - " + t.getNombre();
            }

            @Override
            public Tratamiento fromString(String s) {
                return null;
            }
        };
    }

    private boolean esModoBloque() {
        return comboTipo.getValue() != null && !"CITA".equals(comboTipo.getValue());
    }

    private void aplicarVisibilidadTipo() {
        boolean bloque = esModoBloque();
        etiquetaPaciente.setVisible(!bloque);
        etiquetaPaciente.setManaged(!bloque);
        comboPaciente.setVisible(!bloque);
        comboPaciente.setManaged(!bloque);
        etiquetaTratamiento.setVisible(!bloque);
        etiquetaTratamiento.setManaged(!bloque);
        comboTratamiento.setVisible(!bloque);
        comboTratamiento.setManaged(!bloque);
        etiquetaMotivo.setText(bloque ? "Motivo del bloque:" : "Motivo:");
        etiquetaError.setText("");
    }

    private String hora(Cita cita) {
        if (cita.getHoraInicio() == null) {
            return "";
        }
        String inicio = cita.getHoraInicio().toString();
        String fin = cita.getHoraFin() == null ? "" : cita.getHoraFin().toString();
        return inicio + " - " + fin;
    }

    private String tratamientoODespacho(Cita cita) {
        if (!"CITA".equals(cita.getTipo())) {
            String motivo = cita.getMotivo();
            return motivo == null || motivo.isBlank() ? "Bloque" : motivo;
        }
        if (cita.getTratamientoId() != null) {
            return nombresTratamientos.getOrDefault(cita.getTratamientoId(), "—");
        }
        String motivo = cita.getMotivo();
        return motivo == null || motivo.isBlank() ? "—" : motivo;
    }

    private void recargar() {
        java.time.LocalDate fecha = campoFecha.getValue();
        if (fecha == null) {
            return;
        }
        TareasFX.ejecutar(() -> AppContext.get().agenda().agendaDelDia(fecha),
                this::aplicarAgenda,
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void aplicarAgenda(List<Cita> citas) {
        tablaAgenda.setItems(FXCollections.observableArrayList(citas));
        long bloques = citas.stream().filter(c -> !"CITA".equals(c.getTipo())).count();
        etiquetaConteo.setText((citas.size() - bloques) + " cita(s) · " + bloques + " bloque(s)");
    }

    @FXML
    private void onNuevaCitaClick() {
        limpiarForma();
        comboTipo.setValue("CITA");
        comboPaciente.requestFocus();
    }

    @FXML
    private void onBloquearClick() {
        limpiarForma();
        comboTipo.setValue("ALMUERZO");
        campoInicio.requestFocus();
    }

    @FXML
    private void onRefrescarClick() {
        recargar();
    }

    @FXML
    private void onCancelarClick() {
        limpiarForma();
    }

    @FXML
    private void onGuardarClick() {
        etiquetaError.setText("");
        try {
            Cita cita = new Cita();
            cita.setFechaCita(campoFecha.getValue());
            cita.setHoraInicio(parseHora(campoInicio.getText(), "inicio"));
            cita.setHoraFin(parseHora(campoFin.getText(), "fin"));
            if (comboOdontologo.getValue() == null) {
                throw new DentrixException("Selecciona el odontólogo responsable");
            }
            cita.setUsuarioId(comboOdontologo.getValue().getId());
            if (comboConsultorio.getValue() == null) {
                throw new DentrixException("Selecciona el consultorio");
            }
            cita.setConsultorioId(comboConsultorio.getValue().getId());
            boolean modoBloque = esModoBloque();
            if (modoBloque) {
                cita.setTipo(comboTipo.getValue());
                String motivo = texto(campoMotivo);
                if (motivo == null) {
                    throw new DentrixException("El motivo del bloque es obligatorio");
                }
                cita.setMotivo(motivo);
            } else {
                cita.setTipo("CITA");
                if (comboPaciente.getValue() == null) {
                    throw new DentrixException("Selecciona el paciente de la cita");
                }
                cita.setPacienteId(comboPaciente.getValue().getId());
                if (comboTratamiento.getValue() != null) {
                    cita.setTratamientoId(comboTratamiento.getValue().getId());
                }
                cita.setMotivo(texto(campoMotivo));
            }
            TareasFX.ejecutar(() -> {
                if (modoBloque) {
                    AppContext.get().agenda().bloquear(cita);
                } else {
                    AppContext.get().agenda().agendar(cita);
                }
                return null;
            }, ok -> {
                limpiarForma();
                recargar();
            }, error -> etiquetaError.setText(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            etiquetaError.setText(e.getMessage());
        }
    }

    @FXML
    private void onVolverClick() {
        pe.edu.utp.dentrix.ui.Navegador.ir(pe.edu.utp.dentrix.ui.Navegador.INICIO);
    }

    private void atender(Cita cita) {
        if (cita == null) {
            return;
        }
        String estado = cita.getEstado();
        if ("PENDIENTE".equals(estado)) {
            aplicarTransicion(cita, AppContext.get().agenda()::confirmar, null);
        } else if ("CONFIRMADA".equals(estado)) {
            aplicarTransicion(cita, AppContext.get().agenda()::marcarLlegada, null);
        } else if ("EN_SALA".equals(estado)) {
            aplicarTransicion(cita, AppContext.get().agenda()::iniciarAtencion, () -> abrirFicha(cita));
        } else if ("EN_ATENCION".equals(estado)) {
            aplicarTransicion(cita, AppContext.get().agenda()::finalizar, null);
        }
    }

    private void abrirFicha(Cita cita) {
        if (cita == null || cita.getPacienteId() == null) {
            return;
        }
        try {
            Paciente paciente = AppContext.get().pacientes().buscarPorId(cita.getPacienteId());
            AppContext.get().sesion().setPacienteEnAtencion(paciente);
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
            return;
        }
        pe.edu.utp.dentrix.ui.Navegador.ir(FXML_FICHA);
    }

    private void aplicarTransicion(Cita cita, TransicionAccion accion, Runnable alExito) {
        if (cita == null || !"CITA".equals(cita.getTipo())) {
            return;
        }
        TareasFX.ejecutar(() -> {
            accion.aplicar(cita.getId());
            return null;
        }, ok -> {
            recargar();
            if (alExito != null) {
                alExito.run();
            }
        }, error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void eliminar(Cita cita) {
        if (cita == null) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Eliminar");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Eliminar esta fila de la agenda?");
        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta == javafx.scene.control.ButtonType.OK) {
                TareasFX.ejecutar(() -> {
                    AppContext.get().agenda().eliminar(cita.getId());
                    return null;
                }, ok -> recargar(), error -> mostrarError(TareasFX.mensaje(error)));
            }
        });
    }

    private void limpiarForma() {
        campoInicio.clear();
        campoFin.clear();
        campoMotivo.clear();
        comboPaciente.setValue(null);
        comboOdontologo.setValue(null);
        comboConsultorio.setValue(null);
        comboTratamiento.setValue(null);
        etiquetaError.setText("");
        etiquetaModo.setText(esModoBloque() ? "BLOQUE" : "NUEVA");
        aplicarVisibilidadTipo();
    }

    private LocalTime parseHora(String texto, String campo) throws DentrixException {
        String valor = texto == null ? "" : texto.trim();
        try {
            return LocalTime.parse(valor, HORA);
        } catch (DateTimeParseException e) {
            throw new DentrixException("La hora de " + campo + " debe tener formato HH:mm (ej. 09:00)");
        }
    }

    private String texto(TextField campo) {
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        return valor.isEmpty() ? null : valor;
    }

    private interface TransicionAccion {
        void aplicar(Long id) throws DentrixException;
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Agenda");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
