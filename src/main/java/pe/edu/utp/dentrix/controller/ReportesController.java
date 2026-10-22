package pe.edu.utp.dentrix.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.dto.KpiDia;
import pe.edu.utp.dentrix.dto.KpiMes;
import pe.edu.utp.dentrix.dto.ProductividadOdontologo;
import pe.edu.utp.dentrix.dto.RankingTratamiento;
import pe.edu.utp.dentrix.dto.SaldoPaciente;
import pe.edu.utp.dentrix.model.Paciente;
import pe.edu.utp.dentrix.model.Pago;
import pe.edu.utp.dentrix.ui.TareasFX;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ReportesController {

    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private DatePicker campoFecha;

    @FXML
    private DatePicker campoDesde;

    @FXML
    private DatePicker campoHasta;

    @FXML
    private Label kpiCitas;

    @FXML
    private Label kpiEnSala;

    @FXML
    private Label kpiRecaudado;

    @FXML
    private Label kpiDeudores;

    @FXML
    private Label kpiMesRecaudado;

    @FXML
    private Label kpiMesAtendidas;

    @FXML
    private Label kpiMesNoShow;

    @FXML
    private Label kpiMesModalidades;

    @FXML
    private Label kpiEfectivo;

    @FXML
    private Label kpiYape;

    @FXML
    private Label kpiPlin;

    @FXML
    private Label kpiTarjeta;

    @FXML
    private Label kpiSaldoTotal;

    @FXML
    private Label kpiSaldoPacientes;

    @FXML
    private TableView<ProductividadOdontologo> tablaProductividad;

    @FXML
    private TableColumn<ProductividadOdontologo, String> colProdOdontologo;

    @FXML
    private TableColumn<ProductividadOdontologo, String> colProdAtendidas;

    @FXML
    private TableColumn<ProductividadOdontologo, String> colProdFaltas;

    @FXML
    private TableColumn<ProductividadOdontologo, String> colProdRecaudado;

    @FXML
    private TableView<SaldoPaciente> tablaSaldos;

    @FXML
    private TableColumn<SaldoPaciente, String> colSaldoHc;

    @FXML
    private TableColumn<SaldoPaciente, String> colSaldoNombre;

    @FXML
    private TableColumn<SaldoPaciente, String> colSaldoMonto;

    @FXML
    private TableColumn<SaldoPaciente, String> colSaldoDocs;

    @FXML
    private Label etiquetaSaldoTotal;

    @FXML
    private TableView<RankingTratamiento> tablaRanking;

    @FXML
    private TableColumn<RankingTratamiento, String> colRankCodigo;

    @FXML
    private TableColumn<RankingTratamiento, String> colRankNombre;

    @FXML
    private TableColumn<RankingTratamiento, String> colRankUnidades;

    @FXML
    private TableColumn<RankingTratamiento, String> colRankMonto;

    @FXML
    private TableView<Pago> tablaDocumentos;

    @FXML
    private TableColumn<Pago, String> colDocNumero;

    @FXML
    private TableColumn<Pago, String> colDocFecha;

    @FXML
    private TableColumn<Pago, String> colDocPaciente;

    @FXML
    private TableColumn<Pago, String> colDocMonto;

    @FXML
    private TableColumn<Pago, String> colDocSaldo;

    @FXML
    private TableColumn<Pago, String> colDocEstado;

    @FXML
    private Label etiquetaDocsTotal;

    private final Map<Integer, String> nombresPacientes = new HashMap<>();
    private final AtomicInteger generacionTodo = new AtomicInteger();
    private final AtomicInteger generacionKpis = new AtomicInteger();
    private final AtomicInteger generacionPeriodo = new AtomicInteger();

    @FXML
    private void initialize() {
        colProdOdontologo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nombre()));
        colProdAtendidas.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().atendidas())));
        colProdFaltas.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().faltas())));
        colProdRecaudado.setCellValueFactory(d -> new SimpleStringProperty(
                soles(d.getValue().recaudado())));

        colSaldoHc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().codigoHc()));
        colSaldoNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nombre()));
        colSaldoMonto.setCellValueFactory(d -> new SimpleStringProperty(
                soles(d.getValue().saldo())));
        colSaldoDocs.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().documentos())));

        colRankCodigo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().codigo()));
        colRankNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nombre()));
        colRankUnidades.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().unidades())));
        colRankMonto.setCellValueFactory(d -> new SimpleStringProperty(soles(d.getValue().monto())));

        colDocNumero.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNumero()));
        colDocFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() == null ? "" : FECHA_CORTA.format(d.getValue().getFecha())));
        colDocPaciente.setCellValueFactory(d -> new SimpleStringProperty(
                nombresPacientes.getOrDefault(d.getValue().getPacienteId(), "#"
                        + d.getValue().getPacienteId())));
        colDocMonto.setCellValueFactory(d -> new SimpleStringProperty(
                soles(d.getValue().getMonto())));
        colDocSaldo.setCellValueFactory(d -> new SimpleStringProperty(
                soles(d.getValue().getSaldoDeudor())));
        colDocEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstadoPago()));
        pe.edu.utp.dentrix.ui.Badges.aplicarEn(colDocEstado);

        tablaRanking.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaProductividad.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaSaldos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaDocumentos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        AppContext contexto = AppContext.get();
        if (!contexto.sesion().activa()) {
            return;
        }
        campoFecha.setValue(LocalDate.now());
        campoDesde.setValue(LocalDate.now().withDayOfMonth(1));
        campoHasta.setValue(LocalDate.now());
        campoFecha.valueProperty().addListener((obs, antes, ahora) -> recargarKpis());
        campoDesde.valueProperty().addListener((obs, antes, ahora) -> recargarPeriodo());
        campoHasta.valueProperty().addListener((obs, antes, ahora) -> recargarPeriodo());
        cargarTodo();
    }

    private void recargarKpis() {
        LocalDate fecha = campoFecha.getValue();
        if (fecha == null) {
            return;
        }
        int generacion = generacionKpis.incrementAndGet();
        TareasFX.ejecutar(() -> AppContext.get().reportes().kpisDeFecha(fecha),
                kpi -> {
                    if (generacion != generacionKpis.get()) {
                        return;
                    }
                    kpiCitas.setText(String.valueOf(kpi.citas()));
                    kpiEnSala.setText(String.valueOf(kpi.enSala()));
                    kpiRecaudado.setText(soles(kpi.recaudado()));
                    kpiDeudores.setText(String.valueOf(kpi.deudores()));
                },
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void recargarPeriodo() {
        LocalDate desde = campoDesde.getValue();
        LocalDate hasta = campoHasta.getValue();
        if (desde == null || hasta == null) {
            return;
        }
        int generacion = generacionPeriodo.incrementAndGet();
        TareasFX.ejecutar(() -> new DatosPeriodo(
                        AppContext.get().reportes().kpisDelMes(desde, hasta),
                        AppContext.get().reportes().productividadOdontologos(desde, hasta)),
                datos -> {
                    if (generacion != generacionPeriodo.get()) {
                        return;
                    }
                    aplicarPeriodo(datos);
                },
                error -> mostrarError(TareasFX.mensaje(error)));
    }

    private void aplicarPeriodo(DatosPeriodo datos) {
        KpiMes mes = datos.mes();
        kpiMesRecaudado.setText(soles(mes.recaudado()));
        kpiMesAtendidas.setText(mes.atendidas() + " / " + mes.citas());
        kpiMesNoShow.setText(mes.noShowPct().toPlainString() + " % (" + mes.faltas() + " faltas)");
        kpiMesModalidades.setText(soles(mes.yape()) + " · " + soles(mes.plin())
                + " · " + soles(mes.tarjeta()));
        kpiEfectivo.setText(soles(mes.efectivo()));
        kpiYape.setText(soles(mes.yape()));
        kpiPlin.setText(soles(mes.plin()));
        kpiTarjeta.setText(soles(mes.tarjeta()));
        tablaProductividad.setItems(FXCollections.observableArrayList(datos.productividad()));
    }

    @FXML
    private void onExportarClick() {
        LocalDate desde = campoDesde.getValue();
        LocalDate hasta = campoHasta.getValue();
        if (desde == null || hasta == null) {
            mostrarError("Selecciona el periodo (desde y hasta) antes de exportar.");
            return;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Exportar reportes del periodo");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (Excel)", "*.csv"));
        selector.setInitialFileName("dentrix_reporte_" + desde + "_" + hasta + ".csv");
        File archivo = selector.showSaveDialog(tablaSaldos.getScene().getWindow());
        if (archivo == null) {
            return;
        }
        TareasFX.ejecutar(() -> {
            KpiMes kpi = AppContext.get().reportes().kpisDelMes(desde, hasta);
            List<ProductividadOdontologo> productividad =
                    AppContext.get().reportes().productividadOdontologos(desde, hasta);
            List<SaldoPaciente> saldos = AppContext.get().reportes().saldosPacientes();
            StringBuilder sb = new StringBuilder();
            sb.append("Dentrix - Reporte del periodo ").append(desde).append(" a ").append(hasta).append('\n');
            sb.append('\n');
            sb.append("INDICADORES DEL PERIODO\n");
            sb.append("Recaudado,S/ ").append(kpi.recaudado().toPlainString()).append('\n');
            sb.append("Efectivo,S/ ").append(kpi.efectivo().toPlainString()).append('\n');
            sb.append("Yape,S/ ").append(kpi.yape().toPlainString()).append('\n');
            sb.append("Plin,S/ ").append(kpi.plin().toPlainString()).append('\n');
            sb.append("Tarjeta,S/ ").append(kpi.tarjeta().toPlainString()).append('\n');
            sb.append("Citas,").append(kpi.citas()).append('\n');
            sb.append("Atendidas,").append(kpi.atendidas()).append('\n');
            sb.append("Faltas,").append(kpi.faltas()).append('\n');
            sb.append("No-Show %,").append(kpi.noShowPct().toPlainString()).append('\n');
            sb.append('\n');
            sb.append("PRODUCTIVIDAD POR ODONTÓLOGO\n");
            sb.append("Odontólogo;Atendidas;Faltas;Recaudado\n");
            for (ProductividadOdontologo p : productividad) {
                sb.append(escaparCsv(p.nombre())).append(';')
                        .append(p.atendidas()).append(';')
                        .append(p.faltas()).append(';')
                        .append(p.recaudado().toPlainString()).append('\n');
            }
            sb.append('\n');
            sb.append("SALDOS POR PACIENTE\n");
            sb.append("HC;Paciente;Saldo;Documentos\n");
            for (SaldoPaciente s : saldos) {
                sb.append(escaparCsv(s.codigoHc())).append(';')
                        .append(escaparCsv(s.nombre())).append(';')
                        .append(s.saldo().toPlainString()).append(';')
                        .append(s.documentos()).append('\n');
            }
            Files.writeString(archivo.toPath(), sb.toString(), StandardCharsets.UTF_8);
            return null;
        }, ignorado -> {
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Exportar");
            ok.setHeaderText(null);
            ok.setContentText("Reporte exportado a:\n" + archivo.getAbsolutePath()
                    + "\n\nÁbrelo con Excel (abre CSV sin problema).");
            ok.showAndWait();
        }, error -> mostrarError("No se pudo exportar: " + TareasFX.mensaje(error)));
    }

    private static String soles(BigDecimal valor) {
        if (valor == null) {
            return "S/ 0.00";
        }
        return "S/ " + String.format(Locale.US, "%,.2f", valor);
    }

    private String escaparCsv(String texto) {
        if (texto == null) {
            return "";
        }
        if (texto.contains(";") || texto.contains("\"") || texto.contains("\n")) {
            return "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }

    private void cargarTodo() {
        LocalDate fecha = campoFecha.getValue();
        LocalDate desde = campoDesde.getValue();
        LocalDate hasta = campoHasta.getValue();
        if (fecha == null || desde == null || hasta == null) {
            return;
        }
        int generacion = generacionTodo.incrementAndGet();
        int generacionKpi = generacionKpis.get();
        int generacionRango = generacionPeriodo.get();
        TareasFX.ejecutar(() -> {
            Map<Integer, String> nombres = new HashMap<>();
            for (Paciente p : AppContext.get().pacientes().listar()) {
                nombres.put(p.getId(), p.getApellidos() + ", " + p.getNombres());
            }
            return new DatosReportes(nombres,
                    AppContext.get().reportes().kpisDeFecha(fecha),
                    new DatosPeriodo(AppContext.get().reportes().kpisDelMes(desde, hasta),
                            AppContext.get().reportes().productividadOdontologos(desde, hasta)),
                    AppContext.get().reportes().saldosPacientes(),
                    AppContext.get().reportes().rankingTratamientos(),
                    AppContext.get().reportes().documentosConSaldo());
        }, datos -> {
            if (generacion != generacionTodo.get()) {
                return;
            }
            nombresPacientes.clear();
            nombresPacientes.putAll(datos.nombres());
            if (generacionKpi == generacionKpis.get()) {
                kpiCitas.setText(String.valueOf(datos.kpi().citas()));
                kpiEnSala.setText(String.valueOf(datos.kpi().enSala()));
                kpiRecaudado.setText(soles(datos.kpi().recaudado()));
                kpiDeudores.setText(String.valueOf(datos.kpi().deudores()));
            }
            if (generacionRango == generacionPeriodo.get()) {
                aplicarPeriodo(datos.periodo());
            }
            tablaSaldos.setItems(FXCollections.observableArrayList(datos.saldos()));
            BigDecimal totalSaldos = BigDecimal.ZERO;
            for (SaldoPaciente s : datos.saldos()) {
                totalSaldos = totalSaldos.add(s.saldo());
            }
            etiquetaSaldoTotal.setText("Total por cobrar: " + soles(totalSaldos)
                    + "  |  " + datos.saldos().size() + " paciente(s) con deuda");
            kpiSaldoTotal.setText(soles(totalSaldos));
            kpiSaldoPacientes.setText(datos.saldos().size() + " paciente(s) con saldo pendiente");

            tablaRanking.setItems(FXCollections.observableArrayList(datos.ranking()));

            tablaDocumentos.setItems(FXCollections.observableArrayList(datos.documentos()));
            BigDecimal totalDocumentos = BigDecimal.ZERO;
            for (Pago p : datos.documentos()) {
                totalDocumentos = totalDocumentos.add(p.getSaldoDeudor());
            }
            etiquetaDocsTotal.setText("Saldo total en documentos: " + soles(totalDocumentos)
                    + "  |  " + datos.documentos().size() + " documento(s) con saldo");
        },
        error -> mostrarError(TareasFX.mensaje(error)));
    }

    private record DatosPeriodo(KpiMes mes, List<ProductividadOdontologo> productividad) {
    }

    private record DatosReportes(Map<Integer, String> nombres, KpiDia kpi, DatosPeriodo periodo,
                                 List<SaldoPaciente> saldos, List<RankingTratamiento> ranking,
                                 List<Pago> documentos) {
    }

    @FXML
    private void onRefrescarClick() {
        cargarTodo();
    }

    @FXML
    private void onVolverClick() {
        pe.edu.utp.dentrix.ui.Navegador.ir(pe.edu.utp.dentrix.ui.Navegador.INICIO);
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Reportes");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
