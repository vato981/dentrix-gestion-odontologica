package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.UsuarioDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioDAOImpl implements UsuarioDAO {

    private static final String COLUMNAS = "id, username, password_hash, nombres, apellidos, dni, "
            + "rol_id, colegiatura, especialidad, consultorio_id, estado, intentos_fallidos, "
            + "bloqueado_hasta, ultimo_acceso, fecha_creacion";

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("id"));
        usuario.setUsername(rs.getString("username"));
        usuario.setPasswordHash(rs.getString("password_hash"));
        usuario.setNombres(rs.getString("nombres"));
        usuario.setApellidos(rs.getString("apellidos"));
        usuario.setDni(rs.getString("dni"));
        usuario.setRolId(rs.getInt("rol_id"));
        usuario.setColegiatura(rs.getString("colegiatura"));
        usuario.setEspecialidad(rs.getString("especialidad"));
        int consultorioId = rs.getInt("consultorio_id");
        usuario.setConsultorioId(rs.wasNull() ? null : consultorioId);
        usuario.setEstado(rs.getString("estado"));
        usuario.setIntentosFallidos(rs.getInt("intentos_fallidos"));
        usuario.setBloqueadoHasta(rs.getObject("bloqueado_hasta", LocalDateTime.class));
        usuario.setUltimoAcceso(rs.getObject("ultimo_acceso", LocalDateTime.class));
        usuario.setFechaCreacion(rs.getObject("fecha_creacion", LocalDateTime.class));
        return usuario;
    }

    @Override
    public List<Usuario> listarTodos() throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM usuario ORDER BY id";
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                usuarios.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo listar los usuarios.", e);
        }
        return usuarios;
    }

    @Override
    public Optional<Usuario> buscarPorId(Integer id) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM usuario WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DentrixException("No se pudo buscar el usuario por id.", e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorUsername(String username) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM usuario WHERE username = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DentrixException("No se pudo buscar el usuario por username.", e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorDni(String dni) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM usuario WHERE dni = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DentrixException("No se pudo buscar el usuario por DNI.", e);
        }
    }

    @Override
    public int insertar(Usuario usuario) throws DentrixException {
        String sql = "INSERT INTO usuario (username, password_hash, nombres, apellidos, dni, rol_id, "
                + "colegiatura, especialidad, consultorio_id, estado, intentos_fallidos, "
                + "bloqueado_hasta, ultimo_acceso) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::estado_usuario, ?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, usuario.getUsername());
            ps.setString(2, usuario.getPasswordHash());
            ps.setString(3, usuario.getNombres());
            ps.setString(4, usuario.getApellidos());
            ps.setString(5, usuario.getDni());
            ps.setInt(6, usuario.getRolId());
            ps.setString(7, usuario.getColegiatura());
            ps.setString(8, usuario.getEspecialidad());
            if (usuario.getConsultorioId() == null) {
                ps.setObject(9, null);
            } else {
                ps.setInt(9, usuario.getConsultorioId());
            }
            ps.setString(10, usuario.getEstado() == null ? "ACTIVO" : usuario.getEstado());
            ps.setInt(11, usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos());
            ps.setObject(12, usuario.getBloqueadoHasta());
            ps.setObject(13, usuario.getUltimoAcceso());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            throw new DentrixException("No se generó el identificador del usuario.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo insertar el usuario.", e);
        }
    }

    @Override
    public boolean actualizar(Usuario usuario) throws DentrixException {
        String sql = "UPDATE usuario SET username = ?, password_hash = ?, nombres = ?, apellidos = ?, "
                + "dni = ?, rol_id = ?, colegiatura = ?, especialidad = ?, consultorio_id = ?, "
                + "estado = ?::estado_usuario, intentos_fallidos = ?, bloqueado_hasta = ?, ultimo_acceso = ? WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, usuario.getUsername());
            ps.setString(2, usuario.getPasswordHash());
            ps.setString(3, usuario.getNombres());
            ps.setString(4, usuario.getApellidos());
            ps.setString(5, usuario.getDni());
            ps.setInt(6, usuario.getRolId());
            ps.setString(7, usuario.getColegiatura());
            ps.setString(8, usuario.getEspecialidad());
            if (usuario.getConsultorioId() == null) {
                ps.setObject(9, null);
            } else {
                ps.setInt(9, usuario.getConsultorioId());
            }
            ps.setString(10, usuario.getEstado() == null ? "ACTIVO" : usuario.getEstado());
            ps.setInt(11, usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos());
            ps.setObject(12, usuario.getBloqueadoHasta());
            ps.setObject(13, usuario.getUltimoAcceso());
            ps.setInt(14, usuario.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar el usuario.", e);
        }
    }

    @Override
    public boolean eliminar(Integer id) throws DentrixException {
        String sql = "DELETE FROM usuario WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo eliminar el usuario.", e);
        }
    }
}
