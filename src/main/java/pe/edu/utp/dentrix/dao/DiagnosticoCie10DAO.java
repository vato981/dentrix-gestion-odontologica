package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.DiagnosticoCie10;

import java.util.List;
import java.util.Optional;

public interface DiagnosticoCie10DAO {

    List<DiagnosticoCie10> listarTodos() throws DentrixException;

    List<DiagnosticoCie10> listarFrecuentes() throws DentrixException;

    Optional<DiagnosticoCie10> buscarPorCodigo(String codigo) throws DentrixException;

    int insertar(DiagnosticoCie10 diagnostico) throws DentrixException;

    boolean actualizar(DiagnosticoCie10 diagnostico) throws DentrixException;

    boolean eliminar(String codigo) throws DentrixException;
}
