package pe.edu.utp.dentrix.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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

class EjecucionFxmlTest {

    private static void arrancarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaArrancado) {
            // el toolkit ya estaba iniciado por otra prueba
        }
    }

    @Test
    void laPantallaDeEjecucionCargaConPacientesYTablaDeItems() throws Exception {
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
        CountDownLatch listo = new CountDownLatch(1);
        AtomicReference<Parent> raiz = new AtomicReference<>();
        AtomicReference<Throwable> fallo = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Parent cargado = FXMLLoader.load(getClass().getResource("/pe/edu/utp/dentrix/fxml/ejecucion.fxml"));
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

        assertTrue(listo.await(30, TimeUnit.SECONDS), "La pantalla de ejecucion no termino de cargar");
        if (fallo.get() != null) {
            throw new AssertionError("La pantalla de ejecucion no carga: " + fallo.get(), fallo.get());
        }

        Parent pantalla = raiz.get();
        assertNotNull(pantalla, "El FXML debe producir un nodo raiz");
        ComboBox<?> combo = (ComboBox<?>) pantalla.lookup("#comboPaciente");
        assertNotNull(combo, "La pantalla debe tener el combo de pacientes");
        TableView<?> tabla = (TableView<?>) pantalla.lookup("#tablaItems");
        assertNotNull(tabla, "La pantalla debe tener la tabla de items");
        Button botonAbrir = (Button) pantalla.lookup("#btnAbrirSesion");
        assertNotNull(botonAbrir, "La pantalla debe tener el boton de abrir sesion");
        Button botonLaboratorio = (Button) pantalla.lookup("#btnLaboratorio");
        assertNotNull(botonLaboratorio, "La pantalla debe tener el boton de laboratorio e insumos");

        int pacientes = 0;
        long limite = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < limite) {
            int[] leidos = new int[1];
            CountDownLatch turno = new CountDownLatch(1);
            Platform.runLater(() -> {
                leidos[0] = combo.getItems().size();
                turno.countDown();
            });
            turno.await(3, TimeUnit.SECONDS);
            pacientes = leidos[0];
            if (pacientes >= 5) {
                break;
            }
            Thread.sleep(250);
        }
        assertTrue(pacientes >= 5, "El combo de pacientes debe cargar al menos 5 pacientes y cargo " + pacientes);

        ComboBox<?> comboPresupuesto = (ComboBox<?>) pantalla.lookup("#comboPresupuesto");
        assertNotNull(comboPresupuesto, "La pantalla debe tener el combo de presupuestos aceptados");

        @SuppressWarnings("unchecked")
        ComboBox<pe.edu.utp.dentrix.model.Paciente> comboSel =
                (ComboBox<pe.edu.utp.dentrix.model.Paciente>) combo;
        Platform.runLater(() -> comboSel.getItems().stream()
                .filter(p -> Integer.valueOf(2).equals(p.getId()))
                .findFirst()
                .ifPresent(comboSel::setValue));

        int presupuestos = 0;
        int items = 0;
        long limite2 = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < limite2) {
            int[] leidos = new int[2];
            CountDownLatch turno = new CountDownLatch(1);
            Platform.runLater(() -> {
                leidos[0] = comboPresupuesto.getItems().size();
                leidos[1] = tabla.getItems().size();
                turno.countDown();
            });
            turno.await(3, TimeUnit.SECONDS);
            presupuestos = leidos[0];
            items = leidos[1];
            if (presupuestos >= 1 && items >= 1) {
                break;
            }
            Thread.sleep(250);
        }
        assertTrue(presupuestos >= 1,
                "El plan aceptado del paciente 2 debe aparecer en el combo de la pantalla");
        assertTrue(items >= 1,
                "Al elegir el plan aceptado, la tabla debe traer sus items ejecutables");
    }
}
