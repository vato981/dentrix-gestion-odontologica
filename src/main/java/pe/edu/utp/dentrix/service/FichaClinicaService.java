package pe.edu.utp.dentrix.service;

import pe.edu.utp.dentrix.dao.CitaDAO;
import pe.edu.utp.dentrix.dao.DiagnosticoCie10DAO;
import pe.edu.utp.dentrix.dao.FichaClinicaDAO;
import pe.edu.utp.dentrix.dao.FichaDiagnosticoDAO;
import pe.edu.utp.dentrix.dao.HallazgoDentalDAO;
import pe.edu.utp.dentrix.dao.PlanTratamientoDAO;
import pe.edu.utp.dentrix.dao.TratamientoDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Cita;
import pe.edu.utp.dentrix.model.FichaClinica;
import pe.edu.utp.dentrix.model.FichaDiagnostico;
import pe.edu.utp.dentrix.model.HallazgoDental;
import pe.edu.utp.dentrix.model.PlanTratamiento;
import pe.edu.utp.dentrix.model.Tratamiento;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class FichaClinicaService {

    private final FichaClinicaDAO fichaDAO;
    private final FichaDiagnosticoDAO diagnosticoDAO;
    private final HallazgoDentalDAO hallazgoDAO;
    private final PlanTratamientoDAO planDAO;
    private final CitaDAO citaDAO;
    private final DiagnosticoCie10DAO cie10DAO;
    private final TratamientoDAO tratamientoDAO;

    public FichaClinicaService(FichaClinicaDAO fichaDAO, FichaDiagnosticoDAO diagnosticoDAO,
                               HallazgoDentalDAO hallazgoDAO, PlanTratamientoDAO planDAO,
                               CitaDAO citaDAO, DiagnosticoCie10DAO cie10DAO,
                               TratamientoDAO tratamientoDAO) {
        this.fichaDAO = fichaDAO;
        this.diagnosticoDAO = diagnosticoDAO;
        this.hallazgoDAO = hallazgoDAO;
        this.planDAO = planDAO;
        this.citaDAO = citaDAO;
        this.cie10DAO = cie10DAO;
        this.tratamientoDAO = tratamientoDAO;
    }

    public FichaClinica crear(FichaClinica ficha) throws DentrixException {
        if (ficha == null) {
            throw new DentrixException("La ficha clinica es obligatoria.");
        }
        if (ficha.getPacienteId() == null) {
            throw new DentrixException("La ficha clinica requiere un paciente.");
        }
        if (ficha.getOdontologoId() == null) {
            throw new DentrixException("La ficha clinica requiere un odontologo.");
        }
        if (ficha.getCitaId() != null && citaDAO.buscarPorId(ficha.getCitaId()).isEmpty()) {
            throw new DentrixException("La cita indicada no existe.");
        }
        ficha.setEstadoDerivacion("PENDIENTE");
        ficha.setDerivadoACaja(false);
        ficha.setFecha(LocalDateTime.now());
        fichaDAO.insertar(ficha);
        return ficha;
    }

    public FichaClinica crearDesdeCita(Long citaId) throws DentrixException {
        Cita cita = citaDAO.buscarPorId(citaId)
                .orElseThrow(() -> new DentrixException("La cita indicada no existe."));
        if (!"CITA".equals(cita.getTipo())) {
            throw new DentrixException("La seleccionada no es una cita valida.");
        }
        if (cita.getPacienteId() == null) {
            throw new DentrixException("La cita no tiene paciente asignado.");
        }
        FichaClinica ficha = new FichaClinica();
        ficha.setCitaId(cita.getId());
        ficha.setPacienteId(cita.getPacienteId());
        ficha.setOdontologoId(cita.getUsuarioId());
        return crear(ficha);
    }

    public FichaClinica buscarPorId(Long id) throws DentrixException {
        return fichaDAO.buscarPorId(id)
                .orElseThrow(() -> new DentrixException("Ficha clinica no encontrada: " + id));
    }

    public List<FichaClinica> listarPorPaciente(Integer pacienteId) throws DentrixException {
        return fichaDAO.listarPorPaciente(pacienteId);
    }

    public void actualizarContenido(Long id, String motivoConsulta, String examenClinico,
                                    String prescripcion, String planIndicaciones) throws DentrixException {
        FichaClinica ficha = buscarPorId(id);
        ficha.setMotivoConsulta(motivoConsulta);
        ficha.setExamenClinico(examenClinico);
        ficha.setPrescripcion(prescripcion);
        ficha.setPlanIndicaciones(planIndicaciones);
        if (!fichaDAO.actualizar(ficha)) {
            throw new DentrixException("La ficha clinica no pudo actualizarse: " + id);
        }
    }

    public FichaDiagnostico registrarDiagnostico(Long fichaId, String codigoCie10, Integer piezaFdi,
                                                 boolean principal, String observacion) throws DentrixException {
        buscarPorId(fichaId);
        if (codigoCie10 == null || codigoCie10.isBlank()) {
            throw new DentrixException("El codigo CIE-10 es obligatorio.");
        }
        if (cie10DAO.buscarPorCodigo(codigoCie10).isEmpty()) {
            throw new DentrixException("Codigo CIE-10 inexistente: " + codigoCie10);
        }
        if (piezaFdi != null) {
            validarPiezaFdi(piezaFdi);
        }
        FichaDiagnostico diagnostico = new FichaDiagnostico(fichaId, codigoCie10, piezaFdi, principal, observacion);
        if (principal) {
            diagnosticoDAO.desmarcarPrincipales(fichaId);
        }
        try {
            diagnosticoDAO.insertar(diagnostico);
        } catch (DentrixException e) {
            throw traducirDiagnostico(e, codigoCie10);
        }
        return diagnostico;
    }

    public List<FichaDiagnostico> listarDiagnosticos(Long fichaId) throws DentrixException {
        return diagnosticoDAO.listarPorFicha(fichaId);
    }

    public void actualizarDiagnostico(FichaDiagnostico diagnostico) throws DentrixException {
        if (diagnostico == null) {
            throw new DentrixException("El diagnostico es obligatorio.");
        }
        buscarPorId(diagnostico.getFichaId());
        if (diagnostico.getPiezaFdi() != null) {
            validarPiezaFdi(diagnostico.getPiezaFdi());
        }
        boolean actualizado;
        try {
            actualizado = diagnosticoDAO.actualizar(diagnostico);
        } catch (DentrixException e) {
            throw traducirDiagnostico(e, diagnostico.getCie10Codigo());
        }
        if (!actualizado) {
            throw new DentrixException("El diagnostico no esta registrado en la ficha: "
                    + diagnostico.getCie10Codigo());
        }
    }

    public void eliminarDiagnostico(Long fichaId, String codigo) throws DentrixException {
        diagnosticoDAO.eliminar(fichaId, codigo);
    }

    public void eliminarTodosLosDiagnosticos(Long fichaId) throws DentrixException {
        diagnosticoDAO.eliminarPorFicha(fichaId);
    }

    public List<HallazgoDental> odontograma(Integer pacienteId) throws DentrixException {
        return hallazgoDAO.listarPorPaciente(pacienteId);
    }

    public void registrarHallazgo(HallazgoDental hallazgo) throws DentrixException {
        if (hallazgo == null) {
            throw new DentrixException("El hallazgo dental es obligatorio.");
        }
        if (hallazgo.getPacienteId() == null) {
            throw new DentrixException("El hallazgo dental requiere un paciente.");
        }
        validarPiezaFdi(hallazgo.getPiezaFdi());
        hallazgoDAO.guardar(hallazgo);
    }

    public void actualizarEstadoPieza(Integer pacienteId, Integer pieza, String estado,
                                      String cara, String observacion) throws DentrixException {
        if (pacienteId == null) {
            throw new DentrixException("El paciente es obligatorio.");
        }
        validarPiezaFdi(pieza);
        HallazgoDental hallazgo = hallazgoDAO.buscarPorPacienteYPieza(pacienteId, pieza)
                .orElseGet(HallazgoDental::new);
        hallazgo.setPacienteId(pacienteId);
        hallazgo.setPiezaFdi(pieza);
        hallazgo.setEstado(estado);
        hallazgo.setCara(cara);
        hallazgo.setObservacion(observacion);
        hallazgoDAO.guardar(hallazgo);
    }

    public void eliminarHallazgo(Integer pacienteId, Integer pieza) throws DentrixException {
        if (pacienteId == null) {
            throw new DentrixException("El paciente es obligatorio.");
        }
        validarPiezaFdi(pieza);
        hallazgoDAO.eliminar(pacienteId, pieza);
    }

    public PlanTratamiento agregarLinea(PlanTratamiento linea) throws DentrixException {
        if (linea == null) {
            throw new DentrixException("La linea de plan es obligatoria.");
        }
        if (linea.getPacienteId() == null) {
            throw new DentrixException("La linea de plan requiere un paciente.");
        }
        if (linea.getTratamientoId() == null) {
            throw new DentrixException("La linea de plan requiere un tratamiento.");
        }
        Tratamiento tratamiento = tratamientoDAO.buscarPorId(linea.getTratamientoId())
                .orElseThrow(() -> new DentrixException("El tratamiento indicado no existe."));
        if (linea.getPrecioUnitario() == null || linea.getPrecioUnitario().signum() == 0) {
            linea.setPrecioUnitario(tratamiento.getPrecio());
        }
        if (linea.getCantidad() == null) {
            linea.setCantidad(1);
        }
        if (linea.getCantidad() < 1) {
            throw new DentrixException("La cantidad de la linea de plan debe ser al menos 1.");
        }
        if (linea.getFichaId() != null) {
            buscarPorId(linea.getFichaId());
        }
        if (linea.getPiezaFdi() != null) {
            validarPiezaFdi(linea.getPiezaFdi());
        }
        linea.setEstado("PLANIFICADO");
        try {
            planDAO.insertar(linea);
        } catch (DentrixException e) {
            throw traducirError(e);
        }
        return linea;
    }

    public void marcarEjecutada(Long id) throws DentrixException {
        PlanTratamiento linea = planDAO.buscarPorId(id)
                .orElseThrow(() -> new DentrixException("La linea de plan no existe: " + id));
        if (!"PLANIFICADO".equals(linea.getEstado())) {
            throw new DentrixException("Solo se puede ejecutar una linea en estado PLANIFICADO; estado actual: "
                    + linea.getEstado());
        }
        linea.setEstado("EJECUTADO");
        if (!planDAO.actualizar(linea)) {
            throw new DentrixException("La linea de plan no pudo actualizarse: " + id);
        }
    }

    public List<PlanTratamiento> planPorPaciente(Integer pacienteId) throws DentrixException {
        return planDAO.listarPorPaciente(pacienteId);
    }

    public List<PlanTratamiento> planPorFicha(Long fichaId) throws DentrixException {
        return planDAO.listarPorFicha(fichaId);
    }

    public void eliminarLinea(Long id) throws DentrixException {
        if (!planDAO.eliminar(id)) {
            throw new DentrixException("La linea de plan no existe: " + id);
        }
    }

    public void derivarACaja(Long fichaId) throws DentrixException {
        FichaClinica ficha = buscarPorId(fichaId);
        if ("RECHAZADA".equals(ficha.getEstadoDerivacion())) {
            throw new DentrixException("La ficha tiene la derivacion RECHAZADA; no puede derivarse a caja.");
        }
        ficha.setEstadoDerivacion("ACEPTADA");
        ficha.setDerivadoACaja(true);
        ficha.setFechaDerivacion(LocalDateTime.now());
        boolean actualizada;
        try {
            actualizada = fichaDAO.actualizar(ficha);
        } catch (DentrixException e) {
            throw traducirError(e);
        }
        if (!actualizada) {
            throw new DentrixException("La ficha clinica no pudo derivarse a caja: " + fichaId);
        }
    }

    public boolean esCobrable(Long fichaId) throws DentrixException {
        FichaClinica ficha = buscarPorId(fichaId);
        return "ACEPTADA".equals(ficha.getEstadoDerivacion());
    }

    public void rechazarDerivacion(Long fichaId) throws DentrixException {
        FichaClinica ficha = buscarPorId(fichaId);
        ficha.setEstadoDerivacion("RECHAZADA");
        ficha.setDerivadoACaja(false);
        ficha.setFechaDerivacion(null);
        if (!fichaDAO.actualizar(ficha)) {
            throw new DentrixException("La ficha clinica no pudo rechazarse: " + fichaId);
        }
    }

    private void validarPiezaFdi(Integer pieza) throws DentrixException {
        if (pieza == null) {
            throw new DentrixException("La pieza dental es obligatoria.");
        }
        if (pieza < 11 || pieza > 48 || pieza % 10 < 1 || pieza % 10 > 8) {
            throw new DentrixException("La pieza FDI " + pieza + " es invalida; debe estar entre 11 y 48 "
                    + "con un diente de 1 a 8.");
        }
    }

    private DentrixException traducirDiagnostico(DentrixException e, String codigoCie10) {
        if (mensajeSql(e).contains("RF-06")) {
            return new DentrixException("Solo puede haber un diagnostico principal por ficha (RF-06)", e);
        }
        if ("23505".equals(sqlState(e))) {
            return new DentrixException("El diagnostico " + codigoCie10 + " ya esta registrado en esta ficha", e);
        }
        return traducirError(e);
    }

    private DentrixException traducirError(DentrixException e) {
        String mensaje = mensajeSql(e);
        String estado = sqlState(e);
        if (mensaje.contains("RF-06")) {
            return new DentrixException("Solo puede haber un diagnostico principal por ficha (RF-06)", e);
        }
        if (mensaje.contains("RF-07")) {
            return new DentrixException("La ficha debe tener una derivacion ACEPTADA para ser cobrada (RF-07)", e);
        }
        if ("23505".equals(estado)) {
            return new DentrixException("La operacion genera un registro duplicado.", e);
        }
        if ("23514".equals(estado)) {
            return new DentrixException("La operacion viola una regla de validacion de la base de datos.", e);
        }
        return e;
    }

    private String mensajeSql(DentrixException e) {
        return e.getMessage() == null ? "" : e.getMessage();
    }

    private String sqlState(DentrixException e) {
        Throwable causa = e.getCause();
        while (causa != null) {
            if (causa instanceof SQLException SQLException) {
                return SQLException.getSQLState();
            }
            causa = causa.getCause();
        }
        return null;
    }
}
