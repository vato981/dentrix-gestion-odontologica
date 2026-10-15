package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.RolDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Rol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RolDAOImpl implements RolDAO {

    private Rol mapear(ResultSet rs) throws SQLException {
        Rol rol = new Rol();
        rol.setId(rs.getInt("id"));
        rol.setCodigo(rs.getString("codigo"));
        rol.setNombre(rs.getString("nombre"));
        rol.setDescripcion(rs.getString("descripcion"));
        return rol;
    }

    @Override
    public List<Rol> listarTodos() throws DentrixException {
        String sql = "SELECT id, codigo, nombre, descripcion FROM rol ORDER BY id";
        List<Rol> roles = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                roles.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo listar los roles.", e);
        }
        return roles;
    }

    @Override
    public Optional<Rol> buscarPorId(Integer id) throws DentrixException {
        String sql = "SELECT id, codigo, nombre, descripcion FROM rol WHERE id = ?";
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
            throw new DentrixException("No se pudo buscar el rol por id.", e);
        }
    }

    @Override
    public Optional<Rol> buscarPorCodigo(String codigo) throws DentrixException {
        String sql = "SELECT id, codigo, nombre, descripcion FROM rol WHERE codigo = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DentrixException("No se pudo buscar el rol por código.", e);
        }
    }

    @Override
    public int insertar(Rol rol) throws DentrixException {
        String sql = "INSERT INTO rol (codigo, nombre, descripcion) VALUES (?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, rol.getCodigo());
            ps.setString(2, rol.getNombre());
            ps.setString(3, rol.getDescripcion());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            throw new DentrixException("No se generó el identificador del rol.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo insertar el rol.", e);
        }
    }

    @Override
    public boolean actualizar(Rol rol) throws DentrixException {
        String sql = "UPDATE rol SET codigo = ?, nombre = ?, descripcion = ? WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, rol.getCodigo());
            ps.setString(2, rol.getNombre());
            ps.setString(3, rol.getDescripcion());
            ps.setInt(4, rol.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar el rol.", e);
        }
    }

    @Override
    public boolean eliminar(Integer id) throws DentrixException {
        String sql = "DELETE FROM rol WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo eliminar el rol.", e);
        }
    }
}
