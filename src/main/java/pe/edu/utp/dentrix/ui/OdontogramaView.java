package pe.edu.utp.dentrix.ui;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntConsumer;

/** Odontograma adulto FDI adaptable. La geometría visual no conoce la persistencia. */
public class OdontogramaView extends Pane {
    private static final int[] SUPERIORES = {18,17,16,15,14,13,12,11,21,22,23,24,25,26,27,28};
    private static final int[] INFERIORES = {48,47,46,45,44,43,42,41,31,32,33,34,35,36,37,38};
    private static final double ANCHO = 940, ALTO = 330, CELDA = 56, INICIO_X = 22;
    private static final double Y_SUPERIOR = 10, Y_INFERIOR = 182, MARGEN = 10;

    private final Map<Integer, Tooth> dientes = new LinkedHashMap<>();
    private final Group contenido = new Group();
    private Integer seleccionada;
    private IntConsumer alSeleccionar;

    public OdontogramaView() {
        setPrefSize(ANCHO, ALTO);
        setMinSize(240, 110);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setPickOnBounds(false);
        getStyleClass().add("odontograma");
        getChildren().add(contenido);

        Line lineaMedia = new Line(ANCHO / 2, 18, ANCHO / 2, ALTO - 18);
        lineaMedia.setStroke(Color.web("#CBD5E1"));
        lineaMedia.getStrokeDashArray().setAll(4.0, 5.0);
        lineaMedia.setMouseTransparent(true);
        contenido.getChildren().addAll(lineaMedia,
                etiquetaArcada("ARCADA SUPERIOR", 2), etiquetaArcada("ARCADA INFERIOR", ALTO - 17));
        crearArcada(SUPERIORES, true);
        crearArcada(INFERIORES, false);
    }

    private Label etiquetaArcada(String texto, double y) {
        Label etiqueta = new Label(texto);
        etiqueta.setLayoutX(ANCHO / 2 - 68);
        etiqueta.setLayoutY(y);
        etiqueta.setPrefWidth(136);
        etiqueta.setAlignment(Pos.CENTER);
        etiqueta.setMouseTransparent(true);
        etiqueta.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        return etiqueta;
    }

    @Override
    protected void layoutChildren() {
        double disponibleX = Math.max(0, getWidth() - MARGEN * 2);
        double disponibleY = Math.max(0, getHeight() - MARGEN * 2);
        double escala = Math.min(disponibleX / ANCHO, disponibleY / ALTO);
        if (!Double.isFinite(escala) || escala <= 0) return;
        contenido.setScaleX(escala);
        contenido.setScaleY(escala);
        contenido.setLayoutX((getWidth() - ANCHO) / 2);
        contenido.setLayoutY((getHeight() - ALTO) / 2);
    }

    @Override
    protected double computePrefHeight(double width) {
        return width > 0 ? Math.max(110, (width - MARGEN * 2) * ALTO / ANCHO + MARGEN * 2) : ALTO;
    }

    public int getCantidadPiezas() { return dientes.size(); }

    public void setOnPiezaSeleccionada(IntConsumer listener) { alSeleccionar = listener; }

    public void setEstado(int pieza, String estado) { setEstado(pieza, estado, null); }

    public void setEstado(int pieza, String estado, String cara) {
        Tooth diente = dientes.get(pieza);
        if (diente == null) return;
        diente.estado = estado == null || estado.isBlank() ? "SANO" : estado.toUpperCase();
        diente.cara = cara;
        aplicarEstilo(diente);
    }

    public void limpiarEstados() {
        for (Tooth diente : dientes.values()) {
            diente.estado = "SANO";
            diente.cara = null;
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
        for (int i = 0; i < piezas.length; i++) {
            int numero = piezas[i];
            PiezaDentalView figura = new PiezaDentalView(numero);
            Label etiqueta = new Label(String.valueOf(numero));
            etiqueta.setPrefWidth(CELDA);
            etiqueta.setAlignment(Pos.CENTER);
            etiqueta.setLayoutY(superior ? PiezaDentalView.ALTO - 1 : -17);

            Group contenedor = new Group(figura, etiqueta);
            contenedor.setLayoutX(INICIO_X + i * CELDA);
            contenedor.setLayoutY(superior ? Y_SUPERIOR : Y_INFERIOR);
            contenedor.setCursor(Cursor.HAND);
            Tooltip tooltip = new Tooltip();
            Tooltip.install(contenedor, tooltip);
            Tooth diente = new Tooth(numero, figura, etiqueta, tooltip);
            contenedor.setOnMouseClicked(e -> seleccionar(numero));
            contenedor.setOnMouseEntered(e -> figura.setResaltada(true));
            contenedor.setOnMouseExited(e -> figura.setResaltada(false));
            contenido.getChildren().add(contenedor);
            dientes.put(numero, diente);
            aplicarEstilo(diente);
        }
    }

    private void seleccionar(int numero) {
        seleccionada = numero;
        dientes.values().forEach(this::aplicarEstilo);
        if (alSeleccionar != null) alSeleccionar.accept(numero);
    }

    private void aplicarEstilo(Tooth diente) {
        boolean activa = Objects.equals(seleccionada, diente.pieza);
        diente.figura.setSeleccionada(activa);
        diente.figura.actualizar(diente.estado, diente.cara);
        diente.etiqueta.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: "
                + (activa ? "#0F766E" : "#475569") + ";");
        String superficie = diente.cara == null || diente.cara.isBlank()
                ? "pieza completa" : "cara " + diente.cara;
        diente.tooltip.setText("Pieza " + diente.pieza + " · " + tipoPieza(diente.pieza)
                + " · " + diente.estado + " · " + superficie);
    }

    private static final class Tooth {
        private final int pieza;
        private final PiezaDentalView figura;
        private final Label etiqueta;
        private final Tooltip tooltip;
        private String estado = "SANO";
        private String cara;

        private Tooth(int pieza, PiezaDentalView figura, Label etiqueta, Tooltip tooltip) {
            this.pieza = pieza;
            this.figura = figura;
            this.etiqueta = etiqueta;
            this.tooltip = tooltip;
        }
    }
}
