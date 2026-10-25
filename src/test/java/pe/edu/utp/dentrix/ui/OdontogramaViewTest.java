package pe.edu.utp.dentrix.ui;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OdontogramaViewTest {

    @BeforeAll
    static void arrancarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaArrancado) {
            // JavaFX ya fue iniciado por otra prueba de interfaz.
        }
    }

    @Test
    void conservaLas32PiezasYSeAdaptaAlEspacioDisponible() throws Exception {
        CountDownLatch listo = new CountDownLatch(1);
        AtomicReference<Throwable> fallo = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                OdontogramaView vista = new OdontogramaView();
                assertEquals(32, vista.getCantidadPiezas());

                vista.resize(320, 160);
                vista.layout();
                assertEquals(320, vista.getWidth());
                assertEquals(160, vista.getHeight());

                vista.setEstado(18, "CARIES");
                vista.setEstado(31, "OBTURADO", "oclusal");
                vista.setEstado(48, "AUSENTE");
                vista.setEstado(24, "ENDODONCIA");
                vista.setEstado(36, "IMPLANTE");
                assertEquals("Molar", OdontogramaView.tipoPieza(18));
                assertEquals("Incisivo", OdontogramaView.tipoPieza(31));
            } catch (Throwable t) {
                fallo.set(t);
            } finally {
                listo.countDown();
            }
        });

        assertTrue(listo.await(10, TimeUnit.SECONDS), "JavaFX no terminó la prueba del odontograma");
        if (fallo.get() != null) {
            throw new AssertionError("Falló el odontograma adaptable", fallo.get());
        }
    }
}
