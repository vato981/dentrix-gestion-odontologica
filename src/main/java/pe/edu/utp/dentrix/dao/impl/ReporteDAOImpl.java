package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dto.KpiDia;
import pe.edu.utp.dentrix.dto.KpiMes;
import pe.edu.utp.dentrix.dto.ProductividadOdontologo;
import pe.edu.utp.dentrix.dto.RankingTratamiento;
import pe.edu.utp.dentrix.dto.SaldoPaciente;
import pe.edu.utp.dentrix.excepcion.DentrixException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReporteDAOImpl {

    public List<SaldoPaciente> saldosPacientes() throws DentrixException {
        String sql = "SELECT paciente_id, codigo_hc, apellidos || ', ' || nombres AS nombre, "
                + "saldo_total, documentos_pendientes FROM v_saldos_paciente WHERE saldo_total > 0 "
                + "ORDER BY saldo_total DESC, paciente_id";
        List<SaldoPaciente> saldos = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                saldos.add(new SaldoPaciente(rs.getInt("paciente_id"), rs.getString("codigo_hc"),
                        rs.getString("nombre"), rs.getBigDecimal("saldo_total"),
                        rs.getInt("documentos_pendientes")));
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron calcular los saldos por cobrar.", e);
        }
        return saldos;
    }

    public List<RankingTratamiento> rankingTratamientos() throws DentrixException {
        String sql = "SELECT codigo, nombre, unidades_cobradas, monto_cobrado "
                + "FROM v_ranking_tratamientos ORDER BY monto_cobrado DESC, codigo";
        List<RankingTratamiento> ranking = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ranking.add(new RankingTratamiento(rs.getString("codigo"), rs.getString("nombre"),
                        rs.getLong("unidades_cobradas"), rs.getBigDecimal("monto_cobrado")));
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo construir el ranking de tratamientos.", e);
        }
        return ranking;
    }

    public KpiDia kpisDeFecha(LocalDate fecha) throws DentrixException {
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM cita WHERE fecha_cita = ? AND tipo = 'CITA') AS citas, "
                + "(SELECT COUNT(*) FROM cita WHERE fecha_cita = ? AND estado = 'EN_SALA') AS en_sala, "
                + "(SELECT COALESCE(SUM(monto), 0) FROM pago "
                + " WHERE fecha::date = ? AND pago_origen_id IS NULL AND estado = 'EMITIDO') AS recaudado, "
                + "(SELECT COUNT(DISTINCT paciente_id) FROM pago "
                + " WHERE pago_origen_id IS NULL AND saldo_deudor > 0 AND estado = 'EMITIDO') AS deudores";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, fecha);
            ps.setObject(2, fecha);
            ps.setObject(3, fecha);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new KpiDia(fecha, rs.getLong("citas"), rs.getLong("en_sala"),
                            rs.getBigDecimal("recaudado"), rs.getLong("deudores"));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron calcular los KPI de la fecha.", e);
        }
        throw new DentrixException("No se pudieron calcular los KPI de la fecha.");
    }

    public KpiMes kpisDelMes(LocalDate desde, LocalDate hasta) throws DentrixException {
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM cita WHERE fecha_cita BETWEEN ? AND ? AND tipo = 'CITA') AS citas, "
                + "(SELECT COUNT(*) FROM cita WHERE fecha_cita BETWEEN ? AND ? AND estado = 'ATENDIDA') AS atendidas, "
                + "(SELECT COUNT(*) FROM cita WHERE fecha_cita BETWEEN ? AND ? AND estado = 'FALTO') AS faltas, "
                + "(SELECT COALESCE(SUM(monto), 0) FROM pago "
                + " WHERE fecha::date BETWEEN ? AND ? AND pago_origen_id IS NULL AND estado = 'EMITIDO') AS recaudado, "
                + "(SELECT COALESCE(SUM(monto), 0) FROM pago "
                + " WHERE fecha::date BETWEEN ? AND ? AND pago_origen_id IS NULL AND estado = 'EMITIDO' "
                + "   AND modalidad = 'EFECTIVO') AS efectivo, "
                + "(SELECT COALESCE(SUM(monto), 0) FROM pago "
                + " WHERE fecha::date BETWEEN ? AND ? AND pago_origen_id IS NULL AND estado = 'EMITIDO' "
                + "   AND modalidad = 'YAPE') AS yape, "
                + "(SELECT COALESCE(SUM(monto), 0) FROM pago "
                + " WHERE fecha::date BETWEEN ? AND ? AND pago_origen_id IS NULL AND estado = 'EMITIDO' "
                + "   AND modalidad = 'PLIN') AS plin, "
                + "(SELECT COALESCE(SUM(monto), 0) FROM pago "
                + " WHERE fecha::date BETWEEN ? AND ? AND pago_origen_id IS NULL AND estado = 'EMITIDO' "
                + "   AND modalidad = 'TARJETA') AS tarjeta";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 1; i <= 16; i += 2) {
                ps.setObject(i, desde);
                ps.setObject(i + 1, hasta);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long citas = rs.getLong("citas");
                    long atendidas = rs.getLong("atendidas");
                    long faltas = rs.getLong("faltas");
                    long citadas = atendidas + faltas;
                    java.math.BigDecimal noShow = citadas == 0
                            ? java.math.BigDecimal.ZERO
                            : new java.math.BigDecimal(faltas)
                                    .multiply(new java.math.BigDecimal(100))
                                    .divide(new java.math.BigDecimal(citadas), 2,
                                            java.math.RoundingMode.HALF_UP);
                    return new KpiMes(desde, hasta, citas, atendidas, faltas, noShow,
                            rs.getBigDecimal("recaudado"), rs.getBigDecimal("efectivo"),
                            rs.getBigDecimal("yape"), rs.getBigDecimal("plin"),
                            rs.getBigDecimal("tarjeta"));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron calcular los KPI del periodo.", e);
        }
        throw new DentrixException("No se pudieron calcular los KPI del periodo.");
    }

    public List<ProductividadOdontologo> productividadOdontologos(LocalDate desde, LocalDate hasta)
            throws DentrixException {
        String sql = "SELECT u.id AS usuario_id, u.apellidos || ', ' || u.nombres AS nombre, "
                + "COUNT(*) FILTER (WHERE c.estado = 'ATENDIDA') AS atendidas, "
                + "COUNT(*) FILTER (WHERE c.estado = 'FALTO') AS faltas, "
                + "COALESCE((SELECT SUM(p.monto) FROM pago p "
                + "    WHERE p.pago_origen_id IS NULL AND p.estado = 'EMITIDO' "
                + "      AND p.ficha_id IN (SELECT f.id FROM ficha_clinica f "
                + "                         WHERE f.odontologo_id = u.id "
                + "                           AND f.fecha BETWEEN ? AND ?)), 0) AS recaudado "
                + "FROM cita c JOIN usuario u ON u.id = c.usuario_id "
                + "WHERE c.tipo = 'CITA' AND c.fecha_cita BETWEEN ? AND ? "
                + "GROUP BY u.id, u.apellidos, u.nombres "
                + "ORDER BY atendidas DESC, recaudado DESC, nombre";
        List<ProductividadOdontologo> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, desde);
            ps.setObject(2, hasta);
            ps.setObject(3, desde);
            ps.setObject(4, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ProductividadOdontologo(rs.getInt("usuario_id"),
                            rs.getString("nombre"), rs.getLong("atendidas"),
                            rs.getLong("faltas"), rs.getBigDecimal("recaudado")));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo calcular la productividad por odontólogo.", e);
        }
        return lista;
    }
}
