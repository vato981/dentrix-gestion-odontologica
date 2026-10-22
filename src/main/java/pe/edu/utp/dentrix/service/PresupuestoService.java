package pe.edu.utp.dentrix.service;

import pe.edu.utp.dentrix.dao.FichaClinicaDAO;
import pe.edu.utp.dentrix.dao.PresupuestoDAO;
import pe.edu.utp.dentrix.dao.RolDAO;
import pe.edu.utp.dentrix.dao.TratamientoDAO;
import pe.edu.utp.dentrix.dao.UsuarioDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Aceptacion;
import pe.edu.utp.dentrix.model.Consentimiento;
import pe.edu.utp.dentrix.model.FichaClinica;
import pe.edu.utp.dentrix.model.Presupuesto;
import pe.edu.utp.dentrix.model.PresupuestoItem;
import pe.edu.utp.dentrix.model.Rol;
import pe.edu.utp.dentrix.model.Tratamiento;
import pe.edu.utp.dentrix.model.Usuario;
import pe.edu.utp.dentrix.util.FirmaSimulada;
import pe.edu.utp.dentrix.util.PiezaFdi;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class PresupuestoService {

    private static final Set<String> FASES = Set.of(
            "URGENTE", "BASICA", "RECONSTRUCTIVA", "QUIRURGICA", "REHABILITACION", "MANTENIMIENTO");
    private static final Set<String> ROLES_FIRMANTE = Set.of("PACIENTE", "TUTOR", "PAGADOR");
    private static final Set<String> ESTADOS_COBRABLES =
            Set.of("ACEPTADO", "EN_EJECUCION", "EJECUTADO", "CONTROLADO");

    private final PresupuestoDAO presupuestoDAO;
    private final TarifarioService tarifario;
    private final FichaClinicaDAO fichaDAO;
    private final TratamientoDAO tratamientoDAO;
    private final UsuarioDAO usuarioDAO;
    private final RolDAO rolDAO;

    public PresupuestoService(PresupuestoDAO presupuestoDAO, TarifarioService tarifario,
                              FichaClinicaDAO fichaDAO, TratamientoDAO tratamientoDAO,
                              UsuarioDAO usuarioDAO, RolDAO rolDAO) {
        this.presupuestoDAO = presupuestoDAO;
        this.tarifario = tarifario;
        this.fichaDAO = fichaDAO;
        this.tratamientoDAO = tratamientoDAO;
        this.usuarioDAO = usuarioDAO;
        this.rolDAO = rolDAO;
    }

    public Presupuesto crearDesdeFicha(Long fichaId, Integer usuarioId) throws DentrixException {
        if (fichaId == null) {
            throw new DentrixException("La ficha clinica es obligatoria para crear el presupuesto");
        }
        if (usuarioId == null) {
            throw new DentrixException("El usuario que crea el presupuesto es obligatorio");
        }
        FichaClinica ficha = fichaDAO.buscarPorId(fichaId)
                .orElseThrow(() -> new DentrixException("Ficha clinica no encontrada: " + fichaId));
        List<Presupuesto> existentes = presupuestoDAO.listarPorPaciente(ficha.getPacienteId());
        int version = 1;
        for (Presupuesto previo : existentes) {
            if (!fichaId.equals(previo.getFichaId())) {
                continue;
            }
            if ("BORRADOR".equals(previo.getEstado()) || "PRESENTADO".equals(previo.getEstado())) {
                throw new DentrixException("La ficha ya tiene un presupuesto vigente (v"
                        + previo.getVersion() + "); abralo o descartelo");
            }
            if (previo.getVersion() != null && previo.getVersion() >= version) {
                version = previo.getVersion() + 1;
            }
        }
        Presupuesto presupuesto = new Presupuesto();
        presupuesto.setPacienteId(ficha.getPacienteId());
        presupuesto.setFichaId(fichaId);
        presupuesto.setVersion(version);
        presupuesto.setEstado("BORRADOR");
        presupuesto.setMoneda("PEN");
        presupuesto.setFechaEmision(LocalDateTime.now());
        presupuesto.setVigenciaDias(15);
        presupuesto.setSubtotal(BigDecimal.ZERO);
        presupuesto.setDescuento(BigDecimal.ZERO);
        presupuesto.setCreadoPor(usuarioId);
        Long id;
        try {
            id = presupuestoDAO.insertar(presupuesto);
        } catch (DentrixException e) {
            throw traducir(e);
        }
        return buscar(id);
    }

    public PresupuestoItem agregarItem(Long presupuestoId, PresupuestoItem item) throws DentrixException {
        Presupuesto presupuesto = buscar(presupuestoId);
        if (!presupuesto.esBorrador()) {
            throw new DentrixException("Solo se agregan items a un presupuesto BORRADOR; el presupuesto "
                    + presupuestoId + " esta en " + presupuesto.getEstado());
        }
        if (item == null) {
            throw new DentrixException("El item del presupuesto es obligatorio");
        }
        if (item.getTratamientoId() == null) {
            throw new DentrixException("El item del presupuesto requiere un tratamiento");
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
        item.setPresupuestoId(presupuestoId);
        item.setFase(fase);
        item.setPrioridad(prioridad);
        item.setSecuencia(secuencia);
        item.setCantidad(cantidad);
        item.setSesionesPlanificadas(sesiones);
        item.setPrecioUnitario(precio);
        item.setPrecioCongelado(false);
        item.setEstado("PROPUESTO");
        item.setEnmiendaId(null);
        item.setMotivoRechazo(null);
        try {
            presupuestoDAO.insertarItem(item);
        } catch (DentrixException e) {
            throw traducir(e);
        }
        return presupuestoDAO.buscarItem(item.getId())
                .orElseThrow(() -> new DentrixException(
                        "No se pudo releer el item registrado: " + item.getId()));
    }

    public void quitarItem(Long presupuestoId, Long itemId) throws DentrixException {
        Presupuesto presupuesto = buscar(presupuestoId);
        if (!presupuesto.esBorrador()) {
            throw new DentrixException("Solo se quitan items de un presupuesto BORRADOR; el presupuesto "
                    + presupuestoId + " esta en " + presupuesto.getEstado());
        }
        PresupuestoItem item = presupuestoDAO.buscarItem(itemId)
                .orElseThrow(() -> new DentrixException("Item del presupuesto no encontrado: " + itemId));
        if (!presupuestoId.equals(item.getPresupuestoId())) {
            throw new DentrixException("El item " + itemId + " no pertenece al presupuesto " + presupuestoId);
        }
        try {
            if (!presupuestoDAO.eliminarItem(itemId)) {
                throw new DentrixException("El item " + itemId + " no pudo quitarse del presupuesto");
            }
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public void aplicarDescuento(Long presupuestoId, BigDecimal descuento, String motivo,
                                 Integer autorizadorId) throws DentrixException {
        Presupuesto presupuesto = buscar(presupuestoId);
        if (!presupuesto.esBorrador()) {
            throw new DentrixException("Solo se aplica descuento a un presupuesto BORRADOR; el presupuesto "
                    + presupuestoId + " esta en " + presupuesto.getEstado());
        }
        if (descuento == null) {
            throw new DentrixException("El descuento es obligatorio");
        }
        if (descuento.signum() < 0) {
            throw new DentrixException("El descuento no puede ser negativo");
        }
        BigDecimal subtotal = presupuesto.getSubtotal() == null ? BigDecimal.ZERO : presupuesto.getSubtotal();
        if (descuento.compareTo(subtotal) > 0) {
            throw new DentrixException("El descuento S/ " + descuento
                    + " no puede superar el subtotal S/ " + subtotal);
        }
        if (descuento.signum() > 0) {
            if (motivo == null || motivo.isBlank()) {
                throw new DentrixException("El descuento mayor a cero exige un motivo");
            }
            if (autorizadorId == null) {
                throw new DentrixException("El descuento mayor a cero exige un autorizador");
            }
            Usuario autorizador = usuarioDAO.buscarPorId(autorizadorId)
                    .orElseThrow(() -> new DentrixException(
                            "Autorizador del descuento no encontrado: " + autorizadorId));
            Rol rol = autorizador.getRolId() == null ? null
                    : rolDAO.buscarPorId(autorizador.getRolId()).orElse(null);
            if (rol == null || !"ADMINISTRADOR".equals(rol.getCodigo())) {
                throw new DentrixException("El descuento solo lo autoriza un usuario ADMINISTRADOR; el usuario "
                        + autorizadorId + " no lo es");
            }
        }
        try {
            if (!presupuestoDAO.actualizarDescuento(presupuestoId, descuento,
                    descuento.signum() > 0 ? motivo.trim() : null,
                    descuento.signum() > 0 ? autorizadorId : null)) {
                throw new DentrixException("El descuento no pudo aplicarse al presupuesto " + presupuestoId);
            }
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public void presentar(Long presupuestoId) throws DentrixException {
        Presupuesto presupuesto = buscar(presupuestoId);
        if (!presupuesto.esBorrador()) {
            throw new DentrixException("Solo se presenta un presupuesto BORRADOR; el presupuesto "
                    + presupuestoId + " esta en " + presupuesto.getEstado());
        }
        if (presupuestoDAO.listarItems(presupuestoId).isEmpty()) {
            throw new DentrixException("El presupuesto debe tener al menos un item para presentarse");
        }
        try {
            if (!presupuestoDAO.actualizarEstado(presupuestoId, "PRESENTADO", null)) {
                throw new DentrixException("El presupuesto " + presupuestoId + " no pudo presentarse");
            }
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public void aceptar(Long presupuestoId, List<Long> itemIds, String firmanteNombre,
                        String firmanteDocumento, String rolFirmante, String alternativas,
                        String riesgos, Integer registradoPor) throws DentrixException {
        Presupuesto presupuesto = buscar(presupuestoId);
        if (!"PRESENTADO".equals(presupuesto.getEstado())) {
            throw new DentrixException("Solo se acepta un presupuesto PRESENTADO; el presupuesto "
                    + presupuestoId + " esta en " + presupuesto.getEstado());
        }
        if (itemIds == null || itemIds.isEmpty()) {
            throw new DentrixException("La aceptacion debe incluir al menos un item");
        }
        if (firmanteNombre == null || firmanteNombre.isBlank()) {
            throw new DentrixException("El nombre del firmante es obligatorio");
        }
        if (firmanteDocumento == null || firmanteDocumento.isBlank()) {
            throw new DentrixException("El documento del firmante es obligatorio");
        }
        String rol = rolFirmante == null ? "" : rolFirmante.trim().toUpperCase(Locale.ROOT);
        if (!ROLES_FIRMANTE.contains(rol)) {
            throw new DentrixException("El rol del firmante debe ser PACIENTE, TUTOR o PAGADOR");
        }
        if (registradoPor == null) {
            throw new DentrixException("El usuario que registra la aceptacion es obligatorio");
        }
        List<PresupuestoItem> propuestos = new ArrayList<>();
        for (PresupuestoItem item : presupuestoDAO.listarItems(presupuestoId)) {
            if ("PROPUESTO".equals(item.getEstado())) {
                propuestos.add(item);
            }
        }
        if (propuestos.isEmpty()) {
            throw new DentrixException("El presupuesto no tiene items PROPUESTO para aceptar");
        }
        List<Long> aceptados = new ArrayList<>();
        List<Long> diferidos = new ArrayList<>();
        for (PresupuestoItem item : propuestos) {
            if (itemIds.contains(item.getId())) {
                aceptados.add(item.getId());
            } else {
                diferidos.add(item.getId());
            }
        }
        for (Long itemId : itemIds) {
            boolean pertenece = propuestos.stream().anyMatch(item -> itemId.equals(item.getId()));
            if (!pertenece) {
                throw new DentrixException("El item " + itemId
                        + " no pertenece al presupuesto " + presupuestoId + " o ya no esta PROPUESTO");
            }
        }
        if (aceptados.isEmpty()) {
            throw new DentrixException("La aceptacion debe incluir al menos un item vigente del presupuesto");
        }
        Aceptacion aceptacion = new Aceptacion();
        aceptacion.setPresupuestoId(presupuestoId);
        aceptacion.setTipo(aceptados.size() == propuestos.size() ? "TOTAL" : "PARCIAL");
        aceptacion.setFirmanteNombre(firmanteNombre.trim());
        aceptacion.setFirmanteDocumento(firmanteDocumento.trim());
        aceptacion.setFirmanteRol(rol);
        aceptacion.setFecha(LocalDateTime.now());
        aceptacion.setFirmaHash(FirmaSimulada.hash(firmanteNombre, firmanteDocumento,
                String.valueOf(presupuestoId), String.valueOf(System.currentTimeMillis())));
        aceptacion.setAlternativasOfrecidas(alternativas == null ? "" : alternativas.trim());
        aceptacion.setRiesgosInformados(riesgos == null ? "" : riesgos.trim());
        aceptacion.setRegistradoPor(registradoPor);
        try {
            presupuestoDAO.aplicarAceptacion(presupuestoId, diferidos, aceptacion);
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public void rechazar(Long presupuestoId, String motivo) throws DentrixException {
        Presupuesto presupuesto = buscar(presupuestoId);
        if (!"PRESENTADO".equals(presupuesto.getEstado())) {
            throw new DentrixException("Solo se rechaza un presupuesto PRESENTADO; el presupuesto "
                    + presupuestoId + " esta en " + presupuesto.getEstado());
        }
        if (motivo == null || motivo.isBlank()) {
            throw new DentrixException("El rechazo del presupuesto exige un motivo");
        }
        try {
            presupuestoDAO.aplicarRechazo(presupuestoId, motivo.trim());
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public List<Consentimiento> firmarConsentimiento(List<Long> itemIds, String procedimiento,
                                                     String riesgos, String alternativas,
                                                     String firmanteNombre, String documento,
                                                     String rol, String vinculo, Integer profesionalId)
            throws DentrixException {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new DentrixException("Debe indicar al menos un item para el consentimiento");
        }
        if (procedimiento == null || procedimiento.isBlank()) {
            throw new DentrixException("El procedimiento del consentimiento es obligatorio");
        }
        if (riesgos == null || riesgos.isBlank()) {
            throw new DentrixException("Los riesgos informados son obligatorios");
        }
        if (alternativas == null || alternativas.isBlank()) {
            throw new DentrixException("Las alternativas ofrecidas son obligatorias");
        }
        if (firmanteNombre == null || firmanteNombre.isBlank()) {
            throw new DentrixException("El nombre del firmante del consentimiento es obligatorio");
        }
        if (documento == null || documento.isBlank()) {
            throw new DentrixException("El documento del firmante del consentimiento es obligatorio");
        }
        String rolFirmante = rol == null ? "" : rol.trim().toUpperCase(Locale.ROOT);
        if (!ROLES_FIRMANTE.contains(rolFirmante)) {
            throw new DentrixException("El rol del firmante debe ser PACIENTE, TUTOR o PAGADOR");
        }
        if (profesionalId == null) {
            throw new DentrixException("El profesional que informa el consentimiento es obligatorio");
        }
        LocalDateTime fecha = LocalDateTime.now();
        String hash = FirmaSimulada.hash(firmanteNombre, documento, procedimiento,
                String.valueOf(fecha));
        List<Consentimiento> firmados = new ArrayList<>();
        for (Long itemId : itemIds) {
            PresupuestoItem item = presupuestoDAO.buscarItem(itemId)
                    .orElseThrow(() -> new DentrixException(
                            "Item del presupuesto no encontrado: " + itemId));
            Presupuesto presupuesto = presupuestoDAO.buscarPorId(item.getPresupuestoId())
                    .orElseThrow(() -> new DentrixException(
                            "Presupuesto no encontrado: " + item.getPresupuestoId()));
            if (!presupuesto.estaAceptado()) {
                throw new DentrixException("El item " + itemId + " pertenece a un presupuesto "
                        + presupuesto.getEstado() + "; el consentimiento exige un presupuesto ACEPTADO");
            }
            if ("ANULADO".equals(item.getEstado()) || "RECHAZADO".equals(item.getEstado())) {
                throw new DentrixException("El item " + itemId + " esta " + item.getEstado()
                        + "; no admite consentimiento");
            }
            if (presupuestoDAO.existeConsentimientoFirmado(itemId)) {
                throw new DentrixException("El item " + itemId + " ya tiene un consentimiento firmado");
            }
            Consentimiento consentimiento = new Consentimiento();
            consentimiento.setPacienteId(presupuesto.getPacienteId());
            consentimiento.setPresupuestoItemId(itemId);
            consentimiento.setTratamientoId(item.getTratamientoId());
            consentimiento.setProcedimiento(procedimiento.trim());
            consentimiento.setRiesgos(riesgos.trim());
            consentimiento.setAlternativas(alternativas.trim());
            consentimiento.setEstado("FIRMADO");
            consentimiento.setFirmanteNombre(firmanteNombre.trim());
            consentimiento.setFirmanteDocumento(documento.trim());
            consentimiento.setRepresentanteVinculo(vinculo == null || vinculo.isBlank()
                    ? null : vinculo.trim());
            consentimiento.setProfesionalId(profesionalId);
            consentimiento.setFechaFirma(fecha);
            consentimiento.setFirmaHash(hash);
            try {
                presupuestoDAO.insertarConsentimiento(consentimiento);
            } catch (DentrixException e) {
                throw traducir(e);
            }
            firmados.add(consentimiento);
        }
        return firmados;
    }

    public Presupuesto buscar(Long id) throws DentrixException {
        if (id == null) {
            throw new DentrixException("El identificador del presupuesto es obligatorio");
        }
        return presupuestoDAO.buscarPorId(id)
                .orElseThrow(() -> new DentrixException("Presupuesto no encontrado: " + id));
    }

    public List<PresupuestoItem> items(Long presupuestoId) throws DentrixException {
        buscar(presupuestoId);
        return presupuestoDAO.listarItems(presupuestoId);
    }

    public List<PresupuestoItem> itemsCobrables(Long presupuestoId) throws DentrixException {
        List<PresupuestoItem> cobrables = new ArrayList<>();
        for (PresupuestoItem item : items(presupuestoId)) {
            if (!ESTADOS_COBRABLES.contains(item.getEstado())) {
                continue;
            }
            if (presupuestoDAO.estaFacturado(item.getId())) {
                continue;
            }
            cobrables.add(item);
        }
        return cobrables;
    }

    public List<Presupuesto> listarPorPaciente(Integer pacienteId) throws DentrixException {
        if (pacienteId == null) {
            throw new DentrixException("El paciente es obligatorio para listar presupuestos");
        }
        return presupuestoDAO.listarPorPaciente(pacienteId);
    }

    public List<Presupuesto> listarPorEstado(String estado) throws DentrixException {
        if (estado == null || estado.isBlank()) {
            throw new DentrixException("El estado del presupuesto es obligatorio");
        }
        return presupuestoDAO.listarPorEstado(estado.trim().toUpperCase(Locale.ROOT));
    }

    public List<Presupuesto> listarRecientes(int limite) throws DentrixException {
        if (limite < 1) {
            throw new DentrixException("El limite de presupuestos recientes debe ser al menos 1");
        }
        return presupuestoDAO.listarRecientes(limite);
    }

    public BigDecimal totalAceptado(Long presupuestoId) throws DentrixException {
        buscar(presupuestoId);
        return presupuestoDAO.totalAceptado(presupuestoId);
    }

    public Optional<Aceptacion> aceptacion(Long presupuestoId) throws DentrixException {
        buscar(presupuestoId);
        return presupuestoDAO.buscarAceptacion(presupuestoId);
    }

    public List<Consentimiento> consentimientos(Long presupuestoId) throws DentrixException {
        buscar(presupuestoId);
        return presupuestoDAO.listarConsentimientos(presupuestoId);
    }

    public void descartar(Long presupuestoId) throws DentrixException {
        Presupuesto presupuesto = buscar(presupuestoId);
        if (!"BORRADOR".equals(presupuesto.getEstado()) && !"PRESENTADO".equals(presupuesto.getEstado())) {
            throw new DentrixException("Solo se descarta un presupuesto BORRADOR o PRESENTADO; el presupuesto "
                    + presupuestoId + " esta en " + presupuesto.getEstado()
                    + " y se conserva como evidencia (R22)");
        }
        try {
            if (!presupuestoDAO.eliminar(presupuestoId)) {
                throw new DentrixException("El presupuesto " + presupuestoId + " no pudo descartarse");
            }
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    private DentrixException traducir(DentrixException e) {
        String mensaje = e.getMessage() == null ? "" : e.getMessage();
        String estado = sqlState(e);
        if (mensaje.contains("R22")) {
            return new DentrixException("El presupuesto ya no admite cambios (R22): " + primeraLinea(mensaje), e);
        }
        if (mensaje.contains("R21")) {
            return new DentrixException("El precio del item esta congelado (R21).", e);
        }
        if (mensaje.contains("R7")) {
            return new DentrixException("El item exige presupuesto ACEPTADO y consentimiento firmado (R7).", e);
        }
        if ("23505".equals(estado)) {
            return new DentrixException("La operacion genera un registro duplicado en el presupuesto.", e);
        }
        if ("23514".equals(estado)) {
            return new DentrixException("Los datos del presupuesto no cumplen una regla de validacion.", e);
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
