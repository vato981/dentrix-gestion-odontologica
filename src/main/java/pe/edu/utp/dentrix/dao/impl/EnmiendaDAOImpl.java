package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.EnmiendaDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Enmienda;
import pe.edu.utp.dentrix.model.EnmiendaItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EnmiendaDAOImpl implements EnmiendaDAO {

    private static final String COLUMNAS =
            "e.id, e.presupuesto_id, e.numero, e.tipo, e.motivo, e.estado, e.descuento_adicional, "
                    + "e.descuento_motivo, e.descuento_autorizado_por, e.firmante_nombre, "
                    + "e.firmante_documento, e.firmante_rol, e.fecha_firma, e.firma_hash, "
                    + "e.firmado_por, e.creado_por, e.fecha_creacion";

    private static final String COLUMNAS_ITEM =
            "ei.id, ei.enmienda_id, ei.presupuesto_item_id, ei.accion, ei.motivo, "
                    + "t.nombre AS tratamiento_nombre, i.pieza_fdi, "
                    + "COALESCE(i.monto_neto, i.subtotal) AS monto";

    private static final String DESDE_ITEM =
            " FROM enmienda_item ei "
                    + "JOIN presupuesto_item i ON i.id = ei.presupuesto_item_id "
                    + "JOIN tratamiento t ON t.id = i.tratamiento_id";

    private Enmienda leer(ResultSet rs) throws SQLException {
        Enmienda enmienda = new Enmienda();
        enmienda.setId(rs.getLong("id"));
        enmienda.setPresupuestoId(rs.getObject("presupuesto_id", Long.class));
        enmienda.setNumero(rs.getObject("numero", Integer.class));
        enmienda.setTipo(rs.getString("tipo"));
        enmienda.setMotivo(rs.getString("motivo"));
        enmienda.setEstado(rs.getString("estado"));
        enmienda.setDescuentoAdicional(rs.getBigDecimal("descuento_adicional"));
        enmienda.setDescuentoMotivo(rs.getString("descuento_motivo"));
        enmienda.setDescuentoAutorizadoPor(rs.getObject("descuento_autorizado_por", Integer.class));
        enmienda.setFirmanteNombre(rs.getString("firmante_nombre"));
        enmienda.setFirmanteDocumento(rs.getString("firmante_documento"));
        enmienda.setFirmanteRol(rs.getString("firmante_rol"));
        enmienda.setFechaFirma(rs.getObject("fecha_firma", LocalDateTime.class));
        enmienda.setFirmaHash(rs.getString("firma_hash"));
        enmienda.setFirmadoPor(rs.getObject("firmado_por", Integer.class));
        enmienda.setCreadoPor(rs.getObject("creado_por", Integer.class));
        enmienda.setFechaCreacion(rs.getObject("fecha_creacion", LocalDateTime.class));
        return enmienda;
    }

    private EnmiendaItem leerItem(ResultSet rs) throws SQLException {
        EnmiendaItem item = new EnmiendaItem();
        item.setId(rs.getLong("id"));
        item.setEnmiendaId(rs.getObject("enmienda_id", Long.class));
        item.setPresupuestoItemId(rs.getObject("presupuesto_item_id", Long.class));
        item.setAccion(rs.getString("accion"));
        item.setMotivo(rs.getString("motivo"));
        item.setTratamientoNombre(rs.getString("tratamiento_nombre"));
        item.setPiezaFdi(rs.getObject("pieza_fdi", Integer.class));
        item.setMonto(rs.getBigDecimal("monto"));
        return item;
    }

    @Override
    public Long insertar(Enmienda enmienda) throws DentrixException {
        String sql = "INSERT INTO enmienda (presupuesto_id, numero, motivo, estado, descuento_adicional, "
                + "descuento_motivo, descuento_autorizado_por, creado_por) "
                + "VALUES (?, ?, ?, ?::estado_enmienda, ?, ?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, enmienda.getPresupuestoId());
            ps.setObject(2, enmienda.getNumero(), Types.INTEGER);
            ps.setObject(3, enmienda.getMotivo());
            ps.setString(4, enmienda.getEstado() == null ? "BORRADOR" : enmienda.getEstado());
            ps.setObject(5, enmienda.getDescuentoAdicional() == null
                    ? java.math.BigDecimal.ZERO : enmienda.getDescuentoAdicional());
            ps.setObject(6, enmienda.getDescuentoMotivo());
            ps.setObject(7, enmienda.getDescuentoAutorizadoPor());
            ps.setObject(8, enmienda.getCreadoPor());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    enmienda.setId(id);
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador de la enmienda.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar la enmienda.", e);
        }
    }

    @Override
    public Optional<Enmienda> buscarPorId(Long id) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM enmienda e WHERE e.id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar la enmienda.", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Enmienda> buscarBorrador(Long presupuestoId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM enmienda e "
                + "WHERE e.presupuesto_id = ? AND e.estado = 'BORRADOR'::estado_enmienda "
                + "ORDER BY e.numero DESC LIMIT 1";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, presupuestoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar el borrador de enmienda.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Enmienda> listarPorPresupuesto(Long presupuestoId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM enmienda e "
                + "WHERE e.presupuesto_id = ? ORDER BY e.numero DESC";
        List<Enmienda> enmiendas = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, presupuestoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    enmiendas.add(leer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar las enmiendas del presupuesto.", e);
        }
        return enmiendas;
    }

    @Override
    public Long insertarItem(EnmiendaItem item) throws DentrixException {
        String sql = "INSERT INTO enmienda_item (enmienda_id, presupuesto_item_id, accion, motivo) "
                + "VALUES (?, ?, ?::accion_enmienda, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, item.getEnmiendaId());
            ps.setObject(2, item.getPresupuestoItemId());
            ps.setString(3, item.getAccion());
            ps.setObject(4, item.getMotivo());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    item.setId(id);
                    return id;
                }
            }
            throw new DentrixException("No se genero el identificador del detalle de la enmienda.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el detalle de la enmienda.", e);
        }
    }

    @Override
    public List<EnmiendaItem> listarItems(Long enmiendaId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS_ITEM + DESDE_ITEM
                + " WHERE ei.enmienda_id = ? ORDER BY ei.id";
        List<EnmiendaItem> items = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, enmiendaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(leerItem(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar los items de la enmienda.", e);
        }
        return items;
    }

    @Override
    public void actualizarFirma(Long id, String nombre, String documento, String rol, String hash)
            throws DentrixException {
        String sql = "UPDATE enmienda SET firmante_nombre = ?, firmante_documento = ?, "
                + "firmante_rol = ?, firma_hash = ? WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, nombre);
            ps.setObject(2, documento);
            ps.setObject(3, rol);
            ps.setObject(4, hash);
            ps.setObject(5, id);
            if (ps.executeUpdate() == 0) {
                throw new DentrixException("La enmienda " + id + " no existe; no se pudo registrar la firma.");
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar la firma de la enmienda.", e);
        }
    }

    @Override
    public void firmar(Long enmiendaId, Integer usuarioId) throws DentrixException {
        String sql = "SELECT firmar_enmienda(?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, enmiendaId);
            ps.setObject(2, usuarioId);
            ps.execute();
        } catch (SQLException e) {
            throw new DentrixException("No se pudo firmar la enmienda.", e);
        }
    }

    @Override
    public void descartar(Long enmiendaId) throws DentrixException {
        String sql = "SELECT descartar_enmienda(?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, enmiendaId);
            ps.execute();
        } catch (SQLException e) {
            throw new DentrixException("No se pudo descartar el borrador de enmienda.", e);
        }
    }
}
