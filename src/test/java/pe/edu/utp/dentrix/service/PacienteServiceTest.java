package pe.edu.utp.dentrix.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.PacienteDAO;
import pe.edu.utp.dentrix.dao.impl.PacienteDAOImpl;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Paciente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PacienteServiceTest {

    private final PacienteDAO pacienteDAO = new PacienteDAOImpl();
    private final PacienteService servicio = new PacienteService(pacienteDAO);

    private boolean hayBaseDeDatos() {
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM paciente WHERE 1 = 0");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Paciente pacienteTemporal(String dni, String apellidos, String nombres) {
        Paciente paciente = new Paciente();
        paciente.setDni(dni);
        paciente.setApellidos(apellidos);
        paciente.setNombres(nombres);
        paciente.setFechaNac(LocalDate.of(1995, 3, 15));
        paciente.setCelular("999888777");
        paciente.setEmail("temporal" + dni + "@correo.com");
        paciente.setDireccion("Av. Temporal 123, Chilca - Huancayo");
        paciente.setAlergias("Sin alergias registradas");
        paciente.setAntecedentes("Paciente de prueba temporal");
        return paciente;
    }

    private void limpiar(String dni) throws DentrixException {
        Optional<Paciente> existente = pacienteDAO.buscarPorDni(dni);
        if (existente.isPresent()) {
            pacienteDAO.eliminar(existente.get().getId());
        }
    }

    @Test
    void registraUnPacienteTemporalYGeneraElCodigoHc() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        limpiar("90000011");
        Paciente creado = null;
        try {
            creado = servicio.registrar(pacienteTemporal("90000011", "Pruebas Temporal", "Augusto"));

            assertNotNull(creado.getId(), "El paciente debe quedar registrado con id");
            assertNotNull(creado.getCodigoHc(), "El código de historia clínica es obligatorio");
            assertTrue(creado.getCodigoHc().startsWith("HC-"),
                    "El código debe empezar con HC-: " + creado.getCodigoHc());
            assertTrue(creado.getCodigoHc().matches("HC-\\d{6}"),
                    "El código debe tener 6 dígitos: " + creado.getCodigoHc());
            assertEquals("ACTIVO", creado.getEstado(), "El estado inicial debe ser ACTIVO");
            assertEquals(LocalDate.now(), creado.getFechaApertura(), "La fecha de apertura debe ser hoy");

            Paciente guardado = pacienteDAO.buscarPorId(creado.getId()).orElseThrow();
            assertEquals("90000011", guardado.getDni());
            assertEquals(creado.getCodigoHc(), guardado.getCodigoHc());
        } finally {
            if (creado != null && creado.getId() != null) {
                pacienteDAO.eliminar(creado.getId());
            }
        }
    }

    @Test
    void rechazaElRegistroConDniDuplicado() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        limpiar("90000012");
        Paciente creado = null;
        try {
            creado = servicio.registrar(pacienteTemporal("90000012", "Pruebas Temporal", "Blanca"));

            DentrixException error = assertThrows(DentrixException.class,
                    () -> servicio.registrar(pacienteTemporal("90000012", "Otro Apellido", "Otro Nombre")));
            assertTrue(error.getMessage().contains("90000012"),
                    "El mensaje debe indicar el DNI repetido: " + error.getMessage());
            assertTrue(error.getMessage().contains("RF-04"),
                    "El mensaje debe invocar el RF-04: " + error.getMessage());
        } finally {
            if (creado != null && creado.getId() != null) {
                pacienteDAO.eliminar(creado.getId());
            }
        }
    }

    @Test
    void buscaUnPacientePorApellido() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        limpiar("90000013");
        Paciente creado = null;
        try {
            creado = servicio.registrar(pacienteTemporal("90000013", "ZzzzApellidoUnico", "Zoe"));

            List<Paciente> porApellido = servicio.buscar("ZzzzApellidoUnico");
            assertTrue(porApellido.stream().anyMatch(p -> "90000013".equals(p.getDni())),
                    "La búsqueda por apellido debe encontrar al paciente temporal");

            List<Paciente> porDni = servicio.buscar("90000013");
            Integer creadoId = creado.getId();
            assertTrue(porDni.stream().anyMatch(p -> creadoId.equals(p.getId())),
                    "La búsqueda por DNI debe encontrar al paciente temporal");
        } finally {
            if (creado != null && creado.getId() != null) {
                pacienteDAO.eliminar(creado.getId());
            }
        }
    }

    @Test
    void inactivaElPacienteSinPerderElExpediente() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        limpiar("90000015");
        Paciente creado = null;
        try {
            creado = servicio.registrar(pacienteTemporal("90000015", "Pruebas Temporal", "Rosa"));

            servicio.inactivar(creado.getId());

            Paciente consultado = servicio.buscarPorId(creado.getId());
            assertEquals("INACTIVO", consultado.getEstado(), "La baja debe ser lógica (RF-04)");
            assertEquals(creado.getCodigoHc(), consultado.getCodigoHc(),
                    "El expediente no debe perderse al inactivar");
        } finally {
            if (creado != null && creado.getId() != null) {
                pacienteDAO.eliminar(creado.getId());
            }
        }
    }
}
