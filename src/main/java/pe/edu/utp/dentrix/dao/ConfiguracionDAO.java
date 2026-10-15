package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Configuracion;

import java.util.Optional;

public interface ConfiguracionDAO {

    Optional<Configuracion> obtener() throws DentrixException;

    boolean actualizar(Configuracion configuracion) throws DentrixException;
}
