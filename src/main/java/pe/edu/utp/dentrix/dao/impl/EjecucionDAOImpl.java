package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.EjecucionDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.CierrePlan;
import pe.edu.utp.dentrix.model.ControlPost;
import pe.edu.utp.dentrix.model.EjecucionItem;
import pe.edu.utp.dentrix.model.Evolucion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EjecucionDAOImpl implements EjecucionDAO {

    private static final String COLUMNAS_EJECUCION =
            "e.id, e.presupuesto_item_id, e.numero_sesion, e.sesiones_planificadas, e.fecha, "
                    + "e.profesional_id, u.nombres || ' ' || u.apellidos AS profesional_nombre, "
                    + "e.procedimiento, e.pieza_fdi, e.superficies, e.observaciones, e.estado, "
                    + "e.motivo_anulacion";

    private static final String DESDE_EJECUCION =
            " FROM ejecucion_item e JOIN usuario u ON u.id = e.profesional_id";

    private static final String COLUMNAS_EVOLUCION =
            "id, ficha_id, ejecucion_item_id, texto, estado, profesional_id, fecha, fecha_firma, "
                    + "firma_hash, enmienda_de, motivo_enmienda";

    private static final String COLUMNAS_CONTROL =
            "id, presupuesto_item_id, fecha_programada, fecha_realizada, estado, hallazgo, "
                    + "profesional_id, motivo_cancelacion";

    private static final String COLUMNAS_CIERRE =
            "id, presupuesto_id, estado, motivo, fecha, usuario_id, fecha_recall";

    private EjecucionItem leerEjecucion(ResultSet rs) throws SQLException {
        EjecucionItem ejecucion = new EjecucionItem();
        ejecucion.setId(rs.getLong("id"));
        ejecucion.setPresupuestoItemId(rs.getObject("presupuesto_item_id", Long.class));
        ejecucion.setNumeroSesion(rs.getObject("numero_sesion", Integer.class));
        ejecucion.setSesionesPlanificadas(rs.getObject("sesiones_planificadas", Integer.class));
        ejecucion.setFecha(rs.getObject("fecha", LocalDateTime.class));
        ejecucion.setProfesionalId(rs.getObject("profesional_id", Integer.class));
        ejecucion.setProfesionalNombre(rs.getString("profesional_nombre"));
        ejecucion.setProcedimiento(rs.getString("procedimiento"));
        ejecucion.setPiezaFdi(rs.getObject("pieza_fdi", Integer.class));
        ejecucion.setSuperficies(rs.getString("superficies"));
        ejecucion.setObservaciones(rs.getString("observaciones"));
        ejecucion.setEstado(rs.getString("estado"));
        ejecucion.setMotivoAnulacion(rs.getString("motivo_anulacion"));
        return ejecucion;
    }

    private Evolucion leerEvolucion(ResultSet rs) throws SQLException {
        Evolucion evolucion = new Evolucion();
        evolucion.setId(rs.getLong("id"));
        evolucion.setFichaId(rs.getObject("ficha_id", Long.class));
        evolucion.setEjecucionItemId(rs.getObject("ejecucion_item_id", Long.class));
        evolucion.setTexto(rs.getString("texto"));
        evolucion.setEstado(rs.getString("estado"));
        evolucion.setProfesionalId(rs.getObject("profesional_id", Integer.class));
        evolucion.setFecha(rs.getObject("fecha", LocalDateTime.class));
        evolucion.setFechaFirma(rs.getObject("fecha_firma", LocalDateTime.class));
        evolucion.setFirmaHash(rs.getString("firma_hash"));
        evolucion.setEnmiendaDe(rs.getObject("enmienda_de", Long.class));
        evolucion.setMotivoEnmienda(rs.getString("motivo_enmienda"));
        return evolucion;
    }

    private ControlPost leerControl(ResultSet rs) throws SQLException {
        ControlPost control = new ControlPost();
        control.setId(rs.getLong("id"));
        control.setPresupuestoItemId(rs.getObject("presupuesto_item_id", Long.class));
        control.setFechaProgramada(rs.getObject("fecha_programada", LocalDate.class));
        control.setFechaRealizada(rs.getObject("fecha_realizada", LocalDate.class));
        control.setEstado(rs.getString("estado"));
        control.setHallazgo(rs.getString("hallazgo"));
        control.setProfesionalId(rs.getObject("profesional_id", Integer.class));
        control.setMotivoCancelacion(rs.getString("motivo_cancelacion"));
        return control;
    }

    private CierrePlan leerCierre(ResultSet rs) throws SQLException {
        CierrePlan cierre = new CierrePlan();
        cierre.setId(rs.getLong("id"));
        cierre.setPresupuestoId(rs.getObject("presupuesto_id", Long.class));
        cierre.setEstado(rs.getString("estado"));
        cierre.setMotivo(rs.getString("motivo"));
        cierre.setFecha(rs.getObject("fecha", LocalDateTime.class));
        cierre.setUsuarioId(rs.getObject("usuario_id", Integer.class));
        cierre.setFechaRecall(rs.getObject("fecha_recall", LocalDate.class));
        return cierre;
    }

    @Override
    public Long insertarEjecucion(EjecucionItem ejecucion) throws DentrixException {
        String sql = "INSERT INTO ejecucion_item (presupuesto_item_id, numero_sesion, "
                + "sesiones_planificadas, fecha, profesional_id, procedimiento, pieza_fdi, superficies, "
                + "observaciones) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, ejecucion.getPresupuestoItemId());
            ps.setObject(2, ejecucion.getNumeroSesion());
            ps.setObject(3, ejecucion.getSesionesPlanificadas() == null
                    ? 1 : ejecucion.getSesionesPlanificadas());
            ps.setObject(4, ejecucion.getFecha() == null
                    ? LocalDateTime.now() : ejecucion.getFecha());
            ps.setObject(5, ejecucion.getProfesionalId());
            ps.setObject(6, ejecucion.getProcedimiento());
            ps.setObject(7, ejecucion.getPiezaFdi());
            ps.setObject(8, ejecucion.getSuperficies());
            ps.setObject(9, ejecucion.getObservaciones());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    ejecucion.setId(id);
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador de la sesion ejecutada.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar la sesion ejecutada.", e);
        }
    }

    @Override
    public Optional<EjecucionItem> buscarEjecucion(Long id) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_EJECUCION + DESDE_EJECUCION + " WHERE e.id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leerEjecucion(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar la sesion ejecutada.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<EjecucionItem> listarEjecuciones(Long presupuestoItemId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_EJECUCION + DESDE_EJECUCION
                + " WHERE e.presupuesto_item_id = ? ORDER BY e.numero_sesion";
        List<EjecucionItem> sesiones = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, presupuestoItemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sesiones.add(leerEjecucion(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar las sesiones del item.", e);
        }
        return sesiones;
    }

    @Override
    public Optional<EjecucionItem> buscarSesionBorrador(Long presupuestoItemId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_EJECUCION + DESDE_EJECUCION
                + " WHERE e.presupuesto_item_id = ? AND e.estado = 'BORRADOR'::estado_ejecucion "
                + "ORDER BY e.numero_sesion DESC LIMIT 1";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, presupuestoItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leerEjecucion(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar la sesion en borrador del item.", e);
        }
        return Optional.empty();
    }

    @Override
    public boolean eliminarEjecucion(Long id) throws DentrixException {
        String sql = "DELETE FROM ejecucion_item WHERE id = ? AND estado = 'BORRADOR'::estado_ejecucion";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo descartar la sesion en borrador.", e);
        }
    }

    @Override
    public boolean anularEjecucion(Long id, String motivo) throws DentrixException {
        String sql = "UPDATE ejecucion_item SET estado = 'ANULADA'::estado_ejecucion, motivo_anulacion = ? "
                + "WHERE id = ? AND estado = 'FIRMADA'::estado_ejecucion";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, motivo);
            ps.setObject(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo anular la sesion ejecutada.", e);
        }
    }

    @Override
    public Long insertarEvolucion(Evolucion evolucion) throws DentrixException {
        String sql = "INSERT INTO evolucion (ficha_id, ejecucion_item_id, texto, estado, profesional_id, "
                + "fecha, fecha_firma, firma_hash, enmienda_de, motivo_enmienda) "
                + "VALUES (?, ?, ?, ?::estado_evolucion, ?, ?, ?, ?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, evolucion.getFichaId());
            ps.setObject(2, evolucion.getEjecucionItemId());
            ps.setObject(3, evolucion.getTexto());
            ps.setString(4, evolucion.getEstado() == null ? "BORRADOR" : evolucion.getEstado());
            ps.setObject(5, evolucion.getProfesionalId());
            ps.setObject(6, evolucion.getFecha() == null
                    ? LocalDateTime.now() : evolucion.getFecha());
            ps.setObject(7, evolucion.getFechaFirma());
            ps.setObject(8, evolucion.getFirmaHash());
            ps.setObject(9, evolucion.getEnmiendaDe());
            ps.setObject(10, evolucion.getMotivoEnmienda());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    evolucion.setId(id);
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador de la evolucion.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar la evolucion.", e);
        }
    }

    @Override
    public Optional<Evolucion> buscarEvolucion(Long id) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_EVOLUCION + " FROM evolucion WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leerEvolucion(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar la evolucion.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Evolucion> listarEvoluciones(Long fichaId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_EVOLUCION + " FROM evolucion "
                + "WHERE ficha_id = ? ORDER BY fecha DESC, id DESC";
        List<Evolucion> evoluciones = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, fichaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    evoluciones.add(leerEvolucion(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar las evoluciones de la ficha.", e);
        }
        return evoluciones;
    }

    @Override
    public Optional<Evolucion> evolucionDeSesion(Long ejecucionItemId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_EVOLUCION + " FROM evolucion "
                + "WHERE ejecucion_item_id = ? AND estado = 'FIRMADA'::estado_evolucion "
                + "AND enmienda_de IS NULL";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, ejecucionItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leerEvolucion(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar la evolucion de la sesion.", e);
        }
        return Optional.empty();
    }

    @Override
    public Long insertarControl(ControlPost control) throws DentrixException {
        String sql = "INSERT INTO control_post (presupuesto_item_id, fecha_programada, fecha_realizada, "
                + "estado, hallazgo, profesional_id, motivo_cancelacion) "
                + "VALUES (?, ?, ?, ?::estado_control_post, ?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, control.getPresupuestoItemId());
            ps.setObject(2, control.getFechaProgramada());
            ps.setObject(3, control.getFechaRealizada());
            ps.setString(4, control.getEstado() == null ? "PENDIENTE" : control.getEstado());
            ps.setObject(5, control.getHallazgo());
            ps.setObject(6, control.getProfesionalId());
            ps.setObject(7, control.getMotivoCancelacion());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    control.setId(id);
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador del control post.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo programar el control post.", e);
        }
    }

    @Override
    public Optional<ControlPost> controlPendiente(Long presupuestoItemId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_CONTROL + " FROM control_post "
                + "WHERE presupuesto_item_id = ? AND estado = 'PENDIENTE'::estado_control_post";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, presupuestoItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leerControl(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar el control pendiente del item.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<ControlPost> listarControles(Long presupuestoItemId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_CONTROL + " FROM control_post "
                + "WHERE presupuesto_item_id = ? ORDER BY fecha_programada DESC, id DESC";
        List<ControlPost> controles = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, presupuestoItemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    controles.add(leerControl(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar los controles del item.", e);
        }
        return controles;
    }

    @Override
    public boolean realizarControl(Long id, LocalDate fecha, String hallazgo, Integer profesionalId)
            throws DentrixException {
        String sql = "UPDATE control_post SET estado = 'REALIZADO'::estado_control_post, "
                + "fecha_realizada = ?, hallazgo = ?, profesional_id = ? "
                + "WHERE id = ? AND estado = 'PENDIENTE'::estado_control_post";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, fecha);
            ps.setObject(2, hallazgo);
            ps.setObject(3, profesionalId);
            ps.setObject(4, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el control post realizado.", e);
        }
    }

    @Override
    public Long insertarCierre(CierrePlan cierre) throws DentrixException {
        String sql = "INSERT INTO cierre_plan (presupuesto_id, estado, motivo, fecha, usuario_id, "
                + "fecha_recall) VALUES (?, ?::estado_cierre_plan, ?, ?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, cierre.getPresupuestoId());
            ps.setString(2, cierre.getEstado());
            ps.setObject(3, cierre.getMotivo());
            ps.setObject(4, cierre.getFecha() == null
                    ? LocalDateTime.now() : cierre.getFecha());
            ps.setObject(5, cierre.getUsuarioId());
            ps.setObject(6, cierre.getFechaRecall());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    cierre.setId(id);
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador del cierre del plan.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo cerrar el plan.", e);
        }
    }

    @Override
    public Optional<CierrePlan> buscarCierre(Long presupuestoId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_CIERRE + " FROM cierre_plan WHERE presupuesto_id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, presupuestoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leerCierre(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar el cierre del plan.", e);
        }
        return Optional.empty();
    }
}
