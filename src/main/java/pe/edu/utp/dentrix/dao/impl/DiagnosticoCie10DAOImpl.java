package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.DiagnosticoCie10DAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.DiagnosticoCie10;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DiagnosticoCie10DAOImpl implements DiagnosticoCie10DAO {

    private DiagnosticoCie10 mapear(ResultSet rs) throws SQLException {
        DiagnosticoCie10 diagnostico = new DiagnosticoCie10();
        diagnostico.setCodigo(rs.getString("codigo"));
        diagnostico.setDescripcion(rs.getString("descripcion"));
        diagnostico.setCapitulo(rs.getString("capitulo"));
        diagnostico.setEsFrecuente(rs.getBoolean("es_frecuente"));
        return diagnostico;
    }

    @Override
    public List<DiagnosticoCie10> listarTodos() throws DentrixException {
        String sql = "SELECT codigo, descripcion, capitulo, es_frecuente FROM diagnostico_cie10 ORDER BY codigo";
        List<DiagnosticoCie10> diagnosticos = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                diagnosticos.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo listar los diagnósticos CIE-10.", e);
        }
        return diagnosticos;
    }

    @Override
    public List<DiagnosticoCie10> listarFrecuentes() throws DentrixException {
        String sql = "SELECT codigo, descripcion, capitulo, es_frecuente FROM diagnostico_cie10 "
                + "WHERE es_frecuente = TRUE ORDER BY codigo";
        List<DiagnosticoCie10> diagnosticos = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                diagnosticos.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo listar los diagnósticos CIE-10 frecuentes.", e);
        }
        return diagnosticos;
    }

    @Override
    public Optional<DiagnosticoCie10> buscarPorCodigo(String codigo) throws DentrixException {
        String sql = "SELECT codigo, descripcion, capitulo, es_frecuente FROM diagnostico_cie10 WHERE codigo = ?";
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
            throw new DentrixException("No se pudo buscar el diagnóstico CIE-10 por código.", e);
        }
    }

    @Override
    public int insertar(DiagnosticoCie10 diagnostico) throws DentrixException {
        String sql = "INSERT INTO diagnostico_cie10 (codigo, descripcion, capitulo, es_frecuente) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, diagnostico.getCodigo());
            ps.setString(2, diagnostico.getDescripcion());
            ps.setString(3, diagnostico.getCapitulo());
            ps.setBoolean(4, diagnostico.isEsFrecuente());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DentrixException("No se pudo insertar el diagnóstico CIE-10.", e);
        }
    }

    @Override
    public boolean actualizar(DiagnosticoCie10 diagnostico) throws DentrixException {
        String sql = "UPDATE diagnostico_cie10 SET descripcion = ?, capitulo = ?, es_frecuente = ? "
                + "WHERE codigo = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, diagnostico.getDescripcion());
            ps.setString(2, diagnostico.getCapitulo());
            ps.setBoolean(3, diagnostico.isEsFrecuente());
            ps.setString(4, diagnostico.getCodigo());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar el diagnóstico CIE-10.", e);
        }
    }

    @Override
    public boolean eliminar(String codigo) throws DentrixException {
        String sql = "DELETE FROM diagnostico_cie10 WHERE codigo = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo eliminar el diagnóstico CIE-10.", e);
        }
    }
}
