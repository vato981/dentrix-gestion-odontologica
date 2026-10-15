package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.HallazgoDental;

import java.util.List;
import java.util.Optional;

public interface HallazgoDentalDAO {

    List<HallazgoDental> listarPorPaciente(Integer pacienteId) throws DentrixException;

    Optional<HallazgoDental> buscarPorPacienteYPieza(Integer pacienteId, Integer piezaFdi) throws DentrixException;

    boolean guardar(HallazgoDental hallazgo) throws DentrixException;

    boolean eliminar(Integer pacienteId, Integer piezaFdi) throws DentrixException;
}
