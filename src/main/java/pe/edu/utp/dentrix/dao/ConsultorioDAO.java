package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Consultorio;

import java.util.List;
import java.util.Optional;

public interface ConsultorioDAO {

    List<Consultorio> listarTodos() throws DentrixException;

    Optional<Consultorio> buscarPorId(Integer id) throws DentrixException;

    Optional<Consultorio> buscarPorCodigo(String codigo) throws DentrixException;

    int insertar(Consultorio consultorio) throws DentrixException;

    boolean actualizar(Consultorio consultorio) throws DentrixException;

    boolean eliminar(Integer id) throws DentrixException;
}
