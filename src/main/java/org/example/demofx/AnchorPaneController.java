package org.example.demofx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

public class AnchorPaneController {

    @FXML
    private TextField campoNombre;

    @FXML
    private Label mensaje;

    @FXML
    private Button botonSaludar;

    @FXML
    private void onSaludarClick() {
        String nombre = campoNombre.getText();
        if (nombre == null || nombre.isBlank()) {
            mensaje.setText("Escribe tu nombre primero.");
        } else {
            mensaje.setText("Hola, " + nombre.trim() + "!");
        }
    }

    @FXML
    private void onVolverClick() throws IOException {
        botonSaludar.getScene().setRoot(FXMLLoader.load(App.class.getResource("primary.fxml")));
    }
}
