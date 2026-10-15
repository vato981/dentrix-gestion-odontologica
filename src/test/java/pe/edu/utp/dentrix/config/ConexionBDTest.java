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
    void lasEntidadesMapeanLas14TablasAprobadas() throws Exception {
        ClassPath cp = ClassPath.from(getClass().getClassLoader());
        List<String> entidades = cp.getTopLevelClasses("pe.edu.utp.dentrix.model").stream()
                .map(ClassPath.ClassInfo::getSimpleName)
                .filter(nombre -> !"PackageInfo".equals(nombre))
                .sorted()
                .toList();
        assertEquals(14, entidades.size(),
                () -> "Debe haber 14 entidades (modelo aprobado): " + entidades);
    }

    @Test
    void elEsquemaV2Tiene14TablasY6Vistas() throws Exception {
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

            assertEquals(14, tablas, "El esquema debe tener 14 tablas");
            assertEquals(6, vistas, "El esquema debe tener 6 vistas del M8");
            assertEquals(6, triggers, "El esquema debe tener 6 triggers de reglas de negocio");
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
                assertEquals(2, rs.getLong(1), "KPI M2: 2 pacientes con saldo");
                assertEquals(0, new BigDecimal("650.00").compareTo(rs.getBigDecimal(2)),
                        "KPI M2: saldos por cobrar = S/650");
            }
        }
    }
}
