package pe.edu.utp.dentrix.ui;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntConsumer;

public class OdontogramaView extends Pane {

    private static final int[] SUPERIORES = {
            18, 17, 16, 15, 14, 13, 12, 11,
            21, 22, 23, 24, 25, 26, 27, 28
    };

    private static final int[] INFERIORES = {
            48, 47, 46, 45, 44, 43, 42, 41,
            31, 32, 33, 34, 35, 36, 37, 38
    };

    private static final double ANCHO = 480;
    private static final double ALTO = 222;
    private static final double CELDA = 29;
    private static final double FIGURA_ANCHO = 28;
    private static final double FIGURA_ALTO = 46;
    private static final double CURVA = 16;
    private static final double Y_SUPERIOR = 44;
    private static final double Y_INFERIOR = 138;
    private static final double ESCALA_MAXIMA = 1.2;

    private final Map<Integer, Tooth> dientes = new LinkedHashMap<>();
    private Integer seleccionada;
    private IntConsumer alSeleccionar;

    public OdontogramaView() {
        setPrefSize(ANCHO, ALTO);
        setMinSize(ANCHO, ALTO);
        setMaxSize(ANCHO, ALTO);
        getStyleClass().add("odontograma");
        crearArcada(SUPERIORES, true);
        crearArcada(INFERIORES, false);
    }

    public void setOnPiezaSeleccionada(IntConsumer listener) {
        this.alSeleccionar = listener;
    }

    public void setEstado(int pieza, String estado) {
        Tooth diente = dientes.get(pieza);
        if (diente != null) {
            diente.estado = estado == null || estado.isBlank() ? "SANO" : estado.toUpperCase();
            aplicarEstilo(diente);
        }
    }

    public void limpiarEstados() {
        for (Tooth diente : dientes.values()) {
            diente.estado = "SANO";
            aplicarEstilo(diente);
        }
    }

    public void reiniciar() {
        seleccionada = null;
        limpiarEstados();
    }

    public static String tipoPieza(int pieza) {
        return switch (pieza % 10) {
            case 1, 2 -> "Incisivo";
            case 3 -> "Canino";
            case 4, 5 -> "Premolar";
            default -> "Molar";
        };
    }

    private void crearArcada(int[] piezas, boolean superior) {
        double inicio = (ANCHO - CELDA * piezas.length) / 2;
        for (int i = 0; i < piezas.length; i++) {
            int numero = piezas[i];
            double distancia = (i - 7.5) / 7.5;
            double curva = CURVA * distancia * distancia;
            double x = inicio + i * CELDA;
            double y = superior ? Y_SUPERIOR - curva : Y_INFERIOR + curva;

            SVGPath figura = new SVGPath();
            figura.setContent(forma(numero));
            figura.setFill(Color.WHITE);
            figura.setStroke(Color.web("#64748B"));
            figura.setStrokeWidth(1.3);

            double anchoBase = figura.getLayoutBounds().getWidth();
            double altoBase = figura.getLayoutBounds().getHeight();
            double factor = Math.min((FIGURA_ANCHO - 4) / anchoBase, (FIGURA_ALTO - 4) / altoBase)
                    / ESCALA_MAXIMA * escala(numero);
            figura.setScaleX(factor);
            figura.setScaleY(factor);
            figura.setRotate(superior ? 180 : 0);

            StackPane caja = new StackPane(figura);
            caja.setPrefSize(FIGURA_ANCHO, FIGURA_ALTO);
            caja.setMinSize(FIGURA_ANCHO, FIGURA_ALTO);
            caja.setMaxSize(FIGURA_ANCHO, FIGURA_ALTO);

            Label etiqueta = new Label(String.valueOf(numero));
            VBox contenedor = superior
                    ? new VBox(2, etiqueta, caja)
                    : new VBox(2, caja, etiqueta);
            contenedor.setAlignment(Pos.CENTER);
            contenedor.setPrefWidth(CELDA);
            contenedor.setMinWidth(CELDA);
            contenedor.setMaxWidth(CELDA);
            contenedor.setLayoutX(x);
            contenedor.setLayoutY(y);
            contenedor.setCursor(Cursor.HAND);

            Tooltip tooltip = new Tooltip();
            Tooltip.install(contenedor, tooltip);

            Tooth diente = new Tooth(numero, figura, etiqueta, contenedor, tooltip);
            contenedor.setOnMouseClicked(e -> {
                seleccionada = numero;
                for (Tooth t : dientes.values()) {
                    aplicarEstilo(t);
                }
                if (alSeleccionar != null) {
                    alSeleccionar.accept(numero);
                }
            });
            contenedor.setOnMouseEntered(e -> {
                figura.setStroke(Color.web("#0D9488"));
                figura.setStrokeWidth(2.2);
            });
            contenedor.setOnMouseExited(e -> aplicarEstilo(diente));

            getChildren().add(contenedor);
            dientes.put(numero, diente);
            aplicarEstilo(diente);
        }
    }

