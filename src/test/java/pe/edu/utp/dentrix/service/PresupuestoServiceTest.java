package pe.edu.utp.dentrix.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.impl.FichaClinicaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PresupuestoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.RolDAOImpl;
import pe.edu.utp.dentrix.dao.impl.TarifarioDAOImpl;
import pe.edu.utp.dentrix.dao.impl.TratamientoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.UsuarioDAOImpl;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.FichaClinica;
import pe.edu.utp.dentrix.model.Presupuesto;
import pe.edu.utp.dentrix.model.PresupuestoItem;
import pe.edu.utp.dentrix.util.FirmaSimulada;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PresupuestoServiceTest {

    private static final Integer PACIENTE_PRUEBA = 1;
    private static final Integer USUARIO_ADMIN = 1;
    private static final Integer USUARIO_ODONTOLOGO = 2;

    private final PresupuestoDAOImpl presupuestoDAO = new PresupuestoDAOImpl();
    private final FichaClinicaDAOImpl fichaDAO = new FichaClinicaDAOImpl();
    private final TarifarioService tarifario = new TarifarioService(new TarifarioDAOImpl());
    private final PresupuestoService servicio = new PresupuestoService(presupuestoDAO, tarifario,
            fichaDAO, new TratamientoDAOImpl(), new UsuarioDAOImpl(), new RolDAOImpl());

    private boolean hayBaseDeDatos() {
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM presupuesto WHERE 1 = 0");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private FichaClinica fichaDelPaciente() throws DentrixException {
        return fichaDAO.listarPorPaciente(PACIENTE_PRUEBA).stream()
                .findFirst()
                .orElse(null);
    }

    private void limpiarVigentes(Long fichaId) throws DentrixException {
        for (Presupuesto previo : presupuestoDAO.listarPorPaciente(PACIENTE_PRUEBA)) {
            if (fichaId.equals(previo.getFichaId())
                    && ("BORRADOR".equals(previo.getEstado()) || "PRESENTADO".equals(previo.getEstado()))) {
                servicio.descartar(previo.getId());
            }
        }
    }

    private PresupuestoItem item(int tratamientoId, Integer pieza) {
        PresupuestoItem item = new PresupuestoItem();
        item.setTratamientoId(tratamientoId);
        item.setPiezaFdi(pieza);
        item.setFase("BASICA");
        item.setPrioridad(2);
        item.setSecuencia(1);
        item.setCantidad(1);
        item.setSesionesPlanificadas(1);
        return item;
    }

    @Test
    void firmaSimuladaGeneraUnSha256Hexadecimal() {
        String firma = FirmaSimulada.hash("Juan Perez", "45123698", "1", "123");
        assertEquals(64, firma.length(), "El hash debe tener 64 caracteres hexadecimales");
        assertTrue(firma.matches("[0-9a-f]{64}"), "El hash debe ser hexadecimal en minusculas: " + firma);
        assertEquals(firma, FirmaSimulada.hash("Juan Perez", "45123698", "1", "123"),
                "El mismo contenido debe producir el mismo hash");
        assertFalse(firma.equals(FirmaSimulada.hash("Juan Perez", "45123698", "2", "123")),
                "Un contenido distinto debe producir otro hash");
    }

    @Test
    void elFlujoDeBorradorAPresentadoCuadraLosTotales() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        FichaClinica ficha = fichaDelPaciente();
        assumeTrue(ficha != null, "La semilla no tiene ficha del paciente 1: se omite esta prueba");

        limpiarVigentes(ficha.getId());
        Presupuesto presupuesto = null;
        try {
            presupuesto = servicio.crearDesdeFicha(ficha.getId(), USUARIO_ADMIN);
            assertNotNull(presupuesto.getId(), "El presupuesto debe quedar registrado con id");
            assertEquals("BORRADOR", presupuesto.getEstado(), "El presupuesto nace BORRADOR");
            assertEquals(1, presupuesto.getVersion(), "La primera version debe ser la 1");
            assertEquals("PEN", presupuesto.getMoneda().trim(), "La moneda debe ser PEN");
            assertEquals(0, BigDecimal.ZERO.compareTo(presupuesto.getSubtotal()),
                    "El presupuesto nace con subtotal 0");

            PresupuestoItem resina = servicio.agregarItem(presupuesto.getId(), item(1, 16));
            assertEquals(0, new BigDecimal("80.00").compareTo(resina.getPrecioUnitario()),
                    "El precio lo toma el servicio del tarifario vigente (S/80.00)");
            assertEquals("PROPUESTO", resina.getEstado(), "El item nace PROPUESTO");
            assertFalse(resina.isPrecioCongelado(), "El item nace sin precio congelado");
            assertEquals("BASICA", resina.getFase(), "La fase indicada se conserva");

            PresupuestoItem endodoncia = servicio.agregarItem(presupuesto.getId(), item(5, 36));
            assertEquals(0, new BigDecimal("120.00").compareTo(endodoncia.getPrecioUnitario()),
                    "El precio lo toma el servicio del tarifario vigente (S/120.00)");

            assertEquals(0, new BigDecimal("200.00").compareTo(servicio.buscar(presupuesto.getId()).getSubtotal()),
                    "El subtotal lo recalcula la base al insertar los items (200.00)");

            servicio.aplicarDescuento(presupuesto.getId(), new BigDecimal("20.00"), "Prueba", USUARIO_ADMIN);
            assertEquals(0, new BigDecimal("180.00").compareTo(servicio.buscar(presupuesto.getId()).getTotal()),
                    "El total generado por la base debe ser 180.00");

            servicio.presentar(presupuesto.getId());
            assertEquals("PRESENTADO", servicio.buscar(presupuesto.getId()).getEstado(),
                    "El presupuesto presentado queda PRESENTADO");
            assertEquals(2, servicio.items(presupuesto.getId()).size(),
                    "El presupuesto conserva sus dos items");
        } finally {
            if (presupuesto != null) {
                servicio.descartar(presupuesto.getId());
            }
        }
    }

    @Test
    void validaLasReglasDelPresupuesto() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        FichaClinica ficha = fichaDelPaciente();
        assumeTrue(ficha != null, "La semilla no tiene ficha del paciente 1: se omite esta prueba");

        limpiarVigentes(ficha.getId());
        Presupuesto presupuesto = null;
        try {
            presupuesto = servicio.crearDesdeFicha(ficha.getId(), USUARIO_ADMIN);
            final Long id = presupuesto.getId();

            DentrixException borradorNoSeAcepta = assertThrows(DentrixException.class,
                    () -> servicio.aceptar(id, List.of(), "Juan Perez", "45123698", "PACIENTE",
                            "Ortodoncia", "Riesgos", USUARIO_ADMIN));
            assertTrue(borradorNoSeAcepta.getMessage().contains("PRESENTADO"),
                    "Solo un presupuesto PRESENTADO se acepta: " + borradorNoSeAcepta.getMessage());

            servicio.agregarItem(id, item(1, 16));

            DentrixException autorizadorNoAdmin = assertThrows(DentrixException.class,
                    () -> servicio.aplicarDescuento(id, new BigDecimal("10.00"), "Prueba", USUARIO_ODONTOLOGO));
            assertTrue(autorizadorNoAdmin.getMessage().contains("ADMINISTRADOR"),
                    "El descuento exige autorizador ADMINISTRADOR: " + autorizadorNoAdmin.getMessage());

            DentrixException descuentoExcesivo = assertThrows(DentrixException.class,
                    () -> servicio.aplicarDescuento(id, new BigDecimal("500.00"), "Prueba", USUARIO_ADMIN));
            assertTrue(descuentoExcesivo.getMessage().contains("subtotal"),
                    "El descuento no puede superar el subtotal: " + descuentoExcesivo.getMessage());

            servicio.presentar(id);

            DentrixException presentadoNoAdmiteItems = assertThrows(DentrixException.class,
                    () -> servicio.agregarItem(id, item(5, 36)));
            assertTrue(presentadoNoAdmiteItems.getMessage().contains("BORRADOR"),
                    "Un presupuesto PRESENTADO no admite items nuevos: "
                            + presentadoNoAdmiteItems.getMessage());

            DentrixException rechazoSinMotivo = assertThrows(DentrixException.class,
                    () -> servicio.rechazar(id, "   "));
            assertTrue(rechazoSinMotivo.getMessage().contains("motivo"),
                    "El rechazo exige motivo: " + rechazoSinMotivo.getMessage());

            assertEquals("PRESENTADO", servicio.buscar(id).getEstado(),
                    "Tras los errores el presupuesto debe seguir PRESENTADO");
        } finally {
            if (presupuesto != null) {
                servicio.descartar(presupuesto.getId());
            }
        }
    }
}
