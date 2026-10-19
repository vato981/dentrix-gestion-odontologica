package pe.edu.utp.dentrix.service;

import pe.edu.utp.dentrix.dao.RolDAO;
import pe.edu.utp.dentrix.dao.UsuarioDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Rol;
import pe.edu.utp.dentrix.model.Usuario;
import pe.edu.utp.dentrix.util.PasswordUtil;

import java.time.LocalDateTime;

public class AutenticacionService {

    private static final String ESTADO_BLOQUEADO = "BLOQUEADO";
    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final int MAXIMO_INTENTOS = 3;
    private static final int MINUTOS_BLOQUEO = 15;

    private final UsuarioDAO usuarioDAO;
    private final RolDAO rolDAO;

    public AutenticacionService(UsuarioDAO usuarioDAO, RolDAO rolDAO) {
        this.usuarioDAO = usuarioDAO;
        this.rolDAO = rolDAO;
    }

    public Usuario iniciarSesion(String username, String clave) throws DentrixException {
        Usuario usuario = usuarioDAO.buscarPorUsername(username)
                .orElseThrow(() -> new DentrixException("Usuario o contraseña incorrectos"));

        if ("INACTIVO".equals(usuario.getEstado())) {
            throw new DentrixException("El usuario está inactivo. Pide al administrador que lo reactive.");
        }

        if (ESTADO_BLOQUEADO.equals(usuario.getEstado())) {
            LocalDateTime ahora = LocalDateTime.now();
            if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isBefore(ahora)) {
                usuario.setEstado(ESTADO_ACTIVO);
                usuario.setIntentosFallidos(0);
                usuario.setBloqueadoHasta(null);
                usuarioDAO.actualizar(usuario);
            } else {
                throw new DentrixException("Usuario bloqueado hasta " + usuario.getBloqueadoHasta());
            }
        }

        if (!PasswordUtil.verificar(clave, usuario.getPasswordHash())) {
            int intentos = (usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos()) + 1;
            usuario.setIntentosFallidos(intentos);
            if (intentos >= MAXIMO_INTENTOS) {
                usuario.setEstado(ESTADO_BLOQUEADO);
                usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
            }
            usuarioDAO.actualizar(usuario);
            throw new DentrixException("Usuario o contraseña incorrectos");
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioDAO.actualizar(usuario);
        return usuario;
    }

    public Rol rolDe(Usuario usuario) throws DentrixException {
        return rolDAO.buscarPorId(usuario.getRolId()).orElse(null);
    }

    public boolean esOdontologo(Usuario usuario) throws DentrixException {
        Rol rol = rolDe(usuario);
        return rol != null && "ODONTOLOGO".equals(rol.getCodigo()) && usuario.getColegiatura() != null;
    }
}
