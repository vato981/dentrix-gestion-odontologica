package pe.edu.utp.dentrix.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Cita;
import pe.edu.utp.dentrix.model.Paciente;
import pe.edu.utp.dentrix.model.Tratamiento;
import pe.edu.utp.dentrix.model.Usuario;
import pe.edu.utp.dentrix.ui.Badges;
import pe.edu.utp.dentrix.ui.Navegador;
import pe.edu.utp.dentrix.ui.TareasFX;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SalaEsperaController {

    private static final DateTimeFormatter FECHA_LARGA =
            DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es"));
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final String FXML_AGENDA = "/pe/edu/utp/dentrix/fxml/agenda.fxml";
    private static final String FXML_FICHA = "/pe/edu/utp/dentrix/fxml/ficha-clinica.fxml";
    private static final int TOLERANCIA_MINUTOS = 15;

    @FXML
    private Label etiquetaFecha;

    @FXML
    private Label etiquetaPendientes;

    @FXML
    private Label etiquetaEnSala;

    @FXML
    private Label etiquetaAtendidos;

    @FXML
    private Label etiquetaFaltantes;

    @FXML
    private Label etiquetaSelPaciente;

    @FXML
    private Label etiquetaAvatar;

    @FXML
    private Label etiquetaDni;

    @FXML
    private Label etiquetaEdadSexo;

    @FXML
    private Label etiquetaEstadoBadge;

    @FXML
    private Label etiquetaTratamiento;

    @FXML
    private Label etiquetaOdontologo;

    @FXML
    private Label etiquetaConsultorio;

    @FXML
    private Label etiquetaHorario;

    @FXML
    private Label etiquetaObservaciones;

    @FXML
    private Label etiquetaOperacion;

    @FXML
    private TableView<Cita> tablaCola;

    @FXML
    private TableColumn<Cita, String> colHora;

    @FXML
    private TableColumn<Cita, String> colPaciente;

    @FXML
    private TableColumn<Cita, String> colEstado;

    @FXML
    private TableColumn<Cita, String> colAccion;

    private final Map<Integer, String> nombresPacientes = new HashMap<>();
    private final Map<Integer, String> nombresOdontologos = new HashMap<>();
    private final Map<Integer, String> consultoriosPorId = new HashMap<>();
    private final Map<Integer, String> nombresTratamientos = new HashMap<>();
    private Cita citaSeleccionada;
    private Long idSeleccionado;

    @FXML
    private void initialize() {
        colHora.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getHoraInicio() == null ? "" : HORA.format(d.getValue().getHoraInicio())));
        colPaciente.setCellValueFactory(d -> new SimpleStringProperty(
                "CITA".equals(d.getValue().getTipo())
                        ? nombresPacientes.getOrDefault(d.getValue().getPacienteId(), "—") : "—"));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEstado() == null ? "" : d.getValue().getEstado()));
        Badges.aplicarEn(colEstado);
        colAccion.setCellFactory(col -> new TableCell<>() {
            private final Button btnAsistio = new Button("✔ Asistio");
            private final Button btnNoAsistio = new Button("✖ No asistio");
            private final HBox caja = new HBox(6.0, btnAsistio, btnNoAsistio);

            {
                caja.setAlignment(Pos.CENTER);
                btnAsistio.getStyleClass().addAll("boton-mini", "boton-primario");
                btnNoAsistio.getStyleClass().addAll("boton-mini", "boton-peligro");
                btnAsistio.setOnAction(e -> marcarAsistio(getTableRow().getItem()));
                btnNoAsistio.setOnAction(e -> marcarNoAsistio(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(String item, boolean vacio) {
                super.updateItem(item, vacio);
                Cita cita = vacio ? null : getTableRow().getItem();
                if (cita == null || !"CITA".equals(cita.getTipo())) {
                    setGraphic(null);
                    return;
                }
                boolean admite = admiteAsistencia(cita);
                boolean toleranciaVencida = toleranciaVencida(cita);
                btnAsistio.setDisable(!admite);
                btnNoAsistio.setDisable(!(admite && toleranciaVencida));
                String motivo = motivoNoAsistio(admite, toleranciaVencida);
                btnAsistio.setTooltip(new Tooltip(admite
                        ? "Registrar la llegada del paciente (EN_SALA)"
                        : "La cita ya no admite cambios de asistencia"));
                btnNoAsistio.setTooltip(new Tooltip(motivo));
                setTooltip(!admite || !toleranciaVencida ? new Tooltip(motivo) : null);
                setGraphic(caja);
            }
        });
        tablaCola.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaCola.getSelectionModel().selectedItemProperty()
                .addListener((obs, antes, ahora) -> seleccionar(ahora));
        tablaCola.setRowFactory(tabla -> {
            TableRow<Cita> fila = new TableRow<>();
            fila.setOnMouseClicked(evento -> {
                if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2
                        && !fila.isEmpty()) {
                    abrirFicha(fila.getItem());
                }
            });
            return fila;
        });

        etiquetaFecha.setText(sinTildes(FECHA_LARGA.format(LocalDate.now())));
        AppContext contexto = AppContext.get();
        if (!contexto.sesion().activa()) {
            return;
        }
        TareasFX.ejecutar(this::cargarInicial,
                this::aplicarInicial,
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private record DatosSala(Map<Integer, String> pacientes, Map<Integer, String> odontologos,
                             Map<Integer, String> consultorios, Map<Integer, String> tratamientos,
                             List<Cita> citas) {
    }

    private DatosSala cargarInicial() throws DentrixException {
        AppContext contexto = AppContext.get();
        Map<Integer, String> pacientes = new HashMap<>();
        for (Paciente p : contexto.pacientes().listar()) {
            pacientes.put(p.getId(), p.getApellidos() + ", " + p.getNombres());
        }
        Map<Integer, String> odontologos = new HashMap<>();
        for (Usuario u : contexto.usuarios().listarTodos()) {
            odontologos.put(u.getId(), "Dr. " + u.getNombres() + " " + u.getApellidos());
        }
        Map<Integer, String> consultorios = new HashMap<>();
        for (var c : contexto.catalogo().listarConsultorios()) {
            consultorios.put(c.getId(), c.getNombre());
        }
        Map<Integer, String> tratamientos = new HashMap<>();
        for (Tratamiento t : contexto.catalogo().listarTratamientos()) {
            tratamientos.put(t.getId(), t.getNombre());
        }
        return new DatosSala(pacientes, odontologos, consultorios, tratamientos,
                contexto.agenda().agendaDelDia(LocalDate.now()));
    }

    private void aplicarInicial(DatosSala datos) {
        nombresPacientes.clear();
        nombresPacientes.putAll(datos.pacientes());
        nombresOdontologos.clear();
        nombresOdontologos.putAll(datos.odontologos());
        consultoriosPorId.clear();
        consultoriosPorId.putAll(datos.consultorios());
        nombresTratamientos.clear();
        nombresTratamientos.putAll(datos.tratamientos());
        aplicarCitas(datos.citas());
    }

    private void recargar() {
        TareasFX.ejecutar(this::cargarInicial,
                this::aplicarInicial,
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void aplicarCitas(List<Cita> citas) {
        Long mantener = idSeleccionado;
        tablaCola.setItems(FXCollections.observableArrayList(citas));
        int pendientes = 0;
        int enSala = 0;
        int atendidos = 0;
        int faltantes = 0;
        for (Cita c : citas) {
            String estado = c.getEstado();
            if ("PENDIENTE".equals(estado) || "CONFIRMADA".equals(estado)) {
                pendientes++;
            } else if ("EN_SALA".equals(estado)) {
                enSala++;
            } else if ("EN_ATENCION".equals(estado) || "ATENDIDA".equals(estado)) {
                atendidos++;
            } else if ("FALTO".equals(estado)) {
                faltantes++;
            }
        }
        etiquetaPendientes.setText("Pendientes: " + pendientes);
        etiquetaEnSala.setText("En sala: " + enSala);
        etiquetaAtendidos.setText("Atendidos: " + atendidos);
        etiquetaFaltantes.setText("Faltantes: " + faltantes);

        if (mantener == null) {
            citaSeleccionada = null;
            return;
        }
        Cita restaurada = null;
        for (Cita c : citas) {
            if (mantener.equals(c.getId())) {
                restaurada = c;
                break;
            }
        }
        if (restaurada == null) {
            tablaCola.getSelectionModel().clearSelection();
            seleccionar(null);
        } else {
            tablaCola.getSelectionModel().select(restaurada);
            seleccionar(restaurada);
        }
    }

    private void seleccionar(Cita cita) {
        citaSeleccionada = cita;
        idSeleccionado = cita == null ? null : cita.getId();
        if (cita == null) {
            etiquetaSelPaciente.setText("— selecciona una fila —");
            etiquetaAvatar.setText("--");
            etiquetaDni.setText("");
            etiquetaEdadSexo.setText("");
            etiquetaEstadoBadge.setText("—");
            aplicarBadge(null);
            etiquetaTratamiento.setText("—");
            etiquetaOdontologo.setText("—");
            etiquetaConsultorio.setText("—");
            etiquetaHorario.setText("—");
            etiquetaObservaciones.setText("—");
            return;
        }
        aplicarBadge(cita.getEstado());
        etiquetaHorario.setText(horario(cita));
        etiquetaObservaciones.setText(texto(cita.getMotivo()));

        if (!"CITA".equals(cita.getTipo())) {
            etiquetaSelPaciente.setText("Bloqueo de agenda");
            etiquetaAvatar.setText("--");
            etiquetaDni.setText("");
            etiquetaEdadSexo.setText("");
            etiquetaTratamiento.setText("—");
            etiquetaOdontologo.setText(nombresOdontologos.getOrDefault(cita.getUsuarioId(), "—"));
            etiquetaConsultorio.setText(consultoriosPorId.getOrDefault(cita.getConsultorioId(), "—"));
            return;
        }

        Paciente paciente = buscarPaciente(cita.getPacienteId());
        String nombre = paciente == null
                ? "Paciente #" + cita.getPacienteId()
                : paciente.getApellidos() + ", " + paciente.getNombres();
        etiquetaSelPaciente.setText(nombre);
        etiquetaAvatar.setText(iniciales(paciente));
        etiquetaDni.setText(paciente == null || paciente.getDni() == null
                ? "DNI: —" : "DNI: " + paciente.getDni());
        etiquetaEdadSexo.setText(edadSexo(paciente));
        etiquetaTratamiento.setText(cita.getTratamientoId() == null
                ? texto(cita.getMotivo())
                : nombresTratamientos.getOrDefault(cita.getTratamientoId(), "—"));
        etiquetaOdontologo.setText(nombresOdontologos.getOrDefault(cita.getUsuarioId(), "—"));
        etiquetaConsultorio.setText(consultoriosPorId.getOrDefault(cita.getConsultorioId(), "—"));
    }

    private String iniciales(Paciente paciente) {
        if (paciente == null) {
            return "--";
        }
        String nombres = texto(paciente.getNombres());
        String apellidos = texto(paciente.getApellidos());
        String inicial = "";
        if (!nombres.isEmpty()) {
            inicial += nombres.substring(0, 1).toUpperCase();
        }
        if (!apellidos.isEmpty()) {
            inicial += apellidos.substring(0, 1).toUpperCase();
        }
        return inicial.isEmpty() ? "--" : inicial;
    }

    private String edadSexo(Paciente paciente) {
        if (paciente == null) {
            return "Edad: — · Sexo: no registrado";
        }
        String edad = paciente.getFechaNac() == null
                ? "—" : String.valueOf(Period.between(paciente.getFechaNac(), LocalDate.now()).getYears());
        return "Edad: " + edad + " años · Sexo: no registrado";
    }

    private void aplicarBadge(String estado) {
        etiquetaEstadoBadge.getStyleClass().removeAll("badge-estado-gris", "badge-estado-info",
                "badge-estado-sala", "badge-estado-atendido", "badge-estado-falto");
        String clase = "badge-estado-gris";
        String texto = "—";
        if (estado != null) {
            switch (estado) {
                case "EN_SALA" -> {
                    clase = "badge-estado-sala";
                    texto = "En sala";
                }
                case "EN_ATENCION" -> {
                    clase = "badge-estado-info";
                    texto = "En atención";
                }
                case "ATENDIDA" -> {
                    clase = "badge-estado-atendido";
                    texto = "Atendido";
                }
                case "FALTO" -> {
                    clase = "badge-estado-falto";
                    texto = "No asistió";
                }
                case "CONFIRMADA" -> {
                    clase = "badge-estado-info";
                    texto = "Confirmada";
                }
                case "CANCELADA" -> {
                    clase = "badge-estado-falto";
                    texto = "Cancelada";
                }
                case "REPROGRAMADA" -> {
                    clase = "badge-estado-sala";
                    texto = "Reprogramada";
                }
                default -> {
                    clase = "badge-estado-gris";
                    texto = "Pendiente";
                }
            }
        }
        etiquetaEstadoBadge.setText(texto);
        etiquetaEstadoBadge.getStyleClass().add(clase);
    }

    private Paciente buscarPaciente(Integer pacienteId) {
        if (pacienteId == null) {
            return null;
        }
        try {
            return AppContext.get().pacientes().buscarPorId(pacienteId);
        } catch (DentrixException e) {
            return null;
        }
    }


    private String horario(Cita cita) {
        if (cita.getHoraInicio() == null) {
            return "—";
        }
        String fin = cita.getHoraFin() == null ? "" : HORA.format(cita.getHoraFin());
        return HORA.format(cita.getHoraInicio()) + (fin.isEmpty() ? "" : " - " + fin);
    }

    private String estadoTexto(String estado) {
        return estado == null ? "—" : estado.replace('_', ' ');
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    private boolean admiteAsistencia(Cita cita) {
        return "PENDIENTE".equals(cita.getEstado()) || "CONFIRMADA".equals(cita.getEstado());
    }

    private boolean toleranciaVencida(Cita cita) {
        if (cita.getFechaCita() == null || cita.getHoraInicio() == null) {
            return false;
        }
        LocalDateTime inicio = LocalDateTime.of(cita.getFechaCita(), cita.getHoraInicio());
        return !LocalDateTime.now().isBefore(inicio.plusMinutes(TOLERANCIA_MINUTOS));
    }

    private String motivoNoAsistio(boolean admite, boolean toleranciaVencida) {
        if (!admite) {
            return "La cita ya no admite cambios de asistencia";
        }
        if (!toleranciaVencida) {
            return "R24: la inasistencia se habilita 15 minutos despues de la hora de la cita";
        }
        return "Registrar la inasistencia del paciente (R24)";
    }

    private void marcarAsistio(Cita cita) {
        if (cita == null) {
            return;
        }
        seleccionarFila(cita);
        Long id = cita.getId();
        TareasFX.ejecutar(() -> {
            AppContext.get().agenda().marcarLlegada(id);
            return "Paciente en sala de espera";
        }, mensaje -> {
            indicar(mensaje, true);
            recargar();
        }, error -> {
            indicar(TareasFX.mensaje(error), false);
            seleccionar(citaSeleccionada);
        });
    }

    private void marcarNoAsistio(Cita cita) {
        if (cita == null) {
            return;
        }
        seleccionarFila(cita);
        Long id = cita.getId();
        TareasFX.ejecutar(() -> {
            AppContext.get().agenda().marcarFalto(id);
            return "Paciente marcado como FALTO";
        }, mensaje -> {
            indicar(mensaje, true);
            recargar();
        }, error -> {
            indicar(TareasFX.mensaje(error), false);
            seleccionar(citaSeleccionada);
        });
    }

    private void seleccionarFila(Cita cita) {
        idSeleccionado = cita.getId();
        tablaCola.getSelectionModel().select(cita);
        seleccionar(cita);
    }

    private void indicar(String mensaje, boolean ok) {
        etiquetaOperacion.setText(mensaje);
        etiquetaOperacion.getStyleClass().removeAll("etiqueta-error", "etiqueta-ok");
        etiquetaOperacion.getStyleClass().add(ok ? "etiqueta-ok" : "etiqueta-error");
    }

    private void abrirFicha(Cita cita) {
        if (cita == null || cita.getPacienteId() == null) {
            return;
        }
        Paciente paciente = buscarPaciente(cita.getPacienteId());
        if (paciente == null) {
            return;
        }
        AppContext.get().sesion().setPacienteEnAtencion(paciente);
        Navegador.ir(FXML_FICHA);
    }

    @FXML
    private void onVerAgendaClick() {
        Navegador.ir(FXML_AGENDA);
    }

    @FXML
    private void onRefrescarClick() {
        idSeleccionado = null;
        tablaCola.getSelectionModel().clearSelection();
        seleccionar(null);
        etiquetaOperacion.setText("");
        recargar();
    }

    @FXML
    private void onVolverClick() {
        Navegador.ir(Navegador.INICIO);
    }

    private String sinTildes(String valor) {
        String normalizado = Normalizer.normalize(valor, Normalizer.Form.NFD);
        return normalizado.replaceAll("\\p{M}", "");
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Recepcion / Asistencia");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
