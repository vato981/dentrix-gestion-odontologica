package org.example.demofx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;

public class PrimaryController {

    @FXML
    private Label mensaje;

    @FXML
    private Button boton;

    @FXML
    private Button btnAnchor;

    @FXML
    private void onBotonClick() {
        mensaje.setText("Hola desde JavaFX + Scene Builder!");
        boton.setText("Clic registrado");
    }

    @FXML
    private void onAnchorClick() throws IOException {
        btnAnchor.getScene().setRoot(FXMLLoader.load(App.class.getResource("anchor.fxml")));
    }
}
