package pe.edu.utp.dentrix.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.model.Rol;
import pe.edu.utp.dentrix.model.Usuario;
import pe.edu.utp.dentrix.ui.TareasFX;

public class LoginController {

    private static final String FXML_MENU = "/pe/edu/utp/dentrix/fxml/menu-principal.fxml";

    @FXML
    private TextField campoUsuario;

    @FXML
    private PasswordField campoClave;

    @FXML
    private Label etiquetaError;

    @FXML
    private HBox cajaError;

    private boolean ingresando;

    @FXML
    private void onIngresarClick() {
        if (ingresando) {
            return;
        }
        mostrarError("");
        String usuario = campoUsuario.getText() == null ? "" : campoUsuario.getText().trim();
        String clave = campoClave.getText() == null ? "" : campoClave.getText();
        if (usuario.isEmpty() || clave.isEmpty()) {
            mostrarError("Escribe tu usuario y tu contraseña.");
            return;
        }
        ingresando = true;
        TareasFX.ejecutar(() -> {
            AppContext contexto = AppContext.get();
            Usuario logueado = contexto.autenticacion().iniciarSesion(usuario, clave);
            Rol rol = contexto.autenticacion().rolDe(logueado);
            contexto.sesion().abrir(logueado, rol);
            return null;
        }, ok -> {
            ingresando = false;
            campoClave.clear();
            try {
                campoUsuario.getScene().setRoot(FXMLLoader.load(getClass().getResource(FXML_MENU)));
           } catch (Exception e) {
    System.err.println("========== ERROR AL ABRIR MENU PRINCIPAL ==========");
    e.printStackTrace();
    System.err.println("==================================================");
    mostrarError("No se pudo iniciar la aplicación: " + e.getMessage());
}
        }, error -> {
            ingresando = false;
            mostrarError(TareasFX.mensaje(error));
        });
    }

    private void mostrarError(String mensaje) {
        boolean hay = mensaje != null && !mensaje.isEmpty();
        etiquetaError.setText(hay ? mensaje : "");
        cajaError.setVisible(hay);
        cajaError.setManaged(hay);
    }
}
