package pe.edu.utp.dentrix.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.impl.EjecucionDAOImpl;
import pe.edu.utp.dentrix.dao.impl.FichaClinicaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.LaboratorioDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PresupuestoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.RolDAOImpl;
import pe.edu.utp.dentrix.dao.impl.TarifarioDAOImpl;
import pe.edu.utp.dentrix.dao.impl.TratamientoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.UsuarioDAOImpl;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.ConsumoLote;
import pe.edu.utp.dentrix.model.EjecucionItem;
import pe.edu.utp.dentrix.model.FichaClinica;
import pe.edu.utp.dentrix.model.LoteInsumo;
import pe.edu.utp.dentrix.model.Presupuesto;
import pe.edu.utp.dentrix.model.PresupuestoItem;

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

class LaboratorioServiceTest {

    private static final Integer PACIENTE_PRUEBA = 1;
    private static final Integer USUARIO_ADMIN = 1;
    private static final Integer USUARIO_ODONTOLOGO = 2;
    private static final Long PRESUPUESTO_DEMO = 53L;
    private static final Integer TRATAMIENTO_BASICO = 1;
    private static final Long ORDEN_INEXISTENTE = 999_999L;

    private final LaboratorioDAOImpl laboratorioDAO = new LaboratorioDAOImpl();
    private final EjecucionDAOImpl ejecucionDAO = new EjecucionDAOImpl();
    private final PresupuestoDAOImpl presupuestoDAO = new PresupuestoDAOImpl();
    private final FichaClinicaDAOImpl fichaDAO = new FichaClinicaDAOImpl();
    private final TarifarioService tarifario = new TarifarioService(new TarifarioDAOImpl());
    private final LaboratorioService servicio = new LaboratorioService(laboratorioDAO, ejecucionDAO,
            presupuestoDAO);
    private final EjecucionService ejecucion = new EjecucionService(ejecucionDAO, presupuestoDAO, fichaDAO);
    private final PresupuestoService presupuestos = new PresupuestoService(presupuestoDAO, tarifario,
            fichaDAO, new TratamientoDAOImpl(), new UsuarioDAOImpl(), new RolDAOImpl());

    private boolean hayBaseDeDatos() {
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM lote_insumo WHERE 1 = 0");
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
                presupuestos.descartar(previo.getId());
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

    private PresupuestoItem itemAceptadoDelDemo() throws DentrixException {
        return ejecucion.itemsEjecutables(PRESUPUESTO_DEMO).stream()
                .filter(actual -> !actual.isRequiereConsentimiento())
                .findFirst()
                .orElse(null);
    }

    private void limpiarSesion(EjecucionItem sesion) {
        if (sesion == null || sesion.getId() == null) {
            return;
        }
        try {
            for (ConsumoLote consumo : servicio.consumos(sesion.getId())) {
                servicio.quitarConsumo(consumo.getId());
            }
        } catch (DentrixException e) {
            return;
        }
        try {
            if (ejecucionDAO.buscarEjecucion(sesion.getId())
                    .filter(EjecucionItem::esBorrador).isPresent()) {
                ejecucion.descartarSesion(sesion.getId());
            }
        } catch (DentrixException e) {
            return;
        }
    }

    @Test
    void consumirExigeCantidadMayorQueCero() {
        DentrixException error = assertThrows(DentrixException.class,
                () -> servicio.consumir(1L, 1L, BigDecimal.ZERO, USUARIO_ADMIN));
        assertTrue(error.getMessage().contains("mayor que cero"),
                "La cantidad consumida debe ser positiva: " + error.getMessage());
    }

    @Test
    void solicitarSobreUnItemPropuestoFalla() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        FichaClinica ficha = fichaDelPaciente();
        assumeTrue(ficha != null, "La semilla no tiene ficha del paciente 1: se omite esta prueba");

        limpiarVigentes(ficha.getId());
        Presupuesto borrador = null;
        try {
            borrador = presupuestos.crearDesdeFicha(ficha.getId(), USUARIO_ADMIN);
            PresupuestoItem propuesto = presupuestos.agregarItem(borrador.getId(),
                    item(TRATAMIENTO_BASICO, 16));
            assertEquals("PROPUESTO", propuesto.getEstado(), "El item nuevo nace PROPUESTO");

            final Long itemId = propuesto.getId();
            DentrixException error = assertThrows(DentrixException.class,
                    () -> servicio.solicitar(itemId, null, "Laboratorio Dental Sur",
                            "Corona metal-ceramica", "Zirconio", "A2", null, BigDecimal.ZERO,
                            USUARIO_ADMIN));
            assertTrue(error.getMessage().contains("PROPUESTO"),
                    "Solo un item ACEPTADO o EN_EJECUCION admite orden (R18): " + error.getMessage());
            assertTrue(servicio.ordenes(itemId).isEmpty(),
                    "El intento fallido no debe dejar una orden registrada");
        } finally {
            if (borrador != null) {
                Presupuesto actual = presupuestos.buscar(borrador.getId());
                if ("BORRADOR".equals(actual.getEstado()) || "PRESENTADO".equals(actual.getEstado())) {
                    presupuestos.descartar(actual.getId());
                }
            }
        }
    }

    @Test
    void instalarUnaOrdenInexistenteFalla() {
        DentrixException error = assertThrows(DentrixException.class,
                () -> servicio.instalar(ORDEN_INEXISTENTE));
        assertTrue(error.getMessage().contains("no encontrada"),
                "La orden inexistente no se instala: " + error.getMessage());
    }

    @Test
    void elCaminoLimpioConsumeYQuitaUnLote() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        List<LoteInsumo> lotes = servicio.lotes();
        assumeTrue(!lotes.isEmpty(), "La semilla no tiene lotes de insumo: se omite esta prueba");
        PresupuestoItem aceptado = itemAceptadoDelDemo();
        assumeTrue(aceptado != null, "El presupuesto demo no tiene items ejecutables: se omite esta prueba");

        EjecucionItem sesion = null;
        try {
            sesion = ejecucion.abrirSesion(aceptado.getId(), "Preparacion para corona",
                    aceptado.getPiezaFdi(), "oclusal", USUARIO_ODONTOLOGO);
            assertEquals("BORRADOR", sesion.getEstado(), "La sesion nace BORRADOR");

            LoteInsumo lote = lotes.get(0);
            ConsumoLote consumo = servicio.consumir(sesion.getId(), lote.getId(),
                    new BigDecimal("2.00"), USUARIO_ADMIN);
            assertNotNull(consumo.getId(), "El consumo queda registrado con id");
            assertEquals(lote.getNumero(), consumo.getLote(), "El lote del consumo viene del join");
            assertEquals(lote.getInsumoNombre(), consumo.getInsumoNombre(),
                    "El insumo del consumo viene del join");

            List<ConsumoLote> consumos = servicio.consumos(sesion.getId());
            ConsumoLote registrado = consumos.stream()
                    .filter(actual -> consumo.getId().equals(actual.getId()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(registrado, "El consumo recien registrado debe aparecer en la sesion");
            assertEquals(0, new BigDecimal("2.00").compareTo(registrado.getCantidad()),
                    "La cantidad consumida se conserva");

            servicio.quitarConsumo(consumo.getId());
            assertFalse(servicio.consumos(sesion.getId()).stream()
                            .anyMatch(actual -> consumo.getId().equals(actual.getId())),
                    "El consumo quitado ya no aparece en la sesion");
        } finally {
            limpiarSesion(sesion);
        }
    }
}
