package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioDAO {

    List<Usuario> listarTodos() throws DentrixException;

    Optional<Usuario> buscarPorId(Integer id) throws DentrixException;

    Optional<Usuario> buscarPorUsername(String username) throws DentrixException;

    Optional<Usuario> buscarPorDni(String dni) throws DentrixException;

    int insertar(Usuario usuario) throws DentrixException;

    boolean actualizar(Usuario usuario) throws DentrixException;

    boolean eliminar(Integer id) throws DentrixException;
}
