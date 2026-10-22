package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.PagoDAO;
import pe.edu.utp.dentrix.dto.Imputacion;
import pe.edu.utp.dentrix.dto.ItemFacturado;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Pago;

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

public class PagoDAOImpl implements PagoDAO {

    private static final String COLUMNAS =
            "id, numero, tipo, estado, pago_origen_id, fecha, paciente_id, ficha_id, odontologo_id, "
                    + "usuario_caja_id, cierre_id, modalidad, operacion_ref, subtotal, igv, monto, "
                    + "saldo_deudor, estado_pago";

    private Pago leer(ResultSet rs) throws SQLException {
        Pago pago = new Pago();
        pago.setId(rs.getLong("id"));
        pago.setNumero(rs.getString("numero"));
        pago.setTipo(rs.getString("tipo"));
        pago.setEstado(rs.getString("estado"));
        pago.setPagoOrigenId(rs.getObject("pago_origen_id", Long.class));
        pago.setFecha(rs.getObject("fecha", LocalDateTime.class));
        pago.setPacienteId(rs.getInt("paciente_id"));
        pago.setFichaId(rs.getObject("ficha_id", Long.class));
        pago.setOdontologoId(rs.getObject("odontologo_id", Integer.class));
        pago.setUsuarioCajaId(rs.getInt("usuario_caja_id"));
        pago.setCierreId(rs.getObject("cierre_id", Long.class));
        pago.setModalidad(rs.getString("modalidad"));
        pago.setOperacionRef(rs.getString("operacion_ref"));
        pago.setSubtotal(rs.getBigDecimal("subtotal"));
        pago.setIgv(rs.getBigDecimal("igv"));
        pago.setMonto(rs.getBigDecimal("monto"));
        pago.setSaldoDeudor(rs.getBigDecimal("saldo_deudor"));
        pago.setEstadoPago(rs.getString("estado_pago"));
        return pago;
    }

    private List<Pago> listar(String sql, Object[] parametros) throws DentrixException {
        List<Pago> pagos = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pagos.add(leer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar los pagos.", e);
        }
        return pagos;
    }

