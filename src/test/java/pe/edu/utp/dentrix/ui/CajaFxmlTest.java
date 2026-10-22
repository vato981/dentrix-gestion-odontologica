package pe.edu.utp.dentrix.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.excepcion.DentrixException;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CajaFxmlTest {

    private static void arrancarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaArrancado) {
            // el toolkit ya estaba iniciado por otra prueba
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
    void laPantallaDeCajaTieneLasTresTarjetasYLosControlesDelCierre() throws Exception {
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
        contexto.sesion().setPacienteEnAtencion(null);

        arrancarJavaFx();
        Parent pantalla = enFx(() -> {
            Parent cargado = FXMLLoader.load(getClass().getResource("/pe/edu/utp/dentrix/fxml/caja.fxml"));
            new javafx.scene.Scene(cargado);
            cargado.getStylesheets().add(
                    getClass().getResource("/pe/edu/utp/dentrix/estilos.css").toExternalForm());
            cargado.applyCss();
            cargado.layout();
            return cargado;
        });
        assertNotNull(pantalla, "El FXML debe producir un nodo raiz");

        assertNotNull(pantalla.lookup("#etiquetaCajero"), "Falta el usuario de caja en la cabecera");
        assertNotNull(pantalla.lookup("#btnCierreTurno"), "Falta el acceso al cierre del turno");
        assertNotNull(pantalla.lookup("#btnCobros"), "Falta el acceso a la tarjeta de cobros");

        assertNotNull(pantalla.lookup("#cardCobro"), "Falta la tarjeta de registro de cobro");
        assertNotNull(pantalla.lookup("#comboPaciente"), "Falta el selector de paciente del cobro");
        assertNotNull(pantalla.lookup("#etiquetaPaciente"), "Falta el resumen del paciente");
        assertNotNull(pantalla.lookup("#comboTratamiento"), "Falta el tratamiento del cobro");
        assertNotNull(pantalla.lookup("#tablaLineas"), "Faltan los items del cobro");
        assertNotNull(pantalla.lookup("#lblTotal"), "Falta el total a pagar");
        assertNotNull(pantalla.lookup("#comboModalidad"), "Falta la modalidad de cobro");
        assertNotNull(pantalla.lookup("#campoOperacion"), "Falta el numero de operacion");
        assertNotNull(pantalla.lookup("#campoObservacion"), "Falta la observacion del cobro");
        Button btnCobrarPlan = (Button) pantalla.lookup("#btnCobrarPlan");
        assertNotNull(btnCobrarPlan, "La pantalla de caja debe tener el boton de cobrar plan aceptado");
        assertNotNull(pantalla.lookup("#btnEmitirBoleta"), "Falta el boton de emitir boleta");

        assertNotNull(pantalla.lookup("#cardComprobantes"), "Falta la tarjeta de comprobantes del dia");
        assertNotNull(pantalla.lookup("#tablaBoletas"), "Falta la tabla de comprobantes del dia");
        Button btnVerBoleta = (Button) pantalla.lookup("#btnVerBoleta");
        assertNotNull(btnVerBoleta, "Falta la accion Ver del comprobante");
        Button btnAnularBoleta = (Button) pantalla.lookup("#btnAnularBoleta");
        assertNotNull(btnAnularBoleta, "Falta la accion Anular del comprobante");
        assertTrue(btnAnularBoleta.getText().toLowerCase().contains("anular"),
                "El comprobante no se borra: se anula con motivo");
        TableView<?> tablaBoletas = (TableView<?>) pantalla.lookup("#tablaBoletas");
        List<String> cabecerasBoletas = enFx(() -> tablaBoletas.getColumns().stream()
                .filter(c -> c.isVisible())
                .map(c -> c.getText() == null ? "" : c.getText()).toList());
        assertTrue(cabecerasBoletas.contains("PACIENTE"), "Falta la columna PACIENTE: " + cabecerasBoletas);
        assertTrue(cabecerasBoletas.contains("MONTO"), "Falta la columna MONTO: " + cabecerasBoletas);
        assertTrue(cabecerasBoletas.contains("ESTADO"), "Falta la columna ESTADO: " + cabecerasBoletas);

        assertNotNull(pantalla.lookup("#cardAbonos"), "Falta la tarjeta de abonos");
        assertNotNull(pantalla.lookup("#campoNumeroBoleta"), "Falta el numero de boleta a abonar");
        assertNotNull(pantalla.lookup("#tablaAbonos"), "Falta la tabla de abonos de la boleta");
        assertNotNull(pantalla.lookup("#campoMontoAbono"), "Falta el monto del abono");
        assertNotNull(pantalla.lookup("#comboModalidadAbono"), "Falta la modalidad del abono");

        ScrollPane panelCierre = (ScrollPane) pantalla.lookup("#panelCierre");
        assertNotNull(panelCierre, "Falta la tarjeta de cierre de caja (M7.1)");
        enFx(() -> {
            panelCierre.setVisible(true);
            panelCierre.setManaged(true);
            pantalla.layout();
            return null;
        });

        assertNotNull(pantalla.lookup("#cardCierreFondo"), "Falta la tarjeta de fondo inicial y arqueo");
        assertNotNull(pantalla.lookup("#cardCierreConsolidado"), "Falta la tarjeta del consolidado");
        assertNotNull(pantalla.lookup("#comboTurno"), "Falta el turno del cierre");
        assertNotNull(pantalla.lookup("#campoFondo"), "Falta el fondo inicial del cierre");
        assertNotNull(pantalla.lookup("#btnAbrirCierre"), "Falta el boton Abrir cierre");
        assertNotNull(pantalla.lookup("#lblEfectivoSistema"), "Falta el efectivo del sistema");
        assertNotNull(pantalla.lookup("#lblBilleterasSistema"), "Faltan las billeteras del sistema");
        assertNotNull(pantalla.lookup("#lblTotalSistema"), "Falta el total del sistema");
        assertNotNull(pantalla.lookup("#lblFondoInicial"), "Falta el fondo inicial consolidado");
        assertNotNull(pantalla.lookup("#campoContado"), "Falta el efectivo contado");
        assertNotNull(pantalla.lookup("#lblEfectivoContado"), "Falta el efectivo contado consolidado");
        assertNotNull(pantalla.lookup("#lblDiferencia"), "Falta la diferencia del arqueo");
        assertNotNull(pantalla.lookup("#lblTolerancia"), "Falta la tolerancia del arqueo");
        assertNotNull(pantalla.lookup("#comboMotivoDiferencia"), "Falta el motivo de la diferencia");
        assertNotNull(pantalla.lookup("#lblActaEstado"), "Falta el estado del acta");
        assertNotNull(pantalla.lookup("#etiquetaCierreInfo"), "Falta el detalle del cierre");
        assertNotNull(pantalla.lookup("#btnConsolidar"), "Falta el boton Consolidar cierre");
        assertNotNull(pantalla.lookup("#btnActa"), "Falta el boton Marcar acta");
    }
}
