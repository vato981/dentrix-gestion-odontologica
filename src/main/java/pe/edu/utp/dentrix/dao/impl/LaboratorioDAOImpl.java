package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.LaboratorioDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.ConsumoLote;
import pe.edu.utp.dentrix.model.ControlLaboratorio;
import pe.edu.utp.dentrix.model.Insumo;
import pe.edu.utp.dentrix.model.LoteInsumo;
import pe.edu.utp.dentrix.model.OrdenLaboratorio;

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

public class LaboratorioDAOImpl implements LaboratorioDAO {

    private static final String COLUMNAS_INSUMO =
            "id, codigo, nombre, unidad, requiere_lote, estado";

    private static final String COLUMNAS_LOTE =
            "l.id, l.insumo_id, i.nombre AS insumo_nombre, l.numero, l.vence";

    private static final String COLUMNAS_CONSUMO =
            "c.id, c.ejecucion_item_id, c.lote_insumo_id, l.numero AS lote, i.nombre AS insumo_nombre, "
                    + "c.cantidad, c.fecha, c.usuario_id";

    private static final String DESDE_CONSUMO =
            " FROM consumo_lote c JOIN lote_insumo l ON l.id = c.lote_insumo_id "
                    + "JOIN insumo i ON i.id = l.insumo_id";

    private static final String COLUMNAS_ORDEN =
            "id, presupuesto_item_id, ejecucion_item_id, laboratorio, trabajo, material, color, "
                    + "fecha_solicitud, fecha_prometida, estado, costo, orden_origen_id, motivo_rechazo, "
                    + "motivo_anulacion";

    private static final String COLUMNAS_CONTROL =
            "id, orden_id, fecha, ajuste, color_ok, contactos_ok, conforme, observaciones, usuario_id";

    private Insumo leerInsumo(ResultSet rs) throws SQLException {
        Insumo insumo = new Insumo();
        insumo.setId(rs.getObject("id", Integer.class));
        insumo.setCodigo(rs.getString("codigo"));
        insumo.setNombre(rs.getString("nombre"));
        insumo.setUnidad(rs.getString("unidad"));
        insumo.setRequiereLote(rs.getObject("requiere_lote", Boolean.class));
        insumo.setEstado(rs.getString("estado"));
        return insumo;
    }

    private LoteInsumo leerLote(ResultSet rs) throws SQLException {
        LoteInsumo lote = new LoteInsumo();
        lote.setId(rs.getObject("id", Long.class));
        lote.setInsumoId(rs.getObject("insumo_id", Integer.class));
        lote.setInsumoNombre(rs.getString("insumo_nombre"));
        lote.setNumero(rs.getString("numero"));
        lote.setVence(rs.getObject("vence", LocalDate.class));
        return lote;
    }

    private ConsumoLote leerConsumo(ResultSet rs) throws SQLException {
        ConsumoLote consumo = new ConsumoLote();
        consumo.setId(rs.getObject("id", Long.class));
        consumo.setEjecucionItemId(rs.getObject("ejecucion_item_id", Long.class));
        consumo.setLoteInsumoId(rs.getObject("lote_insumo_id", Long.class));
        consumo.setLote(rs.getString("lote"));
        consumo.setInsumoNombre(rs.getString("insumo_nombre"));
        consumo.setCantidad(rs.getBigDecimal("cantidad"));
        consumo.setFecha(rs.getObject("fecha", LocalDateTime.class));
        consumo.setUsuarioId(rs.getObject("usuario_id", Integer.class));
        return consumo;
    }

