package pe.edu.utp.dentrix.service;

import pe.edu.utp.dentrix.dao.CitaDAO;
import pe.edu.utp.dentrix.dao.ConsultorioDAO;
import pe.edu.utp.dentrix.dao.PacienteDAO;
import pe.edu.utp.dentrix.dao.TratamientoDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Cita;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class AgendaService {

    private static final Set<String> TIPOS_BLOQUE = Set.of("ALMUERZO", "CIERRE", "BLOQUEADO");
    private static final Set<String> ESTADOS_CANCELABLES = Set.of("PENDIENTE", "CONFIRMADA", "EN_SALA");

    private final CitaDAO citaDAO;
    private final PacienteDAO pacienteDAO;
    private final ConsultorioDAO consultorioDAO;
    private final TratamientoDAO tratamientoDAO;

    public AgendaService(CitaDAO citaDAO, PacienteDAO pacienteDAO,
                         ConsultorioDAO consultorioDAO, TratamientoDAO tratamientoDAO) {
        this.citaDAO = citaDAO;
        this.pacienteDAO = pacienteDAO;
        this.consultorioDAO = consultorioDAO;
        this.tratamientoDAO = tratamientoDAO;
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
        cambiarEstado(id, "FALTO", Set.of("PENDIENTE", "CONFIRMADA"));
    }

    public void cancelar(Long id) throws DentrixException {
        cambiarEstado(id, "CANCELADA", ESTADOS_CANCELABLES);
    }

    public void eliminar(Long id) throws DentrixException {
        if (id == null) {
            throw new DentrixException("La cita es obligatoria");
        }
        try {
            citaDAO.eliminar(id);
        } catch (DentrixException e) {
            throw traducir(e);
        }
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
            if ("23505".equals(estado)) {
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
        }
        return e;
    }
}
