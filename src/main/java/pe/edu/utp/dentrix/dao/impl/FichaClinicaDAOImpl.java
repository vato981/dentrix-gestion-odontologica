package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.FichaClinicaDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.FichaClinica;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FichaClinicaDAOImpl implements FichaClinicaDAO {

    private static final String COLUMNAS =
            "id, paciente_id, cita_id, odontologo_id, fecha, motivo_consulta, examen_clinico, "
                    + "prescripcion, plan_indicaciones, estado_derivacion, derivado_a_caja, fecha_derivacion";

    @Override
    public List<FichaClinica> listarPorPaciente(Integer pacienteId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM ficha_clinica WHERE paciente_id = ? ORDER BY fecha DESC, id DESC";
        List<FichaClinica> fichas = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, pacienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fichas.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo listar las fichas clinicas del paciente: " + e.getMessage(), e);
        }
        return fichas;
    }

    @Override
    public Optional<FichaClinica> buscarPorId(Long id) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM ficha_clinica WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo buscar la ficha clinica: " + e.getMessage(), e);
        }
    }

    @Override
    public int insertar(FichaClinica ficha) throws DentrixException {
        String sql = "INSERT INTO ficha_clinica (paciente_id, cita_id, odontologo_id, fecha, motivo_consulta, "
                + "examen_clinico, prescripcion, plan_indicaciones, estado_derivacion, derivado_a_caja, fecha_derivacion) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::estado_derivacion, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, ficha.getPacienteId());
            ps.setObject(2, ficha.getCitaId());
            ps.setObject(3, ficha.getOdontologoId());
            ps.setObject(4, ficha.getFecha() == null ? LocalDateTime.now() : ficha.getFecha());
            ps.setObject(5, ficha.getMotivoConsulta());
            ps.setObject(6, ficha.getExamenClinico());
            ps.setObject(7, ficha.getPrescripcion());
            ps.setObject(8, ficha.getPlanIndicaciones());
            ps.setString(9, ficha.getEstadoDerivacion() == null ? "PENDIENTE" : ficha.getEstadoDerivacion());
            ps.setBoolean(10, ficha.isDerivadoACaja());
            ps.setObject(11, ficha.getFechaDerivacion());
            int filas = ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    ficha.setId(rs.getLong(1));
                }
            }
            return filas;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo insertar la ficha clinica: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizar(FichaClinica ficha) throws DentrixException {
        String sql = "UPDATE ficha_clinica SET paciente_id = ?, cita_id = ?, odontologo_id = ?, fecha = ?, "
                    + "motivo_consulta = ?, examen_clinico = ?, prescripcion = ?, plan_indicaciones = ?, "
                    + "estado_derivacion = ?::estado_derivacion, derivado_a_caja = ?, fecha_derivacion = ? WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, ficha.getPacienteId());
            ps.setObject(2, ficha.getCitaId());
            ps.setObject(3, ficha.getOdontologoId());
            ps.setObject(4, ficha.getFecha());
            ps.setObject(5, ficha.getMotivoConsulta());
            ps.setObject(6, ficha.getExamenClinico());
            ps.setObject(7, ficha.getPrescripcion());
            ps.setObject(8, ficha.getPlanIndicaciones());
            ps.setString(9, ficha.getEstadoDerivacion() == null ? "PENDIENTE" : ficha.getEstadoDerivacion());
            ps.setBoolean(10, ficha.isDerivadoACaja());
            ps.setObject(11, ficha.getFechaDerivacion());
            ps.setObject(12, ficha.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar la ficha clinica: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean eliminar(Long id) throws DentrixException {
        String sql = "DELETE FROM ficha_clinica WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo eliminar la ficha clinica: " + e.getMessage(), e);
        }
    }

    private FichaClinica mapear(ResultSet rs) throws SQLException {
        FichaClinica ficha = new FichaClinica();
        ficha.setId(rs.getLong("id"));
        ficha.setPacienteId(rs.getInt("paciente_id"));
        long citaId = rs.getLong("cita_id");
        ficha.setCitaId(rs.wasNull() ? null : citaId);
        ficha.setOdontologoId(rs.getInt("odontologo_id"));
        ficha.setFecha(rs.getObject("fecha", LocalDateTime.class));
        ficha.setMotivoConsulta(rs.getString("motivo_consulta"));
        ficha.setExamenClinico(rs.getString("examen_clinico"));
        ficha.setPrescripcion(rs.getString("prescripcion"));
        ficha.setPlanIndicaciones(rs.getString("plan_indicaciones"));
        ficha.setEstadoDerivacion(rs.getString("estado_derivacion"));
        ficha.setDerivadoACaja(rs.getBoolean("derivado_a_caja"));
        ficha.setFechaDerivacion(rs.getObject("fecha_derivacion", LocalDateTime.class));
        return ficha;
    }
}
