package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.PresupuestoDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Aceptacion;
import pe.edu.utp.dentrix.model.Consentimiento;
import pe.edu.utp.dentrix.model.Presupuesto;
import pe.edu.utp.dentrix.model.PresupuestoItem;

import java.math.BigDecimal;
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

public class PresupuestoDAOImpl implements PresupuestoDAO {

    private static final String COLUMNAS =
            "p.id, p.paciente_id, pa.apellidos || ', ' || pa.nombres AS paciente_nombre, p.ficha_id, "
                    + "p.presupuesto_anterior_id, p.version, p.estado, p.moneda, p.fecha_emision, "
                    + "p.vigencia_dias, p.vigencia_hasta, p.subtotal, p.descuento, p.total, "
                    + "p.descuento_motivo, p.descuento_autorizado_por, p.motivo_rechazo, p.creado_por";

    private static final String DESDE =
            " FROM presupuesto p JOIN paciente pa ON pa.id = p.paciente_id";

    private static final String COLUMNAS_ITEM =
            "i.id, i.presupuesto_id, i.tratamiento_id, t.nombre AS tratamiento_nombre, i.pieza_fdi, "
                    + "i.fase, i.prioridad, i.secuencia, i.cantidad, i.sesiones_planificadas, "
                    + "i.precio_unitario, i.subtotal, i.monto_neto, i.precio_congelado, i.estado, "
                    + "i.requiere_consentimiento, i.motivo_rechazo, i.enmienda_id";

    private static final String DESDE_ITEM =
            " FROM presupuesto_item i JOIN tratamiento t ON t.id = i.tratamiento_id";

    private static final String COLUMNAS_ACEPTACION =
            "id, presupuesto_id, tipo, firmante_nombre, firmante_documento, firmante_rol, fecha, "
                    + "firma_hash, alternativas_ofrecidas, riesgos_informados, registrado_por";

    private Presupuesto leer(ResultSet rs) throws SQLException {
        Presupuesto presupuesto = new Presupuesto();
        presupuesto.setId(rs.getLong("id"));
        presupuesto.setPacienteId(rs.getObject("paciente_id", Integer.class));
        presupuesto.setPacienteNombre(rs.getString("paciente_nombre"));
        presupuesto.setFichaId(rs.getObject("ficha_id", Long.class));
        presupuesto.setPresupuestoAnteriorId(rs.getObject("presupuesto_anterior_id", Long.class));
        presupuesto.setVersion(rs.getObject("version", Integer.class));
        presupuesto.setEstado(rs.getString("estado"));
        presupuesto.setMoneda(rs.getString("moneda"));
        presupuesto.setFechaEmision(rs.getObject("fecha_emision", LocalDateTime.class));
        presupuesto.setVigenciaDias(rs.getObject("vigencia_dias", Integer.class));
        presupuesto.setVigenciaHasta(rs.getObject("vigencia_hasta", LocalDate.class));
        presupuesto.setSubtotal(rs.getBigDecimal("subtotal"));
        presupuesto.setDescuento(rs.getBigDecimal("descuento"));
        presupuesto.setTotal(rs.getBigDecimal("total"));
        presupuesto.setDescuentoMotivo(rs.getString("descuento_motivo"));
        presupuesto.setDescuentoAutorizadoPor(rs.getObject("descuento_autorizado_por", Integer.class));
        presupuesto.setMotivoRechazo(rs.getString("motivo_rechazo"));
        presupuesto.setCreadoPor(rs.getObject("creado_por", Integer.class));
        return presupuesto;
    }

    private PresupuestoItem leerItem(ResultSet rs) throws SQLException {
        PresupuestoItem item = new PresupuestoItem();
        item.setId(rs.getLong("id"));
        item.setPresupuestoId(rs.getObject("presupuesto_id", Long.class));
        item.setTratamientoId(rs.getObject("tratamiento_id", Integer.class));
        item.setTratamientoNombre(rs.getString("tratamiento_nombre"));
        item.setPiezaFdi(rs.getObject("pieza_fdi", Integer.class));
        item.setFase(rs.getString("fase"));
        item.setPrioridad(rs.getObject("prioridad", Integer.class));
        item.setSecuencia(rs.getObject("secuencia", Integer.class));
        item.setCantidad(rs.getObject("cantidad", Integer.class));
        item.setSesionesPlanificadas(rs.getObject("sesiones_planificadas", Integer.class));
        item.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        item.setSubtotal(rs.getBigDecimal("subtotal"));
        item.setMontoNeto(rs.getBigDecimal("monto_neto"));
        item.setPrecioCongelado(rs.getObject("precio_congelado", Boolean.class));
        item.setEstado(rs.getString("estado"));
        item.setRequiereConsentimiento(rs.getObject("requiere_consentimiento", Boolean.class));
        item.setMotivoRechazo(rs.getString("motivo_rechazo"));
        item.setEnmiendaId(rs.getObject("enmienda_id", Long.class));
        return item;
    }

