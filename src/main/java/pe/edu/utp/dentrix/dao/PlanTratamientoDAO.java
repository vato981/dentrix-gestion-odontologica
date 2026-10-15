package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.PlanTratamiento;

import java.util.List;
import java.util.Optional;

public interface PlanTratamientoDAO {

    List<PlanTratamiento> listarPorPaciente(Integer pacienteId) throws DentrixException;

    List<PlanTratamiento> listarPorFicha(Long fichaId) throws DentrixException;

    List<PlanTratamiento> listarPorPago(Long pagoId) throws DentrixException;

    Optional<PlanTratamiento> buscarPorId(Long id) throws DentrixException;

    int insertar(PlanTratamiento linea) throws DentrixException;

    boolean actualizar(PlanTratamiento linea) throws DentrixException;

    boolean eliminar(Long id) throws DentrixException;
}
