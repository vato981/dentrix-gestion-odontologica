package pe.edu.utp.dentrix.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.excepcion.DentrixException;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SalaEsperaFxmlTest {

    private static void arrancarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaArrancado) {
        }
    }

    @Test
    void laPantallaDeSalaDeEsperaCargaLaListaYElDetalle() throws Exception {
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

        boolean hayCitasHoy;
        try {
            hayCitasHoy = !contexto.agenda().agendaDelDia(LocalDate.now()).isEmpty();
        } catch (DentrixException e) {
            hayCitasHoy = false;
        }

        arrancarJavaFx();
        CountDownLatch listo = new CountDownLatch(1);
        AtomicReference<Parent> raiz = new AtomicReference<>();
        AtomicReference<Throwable> fallo = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Parent cargado = FXMLLoader.load(
                        getClass().getResource("/pe/edu/utp/dentrix/fxml/sala-espera.fxml"));
                new javafx.scene.Scene(cargado);
                cargado.applyCss();
                cargado.layout();
                raiz.set(cargado);
            } catch (Throwable t) {
                fallo.set(t);
            } finally {
                listo.countDown();
            }
        });

        assertTrue(listo.await(30, TimeUnit.SECONDS), "La pantalla de recepcion no termino de cargar");
        if (fallo.get() != null) {
            throw new AssertionError("La pantalla de recepcion no carga: " + fallo.get(), fallo.get());
        }

        Parent pantalla = raiz.get();
        assertNotNull(pantalla, "El FXML debe producir un nodo raiz");
        assertNotNull(pantalla.lookup("#tablaCola"), "Falta la tabla de pacientes del dia");
        assertNotNull(pantalla.lookup("#etiquetaPendientes"), "Falta el contador de pendientes");
        assertNotNull(pantalla.lookup("#etiquetaSelPaciente"), "Falta el detalle del paciente");

        TableView<?> tabla = (TableView<?>) pantalla.lookup("#tablaCola");
        assertNotNull(tabla, "La tabla debe ser una TableView");
        assertEquals(4, tabla.getColumns().size(), "La tabla debe tener HORA, PACIENTE, ESTADO y ACCION");
        assertTrue(tabla.getColumns().stream().anyMatch(c -> "ACCION".equals(c.getText())),
                "La columna ACCION debe existir");

        if (hayCitasHoy) {
            long limite = System.currentTimeMillis() + 15000;
            int filas = 0;
            while (System.currentTimeMillis() < limite && filas == 0) {
                filas = contarFilas(tabla);
                if (filas == 0) {
                    Thread.sleep(200);
                }
            }
            assertTrue(filas > 0, "La tabla debe cargar al menos una fila de las citas del dia");
        }
    }

    private static int contarFilas(TableView<?> tabla) throws InterruptedException {
        AtomicInteger total = new AtomicInteger(0);
        CountDownLatch leido = new CountDownLatch(1);
        Platform.runLater(() -> {
            total.set(tabla.getItems().size());
            leido.countDown();
        });
        if (!leido.await(5, TimeUnit.SECONDS)) {
            return 0;
        }
        return total.get();
    }
}
