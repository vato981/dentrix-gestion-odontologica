package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.CitaHistorial;
import pe.edu.utp.dentrix.model.ConsentimientoDatos;
import pe.edu.utp.dentrix.model.ListaEspera;
import pe.edu.utp.dentrix.model.Recordatorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AgendaExtraDAO {

    List<ListaEspera> listarEspera(String estado) throws DentrixException;

    Long insertarEspera(ListaEspera espera) throws DentrixException;

    boolean retirarEspera(Long id, String motivo) throws DentrixException;

    boolean marcarEsperaAgendada(Long id, Long citaId) throws DentrixException;

    List<Recordatorio> listarRecordatorios(Long citaId) throws DentrixException;

    Long insertarRecordatorio(Recordatorio recordatorio) throws DentrixException;

    boolean actualizarRecordatorio(Long id, String estado, String respuesta, LocalDateTime enviado)
            throws DentrixException;

    List<CitaHistorial> listarHistorial(Long citaId) throws DentrixException;

    int inasistenciasRecientes(Integer pacienteId) throws DentrixException;

    Long reprogramar(Long citaId, LocalDate fecha, LocalTime hora, Integer consultorioId,
                     Integer usuarioId, String motivo) throws DentrixException;

    Long insertarConsentimientoDatos(ConsentimientoDatos consentimiento) throws DentrixException;

    Optional<ConsentimientoDatos> consentimientoDatos(Integer pacienteId, String version)
            throws DentrixException;
}
