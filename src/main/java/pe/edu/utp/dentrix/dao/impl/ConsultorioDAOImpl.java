package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.ConsultorioDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Consultorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConsultorioDAOImpl implements ConsultorioDAO {

    private Consultorio mapear(ResultSet rs) throws SQLException {
        Consultorio consultorio = new Consultorio();
        consultorio.setId(rs.getInt("id"));
        consultorio.setCodigo(rs.getString("codigo"));
        consultorio.setNombre(rs.getString("nombre"));
        consultorio.setEspecialidad(rs.getString("especialidad"));
        consultorio.setEstado(rs.getString("estado"));
        return consultorio;
    }

    @Override
    public List<Consultorio> listarTodos() throws DentrixException {
        String sql = "SELECT id, codigo, nombre, especialidad, estado FROM consultorio ORDER BY codigo";
        List<Consultorio> consultorios = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                consultorios.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo listar los consultorios.", e);
        }
        return consultorios;
    }

    @Override
    public Optional<Consultorio> buscarPorId(Integer id) throws DentrixException {
        String sql = "SELECT id, codigo, nombre, especialidad, estado FROM consultorio WHERE id = ?";
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
            throw new DentrixException("No se pudo buscar el consultorio por id.", e);
        }
    }

    @Override
    public Optional<Consultorio> buscarPorCodigo(String codigo) throws DentrixException {
        String sql = "SELECT id, codigo, nombre, especialidad, estado FROM consultorio WHERE codigo = ?";
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
            throw new DentrixException("No se pudo buscar el consultorio por código.", e);
        }
    }

    @Override
    public int insertar(Consultorio consultorio) throws DentrixException {
        String sql = "INSERT INTO consultorio (codigo, nombre, especialidad, estado) VALUES (?, ?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, consultorio.getCodigo());
            ps.setString(2, consultorio.getNombre());
            ps.setString(3, consultorio.getEspecialidad());
            ps.setString(4, consultorio.getEstado() == null ? "ACTIVO" : consultorio.getEstado());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            throw new DentrixException("No se generó el identificador del consultorio.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo insertar el consultorio.", e);
        }
    }

    @Override
    public boolean actualizar(Consultorio consultorio) throws DentrixException {
        String sql = "UPDATE consultorio SET codigo = ?, nombre = ?, especialidad = ?, estado = ? WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, consultorio.getCodigo());
            ps.setString(2, consultorio.getNombre());
            ps.setString(3, consultorio.getEspecialidad());
            ps.setString(4, consultorio.getEstado() == null ? "ACTIVO" : consultorio.getEstado());
            ps.setInt(5, consultorio.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar el consultorio.", e);
        }
    }

    @Override
    public boolean eliminar(Integer id) throws DentrixException {
        String sql = "DELETE FROM consultorio WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo eliminar el consultorio.", e);
        }
    }
}