    private void aplicarEstilo(Tooth diente) {
        Color[] colores = colores(diente.estado);
        boolean activo = Objects.equals(seleccionada, diente.pieza);

        diente.figura.setFill(colores[0]);
        diente.figura.setOpacity("AUSENTE".equals(diente.estado) ? 0.45 : 1);
        if (activo) {
            diente.figura.setStroke(Color.web("#0F766E"));
            diente.figura.setStrokeWidth(2.4);
        } else {
            diente.figura.setStroke(colores[1]);
            diente.figura.setStrokeWidth(1.3);
        }

        diente.etiqueta.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: "
                + (activo ? "#0F766E" : "#475569") + ";");
        diente.contenedor.setStyle(activo
                ? "-fx-background-color: rgba(13, 148, 136, 0.10); -fx-background-radius: 8;"
                : "");
        diente.tooltip.setText("Pieza " + diente.pieza + " · " + tipoPieza(diente.pieza)
                + " · " + diente.estado);
    }

    private Color[] colores(String estado) {
        return switch (estado) {
            case "CARIES" -> new Color[]{Color.web("#FEE2E2"), Color.web("#991B1B")};
            case "OBTURADO" -> new Color[]{Color.web("#DBEAFE"), Color.web("#1E40AF")};
            case "AUSENTE" -> new Color[]{Color.web("#E5E7EB"), Color.web("#6B7280")};
            case "CORONA" -> new Color[]{Color.web("#FEF3C7"), Color.web("#92400E")};
            case "IMPLANTE" -> new Color[]{Color.web("#D1FAE5"), Color.web("#065F46")};
            case "ENDODONCIA" -> new Color[]{Color.web("#FFEDD5"), Color.web("#9A3412")};
            case "PROTESIS" -> new Color[]{Color.web("#EDE9FE"), Color.web("#5B21B6")};
            default -> new Color[]{Color.WHITE, Color.web("#64748B")};
        };
    }

    private double escala(int numero) {
        return switch (numero % 10) {
            case 1, 2 -> 0.85;
            case 3 -> 0.95;
            case 4, 5 -> 1.05;
            default -> ESCALA_MAXIMA;
        };
    }

    private String forma(int numero) {
        int posicion = numero % 10;
        if (posicion == 1 || posicion == 2) {
            return "M 7 3 Q 15 -1 23 3 "
                    + "L 24 15 Q 23 21 20 26 "
                    + "L 18 43 Q 15 50 12 43 "
                    + "L 10 26 Q 7 21 6 15 Z";
        }
        if (posicion == 3) {
            return "M 15 0 L 27 13 "
                    + "Q 29 21 23 29 "
                    + "L 19 46 Q 15 52 11 46 "
                    + "L 7 29 Q 1 21 3 13 Z";
        }
        if (posicion == 4 || posicion == 5) {
            return "M 4 8 Q 6 0 13 4 "
                    + "L 16 7 L 20 4 "
                    + "Q 27 0 29 8 "
                    + "L 26 27 Q 23 34 20 40 "
                    + "Q 16 47 13 40 "
                    + "Q 9 34 6 27 Z";
        }
        return "M 3 7 Q 4 0 10 3 "
                + "L 14 6 L 18 3 "
                + "Q 24 0 29 7 "
                + "L 28 25 "
                + "Q 25 32 24 43 "
                + "Q 22 49 18 42 "
                + "L 16 33 L 14 42 "
                + "Q 10 49 8 43 "
                + "Q 7 32 4 25 Z";
    }

    private static final class Tooth {
        private final int pieza;
        private final SVGPath figura;
        private final Label etiqueta;
        private final VBox contenedor;
        private final Tooltip tooltip;
        private String estado = "SANO";

        private Tooth(int pieza, SVGPath figura, Label etiqueta, VBox contenedor, Tooltip tooltip) {
            this.pieza = pieza;
            this.figura = figura;
            this.etiqueta = etiqueta;
            this.contenedor = contenedor;
            this.tooltip = tooltip;
        }
    }
}
