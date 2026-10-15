package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.HallazgoDentalDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.HallazgoDental;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HallazgoDentalDAOImpl implements HallazgoDentalDAO {

    private static final String COLUMNAS =
            "paciente_id, pieza_fdi, estado, cara, observacion, ultima_ficha_id, fecha_registro";

    @Override
    public List<HallazgoDental> listarPorPaciente(Integer pacienteId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM hallazgo_dental WHERE paciente_id = ? ORDER BY pieza_fdi";
        List<HallazgoDental> hallazgos = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, pacienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hallazgos.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo listar el odontograma del paciente: " + e.getMessage(), e);
        }
        return hallazgos;
    }

    @Override
    public Optional<HallazgoDental> buscarPorPacienteYPieza(Integer pacienteId, Integer piezaFdi) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM hallazgo_dental WHERE paciente_id = ? AND pieza_fdi = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, pacienteId);
            ps.setObject(2, piezaFdi);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo buscar la pieza del odontograma: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean guardar(HallazgoDental hallazgo) throws DentrixException {
        String sql = "INSERT INTO hallazgo_dental (paciente_id, pieza_fdi, estado, cara, observacion, ultima_ficha_id, fecha_registro) "
                + "VALUES (?, ?, ?::estado_pieza, ?, ?, ?, ?) "
                + "ON CONFLICT (paciente_id, pieza_fdi) "
                + "DO UPDATE SET estado = EXCLUDED.estado, cara = EXCLUDED.cara, "
                + "observacion = EXCLUDED.observacion, fecha_registro = CURRENT_TIMESTAMP";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, hallazgo.getPacienteId());
            ps.setObject(2, hallazgo.getPiezaFdi());
            ps.setString(3, hallazgo.getEstado() == null ? "SANO" : hallazgo.getEstado());
            ps.setObject(4, hallazgo.getCara());
            ps.setObject(5, hallazgo.getObservacion());
            ps.setObject(6, hallazgo.getUltimaFichaId());
            ps.setObject(7, hallazgo.getFechaRegistro() == null ? LocalDateTime.now() : hallazgo.getFechaRegistro());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo guardar el hallazgo dental: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean eliminar(Integer pacienteId, Integer piezaFdi) throws DentrixException {
        String sql = "DELETE FROM hallazgo_dental WHERE paciente_id = ? AND pieza_fdi = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, pacienteId);
            ps.setObject(2, piezaFdi);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo eliminar el hallazgo dental: " + e.getMessage(), e);
        }
    }

    private HallazgoDental mapear(ResultSet rs) throws SQLException {
        HallazgoDental hallazgo = new HallazgoDental();
        hallazgo.setPacienteId(rs.getInt("paciente_id"));
        hallazgo.setPiezaFdi(rs.getInt("pieza_fdi"));
        hallazgo.setEstado(rs.getString("estado"));
        hallazgo.setCara(rs.getString("cara"));
        hallazgo.setObservacion(rs.getString("observacion"));
        long ultimaFicha = rs.getLong("ultima_ficha_id");
        hallazgo.setUltimaFichaId(rs.wasNull() ? null : ultimaFicha);
        hallazgo.setFechaRegistro(rs.getObject("fecha_registro", LocalDateTime.class));
        return hallazgo;
    }
}
