package pe.edu.utp.dentrix.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.excepcion.DentrixException;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ReportesFxmlTest {

    private static void arrancarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaArrancado) {
        }
    }

    private static <T> T enFx(Callable<T> accion) throws Exception {
        CountDownLatch listo = new CountDownLatch(1);
        AtomicReference<T> valor = new AtomicReference<>();
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                valor.set(accion.call());
            } catch (Throwable t) {
                fallo.set(t);
            } finally {
                listo.countDown();
            }
        });
        if (!listo.await(30, TimeUnit.SECONDS)) {
            throw new AssertionError("La operacion en el hilo de JavaFX no termino");
        }
        if (fallo.get() != null) {
            throw new AssertionError(fallo.get().getMessage(), fallo.get());
        }
        return valor.get();
    }

    @Test
    void laPantallaDeReportesCargaFiltrosKpisYLasCuatroTablas() throws Exception {
        Connection conexion = null;
        try {
            conexion = ConexionBD.abrir();
        } catch (DentrixException e) {
            conexion = null;
        }
        assumeTrue(conexion != null, "config.properties o BD no disponibles: se omite esta prueba");
        conexion.close();

        var contexto = pe.edu.utp.dentrix.config.AppContext.get();
        var usuario = contexto.usuarios().buscarPorUsername("jvallejos")
                .orElseThrow(() -> new IllegalStateException("Falta el usuario jvallejos de la semilla"));
        var rol = contexto.roles().buscarPorId(usuario.getRolId())
                .orElseThrow(() -> new IllegalStateException("Falta el rol del usuario jvallejos"));
        contexto.sesion().abrir(usuario, rol);

        LocalDate hoy = LocalDate.now();
        LocalDate desde = hoy.withDayOfMonth(1);
        int rankingEsperado = contexto.reportes().rankingTratamientos().size();
        int saldosEsperados = contexto.reportes().saldosPacientes().size();
        int documentosEsperados = contexto.reportes().documentosConSaldo().size();
        int productividadEsperada = contexto.reportes().productividadOdontologos(desde, hoy).size();

        arrancarJavaFx();
        Parent pantalla = enFx(() -> {
            Parent cargado = FXMLLoader.load(
                    getClass().getResource("/pe/edu/utp/dentrix/fxml/reportes.fxml"));
            new javafx.scene.Scene(cargado);
            cargado.getStylesheets().add(
                    getClass().getResource("/pe/edu/utp/dentrix/estilos.css").toExternalForm());
            cargado.applyCss();
            cargado.layout();
            return cargado;
        });
        assertNotNull(pantalla, "El FXML debe producir un nodo raiz");

        assertNotNull(pantalla.lookup("#campoDesde"), "Falta la fecha desde del periodo");
        assertNotNull(pantalla.lookup("#campoHasta"), "Falta la fecha hasta del periodo");
        assertNotNull(pantalla.lookup("#campoFecha"), "Falta la fecha de los KPIs del dia");

        DatePicker campoDesde = (DatePicker) pantalla.lookup("#campoDesde");
        DatePicker campoHasta = (DatePicker) pantalla.lookup("#campoHasta");
        DatePicker campoFecha = (DatePicker) pantalla.lookup("#campoFecha");
        assertEquals(desde, enFx(campoDesde::getValue), "El periodo debe arrancar el primer dia del mes");
        assertEquals(hoy, enFx(campoHasta::getValue), "El periodo debe cerrar hoy");
        assertEquals(hoy, enFx(campoFecha::getValue), "Los KPIs del dia deben apuntar a hoy");

        for (String id : List.of("kpiMesRecaudado", "kpiMesAtendidas", "kpiMesNoShow",
                "kpiSaldoTotal", "kpiSaldoPacientes", "kpiCitas", "kpiEnSala", "kpiRecaudado",
                "kpiDeudores", "kpiMesModalidades", "kpiEfectivo", "kpiYape", "kpiPlin",
                "kpiTarjeta", "etiquetaSaldoTotal", "etiquetaDocsTotal")) {
            assertNotNull(pantalla.lookup("#" + id), "Falta el indicador " + id);
        }

        TableView<?> tablaRanking = (TableView<?>) pantalla.lookup("#tablaRanking");
        TableView<?> tablaProductividad = (TableView<?>) pantalla.lookup("#tablaProductividad");
        TableView<?> tablaSaldos = (TableView<?>) pantalla.lookup("#tablaSaldos");
        TableView<?> tablaDocumentos = (TableView<?>) pantalla.lookup("#tablaDocumentos");
        assertNotNull(tablaRanking, "Falta la tabla de ranking de tratamientos");
        assertNotNull(tablaProductividad, "Falta la tabla de productividad");
        assertNotNull(tablaSaldos, "Falta la tabla de saldos por paciente");
        assertNotNull(tablaDocumentos, "Falta la tabla de documentos con saldo");

        List<String> cabecerasRanking = enFx(() -> tablaRanking.getColumns().stream()
                .map(c -> c.getText() == null ? "" : c.getText()).toList());
        assertTrue(cabecerasRanking.contains("TRATAMIENTO"), "Falta TRATAMIENTO: " + cabecerasRanking);
        assertTrue(cabecerasRanking.contains("UNIDADES"), "Falta UNIDADES: " + cabecerasRanking);
        List<String> cabecerasProductividad = enFx(() -> tablaProductividad.getColumns().stream()
                .map(c -> c.getText() == null ? "" : c.getText()).toList());
        assertEquals(4, cabecerasProductividad.size(), "Productividad: " + cabecerasProductividad);
        List<String> cabecerasSaldos = enFx(() -> tablaSaldos.getColumns().stream()
                .map(c -> c.getText() == null ? "" : c.getText()).toList());
        assertEquals(4, cabecerasSaldos.size(), "Saldos: " + cabecerasSaldos);
        List<String> cabecerasDocumentos = enFx(() -> tablaDocumentos.getColumns().stream()
                .map(c -> c.getText() == null ? "" : c.getText()).toList());
        assertTrue(cabecerasDocumentos.contains("ESTADO PAGO"), "Falta ESTADO PAGO: " + cabecerasDocumentos);

        long limite = System.currentTimeMillis() + 30000;
        String saldoTexto = "";
        while (System.currentTimeMillis() < limite && saldoTexto.isBlank()) {
            saldoTexto = enFx(() -> ((Label) pantalla.lookup("#etiquetaSaldoTotal")).getText());
            if (saldoTexto.isBlank()) {
                Thread.sleep(200);
            }
        }
        assertFalse(saldoTexto.isBlank(), "La carga asincrona de reportes no termino");

        assertEquals(rankingEsperado, enFx(() -> tablaRanking.getItems().size()),
                "El ranking debe reflejar los tratamientos facturados");
        assertEquals(saldosEsperados, enFx(() -> tablaSaldos.getItems().size()),
                "La tabla de saldos debe cargar los pacientes con deuda");
        assertEquals(documentosEsperados, enFx(() -> tablaDocumentos.getItems().size()),
                "La tabla de documentos debe cargar los comprobantes con saldo");
        assertEquals(productividadEsperada, enFx(() -> tablaProductividad.getItems().size()),
                "La productividad debe cargar el periodo por defecto");

        String recaudado = enFx(() -> ((Label) pantalla.lookup("#kpiMesRecaudado")).getText());
        assertTrue(recaudado.startsWith("S/ "), "La recaudacion del periodo debe estar en soles: " + recaudado);
        String noShow = enFx(() -> ((Label) pantalla.lookup("#kpiMesNoShow")).getText());
        assertTrue(noShow.contains("%"), "La tasa no-show debe expresarse en porcentaje: " + noShow);
    }
}
