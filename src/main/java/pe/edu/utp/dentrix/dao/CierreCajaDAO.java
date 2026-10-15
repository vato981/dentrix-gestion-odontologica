package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.CierreCaja;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CierreCajaDAO {

    List<CierreCaja> listarTodos() throws DentrixException;

    Optional<CierreCaja> buscarPorFecha(LocalDate fecha) throws DentrixException;

    Optional<CierreCaja> buscarPorFechaYTurno(LocalDate fecha, String turno) throws DentrixException;

    int insertar(CierreCaja cierre) throws DentrixException;

    boolean actualizar(CierreCaja cierre) throws DentrixException;
}
