package pe.edu.utp.dentrix.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.excepcion.DentrixException;

import java.sql.Connection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AgendaFxmlCargaTest {

    private static void arrancarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaArrancado) {
        }
    }

    @Test
    void laPantallaDeAgendaCargaConLosPanelesNuevos() throws Exception {
        Connection conexion;
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

        arrancarJavaFx();
        CountDownLatch listo = new CountDownLatch(1);
        AtomicReference<Parent> raiz = new AtomicReference<>();
        AtomicReference<Throwable> fallo = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Parent cargado = FXMLLoader.load(getClass().getResource("/pe/edu/utp/dentrix/fxml/agenda.fxml"));
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

        assertTrue(listo.await(30, TimeUnit.SECONDS), "La pantalla de agenda no termino de cargar");
        if (fallo.get() != null) {
            throw new AssertionError("La pantalla de agenda no carga: " + fallo.get(), fallo.get());
        }

        Parent pantalla = raiz.get();
        assertNotNull(pantalla, "El FXML debe producir un nodo raiz");
        assertNotNull(pantalla.lookup("#tablaEspera"), "Falta la tabla de lista de espera");
        assertNotNull(pantalla.lookup("#tablaRecordatorios"), "Falta la tabla de recordatorios");
        assertNotNull((Button) pantalla.lookup("#btnReprogramar"), "Falta el boton Reprogramar");
        assertNotNull((Button) pantalla.lookup("#btnHistorial"), "Falta el boton Historial");
        assertNotNull((TableView<?>) pantalla.lookup("#tablaAgenda"), "Falta la tabla de agenda");
    }
}
