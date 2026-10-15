package pe.edu.utp.dentrix.service;

import pe.edu.utp.dentrix.dao.ConfiguracionDAO;
import pe.edu.utp.dentrix.dao.ConsultorioDAO;
import pe.edu.utp.dentrix.dao.DiagnosticoCie10DAO;
import pe.edu.utp.dentrix.dao.TratamientoDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Configuracion;
import pe.edu.utp.dentrix.model.Consultorio;
import pe.edu.utp.dentrix.model.DiagnosticoCie10;
import pe.edu.utp.dentrix.model.Tratamiento;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class CatalogoService {

    private static final Pattern PATRON_CIE10 = Pattern.compile("^[A-Za-z]\\d{2}(\\.\\d)?$");

    private final TratamientoDAO tratamientoDAO;
    private final DiagnosticoCie10DAO diagnosticoCie10DAO;
    private final ConsultorioDAO consultorioDAO;
    private final ConfiguracionDAO configuracionDAO;

    public CatalogoService(TratamientoDAO tratamientoDAO,
                           DiagnosticoCie10DAO diagnosticoCie10DAO,
                           ConsultorioDAO consultorioDAO,
                           ConfiguracionDAO configuracionDAO) {
        this.tratamientoDAO = tratamientoDAO;
        this.diagnosticoCie10DAO = diagnosticoCie10DAO;
        this.consultorioDAO = consultorioDAO;
        this.configuracionDAO = configuracionDAO;
    }

    public List<Tratamiento> listarTratamientos() throws DentrixException {
        return tratamientoDAO.listarTodos();
    }

    public List<Tratamiento> listarTratamientosActivos() throws DentrixException {
        return tratamientoDAO.listarActivos();
    }

    public Tratamiento registrarTratamiento(Tratamiento tratamiento) throws DentrixException {
        validarTratamiento(tratamiento);
        if (tratamientoDAO.buscarPorCodigo(tratamiento.getCodigo()).isPresent()) {
            throw new DentrixException("Ya existe un tratamiento con el código " + tratamiento.getCodigo());
        }
        if (tratamiento.getEstado() == null) {
            tratamiento.setEstado("ACTIVO");
        }
        try {
            int id = tratamientoDAO.insertar(tratamiento);
            tratamiento.setId(id);
            return tratamiento;
        } catch (DentrixException e) {
            throw traducir(e,
                    "Ya existe un tratamiento con el código " + tratamiento.getCodigo(),
                    "No se pudo registrar el tratamiento.");
        }
    }

    public Tratamiento editarTratamiento(Tratamiento tratamiento) throws DentrixException {
        validarTratamiento(tratamiento);
        if (tratamiento.getId() == null) {
            throw new DentrixException("El tratamiento a editar no tiene identificador.");
        }
        Optional<Tratamiento> duplicado = tratamientoDAO.buscarPorCodigo(tratamiento.getCodigo());
        if (duplicado.isPresent() && !duplicado.get().getId().equals(tratamiento.getId())) {
            throw new DentrixException("Ya existe un tratamiento con el código " + tratamiento.getCodigo());
        }
        try {
            if (!tratamientoDAO.actualizar(tratamiento)) {
                throw new DentrixException("No se encontró el tratamiento a editar.");
            }
            return tratamiento;
        } catch (DentrixException e) {
            throw traducir(e,
                    "Ya existe un tratamiento con el código " + tratamiento.getCodigo(),
                    "No se pudo editar el tratamiento.");
        }
    }

    public void inactivarTratamiento(Integer id) throws DentrixException {
        Tratamiento tratamiento = tratamientoDAO.buscarPorId(id)
                .orElseThrow(() -> new DentrixException("No existe el tratamiento a inactivar."));
        tratamiento.setEstado("INACTIVO");
        try {
            if (!tratamientoDAO.actualizar(tratamiento)) {
                throw new DentrixException("No se pudo inactivar el tratamiento.");
            }
        } catch (DentrixException e) {
            throw traducir(e, "No se pudo inactivar el tratamiento.", "No se pudo inactivar el tratamiento.");
        }
    }

    public void eliminarTratamiento(Integer id) throws DentrixException {
        try {
            if (!tratamientoDAO.eliminar(id)) {
                throw new DentrixException("No existe el tratamiento a eliminar.");
            }
        } catch (DentrixException e) {
            throw traducir(e,
                    "No se pudo eliminar el tratamiento.",
                    "No se puede eliminar: está en uso por planes de tratamiento o pagos.");
        }
    }

    public List<DiagnosticoCie10> listarDiagnosticos() throws DentrixException {
        return diagnosticoCie10DAO.listarTodos();
    }

    public List<DiagnosticoCie10> listarDiagnosticosFrecuentes() throws DentrixException {
        return diagnosticoCie10DAO.listarFrecuentes();
    }

    public DiagnosticoCie10 registrarDiagnostico(DiagnosticoCie10 diagnostico) throws DentrixException {
        validarDiagnostico(diagnostico);
        if (diagnosticoCie10DAO.buscarPorCodigo(diagnostico.getCodigo()).isPresent()) {
            throw new DentrixException("Ya existe un diagnóstico CIE-10 con el código " + diagnostico.getCodigo());
        }
        try {
            diagnosticoCie10DAO.insertar(diagnostico);
            return diagnostico;
        } catch (DentrixException e) {
            throw traducir(e,
                    "Ya existe un diagnóstico CIE-10 con el código " + diagnostico.getCodigo(),
                    "No se pudo registrar el diagnóstico CIE-10.");
        }
    }

    public DiagnosticoCie10 editarDiagnostico(DiagnosticoCie10 diagnostico) throws DentrixException {
        validarDiagnostico(diagnostico);
        try {
            if (!diagnosticoCie10DAO.actualizar(diagnostico)) {
                throw new DentrixException("No existe el diagnóstico CIE-10 a editar.");
            }
            return diagnostico;
        } catch (DentrixException e) {
            throw traducir(e,
                    "No se pudo editar el diagnóstico CIE-10.",
                    "No se pudo editar el diagnóstico CIE-10.");
        }
    }

    public void eliminarDiagnostico(String codigo) throws DentrixException {
        try {
            if (!diagnosticoCie10DAO.eliminar(codigo)) {
                throw new DentrixException("No existe el diagnóstico CIE-10 a eliminar.");
            }
        } catch (DentrixException e) {
            throw traducir(e,
                    "No se pudo eliminar el diagnóstico CIE-10.",
                    "No se puede eliminar: está en uso por fichas clínicas.");
        }
    }

    public List<Consultorio> listarConsultorios() throws DentrixException {
        return consultorioDAO.listarTodos();
    }

    public Optional<Consultorio> buscarConsultorioPorCodigo(String codigo) throws DentrixException {
        return consultorioDAO.buscarPorCodigo(codigo);
    }

    public Consultorio registrarConsultorio(Consultorio consultorio) throws DentrixException {
        validarConsultorio(consultorio);
        if (consultorioDAO.buscarPorCodigo(consultorio.getCodigo()).isPresent()) {
            throw new DentrixException("Ya existe un consultorio con el código " + consultorio.getCodigo());
        }
        if (consultorio.getEstado() == null) {
            consultorio.setEstado("ACTIVO");
        }
        try {
            int id = consultorioDAO.insertar(consultorio);
            consultorio.setId(id);
            return consultorio;
        } catch (DentrixException e) {
            throw traducir(e,
                    "Ya existe un consultorio con el código " + consultorio.getCodigo(),
                    "No se pudo registrar el consultorio.");
        }
    }

    public Consultorio editarConsultorio(Consultorio consultorio) throws DentrixException {
        validarConsultorio(consultorio);
        if (consultorio.getId() == null) {
            throw new DentrixException("El consultorio a editar no tiene identificador.");
        }
        Optional<Consultorio> duplicado = consultorioDAO.buscarPorCodigo(consultorio.getCodigo());
        if (duplicado.isPresent() && !duplicado.get().getId().equals(consultorio.getId())) {
            throw new DentrixException("Ya existe un consultorio con el código " + consultorio.getCodigo());
        }
        try {
            if (!consultorioDAO.actualizar(consultorio)) {
                throw new DentrixException("No se encontró el consultorio a editar.");
            }
            return consultorio;
        } catch (DentrixException e) {
            throw traducir(e,
                    "Ya existe un consultorio con el código " + consultorio.getCodigo(),
                    "No se pudo editar el consultorio.");
        }
    }

    public void eliminarConsultorio(Integer id) throws DentrixException {
        try {
            if (!consultorioDAO.eliminar(id)) {
                throw new DentrixException("No existe el consultorio a eliminar.");
            }
        } catch (DentrixException e) {
            throw traducir(e,
                    "No se pudo eliminar el consultorio.",
                    "No se puede eliminar: hay citas o usuarios asignados a este consultorio.");
        }
    }

    public Configuracion obtenerConfiguracion() throws DentrixException {
        return configuracionDAO.obtener()
                .orElseThrow(() -> new DentrixException("No se encontró la configuración de la clínica."));
    }

    public void actualizarConfiguracion(Configuracion configuracion) throws DentrixException {
        if (configuracion == null || configuracion.getId() == null) {
            throw new DentrixException("La configuración a actualizar no tiene identificador.");
        }
        try {
            if (!configuracionDAO.actualizar(configuracion)) {
                throw new DentrixException("No se encontró la configuración a actualizar.");
            }
        } catch (DentrixException e) {
            throw traducir(e,
                    "No se pudo actualizar la configuración.",
                    "No se pudo actualizar la configuración.");
        }
    }

    private void validarTratamiento(Tratamiento tratamiento) throws DentrixException {
        if (tratamiento == null) {
            throw new DentrixException("El tratamiento es obligatorio.");
        }
        if (tratamiento.getCodigo() == null || tratamiento.getCodigo().isBlank()) {
            throw new DentrixException("El código del tratamiento es obligatorio.");
        }
        if (tratamiento.getNombre() == null || tratamiento.getNombre().isBlank()) {
            throw new DentrixException("El nombre del tratamiento es obligatorio.");
        }
        if (tratamiento.getEspecialidad() == null || tratamiento.getEspecialidad().isBlank()) {
            throw new DentrixException("La especialidad del tratamiento es obligatoria.");
        }
        if (tratamiento.getCalculo() == null || tratamiento.getCalculo().isBlank()) {
            throw new DentrixException("El tipo de cálculo del tratamiento es obligatorio.");
        }
        if (tratamiento.getPrecio() == null) {
            throw new DentrixException("El precio del tratamiento es obligatorio.");
        }
        if (tratamiento.getPrecio().signum() < 0) {
            throw new DentrixException("El precio del tratamiento no puede ser negativo.");
        }
    }

    private void validarDiagnostico(DiagnosticoCie10 diagnostico) throws DentrixException {
        if (diagnostico == null) {
            throw new DentrixException("El diagnóstico es obligatorio.");
        }
        if (diagnostico.getCodigo() == null || !PATRON_CIE10.matcher(diagnostico.getCodigo()).matches()) {
            throw new DentrixException(
                    "El código CIE-10 debe tener formato tipo K02.1 (letra, dos dígitos y opcionalmente un decimal).");
        }
        if (diagnostico.getDescripcion() == null || diagnostico.getDescripcion().isBlank()) {
            throw new DentrixException("La descripción del diagnóstico es obligatoria.");
        }
    }

    private void validarConsultorio(Consultorio consultorio) throws DentrixException {
        if (consultorio == null) {
            throw new DentrixException("El consultorio es obligatorio.");
        }
        if (consultorio.getCodigo() == null || consultorio.getCodigo().isBlank()) {
            throw new DentrixException("El código del consultorio es obligatorio.");
        }
        if (consultorio.getNombre() == null || consultorio.getNombre().isBlank()) {
            throw new DentrixException("El nombre del consultorio es obligatorio.");
        }
    }

    private DentrixException traducir(DentrixException e, String mensajeDuplicado, String mensajeEnUso) {
        Throwable causa = e.getCause();
        if (causa instanceof SQLException sqle) {
            String sqlState = sqle.getSQLState();
            String texto = sqle.getMessage() == null ? "" : sqle.getMessage();
            if ("23505".equals(sqlState)) {
                return new DentrixException(mensajeDuplicado, e);
            }
            if ("23514".equals(sqlState)) {
                return new DentrixException("El valor ingresado no cumple una regla de validación de la base de datos.", e);
            }
            if ("23503".equals(sqlState)) {
                return new DentrixException(mensajeEnUso, e);
            }
            if (texto.contains("RF-06")) {
                return new DentrixException("Solo puede haber un diagnóstico principal por ficha clínica.", e);
            }
            if (texto.contains("RF-07")) {
                return new DentrixException("La ficha no tiene derivación aceptada; no se puede facturar.", e);
            }
            if (texto.contains("RF-08")) {
                return new DentrixException("El abono debe referenciar una boleta válida, no otro abono.", e);
            }
        }
        return e;
    }
}
