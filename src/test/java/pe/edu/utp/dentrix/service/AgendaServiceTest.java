package pe.edu.utp.dentrix.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.CitaDAO;
import pe.edu.utp.dentrix.dao.PacienteDAO;
import pe.edu.utp.dentrix.dao.impl.CitaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.ConsultorioDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PacienteDAOImpl;
import pe.edu.utp.dentrix.dao.impl.TratamientoDAOImpl;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Cita;
import pe.edu.utp.dentrix.model.Paciente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AgendaServiceTest {

    private static final String DNI_TEMPORAL = "90000014";
    private static final LocalDate FECHA_TEMPORAL = LocalDate.of(2026, 12, 24);

    private final PacienteDAO pacienteDAO = new PacienteDAOImpl();
    private final CitaDAO citaDAO = new CitaDAOImpl();
    private final PacienteService pacientes = new PacienteService(pacienteDAO);
    private final AgendaService agenda = new AgendaService(citaDAO, pacienteDAO,
            new ConsultorioDAOImpl(), new TratamientoDAOImpl());

    private boolean hayBaseDeDatos() {
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM cita WHERE 1 = 0");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Integer buscarId(String sql) throws Exception {
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : null;
        }
    }

    private Paciente pacienteTemporal() {
        Paciente paciente = new Paciente();
        paciente.setDni(DNI_TEMPORAL);
        paciente.setApellidos("Pruebas Agenda");
        paciente.setNombres("Delia");
        paciente.setFechaNac(LocalDate.of(1992, 7, 21));
        paciente.setCelular("999777666");
        paciente.setEmail("temporal" + DNI_TEMPORAL + "@correo.com");
        paciente.setDireccion("Jr. Temporal 456, Huancayo");
        paciente.setAlergias("Sin alergias registradas");
        paciente.setAntecedentes("Paciente de prueba temporal de agenda");
        return paciente;
    }

    private Cita construirCita(Integer pacienteId, Integer usuarioId, Integer consultorioId) {
        Cita cita = new Cita();
        cita.setPacienteId(pacienteId);
        cita.setUsuarioId(usuarioId);
        cita.setConsultorioId(consultorioId);
        cita.setFechaCita(FECHA_TEMPORAL);
        cita.setHoraInicio(LocalTime.of(8, 0));
        cita.setHoraFin(LocalTime.of(8, 45));
        cita.setMotivo("Control de prueba temporal");
        return cita;
    }

    private void limpiar(String dni) throws DentrixException {
        Optional<Paciente> existente = pacienteDAO.buscarPorDni(dni);
        if (existente.isEmpty()) {
            return;
        }
        Integer pacienteId = existente.get().getId();
        for (Cita cita : citaDAO.listarPorPaciente(pacienteId)) {
            citaDAO.eliminar(cita.getId());
        }
        pacienteDAO.eliminar(pacienteId);
    }

    @Test
    void agendaUnaCitaDetectaElSolapamientoYAvanzaElFlujo() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        Integer consultorioId = buscarId("SELECT id FROM consultorio WHERE codigo = 'C01'");
        Integer usuarioId = buscarId("SELECT id FROM usuario ORDER BY id LIMIT 1");
        assumeTrue(consultorioId != null, "Sin consultorio C01 en la semilla: se omite esta prueba");
        assumeTrue(usuarioId != null, "Sin usuarios en la semilla: se omite esta prueba");

        limpiar(DNI_TEMPORAL);
        Paciente paciente = null;
        try {
            paciente = pacientes.registrar(pacienteTemporal());

            Cita agendada = agenda.agendar(construirCita(paciente.getId(), usuarioId, consultorioId));

            assertNotNull(agendada.getId(), "La cita debe quedar registrada con id");
            assertEquals("CITA", agendada.getTipo(), "El tipo por defecto debe ser CITA");
            assertEquals("PENDIENTE", agendada.getEstado(), "El estado inicial debe ser PENDIENTE");

            List<Cita> delDia = agenda.agendaDelDia(FECHA_TEMPORAL);
            assertTrue(delDia.stream().anyMatch(c -> agendada.getId().equals(c.getId())),
                    "La agenda del día debe incluir la cita recién agendada");

            Cita repetida = construirCita(paciente.getId(), usuarioId, consultorioId);
            DentrixException solapamiento = assertThrows(DentrixException.class,
                    () -> agenda.agendar(repetida));
            assertTrue(solapamiento.getMessage().contains("consultorio"),
                    "Debe avisar el solapamiento del consultorio: " + solapamiento.getMessage());

            agenda.marcarLlegada(agendada.getId());
            assertEquals("EN_SALA",
                    citaDAO.buscarPorId(agendada.getId()).orElseThrow().getEstado(),
                    "marcarLlegada debe llevar la cita a EN_SALA");

            agenda.cancelar(agendada.getId());
            assertEquals("CANCELADA",
                    citaDAO.buscarPorId(agendada.getId()).orElseThrow().getEstado(),
                    "cancelar debe llevar la cita a CANCELADA");
        } finally {
            try {
                limpiar(DNI_TEMPORAL);
            } catch (DentrixException ignorada) {
            }
        }
    }
}
