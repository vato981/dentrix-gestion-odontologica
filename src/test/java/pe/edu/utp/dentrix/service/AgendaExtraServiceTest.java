package pe.edu.utp.dentrix.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.impl.AgendaExtraDAOImpl;
import pe.edu.utp.dentrix.dao.impl.CitaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.ConsultorioDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PacienteDAOImpl;
import pe.edu.utp.dentrix.dao.impl.TratamientoDAOImpl;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.ListaEspera;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AgendaExtraServiceTest {

    private static final Integer PACIENTE = 4;
    private static final Integer USUARIO = 3;
    private static final String VERSION_PRUEBA = "2026-PRUEBA";

    private final AgendaService agenda = new AgendaService(new CitaDAOImpl(), new PacienteDAOImpl(),
            new ConsultorioDAOImpl(), new TratamientoDAOImpl(), new AgendaExtraDAOImpl());

    private boolean hayBaseDeDatos() {
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM lista_espera WHERE 1 = 0");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean existePaciente() {
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM paciente WHERE id = ?")) {
            ps.setObject(1, PACIENTE);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void limpiarEspera() throws DentrixException {
        for (ListaEspera espera : agenda.listaEspera()) {
            if (PACIENTE.equals(espera.getPacienteId())) {
                agenda.retirarEspera(espera.getId(), "Limpieza de prueba");
            }
        }
    }

    @Test
    void agregaYRetiraUnaEsperaDelPacienteDePrueba() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        assumeTrue(existePaciente(), "La semilla no tiene el paciente 4: se omite esta prueba");

        limpiarEspera();
        ListaEspera creada = agenda.agregarEspera(PACIENTE, null, 2,
                "Espera de prueba automatizada", USUARIO);
        assertNotNull(creada.getId(), "La espera debe quedar registrada con id");
        assertTrue("ESPERANDO".equals(creada.getEstado()), "La espera nace ESPERANDO");

        List<ListaEspera> esperando = agenda.listaEspera();
        assertTrue(esperando.stream().anyMatch(e -> creada.getId().equals(e.getId())),
                "La espera recien creada debe aparecer en la lista de espera");

        agenda.retirarEspera(creada.getId(), "Prueba terminada");
        assertFalse(agenda.listaEspera().stream().anyMatch(e -> creada.getId().equals(e.getId())),
                "La espera retirada ya no aparece como ESPERANDO");
    }

    @Test
    void programarUnRecordatorioConFechaPasadaFalla() {
        DentrixException error = assertThrows(DentrixException.class,
                () -> agenda.programarRecordatorio(1L, "WHATSAPP",
                        LocalDateTime.now().minusHours(1), USUARIO));
        assertTrue(error.getMessage().contains("futuro"),
                "El recordatorio debe programarse en el futuro: " + error.getMessage());
    }

    @Test
    void registrarUnRechazoDeConsentimientoSinMotivoFalla() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        assumeTrue(existePaciente(), "La semilla no tiene el paciente 4: se omite esta prueba");

        DentrixException error = assertThrows(DentrixException.class,
                () -> agenda.registrarConsentimientoDatos(PACIENTE, VERSION_PRUEBA, false,
                        "PAPEL", null, "  "));
        assertTrue(error.getMessage().contains("motivo"),
                "El rechazo del consentimiento exige motivo: " + error.getMessage());

        assertTrue(agenda.consentimientoDatos(PACIENTE)
                        .map(c -> !VERSION_PRUEBA.equals(c.getVersion()))
                        .orElse(true),
                "El rechazo invalido no debe dejar un consentimiento registrado");
    }
}
