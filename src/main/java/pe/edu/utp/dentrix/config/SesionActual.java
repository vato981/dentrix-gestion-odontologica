package pe.edu.utp.dentrix.config;

import pe.edu.utp.dentrix.model.Rol;
import pe.edu.utp.dentrix.model.Usuario;

public final class SesionActual {

    private Usuario usuario;
    private Rol rol;

    public void abrir(Usuario usuario, Rol rol) {
        this.usuario = usuario;
        this.rol = rol;
    }

    public void cerrar() {
        usuario = null;
        rol = null;
    }

    public boolean activa() {
        return usuario != null;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Rol getRol() {
        return rol;
    }

    public String getNombreCompleto() {
        if (usuario == null) {
            return "";
        }
        return usuario.getNombres() + " " + usuario.getApellidos();
    }

    public String getCodigoRol() {
        return rol == null ? "" : rol.getCodigo();
    }
}
