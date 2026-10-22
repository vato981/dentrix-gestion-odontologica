package pe.edu.utp.dentrix.ui;

import java.util.function.Consumer;

public final class Navegador {

    public static final String INICIO = "/pe/edu/utp/dentrix/fxml/dashboard.fxml";

    public static final String PRESUPUESTOS = "/pe/edu/utp/dentrix/fxml/presupuesto.fxml";

    public static final String EJECUCION = "/pe/edu/utp/dentrix/fxml/ejecucion.fxml";

    private static Consumer<String> manejador;

    private Navegador() {
    }

    public static void registrar(Consumer<String> destino) {
        manejador = destino;
    }

    public static void limpiar() {
        manejador = null;
    }

    public static void ir(String fxml) {
        if (manejador != null) {
            manejador.accept(fxml);
        }
    }
}
