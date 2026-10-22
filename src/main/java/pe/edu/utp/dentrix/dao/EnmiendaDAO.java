package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Enmienda;
import pe.edu.utp.dentrix.model.EnmiendaItem;

import java.util.List;
import java.util.Optional;

public interface EnmiendaDAO {

    Long insertar(Enmienda enmienda) throws DentrixException;

    Optional<Enmienda> buscarPorId(Long id) throws DentrixException;

    Optional<Enmienda> buscarBorrador(Long presupuestoId) throws DentrixException;

    List<Enmienda> listarPorPresupuesto(Long presupuestoId) throws DentrixException;

    Long insertarItem(EnmiendaItem item) throws DentrixException;

    List<EnmiendaItem> listarItems(Long enmiendaId) throws DentrixException;

    void actualizarFirma(Long id, String nombre, String documento, String rol, String hash)
            throws DentrixException;

    void firmar(Long enmiendaId, Integer usuarioId) throws DentrixException;

    void descartar(Long enmiendaId) throws DentrixException;
}
