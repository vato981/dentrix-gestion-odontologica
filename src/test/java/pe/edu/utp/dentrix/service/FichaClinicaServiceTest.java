package pe.edu.utp.dentrix.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.impl.CitaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.DiagnosticoCie10DAOImpl;
import pe.edu.utp.dentrix.dao.impl.FichaClinicaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.FichaDiagnosticoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.HallazgoDentalDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PacienteDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PlanTratamientoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.TratamientoDAOImpl;
import pe.edu.utp.dentrix.dto.CambioPrincipal;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Cita;
import pe.edu.utp.dentrix.model.FichaClinica;
import pe.edu.utp.dentrix.model.FichaDiagnostico;
import pe.edu.utp.dentrix.model.HallazgoDental;
import pe.edu.utp.dentrix.model.Paciente;
import pe.edu.utp.dentrix.model.PlanTratamiento;
import pe.edu.utp.dentrix.model.Tratamiento;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FichaClinicaServiceTest {

    private static final String DNI_PRUEBA = "90000021";
    private static final String CODIGO_HC_PRUEBA = "HC-900021";

    private FichaClinicaService nuevoServicio() {
        return new FichaClinicaService(null, null, null, null, null, null, null);
    }

    private void limpiarPaciente(PacienteDAOImpl pacienteDAO, FichaClinicaDAOImpl fichaDAO,
                                 FichaDiagnosticoDAOImpl diagnosticoDAO, HallazgoDentalDAOImpl hallazgoDAO,
                                 PlanTratamientoDAOImpl planDAO, Integer pacienteId) throws DentrixException {
        for (PlanTratamiento linea : planDAO.listarPorPaciente(pacienteId)) {
            planDAO.eliminar(linea.getId());
        }
        for (FichaClinica ficha : fichaDAO.listarPorPaciente(pacienteId)) {
            diagnosticoDAO.eliminarPorFicha(ficha.getId());
        }
        for (HallazgoDental hallazgo : hallazgoDAO.listarPorPaciente(pacienteId)) {
            hallazgoDAO.eliminar(pacienteId, hallazgo.getPiezaFdi());
        }
        for (FichaClinica ficha : fichaDAO.listarPorPaciente(pacienteId)) {
            fichaDAO.eliminar(ficha.getId());
        }
        pacienteDAO.eliminar(pacienteId);
    }

    private Optional<HallazgoDental> hallazgoDe(FichaClinicaService service, Integer pacienteId, Integer pieza)
            throws DentrixException {
        return service.odontograma(pacienteId).stream()
                .filter(h -> h.getPiezaFdi().equals(pieza))
                .findFirst();
    }

    private void verificarCambioDePrincipal(FichaClinicaService service, Long fichaId) throws DentrixException {
        DentrixException sinMotivo = assertThrows(DentrixException.class,
                () -> service.registrarDiagnostico(fichaId, "K05.1", null, true, "Gingivitis", null));
        assertTrue(sinMotivo.getMessage().contains("motivo"),
                () -> "El cambio de principal debe exigir motivo (RF-06): " + sinMotivo.getMessage());

        service.registrarDiagnostico(fichaId, "K05.1", null, true, "Gingivitis generalizada",
                "Correccion clinica del principal");

        List<FichaDiagnostico> diagnosticos = service.listarDiagnosticos(fichaId);
        FichaDiagnostico principal = diagnosticos.stream()
                .filter(FichaDiagnostico::isEsPrincipal)
                .findFirst()
                .orElseThrow(() -> new DentrixException("La ficha quedo sin diagnostico principal"));
        assertEquals("K05.1", principal.getCie10Codigo(),
                "El principal debe ser el diagnostico nuevo");
        assertTrue(diagnosticos.stream().noneMatch(d -> "K02.1".equals(d.getCie10Codigo()) && d.isEsPrincipal()),
                "El principal anterior debe quedar como secundario");

        List<CambioPrincipal> historial = service.historialPrincipal(fichaId);
        assertEquals(1, historial.size(), "Debe quedar constancia del cambio de principal");
        assertEquals("K02.1", historial.get(0).cie10Anterior());
        assertEquals("K05.1", historial.get(0).cie10Nuevo());
        assertEquals("Correccion clinica del principal", historial.get(0).motivo());
    }

    @Test
    void crearFichaExigePacienteYOdontologo() {
        FichaClinicaService service = nuevoServicio();

        DentrixException sinPaciente = assertThrows(DentrixException.class,
                () -> service.crear(new FichaClinica()));
        assertTrue(sinPaciente.getMessage().contains("paciente"));

        FichaClinica sinOdontologo = new FichaClinica();
        sinOdontologo.setPacienteId(1);
        DentrixException excepcion = assertThrows(DentrixException.class,
                () -> service.crear(sinOdontologo));
        assertTrue(excepcion.getMessage().contains("odontologo"));
    }

    @Test
    void elOdontogramaRechazaPiezasFueraDelRangoFdi() {
        FichaClinicaService service = nuevoServicio();

        HallazgoDental fueraDeRango = new HallazgoDental(1, 99, "CARIES", null);
        DentrixException excepcion = assertThrows(DentrixException.class,
                () -> service.registrarHallazgo(fueraDeRango));
        assertTrue(excepcion.getMessage().contains("invalida"));

        HallazgoDental sinDienteValido = new HallazgoDental(1, 19, "SANO", null);
        assertThrows(DentrixException.class, () -> service.registrarHallazgo(sinDienteValido));
    }

    @Test
    void elFlujoClinicoCompletoSeEjecutaYSeRevirtes() throws Exception {
        Connection conexion = null;
        try {
            conexion = ConexionBD.abrir();
        } catch (DentrixException e) {
            conexion = null;
        }
        assumeTrue(conexion != null, "config.properties o BD no disponibles: se omite esta prueba");
        conexion.close();

        PacienteDAOImpl pacienteDAO = new PacienteDAOImpl();
        FichaClinicaDAOImpl fichaDAO = new FichaClinicaDAOImpl();
        FichaDiagnosticoDAOImpl diagnosticoDAO = new FichaDiagnosticoDAOImpl();
        HallazgoDentalDAOImpl hallazgoDAO = new HallazgoDentalDAOImpl();
        PlanTratamientoDAOImpl planDAO = new PlanTratamientoDAOImpl();
        CitaDAOImpl citaDAO = new CitaDAOImpl();
        DiagnosticoCie10DAOImpl cie10DAO = new DiagnosticoCie10DAOImpl();
        TratamientoDAOImpl tratamientoDAO = new TratamientoDAOImpl();

        FichaClinicaService service = new FichaClinicaService(fichaDAO, diagnosticoDAO, hallazgoDAO,
                planDAO, citaDAO, cie10DAO, tratamientoDAO);

        Cita citaOdontologo = citaDAO.listarTodas().stream()
                .filter(c -> "CITA".equals(c.getTipo()) && c.getUsuarioId() != null)
                .findFirst()
                .orElse(null);
        assumeTrue(citaOdontologo != null, "la semilla no tiene citas con odontologo: se omite esta prueba");

        Paciente previo = pacienteDAO.buscarPorDni(DNI_PRUEBA).orElse(null);
        if (previo != null) {
            limpiarPaciente(pacienteDAO, fichaDAO, diagnosticoDAO, hallazgoDAO, planDAO, previo.getId());
        }

        Paciente paciente = new Paciente();
        paciente.setCodigoHc(CODIGO_HC_PRUEBA);
        paciente.setDni(DNI_PRUEBA);
        paciente.setApellidos("Prueba Dental");
        paciente.setNombres("Paciente Dentrix");
        paciente.setFechaNac(LocalDate.of(1991, 4, 15));
        paciente.setFechaApertura(LocalDate.now());
        paciente.setEstado("ACTIVO");
        paciente.setFechaRegistro(LocalDateTime.now());

        Integer pacienteId = null;
        FichaClinica creada = null;
        try {
            pacienteDAO.insertar(paciente);
            Paciente guardado = pacienteDAO.buscarPorDni(DNI_PRUEBA)
                    .orElseThrow(() -> new DentrixException("El paciente de prueba no pudo crearse."));
            pacienteId = guardado.getId();

            FichaClinica fichaNueva = new FichaClinica();
            fichaNueva.setPacienteId(pacienteId);
            fichaNueva.setOdontologoId(citaOdontologo.getUsuarioId());
            creada = service.crear(fichaNueva);
            final Long idFicha = creada.getId();

            assertNotNull(idFicha, "La ficha creada debe tener id");
            assertEquals("PENDIENTE", creada.getEstadoDerivacion());
            assertFalse(creada.isDerivadoACaja());

            service.registrarDiagnostico(idFicha, "K02.1", 18, true, "Caries oclusal pieza 18", null);
            verificarCambioDePrincipal(service, idFicha);
            assertEquals(2, service.listarDiagnosticos(idFicha).size(),
                    "La ficha debe tener 2 diagnosticos (K02.1 secundario y K05.1 principal)");

            HallazgoDental hallazgo = new HallazgoDental(pacienteId, 48, "CARIES", "Caries distal de prueba");
            hallazgo.setCara("distal");
            service.registrarHallazgo(hallazgo);
            assertEquals("CARIES", hallazgoDe(service, pacienteId, 48).orElseThrow().getEstado());

            service.actualizarEstadoPieza(pacienteId, 48, "SANO", null, "Sin lesion al control");
            assertEquals("SANO", hallazgoDe(service, pacienteId, 48).orElseThrow().getEstado());

            service.eliminarHallazgo(pacienteId, 48);
            assertTrue(hallazgoDe(service, pacienteId, 48).isEmpty());

            Tratamiento trat01 = tratamientoDAO.buscarPorCodigo("TRAT-01")
                    .orElseThrow(() -> new DentrixException("El tratamiento TRAT-01 no existe en el tarifario."));
            PlanTratamiento linea = new PlanTratamiento(pacienteId, trat01.getId(), 18, 1, null, null);
            service.agregarLinea(linea);

            assertNotNull(linea.getId(), "La linea de plan debe tener id");
            assertEquals(0, new BigDecimal("80.00").compareTo(linea.getPrecioUnitario()),
                    "La linea sin precio debe tomar S/80.00 del tarifario");
            assertEquals("PLANIFICADO", linea.getEstado());

            service.marcarEjecutada(linea.getId());
            PlanTratamiento ejecutada = service.planPorPaciente(pacienteId).stream()
                    .filter(p -> p.getId().equals(linea.getId()))
                    .findFirst()
                    .orElseThrow();
            assertEquals("EJECUTADO", ejecutada.getEstado());

            service.derivarACaja(idFicha);
            assertTrue(service.esCobrable(idFicha), "La ficha derivada debe ser cobrable");
            assertEquals("ACEPTADA", service.buscarPorId(idFicha).getEstadoDerivacion());
        } finally {
            try {
                if (pacienteId != null) {
                    limpiarPaciente(pacienteDAO, fichaDAO, diagnosticoDAO, hallazgoDAO, planDAO, pacienteId);
                }
            } catch (DentrixException ignorada) {
            }
        }
    }
}
