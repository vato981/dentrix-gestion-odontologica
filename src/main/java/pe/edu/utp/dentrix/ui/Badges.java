package pe.edu.utp.dentrix.ui;

import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

public final class Badges {

    private Badges() {
    }

    public static Label estado(String valor) {
        Label etiqueta = new Label(valor == null ? "" : valor.replace('_', ' '));
        etiqueta.getStyleClass().addAll("badge", claseDe(valor));
        return etiqueta;
    }

    public static <T> void aplicarEn(TableColumn<T, String> columna) {
        columna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean vacio) {
                super.updateItem(item, vacio);
                if (vacio || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(estado(item));
                }
            }
        });
    }

    private static String claseDe(String valor) {
        if (valor == null) {
            return "badge-gris";
        }
        return switch (valor) {
            case "PENDIENTE" -> "badge-gris";
            case "CONFIRMADA", "PAGADO", "ACTIVO" -> "badge-verde";
            case "EN_SALA" -> "badge-amarillo";
            case "EN_ATENCION" -> "badge-naranja";
            case "ATENDIDA" -> "badge-verde";
            case "FALTO", "ANULADO", "BLOQUEADA" -> "badge-rojo";
            case "CANCELADA", "BAJA", "SUSPENDIDA", "ELIMINADO" -> "badge-gris";
            default -> "badge-gris";
        };
    }
}
