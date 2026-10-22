package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.TratamientoDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Tratamiento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TratamientoDAOImpl implements TratamientoDAO {

    private static final String COLUMNAS =
            "id, codigo, nombre, especialidad, calculo, precio, estado, duracion_minutos";

    private static final int DURACION_POR_DEFECTO = 30;

    private Tratamiento mapear(ResultSet rs) throws SQLException {
        Tratamiento tratamiento = new Tratamiento();
        tratamiento.setId(rs.getInt("id"));
        tratamiento.setCodigo(rs.getString("codigo"));
        tratamiento.setNombre(rs.getString("nombre"));
        tratamiento.setEspecialidad(rs.getString("especialidad"));
        tratamiento.setCalculo(rs.getString("calculo"));
        tratamiento.setPrecio(rs.getBigDecimal("precio"));
        tratamiento.setEstado(rs.getString("estado"));
        tratamiento.setDuracionMinutos(rs.getObject("duracion_minutos", Integer.class));
        return tratamiento;
    }

    private int duracion(Tratamiento tratamiento) {
        return tratamiento.getDuracionMinutos() == null
                ? DURACION_POR_DEFECTO : tratamiento.getDuracionMinutos();
    }

    @Override
    public List<Tratamiento> listarTodos() throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM tratamiento ORDER BY codigo";
        List<Tratamiento> tratamientos = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tratamientos.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo listar los tratamientos.", e);
        }
        return tratamientos;
    }

    @Override
    public List<Tratamiento> listarActivos() throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM tratamiento WHERE estado = 'ACTIVO' ORDER BY codigo";
        List<Tratamiento> tratamientos = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tratamientos.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo listar los tratamientos activos.", e);
        }
        return tratamientos;
    }

    @Override
    public Optional<Tratamiento> buscarPorId(Integer id) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM tratamiento WHERE id = ?";
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
            throw new DentrixException("No se pudo buscar el tratamiento por id.", e);
        }
    }

    @Override
    public Optional<Tratamiento> buscarPorCodigo(String codigo) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM tratamiento WHERE codigo = ?";
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
            throw new DentrixException("No se pudo buscar el tratamiento por código.", e);
        }
    }

    @Override
    public int insertar(Tratamiento tratamiento) throws DentrixException {
        String sql = "INSERT INTO tratamiento (codigo, nombre, especialidad, calculo, precio, estado, "
                + "duracion_minutos) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tratamiento.getCodigo());
            ps.setString(2, tratamiento.getNombre());
            ps.setString(3, tratamiento.getEspecialidad());
            ps.setString(4, tratamiento.getCalculo());
            ps.setBigDecimal(5, tratamiento.getPrecio());
            ps.setString(6, tratamiento.getEstado() == null ? "ACTIVO" : tratamiento.getEstado());
            ps.setInt(7, duracion(tratamiento));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            throw new DentrixException("No se generó el identificador del tratamiento.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo insertar el tratamiento.", e);
        }
    }

    @Override
    public boolean actualizar(Tratamiento tratamiento) throws DentrixException {
        String sql = "UPDATE tratamiento SET codigo = ?, nombre = ?, especialidad = ?, calculo = ?, "
                + "precio = ?, estado = ?, duracion_minutos = ? WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, tratamiento.getCodigo());
            ps.setString(2, tratamiento.getNombre());
            ps.setString(3, tratamiento.getEspecialidad());
            ps.setString(4, tratamiento.getCalculo());
            ps.setBigDecimal(5, tratamiento.getPrecio());
            ps.setString(6, tratamiento.getEstado() == null ? "ACTIVO" : tratamiento.getEstado());
            ps.setInt(7, duracion(tratamiento));
            ps.setInt(8, tratamiento.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar el tratamiento.", e);
        }
    }

    @Override
    public boolean eliminar(Integer id) throws DentrixException {
        String sql = "DELETE FROM tratamiento WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo eliminar el tratamiento.", e);
        }
    }
}
