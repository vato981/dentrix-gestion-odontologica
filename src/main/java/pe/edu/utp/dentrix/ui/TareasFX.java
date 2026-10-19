package pe.edu.utp.dentrix.ui;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class TareasFX {

    private TareasFX() {
    }

    public static <T> void ejecutar(Callable<T> trabajo, Consumer<T> alTerminar, Consumer<Throwable> alFallar) {
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabajo.call();
            }
        };
        tarea.setOnSucceeded(evento -> alTerminar.accept(tarea.getValue()));
        tarea.setOnFailed(evento -> alFallar.accept(
                tarea.getException() == null
                        ? new IllegalStateException("La tarea no pudo completarse")
                        : tarea.getException()));
        Thread.ofVirtual().name("dentrix-tarea").start(tarea);
    }

    public static String mensaje(Throwable error) {
        String mensaje = error.getMessage();
        return mensaje == null || mensaje.isBlank() ? error.getClass().getSimpleName() : mensaje;
    }
}
