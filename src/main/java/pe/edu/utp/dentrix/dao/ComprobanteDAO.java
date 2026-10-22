package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.ComprobanteItem;
import pe.edu.utp.dentrix.model.Pago;

import java.util.List;

public interface ComprobanteDAO {

    Long insertarItem(ComprobanteItem item) throws DentrixException;

    List<ComprobanteItem> listarItems(Long pagoId) throws DentrixException;

    boolean eliminarItem(Long id) throws DentrixException;

    Long crearBorrador(Pago pago) throws DentrixException;

    String siguienteNumero(String serie) throws DentrixException;

    void anular(Long pagoId, String motivo, Integer usuarioId) throws DentrixException;
}
