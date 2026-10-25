package pe.edu.utp.dentrix.ui;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Scale;

import java.util.ArrayList;
import java.util.List;

/** Vista anatómica de una pieza dental, sin responsabilidades de persistencia. */
final class PiezaDentalView extends Group {

    static final double ANCHO = 56;
    static final double ALTO = 136;

    private static final Paint ESMALTE = new RadialGradient(0, 0, 0.38, 0.32, 0.95, true,
            CycleMethod.NO_CYCLE, new Stop(0, Color.WHITE), new Stop(0.55, Color.web("#F8F3E7")),
            new Stop(1, Color.web("#D8CCB4")));
    private static final Paint RAIZ = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#E6C993")), new Stop(0.5, Color.web("#F5E6C2")),
            new Stop(1, Color.web("#C6A263")));
    private static final Paint RAIZ_ENDODONCIA = new LinearGradient(0, 0, 1, 0, true,
            CycleMethod.NO_CYCLE, new Stop(0, Color.web("#8E24AA")),
            new Stop(0.5, Color.web("#CE93D8")), new Stop(1, Color.web("#6A1B9A")));

    private final int fdi;
    private final boolean superior;
    private final boolean mesialDerecha;
    private final Rectangle fondo = new Rectangle(2, 0, ANCHO - 4, ALTO);
    private final List<SVGPath> raices = new ArrayList<>();
    private final SVGPath corona = new SVGPath();
    private final Group grupoRaices = new Group();
    private final Group parches = new Group();
    private final Group extras = new Group();
    private final Group cuerpo;
    private boolean seleccionada;
    private boolean resaltada;

    PiezaDentalView(int fdi) {
        this.fdi = fdi;
        int cuadrante = fdi / 10;
        superior = cuadrante == 1 || cuadrante == 2;
        mesialDerecha = cuadrante == 1 || cuadrante == 4;

        corona.setContent(coronaPath(fdi % 10));
        corona.setFill(ESMALTE);
        corona.setStroke(Color.web("#8F8470"));
        corona.setStrokeWidth(0.9);

        for (String path : raicesPath(fdi, fdi % 10)) {
            SVGPath raiz = new SVGPath();
            raiz.setContent(path);
            raiz.setFill(RAIZ);
            raiz.setStroke(Color.web("#A38650"));
            raiz.setStrokeWidth(0.9);
            raices.add(raiz);
            grupoRaices.getChildren().add(raiz);
        }

        SVGPath recorteCorona = new SVGPath();
        recorteCorona.setContent(coronaPath(fdi % 10));
        parches.setClip(recorteCorona);
        parches.setMouseTransparent(true);
        extras.setMouseTransparent(true);

        cuerpo = new Group(grupoRaices, corona, parches, extras);
        if (!superior) {
            cuerpo.getTransforms().add(new Scale(1, -1, 0, ALTO / 2));
        }

        fondo.setArcWidth(12);
        fondo.setArcHeight(12);
        fondo.setFill(Color.TRANSPARENT);
        fondo.setStroke(Color.TRANSPARENT);
        getChildren().addAll(fondo, cuerpo);
        actualizar("SANO", null);
    }

    void setSeleccionada(boolean valor) {
        seleccionada = valor;
        refrescarFondo();
    }

    void setResaltada(boolean valor) {
        resaltada = valor;
        refrescarFondo();
    }

    private void refrescarFondo() {
        if (seleccionada) {
            fondo.setFill(Color.web("#CCFBF1"));
            fondo.setStroke(Color.web("#0D9488"));
            fondo.setStrokeWidth(1.5);
        } else if (resaltada) {
            fondo.setFill(Color.web("#E6FFFB"));
            fondo.setStroke(Color.TRANSPARENT);
        } else {
            fondo.setFill(Color.TRANSPARENT);
            fondo.setStroke(Color.TRANSPARENT);
        }
    }

    void actualizar(String estado, String cara) {
        String normalizado = estado == null ? "SANO" : estado.toUpperCase();
        parches.getChildren().clear();
        extras.getChildren().clear();
        grupoRaices.setVisible(true);
        cuerpo.setOpacity(1);
        corona.setFill(ESMALTE);
        for (SVGPath raiz : raices) {
            raiz.setFill(RAIZ);
        }

        switch (normalizado) {
            case "CARIES" -> parches.getChildren().add(parche(cara, Color.web("#E53935")));
            case "OBTURADO" -> parches.getChildren().add(parche(cara, Color.web("#1E88E5")));
            case "PROTESIS" -> parches.getChildren().add(parche(cara, Color.web("#7E57C2")));
            case "CORONA" -> corona.setFill(new LinearGradient(0, 0, 1, 1, true,
                    CycleMethod.NO_CYCLE, new Stop(0, Color.web("#FFF3B0")),
                    new Stop(0.5, Color.web("#D4AF37")), new Stop(1, Color.web("#FFF8DC"))));
            case "ENDODONCIA" -> dibujarEndodoncia();
            case "IMPLANTE" -> dibujarImplante();
            case "AUSENTE" -> dibujarAusente();
            default -> { }
        }
    }

    private Ellipse parche(String cara, Color color) {
        String codigo = cara == null ? "" : cara.toLowerCase();
        Ellipse lesion = switch (codigo) {
            case "oclusal" -> new Ellipse(28, 121, 15, 7);
            case "mesial" -> new Ellipse(mesialDerecha ? 45 : 11, 101, 7, 18);
            case "distal" -> new Ellipse(mesialDerecha ? 11 : 45, 101, 7, 18);
            case "lingual" -> new Ellipse(28, 86, 15, 6);
            case "vestibular" -> new Ellipse(28, 103, 14, 17);
            default -> new Ellipse(28, 103, 17, 21);
        };
        lesion.setFill(color.deriveColor(0, 1, 1, 0.88));
        lesion.setStroke(color.darker());
        lesion.setStrokeWidth(1.1);
        return lesion;
    }

    private void dibujarEndodoncia() {
        for (SVGPath raiz : raices) {
            raiz.setFill(RAIZ_ENDODONCIA);
            Line canal = new Line(raiz.getBoundsInLocal().getCenterX(),
                    raiz.getBoundsInLocal().getMinY() + 6,
                    raiz.getBoundsInLocal().getCenterX(),
                    raiz.getBoundsInLocal().getMaxY() - 4);
            canal.setStroke(Color.web("#FFFFFFCC"));
            canal.setStrokeWidth(1.4);
            extras.getChildren().add(canal);
        }
    }

    private void dibujarImplante() {
        grupoRaices.setVisible(false);
        SVGPath tornillo = new SVGPath();
        tornillo.setContent("M 22 18 Q 28 12 34 18 L 34 74 Q 28 80 22 74 Z");
        tornillo.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1565C0")), new Stop(0.45, Color.web("#90CAF9")),
                new Stop(1, Color.web("#0D47A1"))));
        tornillo.setStroke(Color.web("#0D47A1"));
        extras.getChildren().add(tornillo);
        for (int y = 27; y <= 69; y += 6) {
            Line rosca = new Line(21, y, 35, y + 3);
            rosca.setStroke(Color.web("#E3F2FD"));
            extras.getChildren().add(rosca);
        }
    }

    private void dibujarAusente() {
        cuerpo.setOpacity(0.28);
        for (double[] puntos : new double[][]{{8, 10, 48, 128}, {48, 10, 8, 128}}) {
            Line linea = new Line(puntos[0], puntos[1], puntos[2], puntos[3]);
            linea.setStroke(Color.web("#616161"));
            linea.setStrokeWidth(3);
            linea.setStrokeLineCap(StrokeLineCap.ROUND);
            extras.getChildren().add(linea);
        }
    }

    private static String coronaPath(int pos) {
        return switch (pos) {
            case 1 -> "M 8 76 C 7 94 9 112 14 124 C 18 131 38 131 42 124 C 47 112 49 94 48 76 C 40 71 16 71 8 76 Z";
            case 2 -> "M 11 76 C 10 93 12 110 16 121 C 20 128 36 128 40 121 C 44 110 46 93 45 76 C 38 72 18 72 11 76 Z";
            case 3 -> "M 10 78 C 9 94 12 108 28 130 C 44 108 47 94 46 78 C 38 73 18 73 10 78 Z";
            case 4, 5 -> "M 9 78 C 8 94 10 112 15 122 C 19 128 25 125 28 120 C 31 125 37 128 41 122 C 46 112 48 94 47 78 C 39 73 17 73 9 78 Z";
            default -> "M 4 78 C 3 96 5 114 10 124 C 14 130 20 126 24 121 C 27 125 30 125 32 121 C 36 126 42 130 46 124 C 51 114 53 96 52 78 C 42 72 14 72 4 78 Z";
        };
    }

    private static List<String> raicesPath(int fdi, int pos) {
        boolean superior = fdi / 10 <= 2;
        return switch (pos) {
            case 1 -> List.of("M 15 76 C 17 52 22 22 28 4 C 34 22 39 52 41 76 C 34 80 22 80 15 76 Z");
            case 2 -> List.of("M 17 76 C 19 52 24 20 28 2 C 32 20 37 52 39 76 C 33 79 23 79 17 76 Z");
            case 3 -> List.of("M 16 78 C 18 50 24 14 28 0 C 32 14 38 50 40 78 C 34 82 22 82 16 78 Z");
            case 4 -> superior
                    ? List.of("M 15 78 C 15 55 17 30 19 12 C 22 30 26 55 28 78 Z",
                    "M 28 78 C 30 55 34 30 37 12 C 39 30 41 55 41 78 Z")
                    : List.of("M 15 78 C 17 52 23 22 28 6 C 33 22 39 52 41 78 C 34 82 22 82 15 78 Z");
            case 5 -> List.of("M 15 78 C 17 52 23 22 28 6 C 33 22 39 52 41 78 C 34 82 22 82 15 78 Z");
            default -> superior
                    ? List.of("M 8 78 C 8 56 9 34 11 16 C 15 36 20 58 22 78 Z",
                    "M 34 78 C 36 58 41 36 45 16 C 47 34 48 56 48 78 Z",
                    "M 22 78 C 24 52 26 28 28 8 C 30 28 32 52 34 78 Z")
                    : List.of("M 8 78 C 8 56 9 36 12 14 C 16 36 21 58 26 78 Z",
                    "M 30 78 C 35 58 40 36 44 14 C 47 36 48 56 48 78 Z");
        };
    }
}
