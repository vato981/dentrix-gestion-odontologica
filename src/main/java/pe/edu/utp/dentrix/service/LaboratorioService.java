package pe.edu.utp.dentrix.service;

import pe.edu.utp.dentrix.dao.EjecucionDAO;
import pe.edu.utp.dentrix.dao.LaboratorioDAO;
import pe.edu.utp.dentrix.dao.PresupuestoDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.ConsumoLote;
import pe.edu.utp.dentrix.model.ControlLaboratorio;
import pe.edu.utp.dentrix.model.EjecucionItem;
import pe.edu.utp.dentrix.model.Insumo;
import pe.edu.utp.dentrix.model.LoteInsumo;
import pe.edu.utp.dentrix.model.OrdenLaboratorio;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class LaboratorioService {

    private final LaboratorioDAO laboratorioDAO;
    private final EjecucionDAO ejecucionDAO;
    private final PresupuestoDAO presupuestoDAO;

    public LaboratorioService(LaboratorioDAO laboratorioDAO, EjecucionDAO ejecucionDAO,
                              PresupuestoDAO presupuestoDAO) {
        this.laboratorioDAO = laboratorioDAO;
        this.ejecucionDAO = ejecucionDAO;
        this.presupuestoDAO = presupuestoDAO;
    }

    public List<Insumo> insumos() throws DentrixException {
        return laboratorioDAO.listarInsumos();
    }

    public List<LoteInsumo> lotes() throws DentrixException {
        return laboratorioDAO.listarLotes();
    }

    public ConsumoLote consumir(Long ejecucionItemId, Long loteId, BigDecimal cantidad, Integer usuarioId)
            throws DentrixException {
        if (ejecucionItemId == null) {
            throw new DentrixException("La sesion es obligatoria para consumir un lote");
        }
        if (loteId == null) {
            throw new DentrixException("El lote del insumo es obligatorio");
        }
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new DentrixException("La cantidad consumida debe ser mayor que cero");
        }
        if (usuarioId == null) {
            throw new DentrixException("El usuario que registra el consumo es obligatorio");
        }
        LoteInsumo lote = laboratorioDAO.listarLotes().stream()
                .filter(actual -> loteId.equals(actual.getId()))
                .findFirst()
                .orElseThrow(() -> new DentrixException("Lote de insumo no encontrado: " + loteId));
        ConsumoLote consumo = new ConsumoLote();
        consumo.setEjecucionItemId(ejecucionItemId);
        consumo.setLoteInsumoId(loteId);
        consumo.setLote(lote.getNumero());
        consumo.setInsumoNombre(lote.getInsumoNombre());
        consumo.setCantidad(cantidad);
        consumo.setFecha(LocalDateTime.now());
        consumo.setUsuarioId(usuarioId);
        try {
            laboratorioDAO.insertarConsumo(consumo);
        } catch (DentrixException e) {
            throw traducir(e);
        }
        return consumo;
    }

    public void quitarConsumo(Long consumoId) throws DentrixException {
        if (consumoId == null) {
            throw new DentrixException("El consumo a quitar es obligatorio");
        }
        try {
            if (!laboratorioDAO.eliminarConsumo(consumoId)) {
                throw new DentrixException("El consumo " + consumoId + " no existe");
            }
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public List<ConsumoLote> consumos(Long ejecucionItemId) throws DentrixException {
        if (ejecucionItemId == null) {
            throw new DentrixException("La sesion es obligatoria para consultar sus consumos");
        }
        return laboratorioDAO.listarConsumos(ejecucionItemId);
    }

    public OrdenLaboratorio solicitar(Long presupuestoItemId, Long ejecucionItemId, String laboratorio,
                                      String trabajo, String material, String color,
                                      LocalDate fechaPrometida, BigDecimal costo, Integer usuarioId)
            throws DentrixException {
        if (presupuestoItemId == null) {
            throw new DentrixException("El item del presupuesto es obligatorio para solicitar el laboratorio");
        }
        if (laboratorio == null || laboratorio.isBlank()) {
            throw new DentrixException("El laboratorio es obligatorio");
        }
        if (trabajo == null || trabajo.isBlank()) {
            throw new DentrixException("El trabajo solicitado al laboratorio es obligatorio");
        }
        if (fechaPrometida != null && fechaPrometida.isBefore(LocalDate.now())) {
            throw new DentrixException("La fecha prometida no puede ser anterior a hoy");
        }
        if (costo != null && costo.signum() < 0) {
            throw new DentrixException("El costo del laboratorio no puede ser negativo");
        }
        presupuestoDAO.buscarItem(presupuestoItemId)
                .orElseThrow(() -> new DentrixException(
                        "Item del presupuesto no encontrado: " + presupuestoItemId));
        if (ejecucionItemId != null) {
            EjecucionItem sesion = ejecucionDAO.buscarEjecucion(ejecucionItemId)
                    .orElseThrow(() -> new DentrixException(
                            "Sesion ejecutada no encontrada: " + ejecucionItemId));
            if (!presupuestoItemId.equals(sesion.getPresupuestoItemId())) {
                throw new DentrixException("La sesion " + ejecucionItemId
                        + " no pertenece al item " + presupuestoItemId);
            }
        }
        OrdenLaboratorio orden = new OrdenLaboratorio();
        orden.setPresupuestoItemId(presupuestoItemId);
        orden.setEjecucionItemId(ejecucionItemId);
        orden.setLaboratorio(laboratorio.trim());
        orden.setTrabajo(trabajo.trim());
        orden.setMaterial(material == null || material.isBlank() ? null : material.trim());
        orden.setColor(color == null || color.isBlank() ? null : color.trim());
        orden.setFechaSolicitud(LocalDateTime.now());
        orden.setFechaPrometida(fechaPrometida);
        orden.setEstado("SOLICITADA");
        orden.setCosto(costo);
        Long id;
        try {
            id = laboratorioDAO.insertarOrden(orden);
        } catch (DentrixException e) {
            throw traducir(e);
        }
        return laboratorioDAO.buscarOrden(id)
                .orElseThrow(() -> new DentrixException(
                        "No se pudo releer la orden de laboratorio registrada: " + id));
    }

    public void recibir(Long ordenId) throws DentrixException {
        OrdenLaboratorio orden = buscarOrden(ordenId);
        if (!orden.esSolicitada()) {
            throw new DentrixException("Solo se recibe una orden SOLICITADA; la orden " + ordenId
                    + " esta " + orden.getEstado());
        }
        try {
            if (!laboratorioDAO.actualizarEstadoOrden(ordenId, "RECIBIDA", null)) {
                throw new DentrixException("La orden " + ordenId + " no pudo recibirse");
            }
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public ControlLaboratorio controlar(Long ordenId, String ajuste, Boolean colorOk, Boolean contactosOk,
                                        Boolean conforme, String observaciones, Integer usuarioId)
            throws DentrixException {
        OrdenLaboratorio orden = buscarOrden(ordenId);
        if (conforme == null) {
            throw new DentrixException("El resultado del control es obligatorio (conforme o rechazado)");
        }
        if (!conforme && (observaciones == null || observaciones.isBlank())) {
            throw new DentrixException("El rechazo del trabajo exige observaciones");
        }
        if (usuarioId == null) {
            throw new DentrixException("El usuario que controla la orden es obligatorio");
        }
        ControlLaboratorio control = new ControlLaboratorio();
        control.setOrdenId(orden.getId());
        control.setFecha(LocalDateTime.now());
        control.setAjuste(ajuste == null || ajuste.isBlank() ? null : ajuste.trim());
        control.setColorOk(colorOk);
        control.setContactosOk(contactosOk);
        control.setConforme(conforme);
        control.setObservaciones(observaciones == null || observaciones.isBlank()
                ? null : observaciones.trim());
        control.setUsuarioId(usuarioId);
        Long id;
        try {
            id = laboratorioDAO.insertarControl(control);
        } catch (DentrixException e) {
            throw traducir(e);
        }
        return laboratorioDAO.listarControles(orden.getId()).stream()
                .filter(actual -> id.equals(actual.getId()))
                .findFirst()
                .orElseThrow(() -> new DentrixException(
                        "No se pudo releer el control registrado de la orden " + orden.getId()));
    }

    public void instalar(Long ordenId) throws DentrixException {
        OrdenLaboratorio orden = buscarOrden(ordenId);
        if (!orden.esConforme()) {
            throw new DentrixException("Solo se instala una orden CONFORME; la orden " + ordenId
                    + " esta " + orden.getEstado());
        }
        try {
            if (!laboratorioDAO.actualizarEstadoOrden(ordenId, "INSTALADA", null)) {
                throw new DentrixException("La orden " + ordenId + " no pudo instalarse");
            }
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public void anular(Long ordenId, String motivo) throws DentrixException {
        if (motivo == null || motivo.isBlank()) {
            throw new DentrixException("El motivo de la anulacion es obligatorio");
        }
        OrdenLaboratorio orden = buscarOrden(ordenId);
        if (orden.esInstalada()) {
            throw new DentrixException("La orden " + ordenId
                    + " ya esta INSTALADA y no se anula");
        }
        try {
            if (!laboratorioDAO.actualizarEstadoOrden(ordenId, "ANULADA", motivo.trim())) {
                throw new DentrixException("La orden " + ordenId + " no pudo anularse");
            }
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public List<OrdenLaboratorio> ordenes(Long presupuestoItemId) throws DentrixException {
        if (presupuestoItemId == null) {
            throw new DentrixException("El item del presupuesto es obligatorio para consultar sus ordenes");
        }
        return laboratorioDAO.listarOrdenes(presupuestoItemId);
    }

    public List<ControlLaboratorio> controles(Long ordenId) throws DentrixException {
        if (ordenId == null) {
            throw new DentrixException("La orden es obligatoria para consultar sus controles");
        }
        return laboratorioDAO.listarControles(ordenId);
    }

    private OrdenLaboratorio buscarOrden(Long ordenId) throws DentrixException {
        if (ordenId == null) {
            throw new DentrixException("La orden de laboratorio es obligatoria");
        }
        return laboratorioDAO.buscarOrden(ordenId)
                .orElseThrow(() -> new DentrixException(
                        "Orden de laboratorio no encontrada: " + ordenId));
    }

    private DentrixException traducir(DentrixException e) {
        String mensaje = mensajeCompleto(e);
        String estado = sqlState(e);
        if (mensaje.contains("R18")) {
            return new DentrixException("El flujo de laboratorio no permite esa operacion (R18): "
                    + primeraLinea(mensaje), e);
        }
        if (mensaje.contains("R19")) {
            return new DentrixException("La sesion ya no esta BORRADOR y no admite consumo de insumos (R19): "
                    + primeraLinea(mensaje), e);
        }
        if (mensaje.contains("R22")) {
            return new DentrixException("La orden de laboratorio no admite esa operacion (R22): "
                    + primeraLinea(mensaje), e);
        }
        if ("23505".equals(estado)) {
            return new DentrixException("La operacion genera un registro duplicado en el laboratorio "
                    + "(el lote ya se consumio en la sesion o el item ya tiene una orden activa).", e);
        }
        if ("23514".equals(estado)) {
            return new DentrixException("Los datos del laboratorio no cumplen una regla de validacion.", e);
        }
        return e;
    }

    private String primeraLinea(String mensaje) {
        int salto = mensaje.indexOf('\n');
        return salto > 0 ? mensaje.substring(0, salto) : mensaje;
    }

    private String mensajeCompleto(DentrixException e) {
        StringBuilder texto = new StringBuilder();
        Throwable actual = e;
        while (actual != null) {
            String parte = actual.getMessage();
            if (parte != null && !parte.isBlank()) {
                if (texto.length() > 0) {
                    texto.append(" | ");
                }
                texto.append(parte);
            }
            actual = actual.getCause();
        }
        return texto.toString();
    }

    private String sqlState(DentrixException e) {
        Throwable causa = e.getCause();
        while (causa != null) {
            if (causa instanceof SQLException sqle) {
                return sqle.getSQLState();
            }
            causa = causa.getCause();
        }
        return null;
    }
}
