package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.CierreCajaDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.CierreCaja;

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

public class CierreCajaDAOImpl implements CierreCajaDAO {

    private static final String COLUMNAS =
            "id, fecha, turno, cajero_id, fondo_inicial, historial_fondos, efectivo_sistema, "
                    + "billeteras_sistema, total_sistema, efectivo_contado, diferencia, estado, "
                    + "consolidado_por, consolidado_en, acta_generada";

    private CierreCaja leer(ResultSet rs) throws SQLException {
        CierreCaja cierre = new CierreCaja();
        cierre.setId(rs.getLong("id"));
        cierre.setFecha(rs.getObject("fecha", LocalDate.class));
        cierre.setTurno(rs.getString("turno"));
        cierre.setCajeroId(rs.getInt("cajero_id"));
        cierre.setFondoInicial(rs.getBigDecimal("fondo_inicial"));
        cierre.setHistorialFondos(rs.getString("historial_fondos"));
        cierre.setEfectivoSistema(rs.getBigDecimal("efectivo_sistema"));
        cierre.setBilleterasSistema(rs.getBigDecimal("billeteras_sistema"));
        cierre.setTotalSistema(rs.getBigDecimal("total_sistema"));
        cierre.setEfectivoContado(rs.getObject("efectivo_contado", BigDecimal.class));
        cierre.setDiferencia(rs.getObject("diferencia", BigDecimal.class));
        cierre.setEstado(rs.getString("estado"));
        cierre.setConsolidadoPor(rs.getObject("consolidado_por", Integer.class));
        cierre.setConsolidadoEn(rs.getObject("consolidado_en", LocalDateTime.class));
        cierre.setActaGenerada(rs.getBoolean("acta_generada"));
        return cierre;
    }

    private List<CierreCaja> listar(String sql, Object[] parametros) throws DentrixException {
        List<CierreCaja> cierres = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cierres.add(leer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar los cierres de caja.", e);
        }
        return cierres;
    }

    @Override
    public List<CierreCaja> listarTodos() throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM cierre_caja ORDER BY fecha DESC, turno, id";
        return listar(sql, new Object[0]);
    }

    @Override
    public Optional<CierreCaja> buscarPorFecha(LocalDate fecha) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM cierre_caja WHERE fecha = ? ORDER BY id";
        List<CierreCaja> cierres = listar(sql, new Object[]{fecha});
        return cierres.isEmpty() ? Optional.empty() : Optional.of(cierres.get(0));
    }

    @Override
    public Optional<CierreCaja> buscarPorFechaYTurno(LocalDate fecha, String turno) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM cierre_caja WHERE fecha = ? AND turno = ? ORDER BY id";
        List<CierreCaja> cierres = listar(sql, new Object[]{fecha, turno});
        return cierres.isEmpty() ? Optional.empty() : Optional.of(cierres.get(0));
    }

    @Override
    public int insertar(CierreCaja cierre) throws DentrixException {
        String sql = "INSERT INTO cierre_caja (fecha, turno, cajero_id, fondo_inicial, historial_fondos, "
                + "efectivo_sistema, billeteras_sistema, total_sistema, efectivo_contado, estado) "
                + "VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?::estado_cierre)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, cierre.getFecha());
            ps.setObject(2, cierre.getTurno());
            ps.setObject(3, cierre.getCajeroId());
            ps.setObject(4, cierre.getFondoInicial() == null ? BigDecimal.ZERO : cierre.getFondoInicial());
            ps.setObject(5, cierre.getHistorialFondos() == null ? "[]" : cierre.getHistorialFondos());
            ps.setObject(6, cierre.getEfectivoSistema() == null ? BigDecimal.ZERO : cierre.getEfectivoSistema());
            ps.setObject(7, cierre.getBilleterasSistema() == null ? BigDecimal.ZERO : cierre.getBilleterasSistema());
            ps.setObject(8, cierre.getTotalSistema() == null ? BigDecimal.ZERO : cierre.getTotalSistema());
            ps.setObject(9, cierre.getEfectivoContado());
            ps.setString(10, cierre.getEstado() == null ? "ABIERTO" : cierre.getEstado());
            int filas = ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    cierre.setId(rs.getLong(1));
                }
            }
            return filas;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo abrir el cierre de caja.", e);
        }
    }

    @Override
    public boolean actualizar(CierreCaja cierre) throws DentrixException {
        String sql = "UPDATE cierre_caja SET fecha = ?, turno = ?, cajero_id = ?, fondo_inicial = ?, "
                + "historial_fondos = ?::jsonb, efectivo_sistema = ?, billeteras_sistema = ?, "
                + "total_sistema = ?, efectivo_contado = ?, estado = ?::estado_cierre, consolidado_por = ?, "
                + "consolidado_en = ?, acta_generada = ? WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, cierre.getFecha());
            ps.setObject(2, cierre.getTurno());
            ps.setObject(3, cierre.getCajeroId());
            ps.setObject(4, cierre.getFondoInicial() == null ? BigDecimal.ZERO : cierre.getFondoInicial());
            ps.setObject(5, cierre.getHistorialFondos() == null ? "[]" : cierre.getHistorialFondos());
            ps.setObject(6, cierre.getEfectivoSistema() == null ? BigDecimal.ZERO : cierre.getEfectivoSistema());
            ps.setObject(7, cierre.getBilleterasSistema() == null ? BigDecimal.ZERO : cierre.getBilleterasSistema());
            ps.setObject(8, cierre.getTotalSistema() == null ? BigDecimal.ZERO : cierre.getTotalSistema());
            ps.setObject(9, cierre.getEfectivoContado());
            ps.setString(10, cierre.getEstado() == null ? "ABIERTO" : cierre.getEstado());
            ps.setObject(11, cierre.getConsolidadoPor());
            ps.setObject(12, cierre.getConsolidadoEn());
            ps.setBoolean(13, cierre.isActaGenerada());
            ps.setObject(14, cierre.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar el cierre de caja.", e);
        }
    }
}
