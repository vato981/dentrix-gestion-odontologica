package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.FichaDiagnostico;

import java.util.List;

public interface FichaDiagnosticoDAO {

    List<FichaDiagnostico> listarPorFicha(Long fichaId) throws DentrixException;

    int insertar(FichaDiagnostico diagnostico) throws DentrixException;

    boolean actualizar(FichaDiagnostico diagnostico) throws DentrixException;

    boolean eliminar(Long fichaId, String cie10Codigo) throws DentrixException;

    int eliminarPorFicha(Long fichaId) throws DentrixException;
}
