package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.PlanTratamientoDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.PlanTratamiento;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlanTratamientoDAOImpl implements PlanTratamientoDAO {

    private static final String COLUMNAS =
            "id, paciente_id, tratamiento_id, ficha_id, pago_id, presupuesto_item_id, pieza_fdi, cantidad, "
                    + "precio_unitario, subtotal, estado, fecha";

    @Override
    public List<PlanTratamiento> listarPorPaciente(Integer pacienteId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM plan_tratamiento WHERE paciente_id = ? ORDER BY fecha DESC, id DESC";
        return listar(sql, pacienteId, "No se pudo listar el plan de tratamiento del paciente");
    }

    @Override
    public List<PlanTratamiento> listarPorFicha(Long fichaId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM plan_tratamiento WHERE ficha_id = ? ORDER BY id";
        return listar(sql, fichaId, "No se pudo listar el plan de tratamiento de la ficha");
    }

    @Override
    public List<PlanTratamiento> listarPorPago(Long pagoId) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM plan_tratamiento WHERE pago_id = ? ORDER BY id";
        return listar(sql, pagoId, "No se pudo listar el plan de tratamiento del pago");
    }

    @Override
    public Optional<PlanTratamiento> buscarPorId(Long id) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM plan_tratamiento WHERE id = ?";
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
            throw new DentrixException("No se pudo buscar la linea del plan: " + e.getMessage(), e);
        }
    }

    @Override
    public int insertar(PlanTratamiento linea) throws DentrixException {
        String sql = "INSERT INTO plan_tratamiento (paciente_id, tratamiento_id, ficha_id, pago_id, "
                + "presupuesto_item_id, pieza_fdi, cantidad, precio_unitario, estado, fecha) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::estado_linea, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, linea.getPacienteId());
            ps.setObject(2, linea.getTratamientoId());
            ps.setObject(3, linea.getFichaId());
            ps.setObject(4, linea.getPagoId());
            ps.setObject(5, linea.getPresupuestoItemId());
            ps.setObject(6, linea.getPiezaFdi());
            ps.setObject(7, linea.getCantidad() == null ? 1 : linea.getCantidad());
            ps.setObject(8, linea.getPrecioUnitario() == null ? BigDecimal.ZERO : linea.getPrecioUnitario());
            ps.setString(9, linea.getEstado() == null ? "PLANIFICADO" : linea.getEstado());
            ps.setObject(10, linea.getFecha() == null ? LocalDateTime.now() : linea.getFecha());
            int filas = ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    linea.setId(rs.getLong(1));
                }
            }
            return filas;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo insertar la linea del plan: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizar(PlanTratamiento linea) throws DentrixException {
        String sql = "UPDATE plan_tratamiento SET paciente_id = ?, tratamiento_id = ?, ficha_id = ?, pago_id = ?, "
                    + "presupuesto_item_id = ?, pieza_fdi = ?, cantidad = ?, precio_unitario = ?, "
                    + "estado = ?::estado_linea, fecha = ? WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, linea.getPacienteId());
            ps.setObject(2, linea.getTratamientoId());
            ps.setObject(3, linea.getFichaId());
            ps.setObject(4, linea.getPagoId());
            ps.setObject(5, linea.getPresupuestoItemId());
            ps.setObject(6, linea.getPiezaFdi());
            ps.setObject(7, linea.getCantidad() == null ? 1 : linea.getCantidad());
            ps.setObject(8, linea.getPrecioUnitario() == null ? BigDecimal.ZERO : linea.getPrecioUnitario());
            ps.setString(9, linea.getEstado() == null ? "PLANIFICADO" : linea.getEstado());
            ps.setObject(10, linea.getFecha() == null ? LocalDateTime.now() : linea.getFecha());
            ps.setObject(11, linea.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar la linea del plan: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean eliminar(Long id) throws DentrixException {
        String sql = "DELETE FROM plan_tratamiento WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo eliminar la linea del plan: " + e.getMessage(), e);
        }
    }

    private List<PlanTratamiento> listar(String sql, Object parametro, String contexto) throws DentrixException {
        List<PlanTratamiento> lineas = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lineas.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException(contexto + ": " + e.getMessage(), e);
        }
        return lineas;
    }

    private PlanTratamiento mapear(ResultSet rs) throws SQLException {
        PlanTratamiento linea = new PlanTratamiento();
        linea.setId(rs.getLong("id"));
        linea.setPacienteId(rs.getInt("paciente_id"));
        linea.setTratamientoId(rs.getInt("tratamiento_id"));
        long fichaId = rs.getLong("ficha_id");
        linea.setFichaId(rs.wasNull() ? null : fichaId);
        long pagoId = rs.getLong("pago_id");
        linea.setPagoId(rs.wasNull() ? null : pagoId);
        long presupuestoItemId = rs.getLong("presupuesto_item_id");
        linea.setPresupuestoItemId(rs.wasNull() ? null : presupuestoItemId);
        int pieza = rs.getInt("pieza_fdi");
        linea.setPiezaFdi(rs.wasNull() ? null : pieza);
        linea.setCantidad(rs.getInt("cantidad"));
        linea.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        linea.setSubtotal(rs.getBigDecimal("subtotal"));
        linea.setEstado(rs.getString("estado"));
        linea.setFecha(rs.getObject("fecha", LocalDateTime.class));
        return linea;
    }
}
