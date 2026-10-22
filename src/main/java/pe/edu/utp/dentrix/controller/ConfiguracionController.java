package pe.edu.utp.dentrix.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Configuracion;
import pe.edu.utp.dentrix.model.DiagnosticoCie10;
import pe.edu.utp.dentrix.model.Tarifario;
import pe.edu.utp.dentrix.model.Tratamiento;
import pe.edu.utp.dentrix.ui.Badges;
import pe.edu.utp.dentrix.ui.Navegador;
import pe.edu.utp.dentrix.ui.TareasFX;
import pe.edu.utp.dentrix.ui.TarifarioDialog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConfiguracionController {

    private static final List<String> ESPECIALIDADES = List.of("Operatoria", "Periodoncia", "Ortodoncia",
            "Cirugía", "Endodoncia", "Prostodoncia", "Odontopediatría", "Diagnóstico");
    private static final List<String> CALCULOS = List.of("PIEZA", "SESION", "MENSUAL");

    @FXML
    private TextField campoCodTrat;

    @FXML
    private TextField campoNomTrat;

    @FXML
    private ComboBox<String> comboEspecialidad;

    @FXML
    private ComboBox<String> comboCalculo;

    @FXML
    private TextField campoPrecio;

    @FXML
    private Button btnNuevaVersionTarifario;

    @FXML
    private Label etiquetaTratModo;

    @FXML
    private Label etiquetaTratError;

    @FXML
    private Label etiquetaVersionTarifario;

    @FXML
    private Label etiquetaVigenciaTarifario;

    @FXML
    private Label etiquetaIgvTarifario;

    @FXML
    private TableView<Tratamiento> tablaTratamientos;

    @FXML
    private TableColumn<Tratamiento, String> colTCodigo;

    @FXML
    private TableColumn<Tratamiento, String> colTNombre;

    @FXML
    private TableColumn<Tratamiento, String> colTEspecialidad;

    @FXML
    private TableColumn<Tratamiento, String> colTCalculo;

    @FXML
    private TableColumn<Tratamiento, String> colTPrecio;

    @FXML
    private TableColumn<Tratamiento, String> colTEstado;

    @FXML
    private TableColumn<Tratamiento, String> colTAcciones;

    @FXML
    private TextField campoCodCie;

    @FXML
    private TextField campoDescCie;

    @FXML
    private TextField campoBusquedaCie;

    @FXML
    private Label etiquetaCieCodigo;

    @FXML
    private Label etiquetaCieGrupo;

    @FXML
    private Label etiquetaCieDescripcion;

    @FXML
    private Label etiquetaCieFrecuente;

    @FXML
    private TextField campoCapCie;

    @FXML
    private CheckBox checkFrecuente;

    @FXML
    private Label etiquetaCieModo;

    @FXML
    private Label etiquetaCieError;

    @FXML
    private TableView<DiagnosticoCie10> tablaCie;

    @FXML
    private TableColumn<DiagnosticoCie10, String> colCCodigo;

    @FXML
    private TableColumn<DiagnosticoCie10, String> colCDesc;

    @FXML
    private TableColumn<DiagnosticoCie10, String> colCCap;

    @FXML
    private TableColumn<DiagnosticoCie10, String> colCFrec;

    @FXML
    private TableColumn<DiagnosticoCie10, String> colCAcciones;

    @FXML
    private TextField campoRazonSocial;

    @FXML
    private TextField campoRuc;

    @FXML
    private TextField campoDireccion;

    @FXML
    private TextField campoTelefono;

    @FXML
    private TextField campoEmail;

    @FXML
    private TextField campoRepresentante;

    @FXML
    private TextField campoMedicoDirector;

    @FXML
    private TextField campoLicSanitaria;

    @FXML
    private TextField campoLicMunicipal;

    @FXML
    private TextField campoSerieBoleta;

    @FXML
    private TextField campoHoraApertura;

    @FXML
    private TextField campoHoraCierre;

    @FXML
    private TextField campoAlmuerzoIni;

    @FXML
    private TextField campoAlmuerzoFin;

    @FXML
    private TextField campoIgv;

    @FXML
    private Label etiquetaConfigResultado;

    @FXML
    private Label etiquetaRespaldoResultado;

    private final Map<Integer, BigDecimal> preciosVigentes = new HashMap<>();
    private final ObservableList<DiagnosticoCie10> diagnosticos = FXCollections.observableArrayList();
    private FilteredList<DiagnosticoCie10> diagnosticosFiltrados;
    private Tratamiento tratEnEdicion;
    private DiagnosticoCie10 cieEnEdicion;

    @FXML
    private void initialize() {
        comboEspecialidad.setItems(FXCollections.observableArrayList(ESPECIALIDADES));
        comboEspecialidad.setEditable(true);
        comboCalculo.setItems(FXCollections.observableArrayList(CALCULOS));
        comboCalculo.setValue("PIEZA");

        colTCodigo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCodigo()));
        colTNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colTEspecialidad.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEspecialidad()));
        colTCalculo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCalculo()));
        colTPrecio.setCellValueFactory(d -> new SimpleStringProperty(
                precioVigenteTexto(d.getValue().getId())));
        colTEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado()));
        Badges.aplicarEn(colTEstado);
        colTAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnInactivar = new Button("Inactivar");
            private final HBox caja = new HBox(6, btnEditar, btnInactivar);

            {
                btnEditar.getStyleClass().add("boton-mini");
                btnInactivar.getStyleClass().addAll("boton-mini", "boton-peligro");
                btnEditar.setOnAction(e -> editarTratamiento(getTableRow().getItem()));
                btnInactivar.setOnAction(e -> inactivarTratamiento(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(String item, boolean vacio) {
                super.updateItem(item, vacio);
                setGraphic(vacio ? null : caja);
            }
        });
        tablaTratamientos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        colCCodigo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCodigo()));
        colCDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescripcion()));
        colCCap.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCapitulo() == null ? "" : d.getValue().getCapitulo()));
        colCFrec.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isEsFrecuente() ? "Sí" : "No"));
        colCAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnEliminar = new Button("Eliminar");
            private final HBox caja = new HBox(6, btnEditar, btnEliminar);

            {
                btnEditar.getStyleClass().add("boton-mini");
                btnEliminar.getStyleClass().addAll("boton-mini", "boton-peligro");
                btnEditar.setOnAction(e -> editarCie(getTableRow().getItem()));
                btnEliminar.setOnAction(e -> eliminarCie(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(String item, boolean vacio) {
                super.updateItem(item, vacio);
                setGraphic(vacio ? null : caja);
            }
        });
        tablaCie.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        diagnosticosFiltrados = new FilteredList<>(diagnosticos, fila -> true);
        tablaCie.setItems(diagnosticosFiltrados);
        campoBusquedaCie.textProperty().addListener((obs, antes, ahora) -> filtrarDiagnosticos(ahora));
        tablaCie.getSelectionModel().selectedItemProperty()
                .addListener((obs, antes, ahora) -> mostrarDetalleCie(ahora));
        mostrarDetalleCie(null);

        AppContext contexto = AppContext.get();
        if (!contexto.sesion().activa()) {
            return;
        }
        TareasFX.ejecutar(this::cargarDatos,
                this::aplicarDatos,
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private record DatosConfiguracion(List<Tratamiento> tratamientos, List<DiagnosticoCie10> cie,
                                      Configuracion configuracion) {
    }

    private DatosConfiguracion cargarDatos() throws DentrixException {
        var catalogo = AppContext.get().catalogo();
        return new DatosConfiguracion(catalogo.listarTratamientos(), catalogo.listarDiagnosticos(),
                catalogo.obtenerConfiguracion());
    }

    private void aplicarDatos(DatosConfiguracion datos) {
        cargarPreciosVigentes();
        tablaTratamientos.setItems(FXCollections.observableArrayList(datos.tratamientos()));
        diagnosticos.setAll(datos.cie());
        aplicarConfiguracion(datos.configuracion());
        actualizarVersionTarifario();
    }

    private void recargarTratamientos() {
        TareasFX.ejecutar(() -> AppContext.get().catalogo().listarTratamientos(),
                lista -> {
                    cargarPreciosVigentes();
                    actualizarVersionTarifario();
                    tablaTratamientos.setItems(FXCollections.observableArrayList(lista));
                },
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void cargarPreciosVigentes() {
        preciosVigentes.clear();
        try {
            preciosVigentes.putAll(AppContext.get().tarifario().preciosVigentes());
        } catch (DentrixException e) {
            etiquetaTratError.setText("No se pudo leer el tarifario vigente: " + e.getMessage());
        }
    }

    private String precioVigenteTexto(Integer tratamientoId) {
        BigDecimal precio = preciosVigentes.get(tratamientoId);
        return precio == null ? "—" : "S/ " + precio.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void recargarCie() {
        TareasFX.ejecutar(() -> AppContext.get().catalogo().listarDiagnosticos(),
                lista -> {
                    diagnosticos.setAll(lista);
                    mostrarDetalleCie(tablaCie.getSelectionModel().getSelectedItem());
                },
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void filtrarDiagnosticos(String texto) {
        String busqueda = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        diagnosticosFiltrados.setPredicate(d -> busqueda.isEmpty()
                || contiene(d.getCodigo(), busqueda)
                || contiene(d.getDescripcion(), busqueda)
                || contiene(d.getCapitulo(), busqueda));
    }

    private boolean contiene(String valor, String busqueda) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(busqueda);
    }

    private void mostrarDetalleCie(DiagnosticoCie10 diagnostico) {
        if (etiquetaCieCodigo == null) {
            return;
        }
        if (diagnostico == null) {
            etiquetaCieCodigo.setText("—");
            etiquetaCieGrupo.setText("Selecciona un diagnóstico del nomenclátor");
            etiquetaCieDescripcion.setText("El catálogo CIE-10 es de solo consulta desde la ficha clínica: "
                    + "el código no impone un tratamiento.");
            etiquetaCieFrecuente.setText("SIN SELECCION");
            etiquetaCieFrecuente.getStyleClass().removeAll("badge-amarillo", "badge-gris");
            etiquetaCieFrecuente.getStyleClass().add("badge-gris");
            return;
        }
        etiquetaCieCodigo.setText(diagnostico.getCodigo());
        etiquetaCieGrupo.setText(diagnostico.getCapitulo() == null ? "Sin capítulo" : diagnostico.getCapitulo());
        etiquetaCieDescripcion.setText(diagnostico.getDescripcion());
        etiquetaCieFrecuente.setText(diagnostico.isEsFrecuente() ? "FRECUENTE" : "ESTANDAR");
        etiquetaCieFrecuente.getStyleClass().removeAll("badge-amarillo", "badge-gris");
        etiquetaCieFrecuente.getStyleClass().add(diagnostico.isEsFrecuente() ? "badge-amarillo" : "badge-gris");
    }

    private void actualizarVersionTarifario() {
        try {
            var publicado = AppContext.get().tarifario().versionPublicada();
            if (publicado.isEmpty()) {
                etiquetaVersionTarifario.setText("Sin versión publicada");
                etiquetaVigenciaTarifario.setText("—");
                return;
            }
            Tarifario tarifario = publicado.get();
            etiquetaVersionTarifario.setText("Versión v" + tarifario.getVersion()
                    + (tarifario.getEstado() == null ? "" : " · " + tarifario.getEstado()));
            LocalDate desdeFecha = tarifario.getVigenciaDesde();
            LocalDate hastaFecha = tarifario.getVigenciaHasta();
            etiquetaVigenciaTarifario.setText(hastaFecha == null
                    ? "Desde " + formatearFecha(desdeFecha) + " (vigente)"
                    : formatearFecha(desdeFecha) + " al " + formatearFecha(hastaFecha));
        } catch (DentrixException e) {
            etiquetaVersionTarifario.setText("—");
            etiquetaVigenciaTarifario.setText("—");
            etiquetaTratError.setText("No se pudo leer la versión del tarifario: " + e.getMessage());
        }
    }

    private String formatearFecha(LocalDate fecha) {
        return fecha == null ? "—" : fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private void actualizarIgv(BigDecimal igv) {
        if (etiquetaIgvTarifario == null) {
            return;
        }
        etiquetaIgvTarifario.setText(igv == null
                ? "IGV sin configurar"
                : "IGV " + igv.stripTrailingZeros().toPlainString() + " %");
    }

    private void aplicarConfiguracion(Configuracion config) {
        campoRazonSocial.setText(config.getRazonSocial());
        campoRuc.setText(config.getRuc());
        campoDireccion.setText(config.getDireccion());
        campoTelefono.setText(config.getTelefono());
        campoEmail.setText(config.getEmail());
        campoRepresentante.setText(config.getRepresentanteLegal());
        campoMedicoDirector.setText(config.getMedicoDirector());
        campoLicSanitaria.setText(config.getLicenciaSanitaria());
        campoLicMunicipal.setText(config.getLicenciaMunicipal());
        campoSerieBoleta.setText(config.getSerieBoleta());
        campoHoraApertura.setText(formatearHora(config.getHoraApertura()));
        campoHoraCierre.setText(formatearHora(config.getHoraCierre()));
        campoAlmuerzoIni.setText(formatearHora(config.getAlmuerzoInicio()));
        campoAlmuerzoFin.setText(formatearHora(config.getAlmuerzoFin()));
        campoIgv.setText(config.getIgvPorcentaje() == null ? "" : config.getIgvPorcentaje().toPlainString());
        actualizarIgv(config.getIgvPorcentaje());
    }

    private String formatearHora(LocalTime hora) {
        return hora == null ? "" : hora.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @FXML
    private void onNuevoTratamientoClick() {
        tratEnEdicion = null;
        etiquetaTratModo.setText("NUEVO");
        campoCodTrat.clear();
        campoNomTrat.clear();
        comboEspecialidad.setValue(null);
        comboCalculo.setValue("PIEZA");
        campoPrecio.clear();
        campoPrecio.setDisable(false);
        campoPrecio.setTooltip(null);
        etiquetaTratError.setText("");
        campoCodTrat.requestFocus();
    }

    private void editarTratamiento(Tratamiento t) {
        if (t == null) {
            return;
        }
        tratEnEdicion = t;
        etiquetaTratModo.setText("EDITANDO #" + t.getId());
        campoCodTrat.setText(t.getCodigo());
        campoNomTrat.setText(t.getNombre());
        comboEspecialidad.setValue(t.getEspecialidad());
        comboCalculo.setValue(t.getCalculo());
        campoPrecio.setText(precioVigenteTexto(t.getId()));
        campoPrecio.setDisable(true);
        campoPrecio.setTooltip(new Tooltip("El precio se cambia abriendo una versión nueva del tarifario"));
        etiquetaTratError.setText("");
    }

    @FXML
    private void onGuardarTratamientoClick() {
        try {
            Tratamiento tratamiento = new Tratamiento();
            if (tratEnEdicion != null) {
                tratamiento.setId(tratEnEdicion.getId());
                tratamiento.setEstado(tratEnEdicion.getEstado());
            }
            tratamiento.setCodigo(texto(campoCodTrat));
            tratamiento.setNombre(texto(campoNomTrat));
            String especialidad = comboEspecialidad.getEditor() == null
                    ? comboEspecialidad.getValue()
                    : comboEspecialidad.getEditor().getText();
            if (especialidad != null && !especialidad.isBlank()) {
                tratamiento.setEspecialidad(especialidad.trim());
            }
            tratamiento.setCalculo(comboCalculo.getValue());
            if (campoPrecio.isDisable() && tratEnEdicion != null) {
                tratamiento.setPrecio(tratEnEdicion.getPrecio());
            } else {
                try {
                    tratamiento.setPrecio(new BigDecimal(texto(campoPrecio) == null ? "" : texto(campoPrecio)));
                } catch (NumberFormatException e) {
                    throw new DentrixException("El precio debe ser un número (ej. 80.00)");
                }
            }
            boolean nuevo = tratEnEdicion == null;
            TareasFX.ejecutar(() -> {
                if (nuevo) {
                    AppContext.get().catalogo().registrarTratamiento(tratamiento);
                } else {
                    AppContext.get().catalogo().editarTratamiento(tratamiento);
                }
                return null;
            }, ok -> {
                onNuevoTratamientoClick();
                recargarTratamientos();
            }, error -> etiquetaTratError.setText(TareasFX.mensaje(error)));
        } catch (DentrixException e) {
            etiquetaTratError.setText(e.getMessage());
        }
    }

    private void inactivarTratamiento(Tratamiento t) {
        if (t == null) {
            return;
        }
        TareasFX.ejecutar(() -> {
            AppContext.get().catalogo().inactivarTratamiento(t.getId());
            return null;
        }, ok -> recargarTratamientos(), error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onNuevaVersionTarifarioClick() {
        etiquetaTratError.setText("");
        try {
            TarifarioDialog.mostrar(btnNuevaVersionTarifario.getScene().getWindow());
            recargarTratamientos();
        } catch (RuntimeException e) {
            etiquetaTratError.setText("No se pudo abrir el tarifario: " + TareasFX.mensaje(e));
        }
    }

    @FXML
    private void onLimpiarBusquedaCieClick() {
        campoBusquedaCie.clear();
        tablaCie.getSelectionModel().clearSelection();
        mostrarDetalleCie(null);
    }

    @FXML
    private void onNuevoCieClick() {        cieEnEdicion = null;
        etiquetaCieModo.setText("NUEVO");
        campoCodCie.setDisable(false);
        campoCodCie.clear();
        campoDescCie.clear();
        campoCapCie.clear();
        checkFrecuente.setSelected(false);
        etiquetaCieError.setText("");
        campoCodCie.requestFocus();
    }

    private void editarCie(DiagnosticoCie10 d) {
        if (d == null) {
            return;
        }
        cieEnEdicion = d;
        etiquetaCieModo.setText("EDITANDO " + d.getCodigo());
        campoCodCie.setText(d.getCodigo());
        campoCodCie.setDisable(true);
        campoDescCie.setText(d.getDescripcion());
        campoCapCie.setText(d.getCapitulo());
        checkFrecuente.setSelected(d.isEsFrecuente());
        etiquetaCieError.setText("");
    }

    @FXML
    private void onGuardarCieClick() {
        DiagnosticoCie10 diagnostico = new DiagnosticoCie10();
        diagnostico.setCodigo(texto(campoCodCie));
        diagnostico.setDescripcion(texto(campoDescCie));
        diagnostico.setCapitulo(texto(campoCapCie));
        diagnostico.setEsFrecuente(checkFrecuente.isSelected());
        boolean nuevo = cieEnEdicion == null;
        TareasFX.ejecutar(() -> {
            if (nuevo) {
                AppContext.get().catalogo().registrarDiagnostico(diagnostico);
            } else {
                AppContext.get().catalogo().editarDiagnostico(diagnostico);
            }
            return null;
        }, ok -> {
            campoCodCie.setDisable(false);
            onNuevoCieClick();
            recargarCie();
        }, error -> etiquetaCieError.setText(TareasFX.mensaje(error)));
    }

    private void eliminarCie(DiagnosticoCie10 d) {
        if (d == null) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("CIE-10");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Eliminar el diagnóstico " + d.getCodigo() + "?");
        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta == javafx.scene.control.ButtonType.OK) {
                String codigo = d.getCodigo();
                TareasFX.ejecutar(() -> {
                    AppContext.get().catalogo().eliminarDiagnostico(codigo);
                    return null;
                }, ok -> {
                    if (cieEnEdicion != null && codigo.equals(cieEnEdicion.getCodigo())) {
                        onNuevoCieClick();
                    }
                    recargarCie();
                }, error -> mostrarError(TareasFX.mensaje(error)));
            }
        });
    }

    @FXML
    private void onGuardarConfigClick() {
        try {
            Configuracion config = AppContext.get().catalogo().obtenerConfiguracion();
            config.setRazonSocial(texto(campoRazonSocial));
            config.setRuc(texto(campoRuc));
            config.setDireccion(texto(campoDireccion));
            config.setTelefono(texto(campoTelefono));
            config.setEmail(texto(campoEmail));
            config.setRepresentanteLegal(texto(campoRepresentante));
            config.setMedicoDirector(texto(campoMedicoDirector));
            config.setLicenciaSanitaria(texto(campoLicSanitaria));
            config.setLicenciaMunicipal(texto(campoLicMunicipal));
            config.setSerieBoleta(texto(campoSerieBoleta));
            config.setHoraApertura(parseHora(campoHoraApertura, "apertura"));
            config.setHoraCierre(parseHora(campoHoraCierre, "cierre"));
            config.setAlmuerzoInicio(parseHora(campoAlmuerzoIni, "inicio de almuerzo"));
            config.setAlmuerzoFin(parseHora(campoAlmuerzoFin, "fin de almuerzo"));
            try {
                config.setIgvPorcentaje(new BigDecimal(texto(campoIgv) == null ? "" : texto(campoIgv)));
            } catch (NumberFormatException e) {
                throw new DentrixException("El IGV debe ser un número (ej. 18)");
            }
            TareasFX.ejecutar(() -> {
                AppContext.get().catalogo().actualizarConfiguracion(config);
                return null;
            }, ok -> {
                etiquetaConfigResultado.setText("Datos de la clínica guardados");
                etiquetaConfigResultado.getStyleClass().removeAll("etiqueta-error");
                etiquetaConfigResultado.getStyleClass().add("etiqueta-ok");
                actualizarIgv(config.getIgvPorcentaje());
            }, error -> {
                etiquetaConfigResultado.setText(TareasFX.mensaje(error));
                etiquetaConfigResultado.getStyleClass().removeAll("etiqueta-ok");
                etiquetaConfigResultado.getStyleClass().add("etiqueta-error");
            });
        } catch (DentrixException e) {
            etiquetaConfigResultado.setText(e.getMessage());
            etiquetaConfigResultado.getStyleClass().removeAll("etiqueta-ok");
            etiquetaConfigResultado.getStyleClass().add("etiqueta-error");
        }
    }

    @FXML
    private void onRespaldarClick() {
        etiquetaRespaldoResultado.setText("Generando respaldo con pg_dump... puede tardar unos segundos.");
        etiquetaRespaldoResultado.getStyleClass().removeAll("etiqueta-ok", "etiqueta-error");
        new Thread(() -> {
            try {
                java.io.File archivo = AppContext.get().respaldos().generarBackup();
                AppContext.get().respaldos().verificarLegible(archivo);
                String mensaje = "✔ Respaldo EXITOSO (RF-15): " + archivo.getAbsolutePath()
                        + " (" + AppContext.get().respaldos().pesoLegible(archivo) + ") — verificado.";
                javafx.application.Platform.runLater(() -> {
                    etiquetaRespaldoResultado.setText(mensaje);
                    etiquetaRespaldoResultado.getStyleClass().add("etiqueta-ok");
                });
            } catch (Exception e) {
                String error = e.getMessage();
                javafx.application.Platform.runLater(() -> {
                    etiquetaRespaldoResultado.setText("✖ " + error);
                    etiquetaRespaldoResultado.getStyleClass().add("etiqueta-error");
                });
            }
        }, "dentrix-backup").start();
    }

    private LocalTime parseHora(TextField campo, String nombre) throws DentrixException {
        String valor = texto(campo);
        if (valor == null) {
            return null;
        }
        try {
            return LocalTime.parse(valor, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            throw new DentrixException("La hora de " + nombre + " debe tener formato HH:mm (ej. 08:00)");
        }
    }

    @FXML
    private void onRefrescarClick() {
        TareasFX.ejecutar(this::cargarDatos,
                this::aplicarDatos,
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    @FXML
    private void onVolverClick() {
        Navegador.ir(Navegador.INICIO);
    }

    private String texto(TextField campo) {
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        return valor.isEmpty() ? null : valor;
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Configuración");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
