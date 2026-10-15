package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Tratamiento;

import java.util.List;
import java.util.Optional;

public interface TratamientoDAO {

    List<Tratamiento> listarTodos() throws DentrixException;

    List<Tratamiento> listarActivos() throws DentrixException;

    Optional<Tratamiento> buscarPorId(Integer id) throws DentrixException;

    Optional<Tratamiento> buscarPorCodigo(String codigo) throws DentrixException;

    int insertar(Tratamiento tratamiento) throws DentrixException;

    boolean actualizar(Tratamiento tratamiento) throws DentrixException;

    boolean eliminar(Integer id) throws DentrixException;
}
