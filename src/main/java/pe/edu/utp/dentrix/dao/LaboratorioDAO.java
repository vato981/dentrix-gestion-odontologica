package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.ConsumoLote;
import pe.edu.utp.dentrix.model.ControlLaboratorio;
import pe.edu.utp.dentrix.model.Insumo;
import pe.edu.utp.dentrix.model.LoteInsumo;
import pe.edu.utp.dentrix.model.OrdenLaboratorio;

import java.util.List;
import java.util.Optional;

public interface LaboratorioDAO {

    List<Insumo> listarInsumos() throws DentrixException;

    Long insertarInsumo(Insumo insumo) throws DentrixException;

    LoteInsumo insertarLote(LoteInsumo lote) throws DentrixException;

    List<LoteInsumo> listarLotes() throws DentrixException;

    Long insertarConsumo(ConsumoLote consumo) throws DentrixException;

    List<ConsumoLote> listarConsumos(Long ejecucionItemId) throws DentrixException;

    boolean eliminarConsumo(Long id) throws DentrixException;

    Long insertarOrden(OrdenLaboratorio orden) throws DentrixException;

    Optional<OrdenLaboratorio> buscarOrden(Long id) throws DentrixException;

    List<OrdenLaboratorio> listarOrdenes(Long presupuestoItemId) throws DentrixException;

    boolean actualizarEstadoOrden(Long id, String estado, String motivo) throws DentrixException;

    Long insertarControl(ControlLaboratorio control) throws DentrixException;

    List<ControlLaboratorio> listarControles(Long ordenId) throws DentrixException;
}
