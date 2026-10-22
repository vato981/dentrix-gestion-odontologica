package pe.edu.utp.dentrix.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Paciente;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Genera capturas reales de la aplicacion JavaFX para el video de procesos.
 * No forma parte de la suite funcional: se ejecuta solo con -Dtest=CapturasAppTest.
 */
class CapturasAppTest {

    private static final int ANCHO = 1440;
    private static final int ALTO = 920;

    private static void arrancarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaArrancado) {
        }
        Platform.setImplicitExit(false);
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
        if (!listo.await(40, TimeUnit.SECONDS)) {
            throw new AssertionError("La operacion en el hilo de JavaFX no termino");
        }
        if (fallo.get() != null) {
            throw new AssertionError(fallo.get().getMessage(), fallo.get());
        }
        return valor.get();
    }

    private static void esperar(long ms) throws Exception {
        Thread.sleep(ms);
    }

    @Test
    void capturarPantallasReales() throws Exception {
        Connection conexion = null;
        try {
            conexion = ConexionBD.abrir();
        } catch (DentrixException e) {
            conexion = null;
        }
        assumeTrue(conexion != null, "config.properties o BD no disponibles");
        conexion.close();

        String destino = System.getenv("CAPTURAS_DIR");
        if (destino == null || destino.isBlank()) {
            destino = System.getProperty("capturas.dir");
        }
        assumeTrue(destino != null && !destino.isBlank(), "Falta CAPTURAS_DIR");
        File carpeta = new File(destino);
        carpeta.mkdirs();

        AppContext contexto = AppContext.get();
        var usuario = contexto.usuarios().buscarPorUsername("jvallejos")
                .orElseThrow(() -> new IllegalStateException("Falta el usuario jvallejos"));
        var rol = contexto.roles().buscarPorId(usuario.getRolId())
                .orElseThrow(() -> new IllegalStateException("Falta el rol de jvallejos"));
        contexto.sesion().abrir(usuario, rol);
        contexto.sesion().setPacienteEnAtencion(null);

        Paciente maria = contexto.pacientes().listar().stream()
                .filter(p -> Integer.valueOf(2).equals(p.getId()))
                .findFirst().orElse(null);

        arrancarJavaFx();

        capturar(carpeta, "M2_panel", "/pe/edu/utp/dentrix/fxml/dashboard.fxml", null, 2600);
        capturar(carpeta, "M3_recepcion", "/pe/edu/utp/dentrix/fxml/sala-espera.fxml", null, 2600);

        capturar(carpeta, "M4_pacientes", "/pe/edu/utp/dentrix/fxml/pacientes.fxml", raiz -> {
            TableView<?> tabla = (TableView<?>) raiz.lookup("#tablaPacientes");
            if (tabla != null) {
                tabla.getItems().stream()
                        .filter(f -> f instanceof Paciente p && Integer.valueOf(2).equals(p.getId()))
                        .findFirst().ifPresent(f -> ((TableView) tabla).getSelectionModel().select(f));
            }
        }, 2600);

        capturar(carpeta, "M5_agenda", "/pe/edu/utp/dentrix/fxml/agenda.fxml", raiz -> {
            TableView<?> tabla = (TableView<?>) raiz.lookup("#tablaAgenda");
            if (tabla != null && !tabla.getItems().isEmpty()) {
                ((TableView) tabla).getSelectionModel().select(0);
            }
        }, 3000);

        if (maria != null) {
            contexto.sesion().setPacienteEnAtencion(maria);
        }
        capturar(carpeta, "M6_ficha", "/pe/edu/utp/dentrix/fxml/ficha-clinica.fxml", raiz -> {
            ComboBox<?> fichas = (ComboBox<?>) raiz.lookup("#comboFichas");
            if (fichas != null && !fichas.getItems().isEmpty()) {
                Object elegida = null;
                for (Object f : fichas.getItems()) {
                    if (f instanceof pe.edu.utp.dentrix.model.FichaClinica fc
                            && Long.valueOf(2L).equals(fc.getId())) { elegida = f; break; }
                }
                if (elegida == null) { elegida = fichas.getItems().get(0); }
                ((ComboBox) fichas).setValue(elegida);
            }
            TableView<?> diagnosticos = (TableView<?>) raiz.lookup("#tablaDiagnosticos");
            if (diagnosticos != null && !diagnosticos.getItems().isEmpty()) {
                ((TableView) diagnosticos).getSelectionModel().select(0);
            }
        }, 3600);

        capturar(carpeta, "M6_ficha_diag", "/pe/edu/utp/dentrix/fxml/ficha-clinica.fxml", raiz -> {
            ComboBox<?> fichas = (ComboBox<?>) raiz.lookup("#comboFichas");
            if (fichas != null && !fichas.getItems().isEmpty()) {
                Object elegida = null;
                for (Object f : fichas.getItems()) {
                    if (f instanceof pe.edu.utp.dentrix.model.FichaClinica fc
                            && Long.valueOf(2L).equals(fc.getId())) { elegida = f; break; }
                }
                if (elegida == null) { elegida = fichas.getItems().get(0); }
                ((ComboBox) fichas).setValue(elegida);
            }
            for (javafx.scene.Node nodo : raiz.lookupAll(".scroll-pane")) {
                if (nodo instanceof javafx.scene.control.ScrollPane pane
                        && pane.getLayoutX() > 500) {
                    pane.setVvalue(0.62);
                }
            }
        }, 3600);

        capturar(carpeta, "M10_presupuesto", "/pe/edu/utp/dentrix/fxml/presupuesto.fxml", null, 3600);

        contexto.sesion().setPacienteEnAtencion(maria);
        capturar(carpeta, "M7_caja", "/pe/edu/utp/dentrix/fxml/caja.fxml", raiz -> {
            ComboBox<?> combo = (ComboBox<?>) raiz.lookup("#comboPaciente");
            if (combo != null) {
                combo.getItems().stream()
                        .filter(p -> p instanceof Paciente pa && Integer.valueOf(2).equals(pa.getId()))
                        .findFirst().ifPresent(p -> ((ComboBox) combo).setValue(p));
            }
        }, 3000);

        capturar(carpeta, "M11_ejecucion", "/pe/edu/utp/dentrix/fxml/ejecucion.fxml", null, 3600);

        contexto.sesion().setPacienteEnAtencion(null);
        capturar(carpeta, "M8_reportes", "/pe/edu/utp/dentrix/fxml/reportes.fxml", null, 3000);
    }

    private void capturar(File carpeta, String nombre, String fxml, Consumer<Parent> preparar,
                          long esperaCargaMs) throws Exception {
        Parent raiz = enFx(() -> {
            Parent cargado = FXMLLoader.load(getClass().getResource(fxml));
            Scene escena = new Scene(cargado, ANCHO, ALTO);
            escena.setFill(javafx.scene.paint.Color.WHITE);
            String css = getClass().getResource("/pe/edu/utp/dentrix/estilos.css").toExternalForm();
            escena.getStylesheets().add(css);
            Stage stage = new Stage();
            stage.setScene(escena);
            stage.show();
            cargado.applyCss();
            cargado.layout();
            return cargado;
        });
        esperar(esperaCargaMs);
        if (preparar != null) {
            enFx(() -> {
                preparar.accept(raiz);
                return null;
            });
            esperar(1600);
        }
        enFx(() -> {
            raiz.applyCss();
            raiz.layout();
            return null;
        });
        WritableImage imagen = enFx(() -> raiz.getScene().snapshot(null));
        guardar(imagen, new File(carpeta, nombre + ".png"));
        enFx(() -> {
            ((Stage) raiz.getScene().getWindow()).close();
            return null;
        });
        System.out.println("CAPTURA " + nombre + " -> " + new File(carpeta, nombre + ".png").getAbsolutePath());
    }

    private void guardar(WritableImage imagen, File archivo) throws Exception {
        int ancho = (int) imagen.getWidth();
        int alto = (int) imagen.getHeight();
        BufferedImage salida = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        PixelReader lector = imagen.getPixelReader();
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                salida.setRGB(x, y, lector.getArgb(x, y));
            }
        }
        ImageIO.write(salida, "png", archivo);
    }
}