    private OrdenLaboratorio leerOrden(ResultSet rs) throws SQLException {
        OrdenLaboratorio orden = new OrdenLaboratorio();
        orden.setId(rs.getObject("id", Long.class));
        orden.setPresupuestoItemId(rs.getObject("presupuesto_item_id", Long.class));
        orden.setEjecucionItemId(rs.getObject("ejecucion_item_id", Long.class));
        orden.setLaboratorio(rs.getString("laboratorio"));
        orden.setTrabajo(rs.getString("trabajo"));
        orden.setMaterial(rs.getString("material"));
        orden.setColor(rs.getString("color"));
        orden.setFechaSolicitud(rs.getObject("fecha_solicitud", LocalDateTime.class));
        orden.setFechaPrometida(rs.getObject("fecha_prometida", LocalDate.class));
        orden.setEstado(rs.getString("estado"));
        orden.setCosto(rs.getBigDecimal("costo"));
        orden.setOrdenOrigenId(rs.getObject("orden_origen_id", Long.class));
        orden.setMotivoRechazo(rs.getString("motivo_rechazo"));
        orden.setMotivoAnulacion(rs.getString("motivo_anulacion"));
        return orden;
    }

    private ControlLaboratorio leerControl(ResultSet rs) throws SQLException {
        ControlLaboratorio control = new ControlLaboratorio();
        control.setId(rs.getObject("id", Long.class));
        control.setOrdenId(rs.getObject("orden_id", Long.class));
        control.setFecha(rs.getObject("fecha", LocalDateTime.class));
        control.setAjuste(rs.getString("ajuste"));
        control.setColorOk(rs.getObject("color_ok", Boolean.class));
        control.setContactosOk(rs.getObject("contactos_ok", Boolean.class));
        control.setConforme(rs.getObject("conforme", Boolean.class));
        control.setObservaciones(rs.getString("observaciones"));
        control.setUsuarioId(rs.getObject("usuario_id", Integer.class));
        return control;
    }

