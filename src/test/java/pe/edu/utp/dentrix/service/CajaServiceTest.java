package pe.edu.utp.dentrix.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.CierreCajaDAO;
import pe.edu.utp.dentrix.dao.ComprobanteDAO;
import pe.edu.utp.dentrix.dao.FichaClinicaDAO;
import pe.edu.utp.dentrix.dao.PacienteDAO;
import pe.edu.utp.dentrix.dao.PagoDAO;
import pe.edu.utp.dentrix.dao.PresupuestoDAO;
import pe.edu.utp.dentrix.dao.TratamientoDAO;
import pe.edu.utp.dentrix.dao.impl.CierreCajaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.ComprobanteDAOImpl;
import pe.edu.utp.dentrix.dao.impl.FichaClinicaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PacienteDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PagoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PresupuestoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.TarifarioDAOImpl;
import pe.edu.utp.dentrix.dao.impl.TratamientoDAOImpl;
import pe.edu.utp.dentrix.dto.LineaBoleta;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.CierreCaja;
import pe.edu.utp.dentrix.model.Paciente;
import pe.edu.utp.dentrix.model.Pago;
import pe.edu.utp.dentrix.model.Presupuesto;
import pe.edu.utp.dentrix.model.PresupuestoItem;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CajaServiceTest {

    private final PacienteDAO pacienteDAO = new PacienteDAOImpl();
    private final PagoDAO pagoDAO = new PagoDAOImpl();
    private final ComprobanteDAO comprobanteDAO = new ComprobanteDAOImpl();
    private final CierreCajaDAO cierreCajaDAO = new CierreCajaDAOImpl();
    private final TratamientoDAO tratamientoDAO = new TratamientoDAOImpl();
    private final FichaClinicaDAO fichaClinicaDAO = new FichaClinicaDAOImpl();
    private final PresupuestoDAO presupuestoDAO = new PresupuestoDAOImpl();
    private final TarifarioService tarifario = new TarifarioService(new TarifarioDAOImpl());
    private final CajaService servicio = new CajaService(pagoDAO, cierreCajaDAO, pacienteDAO,
            tratamientoDAO, fichaClinicaDAO, presupuestoDAO, tarifario, comprobanteDAO);

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
            try (Statement st = cn.createStatement()) {
                st.execute("ALTER TABLE pago DISABLE TRIGGER tr_pago_no_delete");
            }
            try {
                try (PreparedStatement ps = cn.prepareStatement(
                        "DELETE FROM imputacion WHERE pago_id IN "
                                + "(SELECT id FROM pago WHERE paciente_id = ?)")) {
                    ps.setInt(1, pacienteId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = cn.prepareStatement(
                        "DELETE FROM pago WHERE paciente_id = ? AND pago_origen_id IS NOT NULL")) {
                    ps.setInt(1, pacienteId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = cn.prepareStatement(
                        "DELETE FROM pago WHERE paciente_id = ?")) {
                    ps.setInt(1, pacienteId);
                    ps.executeUpdate();
                }
            } finally {
                try (Statement st = cn.createStatement()) {
                    st.execute("ALTER TABLE pago ENABLE TRIGGER tr_pago_no_delete");
                }
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

            List<LineaBoleta> lineas = List.of(LineaBoleta.directa(1, 21, 1), LineaBoleta.directa(2, null, 2));
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

            List<LineaBoleta> lineas = List.of(LineaBoleta.directa(1, null, 1));
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

            List<LineaBoleta> lineas = List.of(LineaBoleta.directa(5, 31, 1));
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

            List<LineaBoleta> lineas = List.of(LineaBoleta.directa(2, null, 1));
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

    @Test
    void elArqueoExigeMotivoCuandoLaDiferenciaSuperaLaTolerancia() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        LocalDate fecha = LocalDate.of(2026, 12, 24);
        try {
            CierreCaja abierto = servicio.abrirCierre(fecha, "DIURNO", 1, BigDecimal.ZERO);
            Long id = abierto.getId();

            DentrixException sinMotivo = assertThrows(DentrixException.class,
                    () -> servicio.consolidar(id, new BigDecimal("50.00"), null, null, 1));
            assertTrue(sinMotivo.getMessage().contains("P6"),
                    "P6: la diferencia sobre la tolerancia exige motivo: " + sinMotivo.getMessage());

            CierreCaja consolidado = servicio.consolidar(id, new BigDecimal("50.00"), null,
                    "Vuelto pendiente", 1);
            assertEquals("CONSOLIDADO", consolidado.getEstado());
            assertEquals(0, new BigDecimal("1.00").compareTo(consolidado.getTolerancia()),
                    "La tolerancia por defecto es S/ 1.00");
            assertEquals("Vuelto pendiente", consolidado.getMotivoDiferencia(),
                    "El motivo del arqueo debe quedar registrado");
            assertEquals(0, new BigDecimal("50.00").compareTo(consolidado.getDiferencia()),
                    "La diferencia es contado - fondo - efectivo del sistema");
        } finally {
            try (Connection cn = ConexionBD.abrir();
                 PreparedStatement ps = cn.prepareStatement("DELETE FROM cierre_caja WHERE fecha = ?")) {
                ps.setObject(1, fecha);
                ps.executeUpdate();
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    void calculaTotalesDelCierreSoloConAbonos() {
        List<Pago> pagos = List.of(abono("EFECTIVO", "100.00"), abono("YAPE", "50.00"),
                abono("TARJETA", "30.00"));
        CajaService.TotalesCierre totales = CajaService.calcularTotales(pagos);
        assertEquals(0, new BigDecimal("100.00").compareTo(totales.efectivo()),
                "El efectivo del sistema suma solo los abonos en efectivo");
        assertEquals(0, new BigDecimal("50.00").compareTo(totales.billeteras()),
                "Las billeteras del sistema suman Yape y Plin");
        assertEquals(0, new BigDecimal("180.00").compareTo(totales.total()),
                "El total del sistema incluye tarjeta");
    }

    @Test
    void ignoraLasBoletasEnLosTotalesDelCierre() {
        Pago boleta = new Pago();
        boleta.setModalidad("EFECTIVO");
        boleta.setMonto(new BigDecimal("200.00"));
        List<Pago> pagos = List.of(boleta, abono("PLIN", "40.00"));
        CajaService.TotalesCierre totales = CajaService.calcularTotales(pagos);
        assertEquals(0, BigDecimal.ZERO.compareTo(totales.efectivo()),
                "La boleta no es efectivo que entra: solo cuentan los abonos");
        assertEquals(0, new BigDecimal("40.00").compareTo(totales.billeteras()),
                "Solo el abono Plin suma en billeteras");
        assertEquals(0, new BigDecimal("40.00").compareTo(totales.total()),
                "El total del sistema excluye la boleta");
    }

    private Pago abono(String modalidad, String monto) {
        Pago pago = new Pago();
        pago.setPagoOrigenId(1L);
        pago.setModalidad(modalidad);
        pago.setMonto(new BigDecimal(monto));
        return pago;
    }

    @Test
    void rechazaUnItemQueYaFueFacturado() {
        Paciente paciente = pacienteFalso(2);
        Map<Long, PresupuestoItem> items = Map.of(58L, itemDePresupuesto(58L, 53L));
        Map<Long, Presupuesto> presupuestos = Map.of(53L, presupuestoAceptado(53L, 2));
        CajaService validacion = servicioDeValidacion(presupuestosFalsos(items, presupuestos, Set.of(58L)),
                paciente);

        DentrixException error = assertThrows(DentrixException.class,
                () -> validacion.emitirBoleta(2, "EFECTIVO", null, null, null, 1, null,
                        List.of(LineaBoleta.deItem(58L))));
        assertTrue(error.getMessage().contains("ya fue facturado"), error.getMessage());
    }

    @Test
    void rechazaItemsDePresupuestosDistintosEnLaMismaBoleta() {
        Paciente paciente = pacienteFalso(2);
        Map<Long, PresupuestoItem> items = Map.of(
                58L, itemDePresupuesto(58L, 53L),
                60L, itemDePresupuesto(60L, 99L));
        Map<Long, Presupuesto> presupuestos = Map.of(
                53L, presupuestoAceptado(53L, 2),
                99L, presupuestoAceptado(99L, 2));
        CajaService validacion = servicioDeValidacion(presupuestosFalsos(items, presupuestos, Set.of()),
                paciente);

        DentrixException error = assertThrows(DentrixException.class,
                () -> validacion.emitirBoleta(2, "EFECTIVO", null, null, null, 1, null,
                        List.of(LineaBoleta.deItem(58L), LineaBoleta.deItem(60L))));
        assertTrue(error.getMessage().contains("mismo presupuesto"), error.getMessage());
    }

    @Test
    void rechazaMezclarItemsDelPresupuestoConVentaDirecta() {
        Paciente paciente = pacienteFalso(2);
        Map<Long, PresupuestoItem> items = Map.of(58L, itemDePresupuesto(58L, 53L));
        Map<Long, Presupuesto> presupuestos = Map.of(53L, presupuestoAceptado(53L, 2));
        CajaService validacion = servicioDeValidacion(presupuestosFalsos(items, presupuestos, Set.of()),
                paciente);

        DentrixException error = assertThrows(DentrixException.class,
                () -> validacion.emitirBoleta(2, "EFECTIVO", null, null, null, 1, null,
                        List.of(LineaBoleta.deItem(58L), LineaBoleta.directa(5, 31, 1))));
        assertTrue(error.getMessage().contains("mezclar"), error.getMessage());
    }

    private Paciente pacienteFalso(int id) {
        Paciente paciente = new Paciente();
        paciente.setId(id);
        return paciente;
    }

    private PresupuestoItem itemDePresupuesto(long id, long presupuestoId) {
        PresupuestoItem item = new PresupuestoItem();
        item.setId(id);
        item.setPresupuestoId(presupuestoId);
        item.setTratamientoId(5);
        item.setPiezaFdi(31);
        item.setCantidad(1);
        item.setSubtotal(new BigDecimal("120.00"));
        item.setMontoNeto(new BigDecimal("112.00"));
        item.setEstado("ACEPTADO");
        return item;
    }

    private Presupuesto presupuestoAceptado(long id, int pacienteId) {
        Presupuesto presupuesto = new Presupuesto();
        presupuesto.setId(id);
        presupuesto.setPacienteId(pacienteId);
        presupuesto.setEstado("ACEPTADO");
        return presupuesto;
    }

    private CajaService servicioDeValidacion(PresupuestoDAO presupuestos, Paciente paciente) {
        PacienteDAO pacientes = proxy(PacienteDAO.class, (objeto, metodo, argumentos) -> {
            if ("buscarPorId".equals(metodo.getName())) {
                return Optional.of(paciente);
            }
            throw new UnsupportedOperationException(metodo.getName());
        });
        return new CajaService(null, null, pacientes, null, null, presupuestos, null, null);
    }

    private PresupuestoDAO presupuestosFalsos(Map<Long, PresupuestoItem> items,
                                              Map<Long, Presupuesto> presupuestos,
                                              Set<Long> facturados) {
        return proxy(PresupuestoDAO.class, (objeto, metodo, argumentos) -> switch (metodo.getName()) {
            case "buscarItem" -> Optional.ofNullable(items.get((Long) argumentos[0]));
            case "buscarPorId" -> Optional.ofNullable(presupuestos.get((Long) argumentos[0]));
            case "estaFacturado" -> facturados.contains((Long) argumentos[0]);
            default -> throw new UnsupportedOperationException(metodo.getName());
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> tipo, InvocationHandler manejador) {
        return (T) Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo}, manejador);
    }
}
