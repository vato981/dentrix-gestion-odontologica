package pe.edu.utp.dentrix.service;

import pe.edu.utp.dentrix.dao.RolDAO;
import pe.edu.utp.dentrix.dao.UsuarioDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Rol;
import pe.edu.utp.dentrix.model.Usuario;
import pe.edu.utp.dentrix.util.PasswordUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UsuarioService {

    private final UsuarioDAO usuarioDAO;
    private final RolDAO rolDAO;

    public UsuarioService(UsuarioDAO usuarioDAO, RolDAO rolDAO) {
        this.usuarioDAO = usuarioDAO;
        this.rolDAO = rolDAO;
    }

    public List<Usuario> listar() throws DentrixException {
        return usuarioDAO.listarTodos();
    }

    public List<Rol> listarRoles() throws DentrixException {
        return rolDAO.listarTodos();
    }

    public Usuario registrar(Usuario usuario, String clavePlana) throws DentrixException {
        validarBase(usuario);
        validarClaveNueva(clavePlana);
        if (usuarioDAO.buscarPorUsername(usuario.getUsername()).isPresent()) {
            throw new DentrixException("Ya existe un usuario con el username " + usuario.getUsername());
        }
        if (usuario.getDni() != null && usuarioDAO.buscarPorDni(usuario.getDni()).isPresent()) {
            throw new DentrixException("Ya existe un usuario con el DNI " + usuario.getDni());
        }
        usuario.setPasswordHash(PasswordUtil.hash(clavePlana));
        usuario.setEstado("ACTIVO");
        usuario.setIntentosFallidos(0);
        try {
            int id = usuarioDAO.insertar(usuario);
            usuario.setId(id);
            return usuario;
        } catch (DentrixException e) {
            throw traducir(e, "Ya existe un usuario con ese username o DNI.", "No se pudo registrar el usuario.");
        }
    }

    public Usuario editar(Usuario usuario) throws DentrixException {
        validarBase(usuario);
        if (usuario.getId() == null) {
            throw new DentrixException("El usuario a editar no tiene identificador.");
        }
        Optional<Usuario> duplicado = usuarioDAO.buscarPorUsername(usuario.getUsername());
        if (duplicado.isPresent() && !duplicado.get().getId().equals(usuario.getId())) {
            throw new DentrixException("Ya existe un usuario con el username " + usuario.getUsername());
        }
        if (usuario.getDni() != null) {
            Optional<Usuario> porDni = usuarioDAO.buscarPorDni(usuario.getDni());
            if (porDni.isPresent() && !porDni.get().getId().equals(usuario.getId())) {
                throw new DentrixException("Ya existe un usuario con el DNI " + usuario.getDni());
            }
        }
        try {
            if (!usuarioDAO.actualizar(usuario)) {
                throw new DentrixException("No se encontró el usuario a editar.");
            }
            return usuario;
        } catch (DentrixException e) {
            throw traducir(e, "Ya existe un usuario con ese username o DNI.", "No se pudo editar el usuario.");
        }
    }

    public void cambiarClave(Integer usuarioId, String claveNueva) throws DentrixException {
        if (usuarioId == null) {
            throw new DentrixException("El usuario de la clave no está identificado.");
        }
        validarClaveNueva(claveNueva);
        Usuario usuario = usuarioDAO.buscarPorId(usuarioId)
                .orElseThrow(() -> new DentrixException("No existe el usuario."));
        usuario.setPasswordHash(PasswordUtil.hash(claveNueva));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        try {
            if (!usuarioDAO.actualizar(usuario)) {
                throw new DentrixException("No se pudo cambiar la clave.");
            }
        } catch (DentrixException e) {
            throw traducir(e, "No se pudo cambiar la clave.", "No se pudo cambiar la clave.");
        }
    }

    public void cambiarEstado(Integer usuarioId, String nuevoEstado) throws DentrixException {
        if (usuarioId == null) {
            throw new DentrixException("El usuario no está identificado.");
        }
        if (!"ACTIVO".equals(nuevoEstado) && !"INACTIVO".equals(nuevoEstado)) {
            throw new DentrixException("El estado debe ser ACTIVO o INACTIVO.");
        }
        Usuario usuario = usuarioDAO.buscarPorId(usuarioId)
                .orElseThrow(() -> new DentrixException("No existe el usuario."));
        usuario.setEstado(nuevoEstado);
        if ("ACTIVO".equals(nuevoEstado)) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
        }
        try {
            if (!usuarioDAO.actualizar(usuario)) {
                throw new DentrixException("No se pudo cambiar el estado del usuario.");
            }
        } catch (DentrixException e) {
            throw traducir(e, "No se pudo cambiar el estado del usuario.", "No se pudo cambiar el estado del usuario.");
        }
    }

    private void validarBase(Usuario usuario) throws DentrixException {
        if (usuario == null) {
            throw new DentrixException("El usuario es obligatorio.");
        }
        if (usuario.getUsername() == null || usuario.getUsername().isBlank()) {
            throw new DentrixException("El username es obligatorio.");
        }
        if (usuario.getNombres() == null || usuario.getNombres().isBlank()) {
            throw new DentrixException("Los nombres son obligatorios.");
        }
        if (usuario.getApellidos() == null || usuario.getApellidos().isBlank()) {
            throw new DentrixException("Los apellidos son obligatorios.");
        }
        if (usuario.getRolId() == null) {
            throw new DentrixException("Selecciona el rol del usuario.");
        }
        Rol rol = rolDAO.buscarPorId(usuario.getRolId())
                .orElseThrow(() -> new DentrixException("El rol seleccionado no existe."));
        if ("ODONTOLOGO".equals(codigoDeRol(rol)) && (usuario.getColegiatura() == null
                || usuario.getColegiatura().isBlank())) {
            throw new DentrixException("Los odontólogos deben registrar su colegiatura.");
        }
    }

    private String codigoDeRol(Rol rol) {
        return rol == null ? "" : rol.getCodigo();
    }

    private void validarClaveNueva(String clave) throws DentrixException {
        if (clave == null || clave.length() < 8) {
            throw new DentrixException("La clave debe tener al menos 8 caracteres.");
        }
    }

    private DentrixException traducir(DentrixException e, String mensajeDuplicado, String mensajeGenerico) {
        Throwable causa = e.getCause();
        if (causa instanceof SQLException sqle) {
            if ("23505".equals(sqle.getSQLState())) {
                return new DentrixException(mensajeDuplicado, e);
            }
            if ("23503".equals(sqle.getSQLState())) {
                return new DentrixException("No se puede modificar: el usuario está en uso por citas o cierres.", e);
            }
        }
        return new DentrixException(mensajeGenerico, e);
    }
}
