package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.CitaDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Cita;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CitaDAOImpl implements CitaDAO {

    private static final String COLUMNAS =
            "id, tipo, paciente_id, usuario_id, consultorio_id, fecha_cita, hora_inicio, hora_fin, "
                    + "tratamiento_id, estado, motivo, cita_origen_id";

    private Cita leer(ResultSet rs) throws SQLException {
        Cita cita = new Cita();
        cita.setId(rs.getLong(1));
        cita.setTipo(rs.getString(2));
        cita.setPacienteId(rs.getObject(3, Integer.class));
        cita.setUsuarioId(rs.getInt(4));
        cita.setConsultorioId(rs.getInt(5));
        cita.setFechaCita(rs.getObject(6, LocalDate.class));
        cita.setHoraInicio(rs.getObject(7, LocalTime.class));
        cita.setHoraFin(rs.getObject(8, LocalTime.class));
        cita.setTratamientoId(rs.getObject(9, Integer.class));
        cita.setEstado(rs.getString(10));
        cita.setMotivo(rs.getString(11));
        cita.setCitaOrigenId(rs.getObject(12, Long.class));
        return cita;
    }

    private List<Cita> listar(String sql, Object[] parametros) throws DentrixException {
        List<Cita> citas = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    citas.add(leer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron listar las citas.", e);
        }
        return citas;
    }

    @Override
    public List<Cita> listarTodas() throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM cita ORDER BY fecha_cita, hora_inicio, id";
        return listar(sql, new Object[0]);
    }

    @Override
    public List<Cita> listarPorFecha(LocalDate fecha) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM cita WHERE fecha_cita = ?";
        return listar(sql, new Object[]{fecha});
    }

    @Override
    public List<Cita> listarPorFechaYConsultorio(LocalDate fecha, Integer consultorioId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM cita WHERE fecha_cita = ? AND consultorio_id = ? "
                + "ORDER BY hora_inicio, id";
        return listar(sql, new Object[]{fecha, consultorioId});
    }

    @Override
    public List<Cita> listarPorPaciente(Integer pacienteId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM cita WHERE paciente_id = ? ORDER BY fecha_cita, hora_inicio, id";
        return listar(sql, new Object[]{pacienteId});
    }

    @Override
    public List<Cita> listarPorPacienteEnFecha(Integer pacienteId, LocalDate fecha) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM cita WHERE paciente_id = ? AND fecha_cita = ? "
                + "ORDER BY hora_inicio, id";
        return listar(sql, new Object[]{pacienteId, fecha});
    }

    @Override
    public Optional<Cita> buscarPorId(Long id) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM cita WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar la cita.", e);
        }
        return Optional.empty();
    }

    @Override
    public int insertar(Cita cita) throws DentrixException {
        String sql = "INSERT INTO cita (tipo, paciente_id, usuario_id, consultorio_id, fecha_cita, "
                + "hora_inicio, hora_fin, tratamiento_id, estado, motivo, cita_origen_id) "
                + "VALUES (?::tipo_bloque, ?, ?, ?, ?, ?, ?, ?, ?::estado_cita, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, cita.getTipo());
            ps.setObject(2, cita.getPacienteId());
            ps.setObject(3, cita.getUsuarioId());
            ps.setObject(4, cita.getConsultorioId());
            ps.setObject(5, cita.getFechaCita());
            ps.setObject(6, cita.getHoraInicio());
            ps.setObject(7, cita.getHoraFin());
            ps.setObject(8, cita.getTratamientoId());
            ps.setObject(9, cita.getEstado());
            ps.setObject(10, cita.getMotivo());
            ps.setObject(11, cita.getCitaOrigenId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return (int) rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar la cita.", e);
        }
        throw new DentrixException("No se pudo obtener el identificador de la cita.");
    }

    @Override
    public boolean actualizar(Cita cita) throws DentrixException {
        String sql = "UPDATE cita SET tipo = ?::tipo_bloque, paciente_id = ?, usuario_id = ?, consultorio_id = ?, "
                + "fecha_cita = ?, hora_inicio = ?, hora_fin = ?, tratamiento_id = ?, "
                + "estado = ?::estado_cita, motivo = ? WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, cita.getTipo());
            ps.setObject(2, cita.getPacienteId());
            ps.setObject(3, cita.getUsuarioId());
            ps.setObject(4, cita.getConsultorioId());
            ps.setObject(5, cita.getFechaCita());
            ps.setObject(6, cita.getHoraInicio());
            ps.setObject(7, cita.getHoraFin());
            ps.setObject(8, cita.getTratamientoId());
            ps.setObject(9, cita.getEstado());
            ps.setObject(10, cita.getMotivo());
            ps.setObject(11, cita.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar la cita.", e);
        }
    }
}
