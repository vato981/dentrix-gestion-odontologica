package pe.edu.utp.dentrix.config;

import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.model.Rol;
import pe.edu.utp.dentrix.model.Usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppContextTest {

    @Test
    void esUnSingletonYNaceSinSesion() {
        AppContext contexto = AppContext.get();
        assertSame(contexto, AppContext.get());
        assertFalse(contexto.sesion().activa());
    }

    @Test
    void exponeLosSieteServiciosDelNegocio() {
        AppContext contexto = AppContext.get();
        assertNotNull(contexto.autenticacion());
        assertNotNull(contexto.catalogo());
        assertNotNull(contexto.pacientes());
        assertNotNull(contexto.agenda());
        assertNotNull(contexto.fichas());
        assertNotNull(contexto.caja());
        assertNotNull(contexto.reportes());
    }

    @Test
    void laSesionSeAbreSeLeeYSeCierra() {
        AppContext contexto = AppContext.get();
        Usuario usuario = new Usuario();
        usuario.setNombres("Temp");
        usuario.setApellidos("Sesion");
        Rol rol = new Rol();
        rol.setCodigo("RECEPCION");
        rol.setNombre("Recepción");
        contexto.sesion().abrir(usuario, rol);
        assertTrue(contexto.sesion().activa());
        assertEquals("Temp Sesion", contexto.sesion().getNombreCompleto());
        assertEquals("RECEPCION", contexto.sesion().getCodigoRol());
        contexto.sesion().cerrar();
        assertFalse(contexto.sesion().activa());
    }
}
