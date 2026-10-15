package pe.edu.utp.dentrix.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.RolDAO;
import pe.edu.utp.dentrix.dao.UsuarioDAO;
import pe.edu.utp.dentrix.dao.impl.RolDAOImpl;
import pe.edu.utp.dentrix.dao.impl.UsuarioDAOImpl;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Rol;
import pe.edu.utp.dentrix.model.Usuario;
import pe.edu.utp.dentrix.util.PasswordUtil;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AutenticacionServiceTest {

    private static final String USERNAME = "tmp_a1_login";
    private static final String DNI = "90000001";
    private static final String CLAVE = "Prueba123*";

    private boolean baseDisponible() {
        try (Connection cn = ConexionBD.abrir()) {
            return cn != null;
        } catch (DentrixException | SQLException e) {
            return false;
        }
    }

    private void borrarSiExiste(UsuarioDAO usuarioDAO, String username, String dni) throws DentrixException {
        usuarioDAO.buscarPorUsername(username).ifPresent(u -> {
            try {
                usuarioDAO.eliminar(u.getId());
            } catch (DentrixException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
        usuarioDAO.buscarPorDni(dni).ifPresent(u -> {
            try {
                usuarioDAO.eliminar(u.getId());
            } catch (DentrixException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Test
    void elLoginFuncionaYBloqueaTrasTresIntentosFallidos() throws Exception {
        assumeTrue(baseDisponible(), "config.properties o BD no disponibles: se omite esta prueba");

        UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
        RolDAO rolDAO = new RolDAOImpl();
        AutenticacionService servicio = new AutenticacionService(usuarioDAO, rolDAO);

        Rol rolRecepcion = rolDAO.buscarPorCodigo("RECEPCION").orElse(null);
        assumeTrue(rolRecepcion != null, "El rol RECEPCION de la semilla no existe: se omite esta prueba");

        borrarSiExiste(usuarioDAO, USERNAME, DNI);

        try {
            Usuario nuevo = new Usuario();
            nuevo.setUsername(USERNAME);
            nuevo.setPasswordHash(PasswordUtil.hash(CLAVE));
            nuevo.setNombres("Temp");
            nuevo.setApellidos("Temporal");
            nuevo.setDni(DNI);
            nuevo.setRolId(rolRecepcion.getId());
            nuevo.setEstado("ACTIVO");
            nuevo.setIntentosFallidos(0);
            usuarioDAO.insertar(nuevo);

            Usuario logueado = servicio.iniciarSesion(USERNAME, CLAVE);
            assertEquals(USERNAME, logueado.getUsername());
            assertNotNull(logueado.getUltimoAcceso(), "Un login exitoso debe registrar el último acceso");
            assertEquals(0, logueado.getIntentosFallidos());
            assertEquals("RECEPCION", servicio.rolDe(logueado).getCodigo());
            assertFalse(servicio.esOdontologo(logueado), "Un usuario de recepción sin colegiatura no es odontólogo");

            DentrixException errorClave = assertThrows(DentrixException.class,
                    () -> servicio.iniciarSesion(USERNAME, "ClaveMala1*"));
            assertEquals("Usuario o contraseña incorrectos", errorClave.getMessage());

            assertThrows(DentrixException.class,
                    () -> servicio.iniciarSesion(USERNAME, "ClaveMala1*"));
            assertThrows(DentrixException.class,
                    () -> servicio.iniciarSesion(USERNAME, "ClaveMala1*"));

            Usuario releido = usuarioDAO.buscarPorUsername(USERNAME).orElseThrow();
            assertEquals("BLOQUEADO", releido.getEstado(), "Tras 3 intentos el usuario debe quedar BLOQUEADO");
            assertNotNull(releido.getBloqueadoHasta(), "El bloqueo debe fijar bloqueado_hasta");

            DentrixException errorBloqueo = assertThrows(DentrixException.class,
                    () -> servicio.iniciarSesion(USERNAME, CLAVE));
            assertTrue(errorBloqueo.getMessage().startsWith("Usuario bloqueado hasta"),
                    "Con la clave correcta pero bloqueado debe avisar hasta cuándo: " + errorBloqueo.getMessage());
        } finally {
            borrarSiExiste(usuarioDAO, USERNAME, DNI);
        }
    }
}
