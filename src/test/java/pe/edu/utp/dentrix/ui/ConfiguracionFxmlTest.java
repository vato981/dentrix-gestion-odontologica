package pe.edu.utp.dentrix.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.excepcion.DentrixException;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ConfiguracionFxmlTest {

    private static void arrancarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaArrancado) {
            // el toolkit ya estaba iniciado por otra prueba
        }
    }

    @Test
    void laPantallaDeConfiguracionCargaConElBotonDeTarifario() throws Exception {
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

        arrancarJavaFx();
        CountDownLatch listo = new CountDownLatch(1);
        AtomicReference<Parent> raiz = new AtomicReference<>();
        AtomicReference<Throwable> fallo = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Parent cargado = FXMLLoader.load(getClass().getResource("/pe/edu/utp/dentrix/fxml/configuracion.fxml"));
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

        assertTrue(listo.await(30, TimeUnit.SECONDS), "La pantalla de configuracion no termino de cargar");
        if (fallo.get() != null) {
            throw new AssertionError("La pantalla de configuracion no carga: " + fallo.get(), fallo.get());
        }

        Parent pantalla = raiz.get();
        assertNotNull(pantalla, "El FXML debe producir un nodo raiz");
        Button botonTarifario = (Button) pantalla.lookup("#btnNuevaVersionTarifario");
        assertNotNull(botonTarifario, "La tarjeta de tarifario debe tener el boton de version nueva");
        TableView<?> tabla = (TableView<?>) pantalla.lookup("#tablaTratamientos");
        assertNotNull(tabla, "La pantalla debe tener la tabla de tratamientos");

        assertNotNull(pantalla.lookup("#cardTarifario"), "Falta la tarjeta del tarifario (M9.2.1)");
        assertNotNull(pantalla.lookup("#etiquetaVersionTarifario"), "Falta la version publicada del tarifario");
        assertNotNull(pantalla.lookup("#etiquetaVigenciaTarifario"), "Falta la vigencia del tarifario");
        Label igv = (Label) pantalla.lookup("#etiquetaIgvTarifario");
        assertNotNull(igv, "Falta el IGV del tarifario");
        assertTrue(igv.getText().contains("IGV"), "El tarifario debe indicar el IGV: " + igv.getText());
        List<String> cabecerasTarifario = enFx(() -> tabla.getColumns().stream()
                .map(c -> c.getText() == null ? "" : c.getText()).toList());
        assertTrue(cabecerasTarifario.contains("PRECIO VIGENTE"),
                "Falta la columna PRECIO VIGENTE: " + cabecerasTarifario);
        assertTrue(cabecerasTarifario.contains("ESTADO"), "Falta la columna ESTADO: " + cabecerasTarifario);

        assertNotNull(pantalla.lookup("#cardCie10"), "Falta la tarjeta de CIE-10 (M9.2.2)");
        assertNotNull(pantalla.lookup("#tablaCie"), "Falta la tabla de diagnosticos CIE-10");
        assertNotNull(pantalla.lookup("#campoBusquedaCie"), "Falta el buscador de CIE-10");
        assertNotNull(pantalla.lookup("#etiquetaCieCodigo"), "Falta la ficha tecnica del CIE-10");
        assertNotNull(pantalla.lookup("#etiquetaCieDescripcion"), "Falta la descripcion del CIE-10");
        TableView<?> tablaCie = (TableView<?>) pantalla.lookup("#tablaCie");        List<String> cabecerasCie = enFx(() -> tablaCie.getColumns().stream()
                .map(c -> c.getText() == null ? "" : c.getText()).toList());
        assertTrue(cabecerasCie.contains("CÓDIGO"), "Falta la columna CODIGO del CIE-10: " + cabecerasCie);
        assertTrue(cabecerasCie.contains("DESCRIPCIÓN"), "Falta la columna DESCRIPCION del CIE-10: " + cabecerasCie);
        assertTrue(cabecerasCie.contains("CAPÍTULO"), "Falta la columna CAPITULO del CIE-10: " + cabecerasCie);
        assertTrue(cabecerasCie.contains("FRECUENTE"), "Falta la columna FRECUENTE del CIE-10: " + cabecerasCie);

        assertNotNull(pantalla.lookup("#cardDatosFiscales"), "Faltan los datos fiscales de la clinica (M9.3)");
        assertNotNull(pantalla.lookup("#cardAcreditacion"), "Falta la acreditacion y horario de la clinica");
        assertNotNull(pantalla.lookup("#campoRuc"), "Falta el RUC");
        assertNotNull(pantalla.lookup("#campoRazonSocial"), "Falta la razon social");
        assertNotNull(pantalla.lookup("#campoSerieBoleta"), "Falta la serie de boleta");
        assertNotNull(pantalla.lookup("#campoMedicoDirector"), "Falta el medico director");
        assertNotNull(pantalla.lookup("#campoHoraApertura"), "Falta la hora de apertura");
        TextField campoIgv = (TextField) pantalla.lookup("#campoIgv");
        assertNotNull(campoIgv, "Falta el IGV de los datos de la clinica");

        assertNotNull(pantalla.lookup("#cardRespaldo"), "Falta la tarjeta de respaldo de BD");
        assertNotNull(pantalla.lookup("#etiquetaRespaldoResultado"), "Falta el resultado del respaldo");

        int filas = 0;
        long limite = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < limite) {
            int[] leidas = new int[1];
            CountDownLatch turno = new CountDownLatch(1);
            Platform.runLater(() -> {
                leidas[0] = tabla.getItems().size();
                turno.countDown();
            });
            turno.await(3, TimeUnit.SECONDS);
            filas = leidas[0];
            if (filas >= 5) {
                break;
            }
            Thread.sleep(250);
        }
        assertEquals(5, filas,
                "La tabla debe mostrar los 5 procedimientos del catalogo con su precio vigente");

        int diagnosticos = 0;
        long limiteCie = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < limiteCie && diagnosticos == 0) {
            diagnosticos = enFx(() -> tablaCie.getItems().size());
            if (diagnosticos == 0) {
                Thread.sleep(200);
            }
        }
        assertTrue(diagnosticos >= 5, "La tabla CIE-10 debe cargar los diagnosticos de la base");

        enFx(() -> {
            tablaCie.getSelectionModel().select(0);
            return null;
        });
        Label codigoDetalle = (Label) pantalla.lookup("#etiquetaCieCodigo");
        String codigoEsperado = enFx(() -> ((pe.edu.utp.dentrix.model.DiagnosticoCie10)
                ((TableView<pe.edu.utp.dentrix.model.DiagnosticoCie10>) tablaCie).getItems().get(0)).getCodigo());
        assertEquals(codigoEsperado, enFx(codigoDetalle::getText),
                "La ficha tecnica del CIE-10 debe reflejar el diagnostico seleccionado");
    }

    private static <T> T enFx(java.util.concurrent.Callable<T> accion) throws Exception {
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
}
