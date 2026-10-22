package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.dto.CambioPrincipal;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.FichaDiagnostico;

import java.util.List;

public interface FichaDiagnosticoDAO {

    List<FichaDiagnostico> listarPorFicha(Long fichaId) throws DentrixException;

    int insertar(FichaDiagnostico diagnostico) throws DentrixException;

    int desmarcarPrincipales(Long fichaId) throws DentrixException;

    /** Reemplaza el principal y deja constancia del cambio en una sola sentencia (RF-06). */
    int reemplazarPrincipal(Long fichaId, String cie10Nuevo, String motivo, Integer usuarioId)
            throws DentrixException;

    List<CambioPrincipal> historialPrincipal(Long fichaId) throws DentrixException;

    boolean actualizar(FichaDiagnostico diagnostico) throws DentrixException;

    boolean eliminar(Long fichaId, String cie10Codigo) throws DentrixException;

    int eliminarPorFicha(Long fichaId) throws DentrixException;
}