    @Override
    public Optional<Pago> buscarPorId(Long id) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM pago WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar el pago.", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Pago> buscarPorNumero(String numero) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM pago WHERE numero = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar el pago por numero.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Pago> listarPorPaciente(Integer pacienteId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM pago WHERE paciente_id = ? ORDER BY fecha, id";
        return listar(sql, new Object[]{pacienteId});
    }

    @Override
    public List<Pago> listarPorFicha(Long fichaId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM pago WHERE ficha_id = ? ORDER BY fecha, id";
        return listar(sql, new Object[]{fichaId});
    }

    @Override
    public List<Pago> listarPorCierre(Long cierreId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM pago WHERE cierre_id = ? ORDER BY fecha, id";
        return listar(sql, new Object[]{cierreId});
    }

    @Override
    public List<Pago> listarAbonos(Long documentoId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM pago WHERE pago_origen_id = ? ORDER BY fecha, id";
        return listar(sql, new Object[]{documentoId});
    }

    @Override
    public List<Pago> listarDocumentosConSaldo() throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM pago "
                + "WHERE pago_origen_id IS NULL AND estado = 'EMITIDO' AND saldo_deudor > 0 "
                + "ORDER BY fecha, id";
        return listar(sql, new Object[0]);
    }

    @Override
    public List<Pago> listarBoletasPorFecha(LocalDate fecha) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM pago "
                + "WHERE pago_origen_id IS NULL AND estado = 'EMITIDO' AND fecha >= ? AND fecha < ? "
                + "ORDER BY fecha, id";
        return listar(sql, new Object[]{fecha, fecha.plusDays(1)});
    }

    @Override
    public int insertar(Pago pago) throws DentrixException {
        try (Connection cn = ConexionBD.abrir()) {
            return (int) insertar(cn, pago);
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el pago.", e);
        }
    }

    @Override
    public Long insertarConImputaciones(Pago pago, List<Imputacion> imputaciones, Integer usuarioId)
            throws DentrixException {
        try (Connection cn = ConexionBD.abrir()) {
            cn.setAutoCommit(false);
            try {
                long pagoId = insertar(cn, pago);
                if (imputaciones != null && !imputaciones.isEmpty()) {
                    String sql = "INSERT INTO imputacion (pago_id, presupuesto_item_id, monto, usuario_id) "
                            + "VALUES (?, ?, ?, ?)";
                    try (PreparedStatement ps = cn.prepareStatement(sql)) {
                        for (Imputacion imputacion : imputaciones) {
                            ps.setObject(1, pagoId);
                            ps.setObject(2, imputacion.presupuestoItemId());
                            ps.setObject(3, imputacion.monto());
                            ps.setObject(4, usuarioId);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
                cn.commit();
                return pagoId;
            } catch (SQLException e) {
                cn.rollback();
                throw new DentrixException("No se pudo registrar el abono con sus imputaciones.", e);
            } catch (DentrixException e) {
                cn.rollback();
                throw e;
            } finally {
                cn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el abono con sus imputaciones.", e);
        }
    }

    @Override
    public List<ItemFacturado> listarItemsFacturados(Long pagoId) throws DentrixException {
        String sql = "SELECT presupuesto_item_id, SUM(monto) AS monto FROM ("
                + "SELECT presupuesto_item_id, precio_unitario * cantidad AS monto "
                + "FROM plan_tratamiento WHERE pago_id = ? AND presupuesto_item_id IS NOT NULL "
                + "UNION ALL "
                + "SELECT ci.presupuesto_item_id, ci.subtotal AS monto "
                + "FROM comprobante_item ci JOIN pago p ON p.id = ci.pago_id "
                + "WHERE ci.pago_id = ? AND ci.presupuesto_item_id IS NOT NULL AND p.estado = 'EMITIDO'"
                + ") x GROUP BY presupuesto_item_id ORDER BY presupuesto_item_id";
        List<ItemFacturado> items = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, pagoId);
            ps.setObject(2, pagoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new ItemFacturado(rs.getLong("presupuesto_item_id"), rs.getBigDecimal("monto")));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar los items facturados de la boleta.", e);
        }
        return items;
    }

    private long insertar(Connection cn, Pago pago) throws SQLException, DentrixException {
        String sql = "INSERT INTO pago (numero, tipo, estado, pago_origen_id, fecha, paciente_id, ficha_id, "
                + "odontologo_id, usuario_caja_id, cierre_id, modalidad, operacion_ref, subtotal, igv, monto, "
                + "saldo_deudor, estado_pago) "
                + "VALUES (?, ?::tipo_documento, ?::estado_documento, ?, ?, ?, ?, ?, ?, ?, "
                + "?::modalidad_pago, ?, ?, ?, ?, ?, ?::estado_pago)";
        try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, pago.getNumero());
            ps.setString(2, pago.getTipo() == null ? "BOLETA" : pago.getTipo());
            ps.setString(3, pago.getEstado() == null ? "EMITIDO" : pago.getEstado());
            ps.setObject(4, pago.getPagoOrigenId());
            ps.setObject(5, pago.getFecha() == null ? LocalDateTime.now() : pago.getFecha());
            ps.setObject(6, pago.getPacienteId());
            ps.setObject(7, pago.getFichaId());
            ps.setObject(8, pago.getOdontologoId());
            ps.setObject(9, pago.getUsuarioCajaId());
            ps.setObject(10, pago.getCierreId());
            ps.setString(11, pago.getModalidad() == null ? "EFECTIVO" : pago.getModalidad());
            ps.setObject(12, pago.getOperacionRef());
            ps.setObject(13, pago.getSubtotal() == null ? BigDecimal.ZERO : pago.getSubtotal());
            ps.setObject(14, pago.getIgv() == null ? BigDecimal.ZERO : pago.getIgv());
            ps.setObject(15, pago.getMonto() == null ? BigDecimal.ZERO : pago.getMonto());
            ps.setObject(16, pago.getSaldoDeudor() == null ? BigDecimal.ZERO : pago.getSaldoDeudor());
            ps.setString(17, pago.getEstadoPago() == null ? "PENDIENTE" : pago.getEstadoPago());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    pago.setId(rs.getLong(1));
                    return rs.getLong(1);
                }
            }
            throw new DentrixException("No se generó el identificador del pago.");
        }
    }

    @Override
    public boolean actualizar(Pago pago) throws DentrixException {
        String sql = "UPDATE pago SET numero = ?, tipo = ?::tipo_documento, estado = ?::estado_documento, "
                + "pago_origen_id = ?, fecha = ?, paciente_id = ?, ficha_id = ?, odontologo_id = ?, "
                + "usuario_caja_id = ?, cierre_id = ?, modalidad = ?::modalidad_pago, operacion_ref = ?, "
                + "subtotal = ?, igv = ?, monto = ?, saldo_deudor = ?, estado_pago = ?::estado_pago "
                + "WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, pago.getNumero());
            ps.setString(2, pago.getTipo() == null ? "BOLETA" : pago.getTipo());
            ps.setString(3, pago.getEstado() == null ? "EMITIDO" : pago.getEstado());
            ps.setObject(4, pago.getPagoOrigenId());
            ps.setObject(5, pago.getFecha());
            ps.setObject(6, pago.getPacienteId());
            ps.setObject(7, pago.getFichaId());
            ps.setObject(8, pago.getOdontologoId());
            ps.setObject(9, pago.getUsuarioCajaId());
            ps.setObject(10, pago.getCierreId());
            ps.setString(11, pago.getModalidad() == null ? "EFECTIVO" : pago.getModalidad());
            ps.setObject(12, pago.getOperacionRef());
            ps.setObject(13, pago.getSubtotal() == null ? BigDecimal.ZERO : pago.getSubtotal());
            ps.setObject(14, pago.getIgv() == null ? BigDecimal.ZERO : pago.getIgv());
            ps.setObject(15, pago.getMonto() == null ? BigDecimal.ZERO : pago.getMonto());
            ps.setObject(16, pago.getSaldoDeudor() == null ? BigDecimal.ZERO : pago.getSaldoDeudor());
            ps.setString(17, pago.getEstadoPago() == null ? "PENDIENTE" : pago.getEstadoPago());
            ps.setObject(18, pago.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar el pago.", e);
        }
    }
}
