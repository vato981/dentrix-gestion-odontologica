package pe.edu.utp.dentrix.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.impl.EjecucionDAOImpl;
import pe.edu.utp.dentrix.dao.impl.FichaClinicaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PresupuestoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.RolDAOImpl;
import pe.edu.utp.dentrix.dao.impl.TarifarioDAOImpl;
import pe.edu.utp.dentrix.dao.impl.TratamientoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.UsuarioDAOImpl;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.EjecucionItem;
import pe.edu.utp.dentrix.model.FichaClinica;
import pe.edu.utp.dentrix.model.Presupuesto;
import pe.edu.utp.dentrix.model.PresupuestoItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class EjecucionServiceTest {

    private static final Integer PACIENTE_PRUEBA = 1;
    private static final Integer USUARIO_ADMIN = 1;
    private static final Integer USUARIO_ODONTOLOGO = 2;
    private static final Long PRESUPUESTO_DEMO = 53L;
    private static final Integer TRATAMIENTO_BASICO = 1;

    private final EjecucionDAOImpl ejecucionDAO = new EjecucionDAOImpl();
    private final PresupuestoDAOImpl presupuestoDAO = new PresupuestoDAOImpl();
    private final FichaClinicaDAOImpl fichaDAO = new FichaClinicaDAOImpl();
    private final TarifarioService tarifario = new TarifarioService(new TarifarioDAOImpl());
    private final EjecucionService servicio = new EjecucionService(ejecucionDAO, presupuestoDAO, fichaDAO);
    private final PresupuestoService presupuestos = new PresupuestoService(presupuestoDAO, tarifario,
            fichaDAO, new TratamientoDAOImpl(), new UsuarioDAOImpl(), new RolDAOImpl());

    private boolean hayBaseDeDatos() {
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM ejecucion_item WHERE 1 = 0");
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
        List<PresupuestoItem> ejecutables = servicio.itemsEjecutables(PRESUPUESTO_DEMO);
        return ejecutables.stream()
                .filter(item -> !item.isRequiereConsentimiento())
                .findFirst()
                .orElse(ejecutables.isEmpty() ? null : ejecutables.get(0));
    }

    private void descartarSiEsBorrador(EjecucionItem sesion) throws DentrixException {
        if (sesion == null || sesion.getId() == null) {
            return;
        }
        if (ejecucionDAO.buscarEjecucion(sesion.getId())
                .filter(EjecucionItem::esBorrador).isPresent()) {
            servicio.descartarSesion(sesion.getId());
        }
    }

    @Test
    void abrirSesionExigeUnItemAceptado() throws Exception {
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
            DentrixException noEjecutable = assertThrows(DentrixException.class,
                    () -> servicio.abrirSesion(itemId, "Apertura", 16, "oclusal", USUARIO_ODONTOLOGO));
            assertTrue(noEjecutable.getMessage().contains("PROPUESTO"),
                    "Solo un item ACEPTADO o EN_EJECUCION abre sesion: " + noEjecutable.getMessage());
            assertTrue(servicio.sesiones(itemId).isEmpty(),
                    "El intento fallido no debe dejar una sesion registrada");
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
    void firmarSesionExigeTexto() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        PresupuestoItem aceptado = itemAceptadoDelDemo();
        assumeTrue(aceptado != null, "El presupuesto demo no tiene items ejecutables: se omite esta prueba");

        EjecucionItem sesion = null;
        try {
            sesion = servicio.abrirSesion(aceptado.getId(), "Apertura", aceptado.getPiezaFdi(),
                    "oclusal", USUARIO_ODONTOLOGO);
            final Long sesionId = sesion.getId();
            DentrixException sinTexto = assertThrows(DentrixException.class,
                    () -> servicio.firmarSesion(sesionId, "   ", USUARIO_ODONTOLOGO));
            assertTrue(sinTexto.getMessage().contains("texto"),
                    "La evolucion exige texto: " + sinTexto.getMessage());
            assertEquals("BORRADOR", ejecucionDAO.buscarEjecucion(sesionId)
                            .map(EjecucionItem::getEstado).orElse(null),
                    "La sesion debe seguir BORRADOR tras el intento fallido");
            assertTrue(servicio.evolucionDeSesion(sesionId).isEmpty(),
                    "El intento fallido no debe dejar una evolucion");
        } finally {
            descartarSiEsBorrador(sesion);
        }
    }

    @Test
    void programarControlExigeUnItemEjecutado() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        PresupuestoItem aceptado = itemAceptadoDelDemo();
        assumeTrue(aceptado != null, "El presupuesto demo no tiene items ejecutables: se omite esta prueba");

        final Long itemId = aceptado.getId();
        DentrixException noEjecutado = assertThrows(DentrixException.class,
                () -> servicio.programarControl(itemId, LocalDate.now().plusDays(7), USUARIO_ODONTOLOGO));
        assertTrue(noEjecutado.getMessage().contains("EJECUTADO"),
                "El control post exige un item EJECUTADO: " + noEjecutado.getMessage());
        assertTrue(ejecucionDAO.controlPendiente(itemId).isEmpty(),
                "El intento fallido no debe dejar un control programado");
    }

    @Test
    void cerrarTerminadoConItemsAbiertosFalla() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        assumeTrue("ACEPTADO".equals(presupuestoDAO.buscarPorId(PRESUPUESTO_DEMO)
                        .map(Presupuesto::getEstado).orElse(null)),
                "El presupuesto demo 53 debe estar ACEPTADO: se omite esta prueba");
        assumeTrue(servicio.cierre(PRESUPUESTO_DEMO).isEmpty(),
                "El presupuesto demo ya tiene cierre: se omite esta prueba");

        DentrixException abierto = assertThrows(DentrixException.class,
                () -> servicio.cerrarPlan(PRESUPUESTO_DEMO, "TERMINADO", "Todo listo",
                        LocalDate.now().plusDays(180), USUARIO_ADMIN));
        assertTrue(abierto.getMessage().contains("R17"),
                "Un plan con items abiertos no se cierra TERMINADO: " + abierto.getMessage());
        assertTrue(servicio.cierre(PRESUPUESTO_DEMO).isEmpty(),
                "El intento fallido no debe dejar un cierre registrado");
    }

    @Test
    void elCaminoLimpioAbreYDescartaUnBorrador() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        PresupuestoItem aceptado = itemAceptadoDelDemo();
        assumeTrue(aceptado != null, "El presupuesto demo no tiene items ejecutables: se omite esta prueba");

        EjecucionItem sesion = null;
        try {
            sesion = servicio.abrirSesion(aceptado.getId(), "Apertura y conductometria",
                    aceptado.getPiezaFdi(), "oclusal", USUARIO_ODONTOLOGO);
            assertNotNull(sesion.getId(), "La sesion debe quedar registrada con id");
            assertEquals("BORRADOR", sesion.getEstado(), "La sesion nace BORRADOR");
            assertEquals(aceptado.getId(), sesion.getPresupuestoItemId(),
                    "La sesion queda ligada al item");
            assertEquals(aceptado.getSesionesPlanificadas(), sesion.getSesionesPlanificadas(),
                    "Las sesiones planificadas las fija el trigger desde el item");
            assertTrue(sesion.getNumeroSesion() >= 1, "La sesion tiene numero n de m");
            assertNotNull(sesion.getProfesionalNombre(), "El nombre del profesional viene del join");
            assertFalse(sesion.getProfesionalNombre().isBlank(),
                    "El nombre del profesional no debe venir vacio");

            EjecucionItem reabierta = servicio.abrirSesion(aceptado.getId(), "Otra apertura",
                    aceptado.getPiezaFdi(), null, USUARIO_ODONTOLOGO);
            assertEquals(sesion.getId(), reabierta.getId(),
                    "Si ya hay un BORRADOR, abrirSesion devuelve el mismo");

            final Long sesionId = sesion.getId();
            servicio.descartarSesion(sesionId);
            sesion = null;

            assertTrue(ejecucionDAO.buscarEjecucion(sesionId).isEmpty(),
                    "Descartar el borrador no debe dejar la sesion registrada");
            assertEquals("ACEPTADO", presupuestoDAO.buscarItem(aceptado.getId())
                            .map(PresupuestoItem::getEstado).orElse(null),
                    "Descartar el borrador no mueve el estado del plan demo");
        } finally {
            descartarSiEsBorrador(sesion);
        }
    }
}
