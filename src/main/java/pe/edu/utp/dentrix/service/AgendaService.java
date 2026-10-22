package pe.edu.utp.dentrix.service;

import pe.edu.utp.dentrix.dao.AgendaExtraDAO;
import pe.edu.utp.dentrix.dao.CitaDAO;
import pe.edu.utp.dentrix.dao.ConsultorioDAO;
import pe.edu.utp.dentrix.dao.PacienteDAO;
import pe.edu.utp.dentrix.dao.TratamientoDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Cita;
import pe.edu.utp.dentrix.model.CitaHistorial;
import pe.edu.utp.dentrix.model.ConsentimientoDatos;
import pe.edu.utp.dentrix.model.ListaEspera;
import pe.edu.utp.dentrix.model.Recordatorio;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AgendaService {

    private static final Set<String> TIPOS_BLOQUE = Set.of("ALMUERZO", "CIERRE", "BLOQUEADO");
    private static final Set<String> ESTADOS_CANCELABLES = Set.of("PENDIENTE", "CONFIRMADA", "EN_SALA");
    private static final Set<String> CANALES_RECORDATORIO = Set.of("WHATSAPP", "SMS", "LLAMADA", "EMAIL");
    private static final Set<String> ESTADOS_RECORDATORIO =
            Set.of("PENDIENTE", "ENVIADO", "FALLIDO", "CONFIRMADO", "RECHAZADO");
    private static final Set<String> ESTADOS_RECORDABLES = Set.of("PENDIENTE", "CONFIRMADA");

    private final CitaDAO citaDAO;
    private final PacienteDAO pacienteDAO;
    private final ConsultorioDAO consultorioDAO;
    private final TratamientoDAO tratamientoDAO;
    private final AgendaExtraDAO agendaExtraDAO;

    public AgendaService(CitaDAO citaDAO, PacienteDAO pacienteDAO,
                         ConsultorioDAO consultorioDAO, TratamientoDAO tratamientoDAO,
                         AgendaExtraDAO agendaExtraDAO) {
        this.citaDAO = citaDAO;
        this.pacienteDAO = pacienteDAO;
        this.consultorioDAO = consultorioDAO;
        this.tratamientoDAO = tratamientoDAO;
        this.agendaExtraDAO = agendaExtraDAO;
    }

    public List<Cita> agendaDelDia(LocalDate fecha) throws DentrixException {
        if (fecha == null) {
            throw new DentrixException("La fecha de la agenda es obligatoria");
        }
        List<Cita> citas = new ArrayList<>(citaDAO.listarPorFecha(fecha));
        citas.sort(Comparator.comparing(Cita::getHoraInicio,
                Comparator.nullsLast(java.time.LocalTime::compareTo)));
        return citas;
    }

    public Cita agendar(Cita cita) throws DentrixException {
        try {
            if (cita == null) {
                throw new DentrixException("Los datos de la cita son obligatorios");
            }
            if (cita.getTipo() == null) {
                cita.setTipo("CITA");
            } else if (!"CITA".equals(cita.getTipo())) {
                throw new DentrixException(
                        "Para agendar bloques use el método bloquear (tipo " + cita.getTipo() + ")");
            }
            validarHorario(cita);
            validarPaciente(cita);
            validarTratamiento(cita);
            validarSolapamiento(cita);
            cita.setEstado("PENDIENTE");
            int id = citaDAO.insertar(cita);
            cita.setId((long) id);
            return cita;
        } catch (DentrixException e) {
            throw traducirCita(e, cita);
        }
    }

    public Cita bloquear(Cita cita) throws DentrixException {
        try {
            if (cita == null) {
                throw new DentrixException("Los datos del bloque son obligatorios");
            }
            if (cita.getTipo() == null || !TIPOS_BLOQUE.contains(cita.getTipo())) {
                throw new DentrixException("Tipo de bloque no válido: use ALMUERZO, CIERRE o BLOQUEADO");
            }
            if (cita.getMotivo() == null || cita.getMotivo().isBlank()) {
                throw new DentrixException("El motivo del bloque es obligatorio");
            }
            cita.setPacienteId(null);
            validarHorario(cita);
            cita.setEstado("BLOQUEADA");
            int id = citaDAO.insertar(cita);
            cita.setId((long) id);
            return cita;
        } catch (DentrixException e) {
            throw traducirCita(e, cita);
        }
    }

    public void confirmar(Long id) throws DentrixException {
        cambiarEstado(id, "CONFIRMADA", Set.of("PENDIENTE"));
    }

    public void marcarLlegada(Long id) throws DentrixException {
        cambiarEstado(id, "EN_SALA", Set.of("PENDIENTE", "CONFIRMADA"));
    }

    public void iniciarAtencion(Long id) throws DentrixException {
        cambiarEstado(id, "EN_ATENCION", Set.of("EN_SALA"));
    }

    public void finalizar(Long id) throws DentrixException {
        cambiarEstado(id, "ATENDIDA", Set.of("EN_ATENCION"));
    }

    public void marcarFalto(Long id) throws DentrixException {
        if (id == null) {
            throw new DentrixException("La cita es obligatoria");
        }
        try {
            Cita cita = citaDAO.buscarPorId(id)
                    .orElseThrow(() -> new DentrixException("Cita no encontrada"));
            if (!"CITA".equals(cita.getTipo())) {
                throw new DentrixException("Las transiciones de estado solo aplican a citas tipo CITA");
            }
            if (!Set.of("PENDIENTE", "CONFIRMADA").contains(cita.getEstado())) {
                throw new DentrixException("Solo se marca la inasistencia de una cita PENDIENTE o "
                        + "CONFIRMADA; la cita esta en estado " + cita.getEstado());
            }
            if (cita.getFechaCita() == null || cita.getHoraInicio() == null) {
                throw new DentrixException(
                        "La cita no tiene fecha u hora para validar la tolerancia de 15 minutos");
            }
            LocalDateTime inicio = LocalDateTime.of(cita.getFechaCita(), cita.getHoraInicio());
            if (LocalDateTime.now().isBefore(inicio.plusMinutes(15))) {
                throw new DentrixException("Todavia esta dentro de la tolerancia de 15 minutos; "
                        + "no se marca la inasistencia");
            }
            cambiarEstado(id, "FALTO", Set.of("PENDIENTE", "CONFIRMADA"));
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public void cancelar(Long id) throws DentrixException {
        cambiarEstado(id, "CANCELADA", ESTADOS_CANCELABLES);
    }

    public List<ListaEspera> listaEspera() throws DentrixException {
        try {
            return agendaExtraDAO.listarEspera("ESPERANDO");
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public ListaEspera agregarEspera(Integer pacienteId, Integer tratamientoId, Integer prioridad,
                                     String nota, Integer usuarioId) throws DentrixException {
        if (pacienteId == null) {
            throw new DentrixException("El paciente es obligatorio para la lista de espera");
        }
        if (usuarioId == null) {
            throw new DentrixException("El usuario que registra la espera es obligatorio");
        }
        if (pacienteDAO.buscarPorId(pacienteId).isEmpty()) {
            throw new DentrixException("Paciente no encontrado");
        }
        if (tratamientoId != null && tratamientoDAO.buscarPorId(tratamientoId).isEmpty()) {
            throw new DentrixException("Tratamiento no encontrado");
        }
        int orden = prioridad == null ? 3 : prioridad;
        if (orden < 1 || orden > 5) {
            throw new DentrixException("La prioridad debe estar entre 1 y 5");
        }
        ListaEspera espera = new ListaEspera();
        espera.setPacienteId(pacienteId);
        espera.setTratamientoId(tratamientoId);
        espera.setPrioridad(orden);
        espera.setNota(limpiar(nota));
        espera.setEstado("ESPERANDO");
        espera.setUsuarioId(usuarioId);
        try {
            agendaExtraDAO.insertarEspera(espera);
            return espera;
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public void retirarEspera(Long id, String motivo) throws DentrixException {
        if (id == null) {
            throw new DentrixException("La espera es obligatoria");
        }
        String limpio = limpiar(motivo);
        if (limpio == null) {
            throw new DentrixException("El motivo del retiro es obligatorio");
        }
        try {
            if (!agendaExtraDAO.retirarEspera(id, limpio)) {
                throw new DentrixException("La espera no existe o ya no esta ESPERANDO");
            }
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public void marcarEsperaAgendada(Long id, Long citaId) throws DentrixException {
        if (id == null) {
            throw new DentrixException("La espera es obligatoria");
        }
        if (citaId == null) {
            throw new DentrixException("La cita asignada es obligatoria");
        }
        try {
            if (!agendaExtraDAO.marcarEsperaAgendada(id, citaId)) {
                throw new DentrixException("La espera no existe o ya no esta ESPERANDO");
            }
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public List<Recordatorio> recordatorios(Long citaId) throws DentrixException {
        if (citaId == null) {
            throw new DentrixException("La cita es obligatoria");
        }
        try {
            return agendaExtraDAO.listarRecordatorios(citaId);
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public Recordatorio programarRecordatorio(Long citaId, String canal, LocalDateTime cuando,
                                              Integer usuarioId) throws DentrixException {
        if (citaId == null) {
            throw new DentrixException("La cita es obligatoria");
        }
        if (cuando == null) {
            throw new DentrixException("La fecha del recordatorio es obligatoria");
        }
        if (!cuando.isAfter(LocalDateTime.now())) {
            throw new DentrixException("El recordatorio debe programarse en el futuro");
        }
        String canalLimpio = limpiar(canal);
        if (canalLimpio == null || !CANALES_RECORDATORIO.contains(canalLimpio)) {
            throw new DentrixException("Canal no valido: use WHATSAPP, SMS, LLAMADA o EMAIL");
        }
        if (usuarioId == null) {
            throw new DentrixException("El usuario que programa el recordatorio es obligatorio");
        }
        Cita cita = citaDAO.buscarPorId(citaId)
                .orElseThrow(() -> new DentrixException("Cita no encontrada"));
        if (!ESTADOS_RECORDABLES.contains(cita.getEstado())) {
            throw new DentrixException("Solo se programa recordatorio para una cita PENDIENTE o CONFIRMADA");
        }
        Recordatorio recordatorio = new Recordatorio();
        recordatorio.setCitaId(citaId);
        recordatorio.setCanal(canalLimpio);
        recordatorio.setProgramado(cuando);
        recordatorio.setEstado("PENDIENTE");
        recordatorio.setUsuarioId(usuarioId);
        try {
            agendaExtraDAO.insertarRecordatorio(recordatorio);
            return recordatorio;
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public void marcarRecordatorio(Long id, String estado, String respuesta) throws DentrixException {
        if (id == null) {
            throw new DentrixException("El recordatorio es obligatorio");
        }
        String limpio = limpiar(estado);
        if (limpio == null || !ESTADOS_RECORDATORIO.contains(limpio)) {
            throw new DentrixException("Estado de recordatorio no valido");
        }
        LocalDateTime enviado = "PENDIENTE".equals(limpio) ? null : LocalDateTime.now();
        try {
            if (!agendaExtraDAO.actualizarRecordatorio(id, limpio, limpiar(respuesta), enviado)) {
                throw new DentrixException("El recordatorio no existe");
            }
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public List<CitaHistorial> historial(Long citaId) throws DentrixException {
        if (citaId == null) {
            throw new DentrixException("La cita es obligatoria");
        }
        try {
            return agendaExtraDAO.listarHistorial(citaId);
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public int inasistencias(Integer pacienteId) throws DentrixException {
        if (pacienteId == null) {
            throw new DentrixException("El paciente es obligatorio");
        }
        try {
            return agendaExtraDAO.inasistenciasRecientes(pacienteId);
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public Cita reprogramar(Long citaId, LocalDate fecha, LocalTime hora, Integer consultorioId,
                            String motivo, Integer usuarioId) throws DentrixException {
        if (citaId == null) {
            throw new DentrixException("La cita a reprogramar es obligatoria");
        }
        if (fecha == null) {
            throw new DentrixException("La nueva fecha es obligatoria");
        }
        if (hora == null) {
            throw new DentrixException("La nueva hora es obligatoria");
        }
        if (usuarioId == null) {
            throw new DentrixException("El usuario que reprograma es obligatorio");
        }
        if (consultorioId != null && consultorioDAO.buscarPorId(consultorioId).isEmpty()) {
            throw new DentrixException("Consultorio no encontrado");
        }
        String limpio = limpiar(motivo);
        String motivoFinal = limpio == null ? "Reprogramacion" : limpio;
        try {
            Long nuevaId = agendaExtraDAO.reprogramar(citaId, fecha, hora, consultorioId,
                    usuarioId, motivoFinal);
            return citaDAO.buscarPorId(nuevaId)
                    .orElseThrow(() -> new DentrixException("No se encontro la cita reprogramada"));
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public ConsentimientoDatos registrarConsentimientoDatos(Integer pacienteId, String version,
                                                            boolean aceptado, String medio,
                                                            String evidencia, String motivoRechazo)
            throws DentrixException {
        if (pacienteId == null) {
            throw new DentrixException("El paciente es obligatorio");
        }
        if (pacienteDAO.buscarPorId(pacienteId).isEmpty()) {
            throw new DentrixException("Paciente no encontrado");
        }
        String versionLimpia = limpiar(version);
        if (versionLimpia == null) {
            throw new DentrixException("La version del consentimiento es obligatoria");
        }
        String medioLimpio = limpiar(medio);
        if (medioLimpio == null) {
            throw new DentrixException("El medio del consentimiento es obligatorio");
        }
        String rechazo = limpiar(motivoRechazo);
        if (!aceptado && rechazo == null) {
            throw new DentrixException("El rechazo del consentimiento exige un motivo");
        }
        ConsentimientoDatos consentimiento = new ConsentimientoDatos();
        consentimiento.setPacienteId(pacienteId);
        consentimiento.setVersion(versionLimpia);
        consentimiento.setAceptado(aceptado);
        consentimiento.setMedio(medioLimpio);
        consentimiento.setEvidencia(limpiar(evidencia));
        consentimiento.setMotivoRechazo(rechazo);
        try {
            agendaExtraDAO.insertarConsentimientoDatos(consentimiento);
            return agendaExtraDAO.consentimientoDatos(pacienteId, versionLimpia).orElse(consentimiento);
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public Optional<ConsentimientoDatos> consentimientoDatos(Integer pacienteId) throws DentrixException {
        if (pacienteId == null) {
            throw new DentrixException("El paciente es obligatorio");
        }
        try {
            return agendaExtraDAO.consentimientoDatos(pacienteId, null);
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    private String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private void cambiarEstado(Long id, String destino, Set<String> estadosPermitidos) throws DentrixException {
        if (id == null) {
            throw new DentrixException("La cita es obligatoria");
        }
        try {
            Cita cita = citaDAO.buscarPorId(id)
                    .orElseThrow(() -> new DentrixException("Cita no encontrada"));
            if (!"CITA".equals(cita.getTipo())) {
                throw new DentrixException("Las transiciones de estado solo aplican a citas tipo CITA");
            }
            if (!estadosPermitidos.contains(cita.getEstado())) {
                throw new DentrixException(
                        "La cita está en estado " + cita.getEstado() + "; no puede pasar a " + destino);
            }
            cita.setEstado(destino);
            if (!citaDAO.actualizar(cita)) {
                throw new DentrixException("No se pudo actualizar la cita");
            }
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    private void validarHorario(Cita cita) throws DentrixException {
        if (cita.getConsultorioId() == null) {
            throw new DentrixException("El consultorio es obligatorio");
        }
        if (consultorioDAO.buscarPorId(cita.getConsultorioId()).isEmpty()) {
            throw new DentrixException("Consultorio no encontrado");
        }
        if (cita.getUsuarioId() == null) {
            throw new DentrixException("El odontólogo responsable es obligatorio");
        }
        if (cita.getFechaCita() == null) {
            throw new DentrixException("La fecha de la cita es obligatoria");
        }
        if (cita.getHoraInicio() == null || cita.getHoraFin() == null) {
            throw new DentrixException("La hora de inicio y la hora de fin son obligatorias");
        }
        if (!cita.getHoraFin().isAfter(cita.getHoraInicio())) {
            throw new DentrixException("La hora de fin debe ser mayor a la hora de inicio");
        }
    }

    private void validarPaciente(Cita cita) throws DentrixException {
        if (cita.getPacienteId() == null) {
            throw new DentrixException("El paciente es obligatorio para agendar una cita");
        }
        if (pacienteDAO.buscarPorId(cita.getPacienteId()).isEmpty()) {
            throw new DentrixException("Paciente no encontrado");
        }
    }

    private void validarTratamiento(Cita cita) throws DentrixException {
        if (cita.getTratamientoId() != null && tratamientoDAO.buscarPorId(cita.getTratamientoId()).isEmpty()) {
            throw new DentrixException("Tratamiento no encontrado");
        }
    }

    private void validarSolapamiento(Cita cita) throws DentrixException {
        for (Cita otra : citaDAO.listarPorFechaYConsultorio(cita.getFechaCita(), cita.getConsultorioId())) {
            if (cita.getHoraInicio().equals(otra.getHoraInicio()) && !"CANCELADA".equals(otra.getEstado())) {
                throw new DentrixException("El consultorio ya tiene una cita a las " + cita.getHoraInicio());
            }
        }
        for (Cita otra : citaDAO.listarPorPacienteEnFecha(cita.getPacienteId(), cita.getFechaCita())) {
            if (cita.getHoraInicio().equals(otra.getHoraInicio())
                    && !"CANCELADA".equals(otra.getEstado())) {
                throw new DentrixException("El paciente ya tiene una cita a las " + cita.getHoraInicio());
            }
        }
    }

    private DentrixException traducirCita(DentrixException e, Cita cita) {
        if (e.getCause() instanceof SQLException sqle && "23505".equals(sqle.getSQLState())) {
            String mensaje = sqle.getMessage() == null ? "" : sqle.getMessage();
            String hora = cita == null || cita.getHoraInicio() == null ? "" : cita.getHoraInicio().toString();
            if (mensaje.contains("cita_unica_consultorio")) {
                return new DentrixException("El consultorio ya tiene una cita a las " + hora, e);
            }
            if (mensaje.contains("cita_unica_paciente")) {
                return new DentrixException("El paciente ya tiene una cita a las " + hora, e);
            }
            return new DentrixException("Ya existe una cita o un bloque a las " + hora + " en esa fecha.", e);
        }
        return traducir(e);
    }

    private DentrixException traducir(DentrixException e) {
        if (e.getCause() instanceof SQLException sqle) {
            String estado = sqle.getSQLState();
            String mensaje = sqle.getMessage() == null ? "" : sqle.getMessage();
            if ("23P01".equals(estado)) {
                return new DentrixException(
                        "Ese horario se cruza con otra cita del mismo consultorio u odontologo", e);
            }
            if ("23505".equals(estado)) {
                if (mensaje.contains("ux_espera_paciente")) {
                    return new DentrixException("El paciente ya esta en la lista de espera.", e);
                }
                if (mensaje.contains("ux_recordatorio_pendiente")) {
                    return new DentrixException(
                            "La cita ya tiene un recordatorio pendiente en ese canal.", e);
                }
                if (mensaje.contains("consent_datos_unico")) {
                    return new DentrixException(
                            "Ya existe un consentimiento de datos para ese paciente y version.", e);
                }
                return new DentrixException("Ya existe una cita o un bloque en ese horario.", e);
            }
            if ("23514".equals(estado)) {
                return new DentrixException("La cita no cumple una regla de validación de la base de datos.", e);
            }
            if ("23502".equals(estado)) {
                return new DentrixException("Faltan datos obligatorios de la cita.", e);
            }
            if (mensaje.contains("RF-")) {
                return new DentrixException("Regla de negocio: " + mensaje, e);
            }
            if ("P0001".equals(estado) && !mensaje.isBlank()) {
                return new DentrixException(mensaje, e);
            }
        }
        return e;
    }
}