    @Override
    public List<Insumo> listarInsumos() throws DentrixException {
        String sql = "SELECT " + COLUMNAS_INSUMO + " FROM insumo ORDER BY codigo";
        List<Insumo> insumos = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                insumos.add(leerInsumo(rs));
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar los insumos.", e);
        }
        return insumos;
    }

    @Override
    public Long insertarInsumo(Insumo insumo) throws DentrixException {
        String sql = "INSERT INTO insumo (codigo, nombre, unidad, requiere_lote, estado) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, insumo.getCodigo());
            ps.setObject(2, insumo.getNombre());
            ps.setString(3, insumo.getUnidad() == null ? "UNIDAD" : insumo.getUnidad());
            ps.setObject(4, Boolean.TRUE.equals(insumo.getRequiereLote()));
            ps.setString(5, insumo.getEstado() == null ? "ACTIVO" : insumo.getEstado());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    insumo.setId(id.intValue());
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador del insumo.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el insumo.", e);
        }
    }

    @Override
    public LoteInsumo insertarLote(LoteInsumo lote) throws DentrixException {
        String sql = "INSERT INTO lote_insumo (insumo_id, numero, vence) VALUES (?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, lote.getInsumoId());
            ps.setObject(2, lote.getNumero());
            ps.setObject(3, lote.getVence());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    lote.setId(rs.getLong(1));
                    return lote;
                }
            }
            throw new DentrixException("No se genero el identificador del lote del insumo.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el lote del insumo.", e);
        }
    }

    @Override
    public List<LoteInsumo> listarLotes() throws DentrixException {
        String sql = "SELECT " + COLUMNAS_LOTE + " FROM lote_insumo l "
                + "JOIN insumo i ON i.id = l.insumo_id ORDER BY l.vence ASC, l.id ASC";
        List<LoteInsumo> lotes = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lotes.add(leerLote(rs));
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar los lotes de insumo.", e);
        }
        return lotes;
    }

    @Override
    public Long insertarConsumo(ConsumoLote consumo) throws DentrixException {
        String sql = "INSERT INTO consumo_lote (ejecucion_item_id, lote_insumo_id, cantidad, fecha, "
                + "usuario_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, consumo.getEjecucionItemId());
            ps.setObject(2, consumo.getLoteInsumoId());
            ps.setObject(3, consumo.getCantidad());
            ps.setObject(4, consumo.getFecha() == null ? LocalDateTime.now() : consumo.getFecha());
            ps.setObject(5, consumo.getUsuarioId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    consumo.setId(id);
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador del consumo de lote.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el consumo del lote.", e);
        }
    }

    @Override
    public List<ConsumoLote> listarConsumos(Long ejecucionItemId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_CONSUMO + DESDE_CONSUMO
                + " WHERE c.ejecucion_item_id = ? ORDER BY c.fecha DESC, c.id DESC";
        List<ConsumoLote> consumos = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, ejecucionItemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    consumos.add(leerConsumo(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar los consumos de la sesion.", e);
        }
        return consumos;
    }

    @Override
    public boolean eliminarConsumo(Long id) throws DentrixException {
        String sql = "DELETE FROM consumo_lote WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo quitar el consumo del lote.", e);
        }
    }

    @Override
    public Long insertarOrden(OrdenLaboratorio orden) throws DentrixException {
        String sql = "INSERT INTO orden_laboratorio (presupuesto_item_id, ejecucion_item_id, laboratorio, "
                + "trabajo, material, color, fecha_solicitud, fecha_prometida, estado, costo, "
                + "orden_origen_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::estado_orden_lab, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, orden.getPresupuestoItemId());
            ps.setObject(2, orden.getEjecucionItemId());
            ps.setObject(3, orden.getLaboratorio());
            ps.setObject(4, orden.getTrabajo());
            ps.setObject(5, orden.getMaterial());
            ps.setObject(6, orden.getColor());
            ps.setObject(7, orden.getFechaSolicitud() == null
                    ? LocalDateTime.now() : orden.getFechaSolicitud());
            ps.setObject(8, orden.getFechaPrometida());
            ps.setString(9, orden.getEstado() == null ? "SOLICITADA" : orden.getEstado());
            ps.setObject(10, orden.getCosto());
            ps.setObject(11, orden.getOrdenOrigenId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    orden.setId(id);
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador de la orden de laboratorio.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar la orden de laboratorio.", e);
        }
    }

    @Override
    public Optional<OrdenLaboratorio> buscarOrden(Long id) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_ORDEN + " FROM orden_laboratorio WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leerOrden(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar la orden de laboratorio.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<OrdenLaboratorio> listarOrdenes(Long presupuestoItemId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_ORDEN + " FROM orden_laboratorio "
                + "WHERE presupuesto_item_id = ? ORDER BY fecha_solicitud DESC, id DESC";
        List<OrdenLaboratorio> ordenes = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, presupuestoItemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ordenes.add(leerOrden(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar las ordenes de laboratorio del item.", e);
        }
        return ordenes;
    }

    @Override
    public boolean actualizarEstadoOrden(Long id, String estado, String motivo) throws DentrixException {
        String sql = "UPDATE orden_laboratorio SET estado = ?::estado_orden_lab, motivo_anulacion = ? "
                + "WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setObject(2, "ANULADA".equals(estado) ? motivo : null);
            ps.setObject(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar el estado de la orden de laboratorio.", e);
        }
    }

    @Override
    public Long insertarControl(ControlLaboratorio control) throws DentrixException {
        String sql = "INSERT INTO control_laboratorio (orden_id, fecha, ajuste, color_ok, contactos_ok, "
                + "conforme, observaciones, usuario_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, control.getOrdenId());
            ps.setObject(2, control.getFecha() == null ? LocalDateTime.now() : control.getFecha());
            ps.setObject(3, control.getAjuste());
            ps.setObject(4, control.getColorOk());
            ps.setObject(5, control.getContactosOk());
            ps.setObject(6, Boolean.TRUE.equals(control.getConforme()));
            ps.setObject(7, control.getObservaciones());
            ps.setObject(8, control.getUsuarioId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    control.setId(id);
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador del control de laboratorio.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el control de laboratorio.", e);
        }
    }

    @Override
    public List<ControlLaboratorio> listarControles(Long ordenId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_CONTROL + " FROM control_laboratorio "
                + "WHERE orden_id = ? ORDER BY fecha DESC, id DESC";
        List<ControlLaboratorio> controles = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, ordenId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    controles.add(leerControl(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar los controles de la orden.", e);
        }
        return controles;
    }
}
