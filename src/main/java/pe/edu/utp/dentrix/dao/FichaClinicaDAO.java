package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.FichaClinica;

import java.util.List;
import java.util.Optional;

public interface FichaClinicaDAO {

    List<FichaClinica> listarPorPaciente(Integer pacienteId) throws DentrixException;

    Optional<FichaClinica> buscarPorId(Long id) throws DentrixException;

    int insertar(FichaClinica ficha) throws DentrixException;

    boolean actualizar(FichaClinica ficha) throws DentrixException;

    boolean eliminar(Long id) throws DentrixException;
}
