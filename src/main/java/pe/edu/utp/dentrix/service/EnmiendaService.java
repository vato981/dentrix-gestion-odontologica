package pe.edu.utp.dentrix.service;

import pe.edu.utp.dentrix.dao.EnmiendaDAO;
import pe.edu.utp.dentrix.dao.PresupuestoDAO;
import pe.edu.utp.dentrix.dao.TratamientoDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Enmienda;
import pe.edu.utp.dentrix.model.EnmiendaItem;
import pe.edu.utp.dentrix.model.Presupuesto;
import pe.edu.utp.dentrix.model.PresupuestoItem;
import pe.edu.utp.dentrix.model.Tratamiento;
import pe.edu.utp.dentrix.util.FirmaSimulada;
import pe.edu.utp.dentrix.util.PiezaFdi;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class EnmiendaService {

    private static final Set<String> FASES = Set.of(
            "URGENTE", "BASICA", "RECONSTRUCTIVA", "QUIRURGICA", "REHABILITACION", "MANTENIMIENTO");
    private static final Set<String> ROLES_FIRMANTE = Set.of("PACIENTE", "TUTOR", "PAGADOR");
    private static final Set<String> ESTADOS_EJECUTADOS =
            Set.of("EN_EJECUCION", "EJECUTADO", "CONTROLADO");
    private static final Set<String> ESTADOS_ANULABLES =
            Set.of("PROPUESTO", "ACEPTADO", "DIFERIDO");

    private final EnmiendaDAO enmiendaDAO;
    private final PresupuestoDAO presupuestoDAO;
    private final TarifarioService tarifario;
    private final TratamientoDAO tratamientoDAO;

    public EnmiendaService(EnmiendaDAO enmiendaDAO, PresupuestoDAO presupuestoDAO,
                           TarifarioService tarifario, TratamientoDAO tratamientoDAO) {
        this.enmiendaDAO = enmiendaDAO;
        this.presupuestoDAO = presupuestoDAO;
        this.tarifario = tarifario;
        this.tratamientoDAO = tratamientoDAO;
    }

    public Enmienda abrir(Long presupuestoId, String motivo, Integer usuarioId) throws DentrixException {
        if (presupuestoId == null) {
            throw new DentrixException("El presupuesto a enmendar es obligatorio");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new DentrixException("El motivo de la enmienda es obligatorio");
        }
        if (usuarioId == null) {
            throw new DentrixException("El usuario que abre la enmienda es obligatorio");
        }
        Presupuesto presupuesto = presupuestoDAO.buscarPorId(presupuestoId)
                .orElseThrow(() -> new DentrixException("Presupuesto no encontrado: " + presupuestoId));
        if (!presupuesto.estaAceptado()) {
            throw new DentrixException("Solo se enmienda un presupuesto ACEPTADO; el presupuesto "
                    + presupuestoId + " esta en " + presupuesto.getEstado());
        }
        Optional<Enmienda> borrador = enmiendaDAO.buscarBorrador(presupuestoId);
        if (borrador.isPresent()) {
            return borrador.get();
        }
        Enmienda enmienda = new Enmienda();
        enmienda.setPresupuestoId(presupuestoId);
        enmienda.setMotivo(motivo.trim());
        enmienda.setEstado("BORRADOR");
        enmienda.setDescuentoAdicional(BigDecimal.ZERO);
        enmienda.setCreadoPor(usuarioId);
        Long id;
        try {
            id = enmiendaDAO.insertar(enmienda);
        } catch (DentrixException e) {
            throw traducir(e);
        }
        return buscar(id);
    }

    public PresupuestoItem agregarItem(Long enmiendaId, PresupuestoItem item) throws DentrixException {
        Enmienda enmienda = buscar(enmiendaId);
        if (!enmienda.esBorrador()) {
            throw new DentrixException("Solo se agregan items a una enmienda BORRADOR; la enmienda "
                    + enmiendaId + " esta en " + enmienda.getEstado());
        }
        if (item == null) {
            throw new DentrixException("El item de la enmienda es obligatorio");
        }
        if (item.getTratamientoId() == null) {
            throw new DentrixException("El item de la enmienda requiere un tratamiento");
        }
        Tratamiento tratamiento = tratamientoDAO.buscarPorId(item.getTratamientoId())
                .orElseThrow(() -> new DentrixException(
                        "Tratamiento no encontrado: " + item.getTratamientoId()));
        if (!"ACTIVO".equals(tratamiento.getEstado())) {
            throw new DentrixException("El tratamiento " + tratamiento.getCodigo()
                    + " no esta ACTIVO; no se puede presupuestar");
        }
        if (!PiezaFdi.esValida(item.getPiezaFdi())) {
            throw new DentrixException("La pieza FDI " + item.getPiezaFdi()
                    + " no es valida: debe estar entre 11 y 48 (diente 1-8) o entre 51 y 85 (diente 1-5)");
        }
        int cantidad = item.getCantidad() == null ? 1 : item.getCantidad();
        if (cantidad < 1) {
            throw new DentrixException("La cantidad del item debe ser al menos 1");
        }
        int prioridad = item.getPrioridad() == null ? 3 : item.getPrioridad();
        if (prioridad < 1 || prioridad > 6) {
            throw new DentrixException("La prioridad del item debe estar entre 1 y 6");
        }
        int secuencia = item.getSecuencia() == null ? 1 : item.getSecuencia();
        if (secuencia < 1) {
            throw new DentrixException("La secuencia del item debe ser al menos 1");
        }
        int sesiones = item.getSesionesPlanificadas() == null ? 1 : item.getSesionesPlanificadas();
        if (sesiones < 1) {
            throw new DentrixException("Las sesiones planificadas deben ser al menos 1");
        }
        String fase = item.getFase() == null || item.getFase().isBlank()
                ? "BASICA" : item.getFase().trim().toUpperCase(Locale.ROOT);
        if (!FASES.contains(fase)) {
            throw new DentrixException("La fase del item no es valida: " + item.getFase());
        }
        BigDecimal precio = tarifario.precioVigente(item.getTratamientoId());
        item.setPresupuestoId(enmienda.getPresupuestoId());
        item.setEnmiendaId(enmiendaId);
        item.setFase(fase);
        item.setPrioridad(prioridad);
        item.setSecuencia(secuencia);
        item.setCantidad(cantidad);
        item.setSesionesPlanificadas(sesiones);
        item.setPrecioUnitario(precio);
        item.setPrecioCongelado(false);
        item.setEstado("PROPUESTO");
        item.setMotivoRechazo(null);
        try {
            presupuestoDAO.insertarItem(item);
        } catch (DentrixException e) {
            throw traducir(e);
        }
        EnmiendaItem detalle = new EnmiendaItem();
        detalle.setEnmiendaId(enmiendaId);
        detalle.setPresupuestoItemId(item.getId());
        detalle.setAccion("AGREGAR");
        try {
            enmiendaDAO.insertarItem(detalle);
        } catch (DentrixException e) {
            descartarItem(item.getId());
            throw traducir(e);
        }
        return presupuestoDAO.buscarItem(item.getId())
                .orElseThrow(() -> new DentrixException(
                        "No se pudo releer el item agregado: " + item.getId()));
    }

    public void marcarAnulacion(Long enmiendaId, Long presupuestoItemId, String motivo)
            throws DentrixException {
        Enmienda enmienda = buscar(enmiendaId);
        if (!enmienda.esBorrador()) {
            throw new DentrixException("Solo se anulan items en una enmienda BORRADOR; la enmienda "
                    + enmiendaId + " esta en " + enmienda.getEstado());
        }
        if (presupuestoItemId == null) {
            throw new DentrixException("El item a anular es obligatorio");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new DentrixException("El motivo de la anulacion es obligatorio");
        }
        PresupuestoItem item = presupuestoDAO.buscarItem(presupuestoItemId)
                .orElseThrow(() -> new DentrixException(
                        "Item del presupuesto no encontrado: " + presupuestoItemId));
        if (!enmienda.getPresupuestoId().equals(item.getPresupuestoId())) {
            throw new DentrixException("El item " + presupuestoItemId + " no pertenece al presupuesto "
                    + enmienda.getPresupuestoId());
        }
        if (ESTADOS_EJECUTADOS.contains(item.getEstado())) {
            throw new DentrixException("No se anula el item " + presupuestoItemId + ": ya esta "
                    + item.getEstado() + " (lo hecho, hecho esta)");
        }
        if (!ESTADOS_ANULABLES.contains(item.getEstado())) {
            throw new DentrixException("No se anula el item " + presupuestoItemId + ": esta "
                    + item.getEstado());
        }
        BigDecimal imputado = presupuestoDAO.imputadoDeItem(presupuestoItemId);
        if (imputado != null && imputado.signum() > 0) {
            throw new DentrixException("El item " + presupuestoItemId + " tiene S/ " + imputado
                    + " imputado; resuelvalo en caja antes de anularlo");
        }
        EnmiendaItem detalle = new EnmiendaItem();
        detalle.setEnmiendaId(enmiendaId);
        detalle.setPresupuestoItemId(presupuestoItemId);
        detalle.setAccion("ANULAR");
        detalle.setMotivo(motivo.trim());
        try {
            enmiendaDAO.insertarItem(detalle);
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public void firmar(Long enmiendaId, String firmanteNombre, String firmanteDocumento,
                       String rolFirmante, Integer usuarioId) throws DentrixException {
        Enmienda enmienda = buscar(enmiendaId);
        if (!enmienda.esBorrador()) {
            throw new DentrixException("Solo se firma una enmienda BORRADOR; la enmienda "
                    + enmiendaId + " esta en " + enmienda.getEstado());
        }
        if (enmiendaDAO.listarItems(enmiendaId).isEmpty()) {
            throw new DentrixException("La enmienda " + enmiendaId
                    + " no tiene items: no hay nada que firmar");
        }
        if (firmanteNombre == null || firmanteNombre.isBlank()) {
            throw new DentrixException("El nombre del firmante de la enmienda es obligatorio");
        }
        if (firmanteDocumento == null || firmanteDocumento.isBlank()) {
            throw new DentrixException("El documento del firmante de la enmienda es obligatorio");
        }
        String rol = rolFirmante == null ? "" : rolFirmante.trim().toUpperCase(Locale.ROOT);
        if (!ROLES_FIRMANTE.contains(rol)) {
            throw new DentrixException("El rol del firmante debe ser PACIENTE, TUTOR o PAGADOR");
        }
        if (usuarioId == null) {
            throw new DentrixException("El usuario que registra la firma es obligatorio");
        }
        String hash = FirmaSimulada.hash(firmanteNombre, firmanteDocumento, String.valueOf(enmiendaId));
        try {
            enmiendaDAO.actualizarFirma(enmiendaId, firmanteNombre.trim(), firmanteDocumento.trim(),
                    rol, hash);
            enmiendaDAO.firmar(enmiendaId, usuarioId);
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public void descartar(Long enmiendaId) throws DentrixException {
        Enmienda enmienda = buscar(enmiendaId);
        if (!enmienda.esBorrador()) {
            throw new DentrixException("Solo se descarta una enmienda BORRADOR; la enmienda "
                    + enmiendaId + " esta en " + enmienda.getEstado());
        }
        try {
            enmiendaDAO.descartar(enmiendaId);
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public Enmienda buscar(Long id) throws DentrixException {
        if (id == null) {
            throw new DentrixException("El identificador de la enmienda es obligatorio");
        }
        return enmiendaDAO.buscarPorId(id)
                .orElseThrow(() -> new DentrixException("Enmienda no encontrada: " + id));
    }

    public List<Enmienda> listarPorPresupuesto(Long presupuestoId) throws DentrixException {
        if (presupuestoId == null) {
            throw new DentrixException("El presupuesto es obligatorio para listar sus enmiendas");
        }
        return enmiendaDAO.listarPorPresupuesto(presupuestoId);
    }

    public List<EnmiendaItem> items(Long enmiendaId) throws DentrixException {
        buscar(enmiendaId);
        return enmiendaDAO.listarItems(enmiendaId);
    }

    private void descartarItem(Long itemId) {
        try {
            presupuestoDAO.eliminarItem(itemId);
        } catch (DentrixException ignorada) {
            return;
        }
    }

    private DentrixException traducir(DentrixException e) {
        String mensaje = e.getMessage() == null ? "" : e.getMessage();
        String estado = sqlState(e);
        if (mensaje.contains("R22")) {
            return new DentrixException("La enmienda ya no admite cambios (R22): " + primeraLinea(mensaje), e);
        }
        if (mensaje.contains("R21")) {
            return new DentrixException("El precio del item esta congelado (R21).", e);
        }
        if (mensaje.contains("R7")) {
            return new DentrixException("El item exige presupuesto ACEPTADO y consentimiento firmado (R7).", e);
        }
        if ("23505".equals(estado)) {
            return new DentrixException("La operacion genera un registro duplicado en la enmienda.", e);
        }
        if ("23514".equals(estado)) {
            return new DentrixException("Los datos de la enmienda no cumplen una regla de validacion.", e);
        }
        return e;
    }

    private String primeraLinea(String mensaje) {
        int salto = mensaje.indexOf('\n');
        return salto > 0 ? mensaje.substring(0, salto) : mensaje;
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