    private Aceptacion leerAceptacion(ResultSet rs) throws SQLException {
        Aceptacion aceptacion = new Aceptacion();
        aceptacion.setId(rs.getLong("id"));
        aceptacion.setPresupuestoId(rs.getObject("presupuesto_id", Long.class));
        aceptacion.setTipo(rs.getString("tipo"));
        aceptacion.setFirmanteNombre(rs.getString("firmante_nombre"));
        aceptacion.setFirmanteDocumento(rs.getString("firmante_documento"));
        aceptacion.setFirmanteRol(rs.getString("firmante_rol"));
        aceptacion.setFecha(rs.getObject("fecha", LocalDateTime.class));
        aceptacion.setFirmaHash(rs.getString("firma_hash"));
        aceptacion.setAlternativasOfrecidas(rs.getString("alternativas_ofrecidas"));
        aceptacion.setRiesgosInformados(rs.getString("riesgos_informados"));
        aceptacion.setRegistradoPor(rs.getObject("registrado_por", Integer.class));
        return aceptacion;
    }

    private Consentimiento leerConsentimiento(ResultSet rs) throws SQLException {
        Consentimiento consentimiento = new Consentimiento();
        consentimiento.setId(rs.getLong("id"));
        consentimiento.setPacienteId(rs.getObject("paciente_id", Integer.class));
        consentimiento.setPresupuestoItemId(rs.getObject("presupuesto_item_id", Long.class));
        consentimiento.setTratamientoId(rs.getObject("tratamiento_id", Integer.class));
        consentimiento.setProcedimiento(rs.getString("procedimiento"));
        consentimiento.setRiesgos(rs.getString("riesgos"));
        consentimiento.setAlternativas(rs.getString("alternativas"));
        consentimiento.setEstado(rs.getString("estado"));
        consentimiento.setFirmanteNombre(rs.getString("firmante_nombre"));
        consentimiento.setFirmanteDocumento(rs.getString("firmante_documento"));
        consentimiento.setRepresentanteVinculo(rs.getString("representante_vinculo"));
        consentimiento.setProfesionalId(rs.getObject("profesional_id", Integer.class));
        consentimiento.setFechaFirma(rs.getObject("fecha_firma", LocalDateTime.class));
        consentimiento.setFirmaHash(rs.getString("firma_hash"));
        consentimiento.setMotivoRechazo(rs.getString("motivo_rechazo"));
        return consentimiento;
    }

    private List<Presupuesto> listar(String sql, Object[] parametros) throws DentrixException {
        List<Presupuesto> presupuestos = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    presupuestos.add(leer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar los presupuestos.", e);
        }
        return presupuestos;
    }

