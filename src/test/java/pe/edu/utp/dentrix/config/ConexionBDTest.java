package pe.edu.utp.dentrix.config;

import com.google.common.reflect.ClassPath;
import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.excepcion.DentrixException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ConexionBDTest {

    private Connection conectar() {
        try {
            return ConexionBD.abrir();
        } catch (DentrixException e) {
            return null;
        }
    }

    private long consultarLong(Connection cn, String sql) throws Exception {
        try (PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    @Test
    void lasEntidadesCubrenElModeloAprobado() throws Exception {
        ClassPath cp = ClassPath.from(getClass().getClassLoader());
        List<String> entidades = cp.getTopLevelClasses("pe.edu.utp.dentrix.model").stream()
                .map(ClassPath.ClassInfo::getSimpleName)
                .filter(nombre -> !"PackageInfo".equals(nombre))
                .sorted()
                .toList();
        assertEquals(36, entidades.size(),
                () -> "Debe haber 36 clases de modelo (31 previas + ListaEspera + Recordatorio "
                        + "+ ConsentimientoDatos + CitaHistorial + ComprobanteItem): " + entidades);
    }

    @Test
    void elEsquemaV3TieneLasTablasVistasYTriggersEsperados() throws Exception {
        try (Connection cn = conectar()) {
            assumeTrue(cn != null, "config.properties o BD no disponibles: se omite esta prueba");

            long tablas = consultarLong(cn,
                    "SELECT count(*) FROM information_schema.tables "
                            + "WHERE table_schema = 'dentrix' AND table_type = 'BASE TABLE'");
            long vistas = consultarLong(cn,
                    "SELECT count(*) FROM information_schema.tables "
                            + "WHERE table_schema = 'dentrix' AND table_type = 'VIEW'");
            long triggers = consultarLong(cn,
                    "SELECT count(DISTINCT trigger_name) FROM information_schema.triggers "
                            + "WHERE trigger_schema = 'dentrix'");

            assertEquals(39, tablas,
                    "39 tablas = 37 de agenda + comprobante_item y auditoria");
            assertEquals(14, vistas,
                    "14 vistas (se reemplazo v_ranking_tratamientos, no se agrego ninguna)");
            assertEquals(72, triggers,
                    "72 triggers = 60 de agenda + comprobante, borrador, emision, anulacion y 7 de auditoria");
        }
    }

    @Test
    void laSemillaReproduceElDiaOperativo2809() throws Exception {
        try (Connection cn = conectar()) {
            assumeTrue(cn != null, "config.properties o BD no disponibles: se omite esta prueba");

            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT monto, saldo_deudor, estado_pago FROM pago WHERE numero = 'B001-000458'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    assertNotNull(rs.next(), "Debe existir la boleta B001-000458");
                    assertEquals(0, new BigDecimal("150.00").compareTo(rs.getBigDecimal(1)),
                            "La boleta B001-000458 debe ser de S/150");
                    assertEquals(0, rs.getBigDecimal(2).signum(), "Debe estar liquidada");
                    assertEquals("PAGADO", rs.getString(3));
                }
            }

            assertEquals(0, consultarLong(cn,
                    "SELECT COALESCE(diferencia, 0) FROM cierre_caja WHERE fecha = DATE '2026-09-28'"),
                    "La gaveta del 28/09 cuadra con diferencia 0");

            assertEquals(5, consultarLong(cn,
                    "SELECT count(*) FROM cita WHERE fecha_cita = DATE '2026-09-28' AND tipo = 'CITA'"),
                    "El 28/09 deben haber 5 citas atendibles");

            try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery(
                    "SELECT count(*), COALESCE(SUM(saldo_total), 0) "
                            + "FROM v_saldos_paciente WHERE saldo_total > 0")) {
                assertNotNull(rs.next());
                assertTrue(rs.getLong(1) >= 2, "KPI M2: al menos los 2 pacientes del seed deben tener saldo");
                assertTrue(new BigDecimal("650.00").compareTo(rs.getBigDecimal(2)) <= 0,
                        "KPI M2: los saldos por cobrar incluyen al menos los S/650 del seed");
            }
        }
    }
}
