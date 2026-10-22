package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.ComprobanteDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.ComprobanteItem;
import pe.edu.utp.dentrix.model.Pago;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ComprobanteDAOImpl implements ComprobanteDAO {

    private static final String COLUMNAS_ITEM =
            "id, pago_id, presupuesto_item_id, descripcion, cantidad, precio_unitario, subtotal, afectacion_igv";

    private ComprobanteItem leer(ResultSet rs) throws SQLException {
        ComprobanteItem item = new ComprobanteItem();
        item.setId(rs.getLong("id"));
        item.setPagoId(rs.getObject("pago_id", Long.class));
        item.setPresupuestoItemId(rs.getObject("presupuesto_item_id", Long.class));
        item.setDescripcion(rs.getString("descripcion"));
        item.setCantidad(rs.getBigDecimal("cantidad"));
        item.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        item.setSubtotal(rs.getBigDecimal("subtotal"));
        item.setAfectacionIgv(rs.getString("afectacion_igv"));
        return item;
    }

    @Override
    public Long insertarItem(ComprobanteItem item) throws DentrixException {
        String sql = "INSERT INTO comprobante_item (pago_id, presupuesto_item_id, descripcion, cantidad, "
                + "precio_unitario, afectacion_igv) VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, item.getPagoId());
            ps.setObject(2, item.getPresupuestoItemId());
            ps.setString(3, item.getDescripcion());
            ps.setObject(4, item.getCantidad());
            ps.setObject(5, item.getPrecioUnitario());
            ps.setString(6, item.getAfectacionIgv() == null ? "10" : item.getAfectacionIgv());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    item.setId(id);
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador del item del comprobante.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el item del comprobante.", e);
        }
    }

    @Override
    public List<ComprobanteItem> listarItems(Long pagoId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_ITEM + " FROM comprobante_item WHERE pago_id = ? ORDER BY id";
        List<ComprobanteItem> items = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, pagoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(leer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar los items del comprobante.", e);
        }
        return items;
    }

    @Override
    public boolean eliminarItem(Long id) throws DentrixException {
        String sql = "DELETE FROM comprobante_item WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo eliminar el item del comprobante.", e);
        }
    }

    @Override
    public Long crearBorrador(Pago pago) throws DentrixException {
        String sql = "INSERT INTO pago (numero, tipo, estado, pago_origen_id, fecha, paciente_id, ficha_id, "
                + "odontologo_id, usuario_caja_id, cierre_id, modalidad, operacion_ref, subtotal, igv, monto, "
                + "saldo_deudor, estado_pago) "
                + "VALUES (NULL, ?::tipo_documento, ?::estado_documento, ?, ?, ?, ?, ?, ?, ?, "
                + "?::modalidad_pago, ?, ?, ?, ?, ?, ?::estado_pago) RETURNING id";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, pago.getTipo() == null ? "BOLETA" : pago.getTipo());
            ps.setString(2, "BORRADOR");
            ps.setObject(3, pago.getPagoOrigenId());
            ps.setObject(4, pago.getFecha() == null ? LocalDateTime.now() : pago.getFecha());
            ps.setObject(5, pago.getPacienteId());
            ps.setObject(6, pago.getFichaId());
            ps.setObject(7, pago.getOdontologoId());
            ps.setObject(8, pago.getUsuarioCajaId());
            ps.setObject(9, pago.getCierreId());
            ps.setString(10, pago.getModalidad() == null ? "EFECTIVO" : pago.getModalidad());
            ps.setObject(11, pago.getOperacionRef());
            ps.setObject(12, pago.getSubtotal() == null ? BigDecimal.ZERO : pago.getSubtotal());
            ps.setObject(13, pago.getIgv() == null ? BigDecimal.ZERO : pago.getIgv());
            ps.setObject(14, pago.getMonto() == null ? BigDecimal.ZERO : pago.getMonto());
            ps.setObject(15, pago.getSaldoDeudor() == null ? BigDecimal.ZERO : pago.getSaldoDeudor());
            ps.setString(16, pago.getEstadoPago() == null ? "PENDIENTE" : pago.getEstadoPago());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    pago.setId(id);
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador del borrador.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo crear el borrador del comprobante.", e);
        }
    }

    @Override
    public String siguienteNumero(String serie) throws DentrixException {
        String base = (serie == null || serie.isBlank()) ? "B001" : serie.trim();
        if (base.endsWith("-")) {
            base = base.substring(0, base.length() - 1);
        }
        String prefijo = base + "-";
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(numero FROM ?) AS BIGINT)), 0) FROM pago "
                + "WHERE pago_origen_id IS NULL AND starts_with(numero, ?) "
                + "AND SUBSTRING(numero FROM ?) ~ '^[0-9]+$'";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            int inicio = prefijo.length() + 1;
            ps.setInt(1, inicio);
            ps.setString(2, prefijo);
            ps.setInt(3, inicio);
            try (ResultSet rs = ps.executeQuery()) {
                long maximo = rs.next() ? rs.getLong(1) : 0L;
                return prefijo + String.format(Locale.ROOT, "%06d", maximo + 1);
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo calcular el siguiente numero del comprobante.", e);
        }
    }

    @Override
    public void anular(Long pagoId, String motivo, Integer usuarioId) throws DentrixException {
        String sql = "UPDATE pago SET estado = 'ANULADO'::estado_documento, motivo_anulacion = ?, "
                + "anulada_por = ?, fecha_anulacion = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, motivo);
            ps.setObject(2, usuarioId);
            ps.setObject(3, pagoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DentrixException("No se pudo anular el comprobante.", e);
        }
    }
}
