package pe.edu.utp.dentrix.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
import javafx.scene.layout.HBox;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Configuracion;
import pe.edu.utp.dentrix.model.DiagnosticoCie10;
import pe.edu.utp.dentrix.model.Tratamiento;
import pe.edu.utp.dentrix.ui.Badges;
import pe.edu.utp.dentrix.ui.Navegador;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

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
    private Label etiquetaTratModo;

    @FXML
    private Label etiquetaTratError;

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
                d.getValue().getPrecio() == null ? "" : d.getValue().getPrecio().toPlainString()));
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

        AppContext contexto = AppContext.get();
        if (!contexto.sesion().activa()) {
            return;
        }
        recargarTratamientos();
        recargarCie();
        cargarConfiguracion();
    }

    private void recargarTratamientos() {
        try {
            tablaTratamientos.setItems(FXCollections.observableArrayList(
                    AppContext.get().catalogo().listarTratamientos()));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void recargarCie() {
        try {
            tablaCie.setItems(FXCollections.observableArrayList(
                    AppContext.get().catalogo().listarDiagnosticos()));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void cargarConfiguracion() {
        try {
            Configuracion config = AppContext.get().catalogo().obtenerConfiguracion();
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
        } catch (DentrixException e) {
            etiquetaConfigResultado.setText(e.getMessage());
        }
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
        campoPrecio.setText(t.getPrecio() == null ? "" : t.getPrecio().toPlainString());
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
            try {
                tratamiento.setPrecio(new BigDecimal(texto(campoPrecio) == null ? "" : texto(campoPrecio)));
            } catch (NumberFormatException e) {
                throw new DentrixException("El precio debe ser un número (ej. 80.00)");
            }
            if (tratEnEdicion == null) {
                AppContext.get().catalogo().registrarTratamiento(tratamiento);
            } else {
                AppContext.get().catalogo().editarTratamiento(tratamiento);
            }
            onNuevoTratamientoClick();
            recargarTratamientos();
        } catch (DentrixException e) {
            etiquetaTratError.setText(e.getMessage());
        }
    }

    private void inactivarTratamiento(Tratamiento t) {
        if (t == null) {
            return;
        }
        try {
            AppContext.get().catalogo().inactivarTratamiento(t.getId());
            recargarTratamientos();
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onNuevoCieClick() {
        cieEnEdicion = null;
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
        try {
            DiagnosticoCie10 diagnostico = new DiagnosticoCie10();
            diagnostico.setCodigo(texto(campoCodCie));
            diagnostico.setDescripcion(texto(campoDescCie));
            diagnostico.setCapitulo(texto(campoCapCie));
            diagnostico.setEsFrecuente(checkFrecuente.isSelected());
            if (cieEnEdicion == null) {
                AppContext.get().catalogo().registrarDiagnostico(diagnostico);
            } else {
                AppContext.get().catalogo().editarDiagnostico(diagnostico);
            }
            campoCodCie.setDisable(false);
            onNuevoCieClick();
            recargarCie();
        } catch (DentrixException e) {
            etiquetaCieError.setText(e.getMessage());
        }
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
                try {
                    AppContext.get().catalogo().eliminarDiagnostico(d.getCodigo());
                    if (cieEnEdicion != null && cieEnEdicion.getCodigo().equals(d.getCodigo())) {
                        onNuevoCieClick();
                    }
                    recargarCie();
                } catch (DentrixException e) {
                    mostrarError(e.getMessage());
                }
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
            AppContext.get().catalogo().actualizarConfiguracion(config);
            etiquetaConfigResultado.setText("Datos de la clínica guardados");
            etiquetaConfigResultado.getStyleClass().removeAll("etiqueta-error");
            etiquetaConfigResultado.getStyleClass().add("etiqueta-ok");
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
        recargarTratamientos();
        recargarCie();
        cargarConfiguracion();
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
