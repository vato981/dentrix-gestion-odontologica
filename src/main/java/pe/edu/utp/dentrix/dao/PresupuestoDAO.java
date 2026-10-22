package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Aceptacion;
import pe.edu.utp.dentrix.model.Consentimiento;
import pe.edu.utp.dentrix.model.Presupuesto;
import pe.edu.utp.dentrix.model.PresupuestoItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PresupuestoDAO {

    Long insertar(Presupuesto presupuesto) throws DentrixException;

    Optional<Presupuesto> buscarPorId(Long id) throws DentrixException;

    List<Presupuesto> listarPorPaciente(Integer pacienteId) throws DentrixException;

    List<Presupuesto> listarPorEstado(String estado) throws DentrixException;

    List<Presupuesto> listarRecientes(int limite) throws DentrixException;

    boolean actualizarEstado(Long id, String estado, String motivoRechazo) throws DentrixException;

    boolean actualizarDescuento(Long id, BigDecimal descuento, String motivo, Integer autorizadorId)
            throws DentrixException;

    Long insertarItem(PresupuestoItem item) throws DentrixException;

    Optional<PresupuestoItem> buscarItem(Long id) throws DentrixException;

    List<PresupuestoItem> listarItems(Long presupuestoId) throws DentrixException;

    boolean actualizarEstadoItem(Long itemId, String estado, String motivoRechazo) throws DentrixException;

    boolean eliminarItem(Long itemId) throws DentrixException;

    BigDecimal totalAceptado(Long presupuestoId) throws DentrixException;

    boolean estaFacturado(Long presupuestoItemId) throws DentrixException;

    BigDecimal imputadoDeItem(Long presupuestoItemId) throws DentrixException;

    void aplicarAceptacion(Long presupuestoId, List<Long> itemsDiferidos, Aceptacion aceptacion)
            throws DentrixException;

    void aplicarRechazo(Long presupuestoId, String motivo) throws DentrixException;

    Optional<Aceptacion> buscarAceptacion(Long presupuestoId) throws DentrixException;

    List<Consentimiento> listarConsentimientos(Long presupuestoId) throws DentrixException;

    Long insertarConsentimiento(Consentimiento consentimiento) throws DentrixException;

    boolean existeConsentimientoFirmado(Long presupuestoItemId) throws DentrixException;

    boolean eliminar(Long presupuestoId) throws DentrixException;
}
