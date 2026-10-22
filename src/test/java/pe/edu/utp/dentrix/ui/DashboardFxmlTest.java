package pe.edu.utp.dentrix.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DashboardFxmlTest {

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
    void elPanelDeControlCargaSaludoBusquedaKpisYAgendaDelDia() throws Exception {
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
        int citasEsperadas = contexto.agenda().agendaDelDia(hoy).stream()
                .filter(c -> "CITA".equals(c.getTipo()))
                .toList().size();

        arrancarJavaFx();
        Parent pantalla = enFx(() -> {
            Parent cargado = FXMLLoader.load(
                    getClass().getResource("/pe/edu/utp/dentrix/fxml/dashboard.fxml"));
            new javafx.scene.Scene(cargado);
            cargado.getStylesheets().add(
                    getClass().getResource("/pe/edu/utp/dentrix/estilos.css").toExternalForm());
            cargado.applyCss();
            cargado.layout();
            return cargado;
        });
        assertNotNull(pantalla, "El FXML debe producir un nodo raiz");

        for (String id : List.of("etiquetaSaludo", "etiquetaFechaHoy", "campoDni",
                "etiquetaBusqueda", "kpiCitas", "kpiSala", "kpiRecaudado", "kpiDeudores",
                "etiquetaFechaAgenda", "tablaAgenda")) {
            assertNotNull(pantalla.lookup("#" + id), "Falta el elemento " + id);
        }

        String saludo = enFx(() -> ((Label) pantalla.lookup("#etiquetaSaludo")).getText());
        assertEquals("Hola, " + usuario.getNombres() + " " + usuario.getApellidos(), saludo,
                "El panel debe saludar al usuario de la sesion");
        String fechaHoy = enFx(() -> ((Label) pantalla.lookup("#etiquetaFechaHoy")).getText());
        assertFalse(fechaHoy.isBlank(), "Falta la fecha del panel");
        String fechaAgenda = enFx(() -> ((Label) pantalla.lookup("#etiquetaFechaAgenda")).getText());
        assertFalse(fechaAgenda.isBlank(), "Falta la fecha de la agenda");

        TableView<?> tablaAgenda = (TableView<?>) pantalla.lookup("#tablaAgenda");
        List<String> cabeceras = enFx(() -> tablaAgenda.getColumns().stream()
                .map(c -> c.getText() == null ? "" : c.getText()).toList());
        assertEquals(6, cabeceras.size(), "La agenda debe tener HORA, PACIENTE, DNI, TRATAMIENTO, "
                + "ODONTOLOGO y ESTADO: " + cabeceras);
        assertTrue(cabeceras.contains("DNI"), "Falta la columna DNI: " + cabeceras);
        assertTrue(cabeceras.contains("PACIENTE"), "Falta la columna PACIENTE: " + cabeceras);
        assertTrue(cabeceras.contains("ESTADO"), "Falta la columna ESTADO: " + cabeceras);

        List<String> accesos = enFx(() -> pantalla.lookupAll(".nav-boton").stream()
                .filter(n -> n instanceof Button)
                .map(n -> ((Button) n).getText() == null ? "" : ((Button) n).getText()).toList());
        assertTrue(accesos.size() >= 5, "Faltan los accesos rapidos: " + accesos);
        assertTrue(accesos.contains("Pacientes"), "Falta el acceso a Pacientes: " + accesos);
        assertTrue(accesos.contains("Agenda"), "Falta el acceso a Agenda: " + accesos);
        assertTrue(accesos.contains("Reportes"), "Falta el acceso a Reportes: " + accesos);

        long limite = System.currentTimeMillis() + 30000;
        String citas = "—";
        while (System.currentTimeMillis() < limite && "—".equals(citas)) {
            citas = enFx(() -> ((Label) pantalla.lookup("#kpiCitas")).getText());
            if ("—".equals(citas)) {
                Thread.sleep(200);
            }
        }
        assertNotEquals("—", citas, "La carga asincrona del panel de control no termino");
        assertEquals(String.valueOf(citasEsperadas), citas, "El KPI de citas debe ser el de hoy");
        assertEquals(citasEsperadas, enFx(() -> tablaAgenda.getItems().size()),
                "La agenda debe cargar las citas del dia");
    }
}
