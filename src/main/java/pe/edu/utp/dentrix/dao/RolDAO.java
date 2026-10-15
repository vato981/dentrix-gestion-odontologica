package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Rol;

import java.util.List;
import java.util.Optional;

public interface RolDAO {

    List<Rol> listarTodos() throws DentrixException;

    Optional<Rol> buscarPorId(Integer id) throws DentrixException;

    Optional<Rol> buscarPorCodigo(String codigo) throws DentrixException;

    int insertar(Rol rol) throws DentrixException;

    boolean actualizar(Rol rol) throws DentrixException;

    boolean eliminar(Integer id) throws DentrixException;
}
