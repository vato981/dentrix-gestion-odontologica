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
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Paciente;
import pe.edu.utp.dentrix.model.Pago;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    @FXML
    private void initialize() {
        colProdOdontologo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nombre()));
        colProdAtendidas.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().atendidas())));
        colProdFaltas.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().faltas())));
        colProdRecaudado.setCellValueFactory(d -> new SimpleStringProperty(
                "S/ " + d.getValue().recaudado().toPlainString()));

        colSaldoHc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().codigoHc()));
        colSaldoNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nombre()));
        colSaldoMonto.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().saldo().toPlainString()));
        colSaldoDocs.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().documentos())));

        colRankCodigo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().codigo()));
        colRankNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nombre()));
        colRankUnidades.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().unidades())));
        colRankMonto.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().monto().toPlainString()));

        colDocNumero.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNumero()));
        colDocFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() == null ? "" : FECHA_CORTA.format(d.getValue().getFecha())));
        colDocPaciente.setCellValueFactory(d -> new SimpleStringProperty(
                nombresPacientes.getOrDefault(d.getValue().getPacienteId(), "#"
                        + d.getValue().getPacienteId())));
        colDocMonto.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getMonto().toPlainString()));
        colDocSaldo.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getSaldoDeudor().toPlainString()));
        colDocEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstadoPago()));
        pe.edu.utp.dentrix.ui.Badges.aplicarEn(colDocEstado);

        campoFecha.valueProperty().addListener((obs, antes, ahora) -> recargarKpis());
        campoDesde.valueProperty().addListener((obs, antes, ahora) -> recargarPeriodo());
        campoHasta.valueProperty().addListener((obs, antes, ahora) -> recargarPeriodo());

        AppContext contexto = AppContext.get();
        if (!contexto.sesion().activa()) {
            return;
        }
        try {
            for (Paciente p : contexto.pacientes().listar()) {
                nombresPacientes.put(p.getId(), p.getApellidos() + ", " + p.getNombres());
            }
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
        campoFecha.setValue(LocalDate.now());
        campoDesde.setValue(LocalDate.now().withDayOfMonth(1));
        campoHasta.setValue(LocalDate.now());
        recargarTodo();
    }

    private void recargarKpis() {
        LocalDate fecha = campoFecha.getValue();
        if (fecha == null) {
            return;
        }
        try {
            KpiDia kpi = AppContext.get().reportes().kpisDeFecha(fecha);
            kpiCitas.setText(String.valueOf(kpi.citas()));
            kpiEnSala.setText(String.valueOf(kpi.enSala()));
            kpiRecaudado.setText("S/ " + kpi.recaudado().toPlainString());
            kpiDeudores.setText(String.valueOf(kpi.deudores()));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    private void recargarPeriodo() {
        LocalDate desde = campoDesde.getValue();
        LocalDate hasta = campoHasta.getValue();
        if (desde == null || hasta == null) {
            return;
        }
        try {
            KpiMes mes = AppContext.get().reportes().kpisDelMes(desde, hasta);
            kpiMesRecaudado.setText("S/ " + mes.recaudado().toPlainString());
            kpiMesAtendidas.setText(mes.atendidas() + " / " + mes.citas());
            kpiMesNoShow.setText(mes.noShowPct().toPlainString() + " % (" + mes.faltas() + " faltas)");
            kpiMesModalidades.setText("S/ " + mes.yape().toPlainString()
                    + " · S/ " + mes.plin().toPlainString()
                    + " · S/ " + mes.tarjeta().toPlainString());
            List<ProductividadOdontologo> productividad =
                    AppContext.get().reportes().productividadOdontologos(desde, hasta);
            tablaProductividad.setItems(FXCollections.observableArrayList(productividad));
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
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
        try {
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
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Exportar");
            ok.setHeaderText(null);
            ok.setContentText("Reporte exportado a:\n" + archivo.getAbsolutePath()
                    + "\n\nÁbrelo con Excel (abre CSV sin problema).");
            ok.showAndWait();
        } catch (DentrixException | IOException e) {
            mostrarError("No se pudo exportar: " + e.getMessage());
        }
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

    private void recargarTodo() {
        recargarKpis();
        recargarPeriodo();
        try {
            List<SaldoPaciente> saldos = AppContext.get().reportes().saldosPacientes();
            tablaSaldos.setItems(FXCollections.observableArrayList(saldos));
            BigDecimal totalSaldos = BigDecimal.ZERO;
            for (SaldoPaciente s : saldos) {
                totalSaldos = totalSaldos.add(s.saldo());
            }
            etiquetaSaldoTotal.setText("Total por cobrar: S/ " + totalSaldos.toPlainString()
                    + "  |  " + saldos.size() + " paciente(s) con deuda");

            List<RankingTratamiento> ranking = AppContext.get().reportes().rankingTratamientos();
            tablaRanking.setItems(FXCollections.observableArrayList(ranking));

            List<Pago> documentos = AppContext.get().reportes().documentosConSaldo();
            tablaDocumentos.setItems(FXCollections.observableArrayList(documentos));
            BigDecimal totalDocumentos = BigDecimal.ZERO;
            for (Pago p : documentos) {
                totalDocumentos = totalDocumentos.add(p.getSaldoDeudor());
            }
            etiquetaDocsTotal.setText("Saldo total en documentos: S/ " + totalDocumentos.toPlainString()
                    + "  |  " + documentos.size() + " documento(s) con saldo");
        } catch (DentrixException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onRefrescarClick() {
        recargarTodo();
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
