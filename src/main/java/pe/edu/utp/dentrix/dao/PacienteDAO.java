package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Paciente;

import java.util.List;
import java.util.Optional;

public interface PacienteDAO {

    List<Paciente> listarTodos() throws DentrixException;

    List<Paciente> listarPorTexto(String texto) throws DentrixException;

    Optional<Paciente> buscarPorId(Integer id) throws DentrixException;

    Optional<Paciente> buscarPorDni(String dni) throws DentrixException;

    int insertar(Paciente paciente) throws DentrixException;

    boolean actualizar(Paciente paciente) throws DentrixException;

    boolean eliminar(Integer id) throws DentrixException;
}
