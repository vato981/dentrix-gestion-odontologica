package pe.edu.utp.dentrix.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.PagoDAO;
import pe.edu.utp.dentrix.dao.impl.PagoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.ReporteDAOImpl;
import pe.edu.utp.dentrix.dto.KpiDia;
import pe.edu.utp.dentrix.dto.RankingTratamiento;
import pe.edu.utp.dentrix.dto.SaldoPaciente;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ReporteServiceTest {

    private final PagoDAO pagoDAO = new PagoDAOImpl();
    private final ReporteService servicio = new ReporteService(new ReporteDAOImpl(), pagoDAO);

    private boolean hayBaseDeDatos() {
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM pago WHERE 1 = 0");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void calculaLosKpisDeLaFechaSemilla() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");

        KpiDia kpi = servicio.kpisDeFecha(LocalDate.of(2026, 9, 28));

        assertNotNull(kpi, "El KPI del día no debe ser nulo");
        assertEquals(LocalDate.of(2026, 9, 28), kpi.fecha(), "La fecha debe ser la solicitada");
        assertEquals(5, kpi.citas(), "La fecha semilla tiene 5 citas");
        assertEquals(2, kpi.enSala(), "La fecha semilla tiene 2 citas EN_SALA");
        assertEquals(0, new BigDecimal("2030.00").compareTo(kpi.recaudado()),
                "El recaudado real del 28/09 es 2030.00 (el 2230 del enunciado es el total_sistema escrito a mano)");
        assertTrue(kpi.deudores() >= 2, "Al menos los 2 pacientes del seed tienen saldo deudor");
    }

    @Test
    void listaLosPacientesConSaldoPendiente() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");

        List<SaldoPaciente> saldos = servicio.saldosPacientes();

        assertFalse(saldos.isEmpty(), "Debe haber al menos un paciente con saldo");
        for (SaldoPaciente saldo : saldos) {
            assertTrue(saldo.saldo().compareTo(BigDecimal.ZERO) > 0,
                    "Solo deben listarse saldos mayores a cero: " + saldo.codigoHc());
            assertNotNull(saldo.codigoHc(), "Cada fila debe traer el código de historia clínica");
            assertNotNull(saldo.nombre(), "Cada fila debe traer el nombre del paciente");
        }
        assertTrue(saldos.stream().anyMatch(s -> s.pacienteId() == 1),
                "El paciente 1 (Juan Carlos) tiene saldo 500.00");
        assertTrue(saldos.stream().anyMatch(s -> s.pacienteId() == 3),
                "El paciente 3 (Pedro) tiene saldo 150.00");
    }

    @Test
    void construyeElRankingDeTratamientosOrdenadoPorMonto() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");

        List<RankingTratamiento> ranking = servicio.rankingTratamientos();

        assertFalse(ranking.isEmpty(), "El ranking no debe estar vacío");
        for (int i = 0; i < ranking.size() - 1; i++) {
            assertTrue(ranking.get(i).monto().compareTo(ranking.get(i + 1).monto()) >= 0,
                    "El ranking debe estar ordenado por monto descendente");
        }
        assertEquals("TRAT-05", ranking.get(0).codigo(),
                "El tratamiento más cobrado es TRAT-05 (Endodoncia, 840.00)");
        assertTrue(ranking.get(0).monto().compareTo(new BigDecimal("800.00")) > 0,
                "El monto líder debe superar los 800.00");
    }

    @Test
    void listaSoloLosDocumentosConSaldoDeudor() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");

        assertTrue(servicio.documentosConSaldo().stream()
                        .allMatch(p -> p.getSaldoDeudor().compareTo(BigDecimal.ZERO) > 0),
                "Solo deben listarse documentos con saldo deudor mayor a cero");
    }
}
