package pe.edu.utp.dentrix.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.CierreCajaDAO;
import pe.edu.utp.dentrix.dao.ConfiguracionDAO;
import pe.edu.utp.dentrix.dao.FichaClinicaDAO;
import pe.edu.utp.dentrix.dao.PacienteDAO;
import pe.edu.utp.dentrix.dao.PagoDAO;
import pe.edu.utp.dentrix.dao.PlanTratamientoDAO;
import pe.edu.utp.dentrix.dao.TratamientoDAO;
import pe.edu.utp.dentrix.dao.impl.CierreCajaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.ConfiguracionDAOImpl;
import pe.edu.utp.dentrix.dao.impl.FichaClinicaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PacienteDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PagoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PlanTratamientoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.TratamientoDAOImpl;
import pe.edu.utp.dentrix.dto.LineaBoleta;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.CierreCaja;
import pe.edu.utp.dentrix.model.Paciente;
import pe.edu.utp.dentrix.model.Pago;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CajaServiceTest {

    private final PacienteDAO pacienteDAO = new PacienteDAOImpl();
    private final PagoDAO pagoDAO = new PagoDAOImpl();
    private final CierreCajaDAO cierreCajaDAO = new CierreCajaDAOImpl();
    private final TratamientoDAO tratamientoDAO = new TratamientoDAOImpl();
    private final FichaClinicaDAO fichaClinicaDAO = new FichaClinicaDAOImpl();
    private final PlanTratamientoDAO planTratamientoDAO = new PlanTratamientoDAOImpl();
    private final ConfiguracionDAO configuracionDAO = new ConfiguracionDAOImpl();
    private final CajaService servicio = new CajaService(pagoDAO, cierreCajaDAO, pacienteDAO,
            tratamientoDAO, fichaClinicaDAO, planTratamientoDAO, configuracionDAO);

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

    private Paciente pacienteTemporal(String dni, String apellidos, String nombres) {
        Paciente paciente = new Paciente();
        paciente.setCodigoHc("HC-" + dni.substring(dni.length() - 6));
        paciente.setFechaApertura(LocalDate.now());
        paciente.setEstado("ACTIVO");
        paciente.setDni(dni);
        paciente.setApellidos(apellidos);
        paciente.setNombres(nombres);
        paciente.setFechaNac(LocalDate.of(1990, 7, 20));
        paciente.setCelular("999111222");
        paciente.setEmail("caja" + dni + "@correo.com");
        paciente.setDireccion("Av. Caja 456, Chilca - Huancayo");
        paciente.setAlergias("Sin alergias registradas");
        paciente.setAntecedentes("Paciente de prueba de caja");
        return paciente;
    }

    private void limpiarTodo(String dni) {
        try (Connection cn = ConexionBD.abrir()) {
            Integer pacienteId = null;
            try (PreparedStatement ps = cn.prepareStatement("SELECT id FROM paciente WHERE dni = ?")) {
                ps.setString(1, dni);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        pacienteId = rs.getInt(1);
                    }
                }
            }
            if (pacienteId == null) {
                return;
            }
            try (PreparedStatement ps = cn.prepareStatement(
                    "DELETE FROM plan_tratamiento WHERE paciente_id = ?")) {
                ps.setInt(1, pacienteId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = cn.prepareStatement(
                    "DELETE FROM pago WHERE paciente_id = ?")) {
                ps.setInt(1, pacienteId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = cn.prepareStatement(
                    "DELETE FROM paciente WHERE id = ?")) {
                ps.setInt(1, pacienteId);
                ps.executeUpdate();
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    void emiteUnaBoletaYElMotorDeLaBaseCalculaLosTotales() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        limpiarTodo("90000031");
        Paciente creado = null;
        try {
            Paciente nuevo = pacienteTemporal("90000031", "Caja Temporal", "Emisor");
            pacienteDAO.insertar(nuevo);
            creado = pacienteDAO.buscarPorDni("90000031").orElseThrow();

            List<LineaBoleta> lineas = List.of(new LineaBoleta(1, 21, 1), new LineaBoleta(2, null, 2));
            Pago boleta = servicio.emitirBoleta(creado.getId(), "EFECTIVO", null, null, null, 1, null, lineas);

            assertNotNull(boleta.getId(), "La boleta debe quedar registrada con id");
            assertTrue(boleta.getNumero().startsWith("B001-"),
                    "El número debe seguir la serie B001: " + boleta.getNumero());
            assertEquals(new BigDecimal("186.44"), boleta.getSubtotal(),
                    "El subtotal lo calcula el motor de la base (186.44)");
            assertEquals(new BigDecimal("33.56"), boleta.getIgv(),
                    "El IGV del 18% lo calcula el motor (33.56)");
            assertEquals(new BigDecimal("220.00"), boleta.getMonto(),
                    "El monto total debe ser 220.00 (80 + 2x70)");
            assertEquals("PENDIENTE", boleta.getEstadoPago(),
                    "Sin abonos el estado de pago es PENDIENTE");
            assertEquals("EMITIDO", boleta.getEstado(), "El estado del comprobante es EMITIDO");
        } finally {
            limpiarTodo("90000031");
        }
    }

    @Test
    void rechazaLaBoletaCuandoLaFichaNoEstaAceptada() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        limpiarTodo("90000032");
        try {
            Paciente nuevo = pacienteTemporal("90000032", "Caja Temporal", "Derivacion");
            pacienteDAO.insertar(nuevo);
            Paciente creado = pacienteDAO.buscarPorDni("90000032").orElseThrow();

            List<LineaBoleta> lineas = List.of(new LineaBoleta(1, null, 1));
            DentrixException error = assertThrows(DentrixException.class,
                    () -> servicio.emitirBoleta(creado.getId(), "EFECTIVO", null, 2L, null, 1, null, lineas));
            assertTrue(error.getMessage().contains("RF-07"),
                    "El mensaje debe invocar el RF-07: " + error.getMessage());
        } finally {
            limpiarTodo("90000032");
        }
    }

    @Test
    void registraUnAbonoYRechazaElQueExcedeElSaldo() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        limpiarTodo("90000033");
        Paciente creado = null;
        try {
            Paciente nuevo = pacienteTemporal("90000033", "Caja Temporal", "Abonista");
            pacienteDAO.insertar(nuevo);
            creado = pacienteDAO.buscarPorDni("90000033").orElseThrow();

            List<LineaBoleta> lineas = List.of(new LineaBoleta(5, 31, 1));
            Pago boleta = servicio.emitirBoleta(creado.getId(), "EFECTIVO", null, null, null, 1, null, lineas);

            Pago abono = servicio.registrarAbono(boleta.getId(), new BigDecimal("50.00"),
                    "YAPE", "OP-778899", 1);
            assertNotNull(abono.getId(), "El abono debe quedar registrado con id");
            assertEquals(boleta.getId(), abono.getPagoOrigenId(),
                    "El abono debe apuntar a la boleta original");
            assertEquals("PENDIENTE", abono.getEstadoPago(),
                    "El estado del propio abono no lo gestiona el servicio (queda PENDIENTE)");

            DentrixException error = assertThrows(DentrixException.class,
                    () -> servicio.registrarAbono(boleta.getId(), new BigDecimal("80.00"),
                            "EFECTIVO", null, 1));
            assertTrue(error.getMessage().contains("RF-08"),
                    "El exceso de saldo debe invocar el RF-08: " + error.getMessage());
        } finally {
            limpiarTodo("90000033");
        }
    }

    @Test
    void rechazaLaModalidadBilleteraSinNumeroDeOperacion() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        limpiarTodo("90000034");
        try {
            Paciente nuevo = pacienteTemporal("90000034", "Caja Temporal", "Billetera");
            pacienteDAO.insertar(nuevo);
            Paciente creado = pacienteDAO.buscarPorDni("90000034").orElseThrow();

            List<LineaBoleta> lineas = List.of(new LineaBoleta(2, null, 1));
            DentrixException error = assertThrows(DentrixException.class,
                    () -> servicio.emitirBoleta(creado.getId(), "YAPE", null, null, null, 1, null, lineas));
            assertTrue(error.getMessage().contains("RF-08"),
                    "Yape sin operación debe invocar el RF-08: " + error.getMessage());
        } finally {
            limpiarTodo("90000034");
        }
    }

    @Test
    void abreCierreRegistraElFondoYSeConsolida() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        LocalDate fecha = LocalDate.of(2026, 12, 23);
        try {
            CierreCaja abierto = servicio.abrirCierre(fecha, "DIURNO", 1, new BigDecimal("100.00"));
            assertEquals("ABIERTO", abierto.getEstado(), "El cierre nace en estado ABIERTO");
            assertEquals(new BigDecimal("100.00"), abierto.getFondoInicial(),
                    "El fondo inicial debe ser el solicitado");
            assertNotNull(abierto.getId(), "El cierre debe quedar registrado con id");

            DentrixException duplicado = assertThrows(DentrixException.class,
                    () -> servicio.abrirCierre(fecha, "DIURNO", 1, new BigDecimal("100.00")));
            assertTrue(duplicado.getMessage().contains("Ya existe"),
                    "No puede haber dos cierres para la misma fecha y turno: " + duplicado.getMessage());

            DentrixException actaPrematura = assertThrows(DentrixException.class,
                    () -> servicio.marcarActaGenerada(abierto.getId()));
            assertTrue(actaPrematura.getMessage().contains("CONSOLIDADO"),
                    "El acta solo se marca en un cierre CONSOLIDADO: " + actaPrematura.getMessage());

            CierreCaja consolidado = servicio.consolidar(abierto.getId(), new BigDecimal("100.00"), 1);
            assertEquals("CONSOLIDADO", consolidado.getEstado(),
                    "El cierre debe pasar a CONSOLIDADO");
            assertNotNull(consolidado.getConsolidadoEn(),
                    "El cierre consolidado debe registrar la fecha/hora de consolidación");
            assertEquals(1, consolidado.getConsolidadoPor(),
                    "El usuario que consolida debe quedar registrado");

            servicio.marcarActaGenerada(abierto.getId());
            CierreCaja conActa = cierreCajaDAO.buscarPorFechaYTurno(fecha, "DIURNO").orElseThrow();
            assertTrue(conActa.isActaGenerada(),
                    "Tras consolidar, el acta debe poder marcarse como generada");
        } finally {
            try (Connection cn = ConexionBD.abrir();
                 PreparedStatement ps = cn.prepareStatement("DELETE FROM cierre_caja WHERE fecha = ?")) {
                ps.setObject(1, fecha);
                ps.executeUpdate();
            } catch (Exception ignored) {
            }
        }
    }
}
