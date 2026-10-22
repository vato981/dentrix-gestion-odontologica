package pe.edu.utp.dentrix.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.excepcion.DentrixException;

import java.sql.Connection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FichaClinicaFxmlTest {

    private static void arrancarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaArrancado) {
        }
    }

    @Test
    void laFichaClinicaCargaPacienteDiagnosticoOdontogramaPlanYEvolucion() throws Exception {
        Connection conexion = null;
        try {
            conexion = ConexionBD.abrir();
        } catch (DentrixException e) {
            conexion = null;
        }
        assumeTrue(conexion != null, "config.properties o BD no disponibles: se omite esta prueba");
        conexion.close();

        AppContext contexto = AppContext.get();
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
                Parent cargado = FXMLLoader.load(
                        getClass().getResource("/pe/edu/utp/dentrix/fxml/ficha-clinica.fxml"));
                new javafx.scene.Scene(cargado);
                cargado.getStylesheets().add(
                        getClass().getResource("/pe/edu/utp/dentrix/estilos.css").toExternalForm());
                cargado.applyCss();
                cargado.layout();
                raiz.set(cargado);
            } catch (Throwable t) {
                fallo.set(t);
            } finally {
                listo.countDown();
            }
        });

        assertTrue(listo.await(30, TimeUnit.SECONDS), "La ficha clinica no termino de cargar");
        if (fallo.get() != null) {
            throw new AssertionError("La ficha clinica no carga: " + fallo.get(), fallo.get());
        }

        Parent pantalla = raiz.get();
        assertNotNull(pantalla, "El FXML debe producir un nodo raiz");

        ComboBox<?> comboPaciente = (ComboBox<?>) pantalla.lookup("#comboPaciente");
        assertNotNull(comboPaciente, "Falta el selector de paciente");
        assertNotNull(pantalla.lookup("#comboFichas"), "Falta el selector de ficha");
        assertNotNull(pantalla.lookup("#nombrePaciente"), "Falta el nombre del paciente en la cabecera");
        assertNotNull(pantalla.lookup("#etiquetaDni"), "Falta el DNI de solo lectura");
        assertNotNull(pantalla.lookup("#etiquetaEdad"), "Falta la edad de solo lectura");
        assertNotNull(pantalla.lookup("#etiquetaEstadoFicha"), "Falta el badge de estado de la ficha");

        assertNotNull(pantalla.lookup("#comboCie10"), "Falta el combo CIE-10");
        assertNotNull(pantalla.lookup("#tablaDiagnosticos"), "Falta la tabla de diagnosticos");
        assertNotNull(pantalla.lookup("#tablaHistorialPrincipal"), "Falta el historial RF-06");

        Pane panelOdontograma = (Pane) pantalla.lookup("#panelOdontograma");
        assertNotNull(panelOdontograma, "Falta el contenedor del odontograma");
        assertNotNull(pantalla.lookup("#tablaOdontograma"), "Falta la tabla de hallazgos");
        assertNotNull(pantalla.lookup("#herrCaries"), "Falta la herramienta Caries");
        assertTrue(panelOdontograma.getChildren().stream().anyMatch(OdontogramaView.class::isInstance),
                "El odontograma debe dibujarse dentro de panelOdontograma");

        assertNotNull(pantalla.lookup("#tablaPlan"), "Falta la tabla del plan");
        assertNotNull(pantalla.lookup("#comboTratamiento"), "Falta el combo de tratamientos");
        assertNotNull(pantalla.lookup("#etiquetaTotalPlan"), "Falta el resumen del plan");

        TableView<?> tablaEvoluciones = (TableView<?>) pantalla.lookup("#tablaEvoluciones");
        assertNotNull(tablaEvoluciones, "Falta la tabla de evoluciones");
        assertTrue(tablaEvoluciones.getColumns().size() >= 4,
                "La tabla de evoluciones debe mostrar fecha, estado, texto y enmienda");
    }
}
