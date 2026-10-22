package pe.edu.utp.dentrix.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.excepcion.DentrixException;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class UsuariosFxmlTest {

    private static void arrancarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaArrancado) {
            // el toolkit ya estaba iniciado por otra prueba
        }
    }

    private static <T> T enFx(Callable<T> accion) throws Exception {
        CountDownLatch listo = new CountDownLatch(1);
        AtomicReference<T> valor = new AtomicReference<>();
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                valor.set(accion.call());
            } catch (Throwable t) {
                fallo.set(t);
            } finally {
                listo.countDown();
            }
        });
        if (!listo.await(30, TimeUnit.SECONDS)) {
            throw new AssertionError("La operacion en el hilo de JavaFX no termino");
        }
        if (fallo.get() != null) {
            throw new AssertionError(fallo.get().getMessage(), fallo.get());
        }
        return valor.get();
    }

    @Test
    void laPantallaDeUsuariosCargaElPadronLaFichaYLaMatrizDeRoles() throws Exception {
        Connection conexion = null;
        try {
            conexion = ConexionBD.abrir();
        } catch (DentrixException e) {
            conexion = null;
        }
        assumeTrue(conexion != null, "config.properties o BD no disponibles: se omite esta prueba");
        conexion.close();

        var contexto = pe.edu.utp.dentrix.config.AppContext.get();
        var usuario = contexto.usuarios().buscarPorUsername("jvallejos")
                .orElseThrow(() -> new IllegalStateException("Falta el usuario jvallejos de la semilla"));
        var rol = contexto.roles().buscarPorId(usuario.getRolId())
                .orElseThrow(() -> new IllegalStateException("Falta el rol del usuario jvallejos"));
        contexto.sesion().abrir(usuario, rol);

        int usuariosEnBd = contexto.gestionUsuarios().listar().size();
        int rolesEnBd = contexto.gestionUsuarios().listarRoles().size();

        arrancarJavaFx();
        Parent pantalla = enFx(() -> {
            Parent cargado = FXMLLoader.load(
                    getClass().getResource("/pe/edu/utp/dentrix/fxml/usuarios.fxml"));
            new javafx.scene.Scene(cargado);
            cargado.getStylesheets().add(
                    getClass().getResource("/pe/edu/utp/dentrix/estilos.css").toExternalForm());
            cargado.applyCss();
            cargado.layout();
            return cargado;
        });
        assertNotNull(pantalla, "El FXML debe producir un nodo raiz");

        assertNotNull(pantalla.lookup("#campoUsername"), "Falta el usuario de la ficha");
        assertNotNull(pantalla.lookup("#campoNombres"), "Faltan los nombres de la ficha");
        assertNotNull(pantalla.lookup("#campoApellidos"), "Faltan los apellidos de la ficha");
        assertNotNull(pantalla.lookup("#campoDni"), "Falta el DNI de la ficha");
        assertNotNull(pantalla.lookup("#campoColegiatura"), "Falta la colegiatura de la ficha");
        assertNotNull(pantalla.lookup("#campoEspecialidad"), "Falta la especialidad de la ficha");
        assertNotNull(pantalla.lookup("#comboRol"), "Falta el rol de la ficha");
        assertNotNull(pantalla.lookup("#comboConsultorio"), "Falta el consultorio de la ficha");
        assertNotNull(pantalla.lookup("#campoClave"), "Falta la clave de la ficha");
        assertNotNull(pantalla.lookup("#etiquetaModo"), "Falta la etiqueta de modo de la ficha");
        assertNotNull(pantalla.lookup("#etiquetaSesiones"), "Falta el contador de cuentas activas");

        ComboBox<?> comboEstado = (ComboBox<?>) pantalla.lookup("#comboEstado");
        assertNotNull(comboEstado, "Falta el estado del usuario en la ficha");
        assertEquals(2, enFx(() -> comboEstado.getItems().size()),
                "El estado solo admite ACTIVO e INACTIVO");
        ComboBox<?> comboRol = (ComboBox<?>) pantalla.lookup("#comboRol");
        assertNotNull(comboRol, "El rol debe ser un ComboBox");
        assertTrue(enFx(() -> comboRol.getItems().size()) >= rolesEnBd,
                "El combo de rol debe ofrecer los roles del sistema (Administrador, Odontologo, Recepcion)");

        TableView<?> tabla = (TableView<?>) pantalla.lookup("#tablaUsuarios");
        assertNotNull(tabla, "Falta el padron de usuarios");
        List<String> cabeceras = enFx(() -> tabla.getColumns().stream()
                .map(c -> c.getText() == null ? "" : c.getText()).toList());
        assertTrue(cabeceras.contains("USUARIO"), "Falta la columna USUARIO: " + cabeceras);
        assertTrue(cabeceras.contains("NOMBRE"), "Falta la columna NOMBRE: " + cabeceras);
        assertTrue(cabeceras.contains("ROL"), "Falta la columna ROL: " + cabeceras);
        assertTrue(cabeceras.contains("ESTADO"), "Falta la columna ESTADO: " + cabeceras);
        assertTrue(cabeceras.contains("ACCIONES"), "Falta la columna ACCIONES: " + cabeceras);

        TableView<?> matriz = (TableView<?>) pantalla.lookup("#tablaPermisos");
        assertNotNull(matriz, "Falta la matriz de acceso por rol (RBAC)");
        List<String> cabecerasMatriz = enFx(() -> matriz.getColumns().stream()
                .map(c -> c.getText() == null ? "" : c.getText()).toList());
        assertTrue(cabecerasMatriz.contains("MODULO"), "Falta la columna MODULO: " + cabecerasMatriz);
        assertTrue(cabecerasMatriz.contains("ADMINISTRADOR"), "Falta la columna ADMINISTRADOR: " + cabecerasMatriz);
        assertTrue(cabecerasMatriz.contains("ODONTOLOGO"), "Falta la columna ODONTOLOGO: " + cabecerasMatriz);
        assertTrue(cabecerasMatriz.contains("RECEPCION"), "Falta la columna RECEPCION: " + cabecerasMatriz);
        assertEquals(4, enFx(() -> matriz.getItems().size()),
                "La matriz M9.1 debe listar Ficha clinica, Agenda, Caja y Configuracion");

        long limite = System.currentTimeMillis() + 15000;
        int filas = 0;
        while (System.currentTimeMillis() < limite && filas == 0) {
            filas = enFx(() -> tabla.getItems().size());
            if (filas == 0) {
                Thread.sleep(200);
            }
        }
        assertEquals(usuariosEnBd, filas, "El padron debe cargar los usuarios de la base");
    }
}
