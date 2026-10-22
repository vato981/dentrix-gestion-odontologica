package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.AgendaExtraDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.CitaHistorial;
import pe.edu.utp.dentrix.model.ConsentimientoDatos;
import pe.edu.utp.dentrix.model.ListaEspera;
import pe.edu.utp.dentrix.model.Recordatorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AgendaExtraDAOImpl implements AgendaExtraDAO {

    private static final String COLUMNAS_ESPERA =
            "e.id, e.paciente_id, p.apellidos || ', ' || p.nombres AS paciente_nombre, "
                    + "e.tratamiento_id, t.nombre AS tratamiento_nombre, e.prioridad, e.nota, "
                    + "e.fecha_alta, e.estado, e.cita_id, e.motivo_retiro, e.usuario_id";

    private static final String DESDE_ESPERA =
            " FROM lista_espera e JOIN paciente p ON p.id = e.paciente_id "
                    + "LEFT JOIN tratamiento t ON t.id = e.tratamiento_id";

    private static final String ORDEN_ESPERA = " ORDER BY e.prioridad, e.fecha_alta, e.id";

    private static final String COLUMNAS_RECORDATORIO =
            "id, cita_id, canal, programado, enviado, estado, respuesta, usuario_id";

    private static final String COLUMNAS_HISTORIAL =
            "id, cita_id, estado_anterior, estado_nuevo, motivo, usuario_id, fecha";

    private static final String COLUMNAS_CONSENTIMIENTO =
            "id, paciente_id, version, aceptado, fecha, medio, evidencia, motivo_rechazo";

    private ListaEspera leerEspera(ResultSet rs) throws SQLException {
        ListaEspera espera = new ListaEspera();
        espera.setId(rs.getObject("id", Long.class));
        espera.setPacienteId(rs.getObject("paciente_id", Integer.class));
        espera.setPacienteNombre(rs.getString("paciente_nombre"));
        espera.setTratamientoId(rs.getObject("tratamiento_id", Integer.class));
        espera.setTratamientoNombre(rs.getString("tratamiento_nombre"));
        espera.setPrioridad(rs.getObject("prioridad", Integer.class));
        espera.setNota(rs.getString("nota"));
        espera.setFechaAlta(rs.getObject("fecha_alta", LocalDateTime.class));
        espera.setEstado(rs.getString("estado"));
        espera.setCitaId(rs.getObject("cita_id", Long.class));
        espera.setMotivoRetiro(rs.getString("motivo_retiro"));
        espera.setUsuarioId(rs.getObject("usuario_id", Integer.class));
        return espera;
    }

    private Recordatorio leerRecordatorio(ResultSet rs) throws SQLException {
        Recordatorio recordatorio = new Recordatorio();
        recordatorio.setId(rs.getObject("id", Long.class));
        recordatorio.setCitaId(rs.getObject("cita_id", Long.class));
        recordatorio.setCanal(rs.getString("canal"));
        recordatorio.setProgramado(rs.getObject("programado", LocalDateTime.class));
        recordatorio.setEnviado(rs.getObject("enviado", LocalDateTime.class));
        recordatorio.setEstado(rs.getString("estado"));
        recordatorio.setRespuesta(rs.getString("respuesta"));
        recordatorio.setUsuarioId(rs.getObject("usuario_id", Integer.class));
        return recordatorio;
    }

    private CitaHistorial leerHistorial(ResultSet rs) throws SQLException {
        CitaHistorial historial = new CitaHistorial();
        historial.setId(rs.getObject("id", Long.class));
        historial.setCitaId(rs.getObject("cita_id", Long.class));
        historial.setEstadoAnterior(rs.getString("estado_anterior"));
        historial.setEstadoNuevo(rs.getString("estado_nuevo"));
        historial.setMotivo(rs.getString("motivo"));
        historial.setUsuarioId(rs.getObject("usuario_id", Integer.class));
        historial.setFecha(rs.getObject("fecha", LocalDateTime.class));
        return historial;
    }

    private ConsentimientoDatos leerConsentimiento(ResultSet rs) throws SQLException {
        ConsentimientoDatos consentimiento = new ConsentimientoDatos();
        consentimiento.setId(rs.getObject("id", Long.class));
        consentimiento.setPacienteId(rs.getObject("paciente_id", Integer.class));
        consentimiento.setVersion(rs.getString("version"));
        consentimiento.setAceptado(rs.getObject("aceptado", Boolean.class));
        consentimiento.setFecha(rs.getObject("fecha", LocalDateTime.class));
        consentimiento.setMedio(rs.getString("medio"));
        consentimiento.setEvidencia(rs.getString("evidencia"));
        consentimiento.setMotivoRechazo(rs.getString("motivo_rechazo"));
        return consentimiento;
    }

    @Override
    public List<ListaEspera> listarEspera(String estado) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_ESPERA + DESDE_ESPERA;
        if (estado != null) {
            sql += " WHERE e.estado = ?::estado_espera";
        }
        sql += ORDEN_ESPERA;
        List<ListaEspera> esperas = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            if (estado != null) {
                ps.setString(1, estado);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    esperas.add(leerEspera(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar la lista de espera.", e);
        }
        return esperas;
    }

    @Override
    public Long insertarEspera(ListaEspera espera) throws DentrixException {
        String sql = "INSERT INTO lista_espera (paciente_id, tratamiento_id, prioridad, nota, "
                + "fecha_alta, estado, usuario_id) "
                + "VALUES (?, ?, ?, ?, COALESCE(?, CURRENT_TIMESTAMP), ?::estado_espera, ?) "
                + "RETURNING id";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, espera.getPacienteId());
            ps.setObject(2, espera.getTratamientoId());
            ps.setObject(3, espera.getPrioridad() == null ? 3 : espera.getPrioridad());
            ps.setObject(4, espera.getNota());
            ps.setObject(5, espera.getFechaAlta());
            ps.setString(6, espera.getEstado() == null ? "ESPERANDO" : espera.getEstado());
            ps.setObject(7, espera.getUsuarioId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    espera.setId(id);
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador de la espera.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar la espera.", e);
        }
    }

    @Override
    public boolean retirarEspera(Long id, String motivo) throws DentrixException {
        String sql = "UPDATE lista_espera SET estado = 'RETIRADO', motivo_retiro = ? "
                + "WHERE id = ? AND estado = 'ESPERANDO'";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, motivo);
            ps.setObject(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo retirar la espera.", e);
        }
    }

    @Override
    public boolean marcarEsperaAgendada(Long id, Long citaId) throws DentrixException {
        String sql = "UPDATE lista_espera SET estado = 'AGENDADO', cita_id = ? "
                + "WHERE id = ? AND estado = 'ESPERANDO'";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, citaId);
            ps.setObject(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo marcar la espera como agendada.", e);
        }
    }

    @Override
    public List<Recordatorio> listarRecordatorios(Long citaId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_RECORDATORIO + " FROM recordatorio "
                + "WHERE cita_id = ? ORDER BY programado DESC, id DESC";
        List<Recordatorio> recordatorios = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, citaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    recordatorios.add(leerRecordatorio(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar los recordatorios.", e);
        }
        return recordatorios;
    }

    @Override
    public Long insertarRecordatorio(Recordatorio recordatorio) throws DentrixException {
        String sql = "INSERT INTO recordatorio (cita_id, canal, programado, enviado, estado, "
                + "respuesta, usuario_id) "
                + "VALUES (?, ?::canal_recordatorio, ?, ?, ?::estado_recordatorio, ?, ?) "
                + "RETURNING id";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, recordatorio.getCitaId());
            ps.setString(2, recordatorio.getCanal() == null ? "WHATSAPP" : recordatorio.getCanal());
            ps.setObject(3, recordatorio.getProgramado());
            ps.setObject(4, recordatorio.getEnviado());
            ps.setString(5, recordatorio.getEstado() == null ? "PENDIENTE" : recordatorio.getEstado());
            ps.setObject(6, recordatorio.getRespuesta());
            ps.setObject(7, recordatorio.getUsuarioId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    recordatorio.setId(id);
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador del recordatorio.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el recordatorio.", e);
        }
    }

    @Override
    public boolean actualizarRecordatorio(Long id, String estado, String respuesta, LocalDateTime enviado)
            throws DentrixException {
        String sql = "UPDATE recordatorio SET estado = ?::estado_recordatorio, respuesta = ?, "
                + "enviado = COALESCE(?, enviado) WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setObject(2, respuesta);
            ps.setObject(3, enviado);
            ps.setObject(4, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar el recordatorio.", e);
        }
    }

    @Override
    public List<CitaHistorial> listarHistorial(Long citaId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_HISTORIAL + " FROM cita_historial "
                + "WHERE cita_id = ? ORDER BY fecha, id";
        List<CitaHistorial> historial = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, citaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    historial.add(leerHistorial(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar el historial de la cita.", e);
        }
        return historial;
    }

    @Override
    public int inasistenciasRecientes(Integer pacienteId) throws DentrixException {
        String sql = "SELECT inasistencias_recientes(?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, pacienteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron contar las inasistencias del paciente.", e);
        }
    }

    @Override
    public Long reprogramar(Long citaId, LocalDate fecha, LocalTime hora, Integer consultorioId,
                            Integer usuarioId, String motivo) throws DentrixException {
        String sql = "SELECT reprogramar_cita(?, ?::date, ?::time, ?::int, ?::int, ?::varchar)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, citaId);
            ps.setObject(2, fecha);
            ps.setObject(3, hora);
            ps.setObject(4, consultorioId);
            ps.setObject(5, usuarioId);
            ps.setString(6, motivo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            throw new DentrixException("No se pudo reprogramar la cita.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo reprogramar la cita.", e);
        }
    }

    @Override
    public Long insertarConsentimientoDatos(ConsentimientoDatos consentimiento) throws DentrixException {
        String sql = "INSERT INTO consentimiento_datos (paciente_id, version, aceptado, fecha, medio, "
                + "evidencia, motivo_rechazo) "
                + "VALUES (?, ?, ?, COALESCE(?, CURRENT_TIMESTAMP), ?, ?, ?) RETURNING id";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, consentimiento.getPacienteId());
            ps.setObject(2, consentimiento.getVersion());
            ps.setObject(3, consentimiento.getAceptado());
            ps.setObject(4, consentimiento.getFecha());
            ps.setObject(5, consentimiento.getMedio());
            ps.setObject(6, consentimiento.getEvidencia());
            ps.setObject(7, consentimiento.getMotivoRechazo());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    consentimiento.setId(id);
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador del consentimiento.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el consentimiento de datos.", e);
        }
    }

    @Override
    public Optional<ConsentimientoDatos> consentimientoDatos(Integer pacienteId, String version)
            throws DentrixException {
        String sql = "SELECT " + COLUMNAS_CONSENTIMIENTO + " FROM consentimiento_datos "
                + "WHERE paciente_id = ?";
        if (version != null) {
            sql += " AND version = ?";
        }
        sql += " ORDER BY fecha DESC, id DESC LIMIT 1";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, pacienteId);
            if (version != null) {
                ps.setObject(2, version);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leerConsentimiento(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar el consentimiento de datos.", e);
        }
        return Optional.empty();
    }
}
