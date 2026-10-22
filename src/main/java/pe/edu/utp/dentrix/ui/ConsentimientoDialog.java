package pe.edu.utp.dentrix.ui;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.Optional;

public final class ConsentimientoDialog {

    public record Datos(String procedimiento, String riesgos, String alternativas,
                        String firmante, String documento, String rol, String vinculo) {
    }

    private ConsentimientoDialog() {
    }

    public static Optional<Datos> mostrar(Window duenio, String procedimientoSugerido) {
        Dialog<Datos> dialogo = new Dialog<>();
        dialogo.setTitle("Consentimiento informado");
        dialogo.setHeaderText("R7: firma del paciente, tutor o pagador");
        dialogo.initOwner(duenio);
        ButtonType firmar = new ButtonType("Firmar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getDialogPane().getButtonTypes().setAll(firmar, cancelar);

        TextField procedimiento = new TextField(procedimientoSugerido == null ? "" : procedimientoSugerido);
        procedimiento.setPrefWidth(440.0);
        procedimiento.getStyleClass().add("campo-formulario");
        TextArea riesgos = new TextArea();
        riesgos.setPrefRowCount(3);
        riesgos.setPrefWidth(440.0);
        riesgos.setWrapText(true);
        riesgos.getStyleClass().add("campo-formulario");
        TextArea alternativas = new TextArea();
        alternativas.setPrefRowCount(3);
        alternativas.setPrefWidth(440.0);
        alternativas.setWrapText(true);
        alternativas.getStyleClass().add("campo-formulario");
        TextField firmante = new TextField();
        firmante.getStyleClass().add("campo-formulario");
        TextField documento = new TextField();
        documento.getStyleClass().add("campo-formulario");
        ComboBox<String> rol = new ComboBox<>();
        rol.getItems().addAll("PACIENTE", "TUTOR", "PAGADOR");
        rol.setValue("PACIENTE");
        TextField vinculo = new TextField();
        vinculo.setPromptText("Vinculo del representante (tutor o pagador)");
        vinculo.getStyleClass().add("campo-formulario");

        GridPane grilla = new GridPane();
        grilla.setHgap(8.0);
        grilla.setVgap(8.0);
        grilla.setPadding(new Insets(10.0));
        grilla.addRow(0, new Label("Procedimiento:"), procedimiento);
        grilla.addRow(1, new Label("Riesgos:"), riesgos);
        grilla.addRow(2, new Label("Alternativas:"), alternativas);
        grilla.addRow(3, new Label("Firmante:"), firmante);
        grilla.addRow(4, new Label("Documento:"), documento);
        grilla.addRow(5, new Label("Rol:"), rol);
        grilla.addRow(6, new Label("Vinculo:"), vinculo);
        dialogo.getDialogPane().setContent(grilla);

        dialogo.setResultConverter(boton -> boton != firmar ? null : new Datos(
                texto(procedimiento), texto(riesgos), texto(alternativas), texto(firmante),
                texto(documento), rol.getValue(), texto(vinculo)));
        return dialogo.showAndWait();
    }

    private static String texto(TextField campo) {
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        return valor.isEmpty() ? null : valor;
    }

    private static String texto(TextArea area) {
        String valor = area.getText() == null ? "" : area.getText().trim();
        return valor.isEmpty() ? null : valor;
    }
}
