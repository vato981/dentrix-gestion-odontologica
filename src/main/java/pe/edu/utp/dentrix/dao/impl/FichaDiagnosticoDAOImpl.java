package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.FichaDiagnosticoDAO;
import pe.edu.utp.dentrix.dto.CambioPrincipal;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.FichaDiagnostico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FichaDiagnosticoDAOImpl implements FichaDiagnosticoDAO {

    private static final String COLUMNAS = "ficha_id, cie10_codigo, pieza_fdi, es_principal, observacion";

    @Override
    public List<FichaDiagnostico> listarPorFicha(Long fichaId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM ficha_diagnostico WHERE ficha_id = ? ORDER BY es_principal DESC, cie10_codigo";
        List<FichaDiagnostico> diagnosticos = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, fichaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    diagnosticos.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo listar los diagnosticos de la ficha: " + e.getMessage(), e);
        }
        return diagnosticos;
    }

    @Override
    public int insertar(FichaDiagnostico diagnostico) throws DentrixException {
        String sql = "INSERT INTO ficha_diagnostico (ficha_id, cie10_codigo, pieza_fdi, es_principal, observacion) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, diagnostico.getFichaId());
            ps.setObject(2, diagnostico.getCie10Codigo());
            ps.setObject(3, diagnostico.getPiezaFdi());
            ps.setBoolean(4, diagnostico.isEsPrincipal());
            ps.setObject(5, diagnostico.getObservacion());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el diagnostico: " + e.getMessage(), e);
        }
    }

    @Override
    public int desmarcarPrincipales(Long fichaId) throws DentrixException {
        String sql = "UPDATE ficha_diagnostico SET es_principal = FALSE "
                + "WHERE ficha_id = ? AND es_principal = TRUE";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, fichaId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar el diagnostico principal: " + e.getMessage(), e);
        }
    }

    @Override
    public int reemplazarPrincipal(Long fichaId, String cie10Nuevo, String motivo, Integer usuarioId)
            throws DentrixException {
        String sql = "WITH anterior AS ("
                + " UPDATE ficha_diagnostico SET es_principal = FALSE"
                + "  WHERE ficha_id = ? AND es_principal = TRUE AND cie10_codigo <> ?"
                + " RETURNING ficha_id, cie10_codigo)"
                + " INSERT INTO historial_diagnostico_principal"
                + " (ficha_id, cie10_anterior, cie10_nuevo, motivo, usuario_id)"
                + " SELECT ficha_id, cie10_codigo, ?, ?, ? FROM anterior";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, fichaId);
            ps.setObject(2, cie10Nuevo);
            ps.setObject(3, cie10Nuevo);
            ps.setObject(4, motivo);
            ps.setObject(5, usuarioId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DentrixException("No se pudo cambiar el diagnostico principal: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CambioPrincipal> historialPrincipal(Long fichaId) throws DentrixException {
        String sql = "SELECT ficha_id, cie10_anterior, cie10_nuevo, motivo, usuario_id, fecha "
                + "FROM historial_diagnostico_principal WHERE ficha_id = ? ORDER BY fecha DESC, id DESC";
        List<CambioPrincipal> cambios = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, fichaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cambios.add(new CambioPrincipal(rs.getLong(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), (Integer) rs.getObject(5), rs.getTimestamp(6).toLocalDateTime()));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo leer el historial del diagnostico principal: " + e.getMessage(), e);
        }
        return cambios;
    }

    @Override
    public boolean actualizar(FichaDiagnostico diagnostico) throws DentrixException {
        String sql = "UPDATE ficha_diagnostico SET pieza_fdi = ?, es_principal = ?, observacion = ? "
                + "WHERE ficha_id = ? AND cie10_codigo = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, diagnostico.getPiezaFdi());
            ps.setBoolean(2, diagnostico.isEsPrincipal());
            ps.setObject(3, diagnostico.getObservacion());
            ps.setObject(4, diagnostico.getFichaId());
            ps.setObject(5, diagnostico.getCie10Codigo());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar el diagnostico: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean eliminar(Long fichaId, String cie10Codigo) throws DentrixException {
        String sql = "DELETE FROM ficha_diagnostico WHERE ficha_id = ? AND cie10_codigo = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, fichaId);
            ps.setObject(2, cie10Codigo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo eliminar el diagnostico: " + e.getMessage(), e);
        }
    }

    @Override
    public int eliminarPorFicha(Long fichaId) throws DentrixException {
        String sql = "DELETE FROM ficha_diagnostico WHERE ficha_id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, fichaId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron eliminar los diagnosticos de la ficha: " + e.getMessage(), e);
        }
    }

    private FichaDiagnostico mapear(ResultSet rs) throws SQLException {
        FichaDiagnostico diagnostico = new FichaDiagnostico();
        diagnostico.setFichaId(rs.getLong("ficha_id"));
        diagnostico.setCie10Codigo(rs.getString("cie10_codigo"));
        int pieza = rs.getInt("pieza_fdi");
        diagnostico.setPiezaFdi(rs.wasNull() ? null : pieza);
        diagnostico.setEsPrincipal(rs.getBoolean("es_principal"));
        diagnostico.setObservacion(rs.getString("observacion"));
        return diagnostico;
    }
}
