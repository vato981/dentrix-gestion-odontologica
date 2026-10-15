package pe.edu.utp.dentrix.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Rol;
import pe.edu.utp.dentrix.model.Usuario;

public class LoginController {

    private static final String FXML_MENU = "/pe/edu/utp/dentrix/fxml/menu-principal.fxml";

    @FXML
    private TextField campoUsuario;

    @FXML
    private PasswordField campoClave;

    @FXML
    private Label etiquetaError;

    @FXML
    private void initialize() {
        campoClave.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                onIngresarClick();
            }
        });
    }

    @FXML
    private void onIngresarClick() {
        etiquetaError.setText("");
        String usuario = campoUsuario.getText() == null ? "" : campoUsuario.getText().trim();
        String clave = campoClave.getText() == null ? "" : campoClave.getText();
        if (usuario.isEmpty() || clave.isEmpty()) {
            etiquetaError.setText("Escribe tu usuario y tu contraseña.");
            return;
        }
        try {
            AppContext contexto = AppContext.get();
            Usuario logueado = contexto.autenticacion().iniciarSesion(usuario, clave);
            Rol rol = contexto.autenticacion().rolDe(logueado);
            contexto.sesion().abrir(logueado, rol);
            campoClave.clear();
            campoUsuario.getScene().setRoot(FXMLLoader.load(getClass().getResource(FXML_MENU)));
        } catch (DentrixException e) {
            etiquetaError.setText(e.getMessage());
        } catch (Exception e) {
            etiquetaError.setText("No se pudo iniciar la aplicación: " + e.getMessage());
        }
    }
}