    @Override
    public Long insertar(Presupuesto presupuesto) throws DentrixException {
        String sql = "INSERT INTO presupuesto (paciente_id, ficha_id, presupuesto_anterior_id, version, "
                + "estado, moneda, fecha_emision, vigencia_dias, subtotal, descuento, descuento_motivo, "
                + "descuento_autorizado_por, motivo_rechazo, creado_por) "
                + "VALUES (?, ?, ?, ?, ?::estado_presupuesto, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, presupuesto.getPacienteId());
            ps.setObject(2, presupuesto.getFichaId());
            ps.setObject(3, presupuesto.getPresupuestoAnteriorId());
            ps.setObject(4, presupuesto.getVersion() == null ? 1 : presupuesto.getVersion());
            ps.setString(5, presupuesto.getEstado() == null ? "BORRADOR" : presupuesto.getEstado());
            ps.setString(6, presupuesto.getMoneda() == null ? "PEN" : presupuesto.getMoneda());
            ps.setObject(7, presupuesto.getFechaEmision() == null
                    ? LocalDateTime.now() : presupuesto.getFechaEmision());
            ps.setObject(8, presupuesto.getVigenciaDias() == null ? 15 : presupuesto.getVigenciaDias());
            ps.setObject(9, presupuesto.getSubtotal() == null ? BigDecimal.ZERO : presupuesto.getSubtotal());
            ps.setObject(10, presupuesto.getDescuento() == null ? BigDecimal.ZERO : presupuesto.getDescuento());
            ps.setObject(11, presupuesto.getDescuentoMotivo());
            ps.setObject(12, presupuesto.getDescuentoAutorizadoPor());
            ps.setObject(13, presupuesto.getMotivoRechazo());
            ps.setObject(14, presupuesto.getCreadoPor());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    presupuesto.setId(id);
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador del presupuesto.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el presupuesto.", e);
        }
    }

    @Override
    public Optional<Presupuesto> buscarPorId(Long id) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + DESDE + " WHERE p.id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar el presupuesto.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Presupuesto> listarPorPaciente(Integer pacienteId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + DESDE
                + " WHERE p.paciente_id = ? ORDER BY p.fecha_emision DESC, p.id DESC";
        return listar(sql, new Object[]{pacienteId});
    }

    @Override
    public List<Presupuesto> listarPorEstado(String estado) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + DESDE
                + " WHERE p.estado = ?::estado_presupuesto ORDER BY p.fecha_emision DESC, p.id DESC";
        return listar(sql, new Object[]{estado});
    }

    @Override
    public List<Presupuesto> listarRecientes(int limite) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + DESDE
                + " ORDER BY p.fecha_emision DESC, p.id DESC LIMIT ?";
        return listar(sql, new Object[]{limite});
    }

    @Override
    public boolean actualizarEstado(Long id, String estado, String motivoRechazo) throws DentrixException {
        String sql = "UPDATE presupuesto SET estado = ?::estado_presupuesto, motivo_rechazo = ? WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setObject(2, "RECHAZADO".equals(estado) ? motivoRechazo : null);
            ps.setObject(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar el estado del presupuesto.", e);
        }
    }

    @Override
    public boolean actualizarDescuento(Long id, BigDecimal descuento, String motivo, Integer autorizadorId)
            throws DentrixException {
        String sql = "UPDATE presupuesto SET descuento = ?, descuento_motivo = ?, "
                + "descuento_autorizado_por = ? WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, descuento);
            ps.setObject(2, motivo);
            ps.setObject(3, autorizadorId);
            ps.setObject(4, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo aplicar el descuento al presupuesto.", e);
        }
    }

    @Override
    public Long insertarItem(PresupuestoItem item) throws DentrixException {
        String sql = "INSERT INTO presupuesto_item (presupuesto_id, tratamiento_id, pieza_fdi, fase, "
                + "prioridad, secuencia, cantidad, sesiones_planificadas, precio_unitario, estado, "
                + "requiere_consentimiento, motivo_rechazo, enmienda_id) "
                + "VALUES (?, ?, ?, ?::fase_tratamiento, ?, ?, ?, ?, ?, ?::estado_item, ?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, item.getPresupuestoId());
            ps.setObject(2, item.getTratamientoId());
            ps.setObject(3, item.getPiezaFdi());
            ps.setString(4, item.getFase() == null ? "BASICA" : item.getFase());
            ps.setObject(5, item.getPrioridad() == null ? 3 : item.getPrioridad());
            ps.setObject(6, item.getSecuencia() == null ? 1 : item.getSecuencia());
            ps.setObject(7, item.getCantidad() == null ? 1 : item.getCantidad());
            ps.setObject(8, item.getSesionesPlanificadas() == null ? 1 : item.getSesionesPlanificadas());
            ps.setObject(9, item.getPrecioUnitario() == null ? BigDecimal.ZERO : item.getPrecioUnitario());
            ps.setString(10, item.getEstado() == null ? "PROPUESTO" : item.getEstado());
            ps.setObject(11, Boolean.TRUE.equals(item.getRequiereConsentimiento()));
            ps.setObject(12, item.getMotivoRechazo());
            ps.setObject(13, item.getEnmiendaId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    item.setId(id);
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador del item del presupuesto.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el item del presupuesto.", e);
        }
    }

    @Override
    public Optional<PresupuestoItem> buscarItem(Long id) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_ITEM + DESDE_ITEM + " WHERE i.id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leerItem(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar el item del presupuesto.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<PresupuestoItem> listarItems(Long presupuestoId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_ITEM + DESDE_ITEM
                + " WHERE i.presupuesto_id = ? ORDER BY i.fase, i.secuencia, i.id";
        List<PresupuestoItem> items = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, presupuestoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(leerItem(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar los items del presupuesto.", e);
        }
        return items;
    }

    @Override
    public boolean actualizarEstadoItem(Long itemId, String estado, String motivoRechazo)
            throws DentrixException {
        String sql = "UPDATE presupuesto_item SET estado = ?::estado_item, motivo_rechazo = ? WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setObject(2, "RECHAZADO".equals(estado) ? motivoRechazo : null);
            ps.setObject(3, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar el estado del item del presupuesto.", e);
        }
    }

    @Override
    public boolean eliminarItem(Long itemId) throws DentrixException {
        String sql = "DELETE FROM presupuesto_item WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo quitar el item del presupuesto.", e);
        }
    }

    @Override
    public BigDecimal totalAceptado(Long presupuestoId) throws DentrixException {
        String sql = "SELECT COALESCE(SUM(subtotal), 0) FROM presupuesto_item WHERE presupuesto_id = ? "
                + "AND estado IN ('ACEPTADO', 'EN_EJECUCION', 'EJECUTADO', 'CONTROLADO')";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, presupuestoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo calcular el total aceptado del presupuesto.", e);
        }
    }

    @Override
    public boolean estaFacturado(Long presupuestoItemId) throws DentrixException {
        String sql = "SELECT 1 FROM plan_tratamiento WHERE presupuesto_item_id = ? "
                + "UNION ALL "
                + "SELECT 1 FROM comprobante_item ci JOIN pago p ON p.id = ci.pago_id "
                + "WHERE ci.presupuesto_item_id = ? AND p.estado = 'EMITIDO' "
                + "LIMIT 1";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, presupuestoItemId);
            ps.setObject(2, presupuestoItemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo verificar si el item ya fue facturado.", e);
        }
    }

    @Override
    public BigDecimal imputadoDeItem(Long presupuestoItemId) throws DentrixException {
        String sql = "SELECT COALESCE(SUM(monto), 0) FROM imputacion WHERE presupuesto_item_id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, presupuestoItemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo calcular lo imputado al item del presupuesto.", e);
        }
    }

    @Override
    public void aplicarAceptacion(Long presupuestoId, List<Long> itemsDiferidos, Aceptacion aceptacion)
            throws DentrixException {
        try (Connection cn = ConexionBD.abrir()) {
            cn.setAutoCommit(false);
            try {
                if (itemsDiferidos != null && !itemsDiferidos.isEmpty()) {
                    String sqlDiferir = "UPDATE presupuesto_item SET estado = 'DIFERIDO'::estado_item "
                            + "WHERE id = ? AND presupuesto_id = ?";
                    try (PreparedStatement ps = cn.prepareStatement(sqlDiferir)) {
                        for (Long itemId : itemsDiferidos) {
                            ps.setObject(1, itemId);
                            ps.setObject(2, presupuestoId);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
                String sqlAceptacion = "INSERT INTO aceptacion (presupuesto_id, tipo, firmante_nombre, "
                        + "firmante_documento, firmante_rol, fecha, firma_hash, alternativas_ofrecidas, "
                        + "riesgos_informados, registrado_por) "
                        + "VALUES (?, ?::tipo_aceptacion, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = cn.prepareStatement(sqlAceptacion,
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setObject(1, presupuestoId);
                    ps.setString(2, aceptacion.getTipo());
                    ps.setObject(3, aceptacion.getFirmanteNombre());
                    ps.setObject(4, aceptacion.getFirmanteDocumento());
                    ps.setObject(5, aceptacion.getFirmanteRol());
                    ps.setObject(6, aceptacion.getFecha() == null
                            ? LocalDateTime.now() : aceptacion.getFecha());
                    ps.setObject(7, aceptacion.getFirmaHash());
                    ps.setObject(8, aceptacion.getAlternativasOfrecidas());
                    ps.setObject(9, aceptacion.getRiesgosInformados());
                    ps.setObject(10, aceptacion.getRegistradoPor());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            aceptacion.setId(rs.getLong(1));
                        }
                    }
                }
                String sqlPresupuesto = "UPDATE presupuesto SET estado = 'ACEPTADO'::estado_presupuesto "
                        + "WHERE id = ?";
                try (PreparedStatement ps = cn.prepareStatement(sqlPresupuesto)) {
                    ps.setObject(1, presupuestoId);
                    ps.executeUpdate();
                }
                cn.commit();
            } catch (SQLException e) {
                cn.rollback();
                throw new DentrixException("No se pudo aplicar la aceptacion del presupuesto.", e);
            } finally {
                cn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo aplicar la aceptacion del presupuesto.", e);
        }
    }

    @Override
    public void aplicarRechazo(Long presupuestoId, String motivo) throws DentrixException {
        try (Connection cn = ConexionBD.abrir()) {
            cn.setAutoCommit(false);
            try {
                String sqlItems = "UPDATE presupuesto_item SET estado = 'RECHAZADO'::estado_item, "
                        + "motivo_rechazo = ? WHERE presupuesto_id = ? AND estado = 'PROPUESTO'::estado_item";
                try (PreparedStatement ps = cn.prepareStatement(sqlItems)) {
                    ps.setObject(1, motivo);
                    ps.setObject(2, presupuestoId);
                    ps.executeUpdate();
                }
                String sqlPresupuesto = "UPDATE presupuesto SET estado = 'RECHAZADO'::estado_presupuesto, "
                        + "motivo_rechazo = ? WHERE id = ?";
                try (PreparedStatement ps = cn.prepareStatement(sqlPresupuesto)) {
                    ps.setObject(1, motivo);
                    ps.setObject(2, presupuestoId);
                    ps.executeUpdate();
                }
                cn.commit();
            } catch (SQLException e) {
                cn.rollback();
                throw new DentrixException("No se pudo aplicar el rechazo del presupuesto.", e);
            } finally {
                cn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo aplicar el rechazo del presupuesto.", e);
        }
    }

    @Override
    public Optional<Aceptacion> buscarAceptacion(Long presupuestoId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_ACEPTACION + " FROM aceptacion WHERE presupuesto_id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, presupuestoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leerAceptacion(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar la aceptacion del presupuesto.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Consentimiento> listarConsentimientos(Long presupuestoId) throws DentrixException {
        String sql = "SELECT c.id, c.paciente_id, c.presupuesto_item_id, c.tratamiento_id, c.procedimiento, "
                + "c.riesgos, c.alternativas, c.estado, c.firmante_nombre, c.firmante_documento, "
                + "c.representante_vinculo, c.profesional_id, c.fecha_firma, c.firma_hash, c.motivo_rechazo "
                + "FROM consentimiento c "
                + "JOIN presupuesto_item i ON i.id = c.presupuesto_item_id "
                + "WHERE i.presupuesto_id = ? ORDER BY c.id";
        List<Consentimiento> consentimientos = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, presupuestoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    consentimientos.add(leerConsentimiento(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar los consentimientos del presupuesto.", e);
        }
        return consentimientos;
    }

    @Override
    public Long insertarConsentimiento(Consentimiento consentimiento) throws DentrixException {
        String sql = "INSERT INTO consentimiento (paciente_id, presupuesto_item_id, tratamiento_id, "
                + "procedimiento, riesgos, alternativas, estado, firmante_nombre, firmante_documento, "
                + "representante_vinculo, profesional_id, fecha_firma, firma_hash, motivo_rechazo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?::estado_consentimiento, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, consentimiento.getPacienteId());
            ps.setObject(2, consentimiento.getPresupuestoItemId());
            ps.setObject(3, consentimiento.getTratamientoId());
            ps.setObject(4, consentimiento.getProcedimiento());
            ps.setObject(5, consentimiento.getRiesgos());
            ps.setObject(6, consentimiento.getAlternativas());
            ps.setString(7, consentimiento.getEstado() == null ? "PENDIENTE" : consentimiento.getEstado());
            ps.setObject(8, consentimiento.getFirmanteNombre());
            ps.setObject(9, consentimiento.getFirmanteDocumento());
            ps.setObject(10, consentimiento.getRepresentanteVinculo());
            ps.setObject(11, consentimiento.getProfesionalId());
            ps.setObject(12, consentimiento.getFechaFirma());
            ps.setObject(13, consentimiento.getFirmaHash());
            ps.setObject(14, consentimiento.getMotivoRechazo());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    consentimiento.setId(id);
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador del consentimiento.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el consentimiento.", e);
        }
    }

    @Override
    public boolean existeConsentimientoFirmado(Long presupuestoItemId) throws DentrixException {
        String sql = "SELECT 1 FROM consentimiento WHERE presupuesto_item_id = ? AND estado = 'FIRMADO'";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, presupuestoItemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo verificar el consentimiento del item.", e);
        }
    }

    @Override
    public boolean eliminar(Long presupuestoId) throws DentrixException {
        String sql = "DELETE FROM presupuesto WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, presupuestoId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo descartar el presupuesto.", e);
        }
    }
}
