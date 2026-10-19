package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Pago;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PagoDAO {

    Optional<Pago> buscarPorId(Long id) throws DentrixException;

    Optional<Pago> buscarPorNumero(String numero) throws DentrixException;

    List<Pago> listarPorPaciente(Integer pacienteId) throws DentrixException;

    List<Pago> listarPorFicha(Long fichaId) throws DentrixException;

    List<Pago> listarPorCierre(Long cierreId) throws DentrixException;

    List<Pago> listarAbonos(Long documentoId) throws DentrixException;

    List<Pago> listarDocumentosConSaldo() throws DentrixException;

    List<Pago> listarBoletasPorFecha(LocalDate fecha) throws DentrixException;

    long maximoCorrelativo(String prefijo) throws DentrixException;

    int insertar(Pago pago) throws DentrixException;

    boolean actualizar(Pago pago) throws DentrixException;
}
