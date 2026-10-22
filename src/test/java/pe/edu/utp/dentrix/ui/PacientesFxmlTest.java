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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PacientesFxmlTest {

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
        if (!listo.await(20, TimeUnit.SECONDS)) {
            throw new AssertionError("La operacion en el hilo de JavaFX no termino");
        }
        if (fallo.get() != null) {
            throw new AssertionError(fallo.get().getMessage(), fallo.get());
        }
        return valor.get();
    }

    @Test
    void laPantallaDePacientesCargaLaFichaElPadronYLosCamposNuevos() throws Exception {
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

        long totalPacientes = contexto.pacientes().listar().size();

        arrancarJavaFx();
        Parent pantalla = enFx(() -> {
            Parent cargado = FXMLLoader.load(
                    getClass().getResource("/pe/edu/utp/dentrix/fxml/pacientes.fxml"));
            new javafx.scene.Scene(cargado);
            cargado.getStylesheets().add(
                    getClass().getResource("/pe/edu/utp/dentrix/estilos.css").toExternalForm());
            cargado.applyCss();
            cargado.layout();
            return cargado;
        });
        assertNotNull(pantalla, "El FXML debe producir un nodo raiz");

        assertNotNull(pantalla.lookup("#campoBusqueda"), "Falta el buscador por DNI, nombre o codigo");
        assertNotNull(pantalla.lookup("#tablaPacientes"), "Falta el padron de pacientes");
        assertNotNull(pantalla.lookup("#campoDni"), "Falta el campo DNI de la ficha");
        assertNotNull(pantalla.lookup("#campoCodigo"), "Falta el codigo HC de la ficha");
        assertNotNull(pantalla.lookup("#campoFechaNac"), "Falta la fecha de nacimiento");
        assertNotNull(pantalla.lookup("#campoTutorNombre"), "Falta el nombre del tutor");
        assertNotNull(pantalla.lookup("#campoTutorDocumento"), "Falta el documento del tutor");
        assertNotNull(pantalla.lookup("#comboTutorVinculo"), "Falta el vinculo del tutor");
        assertNotNull(pantalla.lookup("#campoPagadorNombre"), "Falta el responsable de pago");
        assertNotNull(pantalla.lookup("#campoPagadorDocumento"), "Falta el documento del pagador");
        assertNotNull(pantalla.lookup("#comboEstado"), "Falta el estado del paciente");
        assertNotNull((Button) pantalla.lookup("#btnAnular"), "Falta el boton Anular");
        assertNotNull((Button) pantalla.lookup("#btnReactivar"), "Falta el boton Reactivar");
        assertTrue(enFx(() -> ((Button) pantalla.lookup("#btnAnular")).getText().toLowerCase()
                        .contains("anular")),
                "R22: el paciente no se elimina, se anula");

        TableView<?> tabla = (TableView<?>) pantalla.lookup("#tablaPacientes");
        assertNotNull(tabla, "La tabla debe ser una TableView");
        List<String> cabeceras = enFx(() -> tabla.getColumns().stream()
                .map(c -> c.getText() == null ? "" : c.getText()).toList());
        assertEquals(7, cabeceras.size(), "El padron debe tener CODIGO, PACIENTE, DNI, EDAD, TELEFONO, "
                + "ESTADO y ACCIONES: " + cabeceras);
        assertTrue(cabeceras.contains("CODIGO"), "Falta la columna CODIGO: " + cabeceras);
        assertTrue(cabeceras.contains("PACIENTE"), "Falta la columna PACIENTE: " + cabeceras);
        assertTrue(cabeceras.contains("DNI"), "Falta la columna DNI: " + cabeceras);
        assertTrue(cabeceras.contains("EDAD"), "Falta la columna EDAD: " + cabeceras);
        assertTrue(cabeceras.contains("TELEFONO"), "Falta la columna TELEFONO: " + cabeceras);
        assertTrue(cabeceras.contains("ESTADO"), "Falta la columna ESTADO: " + cabeceras);

        ComboBox<?> comboEstado = (ComboBox<?>) pantalla.lookup("#comboEstado");
        assertEquals(2, enFx(() -> comboEstado.getItems().size()),
                "El estado solo admite ACTIVO e INACTIVO");
        ComboBox<?> comboVinculo = (ComboBox<?>) pantalla.lookup("#comboTutorVinculo");
        assertTrue(enFx(() -> comboVinculo.getItems().size()) >= 3,
                "El vinculo del tutor debe ofrecer al menos Padre, Madre y Apoderado");

        if (totalPacientes > 0) {
            long limite = System.currentTimeMillis() + 20000;
            int filas = 0;
            while (System.currentTimeMillis() < limite && filas == 0) {
                filas = enFx(() -> tabla.getItems().size());
                if (filas == 0) {
                    Thread.sleep(200);
                }
            }
            assertTrue(filas > 0, "El padron debe cargar los pacientes de la base");
        }
    }
}
