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

    public static Label rol(String nombre) {
        String texto = nombre == null ? "" : nombre;
        String clave = texto.toLowerCase();
        String clase;
        if (clave.contains("admin")) {
            clase = "badge-amarillo";
        } else if (clave.contains("odonto")) {
            clase = "badge-azul";
        } else if (clave.contains("recep")) {
            clase = "badge-gris";
        } else {
            clase = "badge-gris";
        }
        Label etiqueta = new Label(texto.isBlank() ? "—" : texto);
        etiqueta.getStyleClass().addAll("badge", clase);
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
            case "PENDIENTE", "BORRADOR" -> "badge-gris";
            case "CONFIRMADA", "PAGADO", "ACTIVO", "GENERADA" -> "badge-verde";
            case "EMITIDO" -> "badge-azul";
            case "PARCIAL" -> "badge-amarillo";
            case "EN_SALA" -> "badge-amarillo";
            case "EN_ATENCION" -> "badge-naranja";
            case "ATENDIDA" -> "badge-verde";
            case "FALTO", "ANULADO", "BLOQUEADA" -> "badge-rojo";
            case "CANCELADA", "BAJA", "SUSPENDIDA", "ELIMINADO" -> "badge-gris";
            default -> "badge-gris";
        };
    }
}
