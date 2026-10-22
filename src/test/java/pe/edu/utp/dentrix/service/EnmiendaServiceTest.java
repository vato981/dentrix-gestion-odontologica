package pe.edu.utp.dentrix.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.impl.EnmiendaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.FichaClinicaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PresupuestoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.RolDAOImpl;
import pe.edu.utp.dentrix.dao.impl.TarifarioDAOImpl;
import pe.edu.utp.dentrix.dao.impl.TratamientoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.UsuarioDAOImpl;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Enmienda;
import pe.edu.utp.dentrix.model.FichaClinica;
import pe.edu.utp.dentrix.model.Presupuesto;
import pe.edu.utp.dentrix.model.PresupuestoItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class EnmiendaServiceTest {

    private static final Integer PACIENTE_PRUEBA = 1;
    private static final Integer USUARIO_ADMIN = 1;
    private static final Integer TRATAMIENTO_BASICO = 1;

    private final PresupuestoDAOImpl presupuestoDAO = new PresupuestoDAOImpl();
    private final EnmiendaDAOImpl enmiendaDAO = new EnmiendaDAOImpl();
    private final FichaClinicaDAOImpl fichaDAO = new FichaClinicaDAOImpl();
    private final TarifarioService tarifario = new TarifarioService(new TarifarioDAOImpl());
    private final EnmiendaService servicio = new EnmiendaService(enmiendaDAO, presupuestoDAO,
            tarifario, new TratamientoDAOImpl());
    private final PresupuestoService presupuestos = new PresupuestoService(presupuestoDAO, tarifario,
            fichaDAO, new TratamientoDAOImpl(), new UsuarioDAOImpl(), new RolDAOImpl());

    private boolean hayBaseDeDatos() {
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM enmienda WHERE 1 = 0");
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

    private Presupuesto primerAceptado() throws DentrixException {
        return presupuestoDAO.listarPorEstado("ACEPTADO").stream()
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

    private void descartarBorradores(Long presupuestoId) throws DentrixException {
        Optional<Enmienda> borrador = enmiendaDAO.buscarBorrador(presupuestoId);
        if (borrador.isPresent()) {
            servicio.descartar(borrador.get().getId());
        }
    }

    private void descartarSiExiste(Enmienda enmienda) throws DentrixException {
        if (enmienda == null) {
            return;
        }
        Optional<Enmienda> actual = enmiendaDAO.buscarPorId(enmienda.getId());
        if (actual.isPresent() && actual.get().esBorrador()) {
            servicio.descartar(actual.get().getId());
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
    void abrirExigeUnPresupuestoAceptado() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        FichaClinica ficha = fichaDelPaciente();
        assumeTrue(ficha != null, "La semilla no tiene ficha del paciente 1: se omite esta prueba");

        limpiarVigentes(ficha.getId());
        Presupuesto borrador = null;
        try {
            borrador = presupuestos.crearDesdeFicha(ficha.getId(), USUARIO_ADMIN);
            final Long presupuestoId = borrador.getId();
            DentrixException noAceptado = assertThrows(DentrixException.class,
                    () -> servicio.abrir(presupuestoId, "Ampliacion de prueba", USUARIO_ADMIN));
            assertTrue(noAceptado.getMessage().contains("ACEPTADO"),
                    "Solo un presupuesto ACEPTADO se enmienda: " + noAceptado.getMessage());
            assertTrue(enmiendaDAO.buscarBorrador(presupuestoId).isEmpty(),
                    "No debe quedar un borrador de enmienda para un presupuesto no ACEPTADO");
            assertEquals("BORRADOR", presupuestos.buscar(presupuestoId).getEstado(),
                    "El presupuesto debe seguir BORRADOR tras el intento fallido");
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
    void firmarUnaEnmiendaVaciaFalla() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        Presupuesto aceptado = primerAceptado();
        assumeTrue(aceptado != null, "No hay presupuesto ACEPTADO en la semilla: se omite esta prueba");

        descartarBorradores(aceptado.getId());
        Enmienda enmienda = null;
        try {
            enmienda = servicio.abrir(aceptado.getId(), "Ampliacion de prueba", USUARIO_ADMIN);
            assertEquals("BORRADOR", enmienda.getEstado(), "La enmienda nace BORRADOR");
            assertTrue(servicio.items(enmienda.getId()).isEmpty(),
                    "La enmienda recien abierta no tiene items");

            final Long enmiendaId = enmienda.getId();
            DentrixException vacia = assertThrows(DentrixException.class,
                    () -> servicio.firmar(enmiendaId, "Juan Perez", "45123698", "PACIENTE", USUARIO_ADMIN));
            assertTrue(vacia.getMessage().contains("no hay nada que firmar"),
                    "Una enmienda vacia no se firma: " + vacia.getMessage());
            assertEquals("BORRADOR", servicio.buscar(enmiendaId).getEstado(),
                    "La enmienda vacia debe seguir BORRADOR");
        } finally {
            descartarSiExiste(enmienda);
        }
    }

    @Test
    void descartarUnBorradorNoDejaItemsNuevos() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        Presupuesto aceptado = primerAceptado();
        assumeTrue(aceptado != null, "No hay presupuesto ACEPTADO en la semilla: se omite esta prueba");

        descartarBorradores(aceptado.getId());
        int itemsAntes = presupuestoDAO.listarItems(aceptado.getId()).size();
        Enmienda enmienda = null;
        try {
            enmienda = servicio.abrir(aceptado.getId(), "Ampliacion de prueba", USUARIO_ADMIN);
            PresupuestoItem agregado = servicio.agregarItem(enmienda.getId(), item(TRATAMIENTO_BASICO, 16));
            assertEquals(enmienda.getId(), agregado.getEnmiendaId(),
                    "El item agregado queda ligado a la enmienda");
            assertEquals("PROPUESTO", agregado.getEstado(), "El item agregado nace PROPUESTO");
            assertEquals(itemsAntes + 1, presupuestoDAO.listarItems(aceptado.getId()).size(),
                    "Mientras el borrador vive, el item agregado esta en el presupuesto");

            servicio.descartar(enmienda.getId());
            enmienda = null;

            assertEquals(itemsAntes, presupuestoDAO.listarItems(aceptado.getId()).size(),
                    "Descartar el borrador no debe dejar items nuevos en el plan aceptado");
            assertTrue(enmiendaDAO.listarPorPresupuesto(aceptado.getId()).stream()
                            .noneMatch(Enmienda::esBorrador),
                    "No debe quedar borrador de enmienda tras descartarlo");
        } finally {
            descartarSiExiste(enmienda);
        }
    }
}
