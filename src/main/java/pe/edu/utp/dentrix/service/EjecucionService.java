package pe.edu.utp.dentrix.service;

import pe.edu.utp.dentrix.dao.EjecucionDAO;
import pe.edu.utp.dentrix.dao.FichaClinicaDAO;
import pe.edu.utp.dentrix.dao.PresupuestoDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.CierrePlan;
import pe.edu.utp.dentrix.model.ControlPost;
import pe.edu.utp.dentrix.model.EjecucionItem;
import pe.edu.utp.dentrix.model.Evolucion;
import pe.edu.utp.dentrix.model.Presupuesto;
import pe.edu.utp.dentrix.model.PresupuestoItem;
import pe.edu.utp.dentrix.util.FirmaSimulada;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class EjecucionService {

    private static final Set<String> ESTADOS_EJECUTABLES = Set.of("ACEPTADO", "EN_EJECUCION");
    private static final Set<String> ESTADOS_CIERRE = Set.of("TERMINADO", "ABANDONADO", "DERIVADO");
    private static final List<String> ORDEN_FASE = List.of(
            "URGENTE", "BASICA", "RECONSTRUCTIVA", "QUIRURGICA", "REHABILITACION", "MANTENIMIENTO");

    private static final Comparator<PresupuestoItem> ORDEN_EJECUCION =
            Comparator.comparingInt((PresupuestoItem item) -> ordenFase(item.getFase()))
                    .thenComparingInt(item -> item.getPrioridad() == null ? 3 : item.getPrioridad())
                    .thenComparingInt(item -> item.getSecuencia() == null ? 1 : item.getSecuencia())
                    .thenComparing(item -> item.getId() == null ? 0L : item.getId());

    private final EjecucionDAO ejecucionDAO;
    private final PresupuestoDAO presupuestoDAO;
    private final FichaClinicaDAO fichaClinicaDAO;

    public EjecucionService(EjecucionDAO ejecucionDAO, PresupuestoDAO presupuestoDAO,
                            FichaClinicaDAO fichaClinicaDAO) {
        this.ejecucionDAO = ejecucionDAO;
        this.presupuestoDAO = presupuestoDAO;
        this.fichaClinicaDAO = fichaClinicaDAO;
    }

    public List<PresupuestoItem> itemsEjecutables(Long presupuestoId) throws DentrixException {
        if (presupuestoId == null) {
            throw new DentrixException("El presupuesto es obligatorio para listar sus items ejecutables");
        }
        List<PresupuestoItem> ejecutables = new ArrayList<>();
        for (PresupuestoItem item : presupuestoDAO.listarItems(presupuestoId)) {
            if (!ESTADOS_EJECUTABLES.contains(item.getEstado())) {
                continue;
            }
            if (item.isRequiereConsentimiento()
                    && !presupuestoDAO.existeConsentimientoFirmado(item.getId())) {
                continue;
            }
            ejecutables.add(item);
        }
        ejecutables.sort(ORDEN_EJECUCION);
        return ejecutables;
    }

    public EjecucionItem abrirSesion(Long presupuestoItemId, String procedimiento, Integer piezaFdi,
                                     String superficies, Integer profesionalId) throws DentrixException {
        if (presupuestoItemId == null) {
            throw new DentrixException("El item del presupuesto es obligatorio para abrir la sesion");
        }
        if (procedimiento == null || procedimiento.isBlank()) {
            throw new DentrixException("El procedimiento de la sesion es obligatorio");
        }
        if (profesionalId == null) {
            throw new DentrixException("El profesional de la sesion es obligatorio");
        }
        PresupuestoItem item = presupuestoDAO.buscarItem(presupuestoItemId)
                .orElseThrow(() -> new DentrixException(
                        "Item del presupuesto no encontrado: " + presupuestoItemId));
        if (!ESTADOS_EJECUTABLES.contains(item.getEstado())) {
            throw new DentrixException("El item " + presupuestoItemId + " esta " + item.getEstado()
                    + " y no se puede ejecutar (R7)");
        }
        Optional<EjecucionItem> borrador = ejecucionDAO.buscarSesionBorrador(presupuestoItemId);
        if (borrador.isPresent()) {
            return borrador.get();
        }
        if (item.isRequiereConsentimiento() && !presupuestoDAO.existeConsentimientoFirmado(presupuestoItemId)) {
            throw new DentrixException("El item " + presupuestoItemId
                    + " exige consentimiento informado firmado (R7)");
        }
        int numero = 1;
        for (EjecucionItem previa : ejecucionDAO.listarEjecuciones(presupuestoItemId)) {
            if ("ANULADA".equals(previa.getEstado())) {
                continue;
            }
            if (previa.getNumeroSesion() != null && previa.getNumeroSesion() >= numero) {
                numero = previa.getNumeroSesion() + 1;
            }
        }
        EjecucionItem sesion = new EjecucionItem();
        sesion.setPresupuestoItemId(presupuestoItemId);
        sesion.setNumeroSesion(numero);
        sesion.setSesionesPlanificadas(item.getSesionesPlanificadas());
        sesion.setFecha(LocalDateTime.now());
        sesion.setProfesionalId(profesionalId);
        sesion.setProcedimiento(procedimiento.trim());
        sesion.setPiezaFdi(piezaFdi == null ? item.getPiezaFdi() : piezaFdi);
        sesion.setSuperficies(superficies == null || superficies.isBlank()
                ? null : superficies.trim());
        Long id;
        try {
            id = ejecucionDAO.insertarEjecucion(sesion);
        } catch (DentrixException e) {
            throw traducir(e);
        }
        return buscarEjecucion(id);
    }

    public Evolucion firmarSesion(Long ejecucionId, String texto, Integer profesionalId)
            throws DentrixException {
        if (ejecucionId == null) {
            throw new DentrixException("La sesion a firmar es obligatoria");
        }
        if (texto == null || texto.isBlank()) {
            throw new DentrixException("El texto de la evolucion es obligatorio");
        }
        if (profesionalId == null) {
            throw new DentrixException("El profesional que firma la evolucion es obligatorio");
        }
        EjecucionItem sesion = buscarEjecucion(ejecucionId);
        if (!sesion.esBorrador()) {
            throw new DentrixException("Solo se firma una sesion BORRADOR; la sesion " + ejecucionId
                    + " esta " + sesion.getEstado());
        }
        Long fichaId = fichaDeItem(sesion.getPresupuestoItemId());
        fichaClinicaDAO.buscarPorId(fichaId)
                .orElseThrow(() -> new DentrixException("Ficha clinica no encontrada: " + fichaId));
        String contenido = texto.trim();
        LocalDateTime ahora = LocalDateTime.now();
        Evolucion evolucion = new Evolucion();
        evolucion.setFichaId(fichaId);
        evolucion.setEjecucionItemId(ejecucionId);
        evolucion.setTexto(contenido);
        evolucion.setEstado("FIRMADA");
        evolucion.setProfesionalId(profesionalId);
        evolucion.setFecha(ahora);
        evolucion.setFechaFirma(ahora);
        evolucion.setFirmaHash(FirmaSimulada.hash(contenido, String.valueOf(profesionalId),
                String.valueOf(ejecucionId)));
        try {
            ejecucionDAO.insertarEvolucion(evolucion);
        } catch (DentrixException e) {
            throw traducir(e);
        }
        return ejecucionDAO.evolucionDeSesion(ejecucionId)
                .orElseThrow(() -> new DentrixException(
                        "No se pudo releer la evolucion firmada de la sesion " + ejecucionId));
    }

    public Evolucion enmendarEvolucion(Long evolucionId, String texto, String motivo,
                                       Integer profesionalId) throws DentrixException {
        if (evolucionId == null) {
            throw new DentrixException("La evolucion a enmendar es obligatoria");
        }
        if (texto == null || texto.isBlank()) {
            throw new DentrixException("El texto de la enmienda es obligatorio");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new DentrixException("El motivo de la enmienda es obligatorio");
        }
        if (profesionalId == null) {
            throw new DentrixException("El profesional que enmienda la evolucion es obligatorio");
        }
        Evolucion original = ejecucionDAO.buscarEvolucion(evolucionId)
                .orElseThrow(() -> new DentrixException("Evolucion no encontrada: " + evolucionId));
        if (!original.esFirmada()) {
            throw new DentrixException("Solo se enmienda una evolucion FIRMADA; la evolucion "
                    + evolucionId + " esta " + original.getEstado());
        }
        String contenido = texto.trim();
        LocalDateTime ahora = LocalDateTime.now();
        Evolucion enmienda = new Evolucion();
        enmienda.setFichaId(original.getFichaId());
        enmienda.setEjecucionItemId(original.getEjecucionItemId());
        enmienda.setTexto(contenido);
        enmienda.setEstado("FIRMADA");
        enmienda.setProfesionalId(profesionalId);
        enmienda.setFecha(ahora);
        enmienda.setFechaFirma(ahora);
        enmienda.setFirmaHash(FirmaSimulada.hash(contenido, String.valueOf(profesionalId),
                String.valueOf(evolucionId)));
        enmienda.setEnmiendaDe(evolucionId);
        enmienda.setMotivoEnmienda(motivo.trim());
        Long id;
        try {
            id = ejecucionDAO.insertarEvolucion(enmienda);
        } catch (DentrixException e) {
            throw traducir(e);
        }
        return ejecucionDAO.buscarEvolucion(id)
                .orElseThrow(() -> new DentrixException(
                        "No se pudo releer la enmienda registrada: " + id));
    }

    public void anularSesion(Long ejecucionId, String motivo) throws DentrixException {
        if (ejecucionId == null) {
            throw new DentrixException("La sesion a anular es obligatoria");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new DentrixException("El motivo de la anulacion es obligatorio");
        }
        EjecucionItem sesion = buscarEjecucion(ejecucionId);
        if (!sesion.esFirmada()) {
            if (sesion.esBorrador()) {
                throw new DentrixException("La sesion " + ejecucionId
                        + " esta BORRADOR; descartela en vez de anularla");
            }
            throw new DentrixException("Solo se anula una sesion FIRMADA; la sesion " + ejecucionId
                    + " esta " + sesion.getEstado());
        }
        PresupuestoItem item = presupuestoDAO.buscarItem(sesion.getPresupuestoItemId())
                .orElseThrow(() -> new DentrixException(
                        "Item del presupuesto no encontrado: " + sesion.getPresupuestoItemId()));
        if ("CONTROLADO".equals(item.getEstado())) {
            throw new DentrixException("El item ya tiene control realizado; no se anula la sesion");
        }
        try {
            if (!ejecucionDAO.anularEjecucion(ejecucionId, motivo.trim())) {
                throw new DentrixException("La sesion " + ejecucionId + " no pudo anularse");
            }
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public ControlPost programarControl(Long presupuestoItemId, LocalDate fecha, Integer usuarioId)
            throws DentrixException {
        if (presupuestoItemId == null) {
            throw new DentrixException("El item del presupuesto es obligatorio para programar el control");
        }
        if (fecha == null) {
            throw new DentrixException("La fecha programada del control es obligatoria");
        }
        if (fecha.isBefore(LocalDate.now())) {
            throw new DentrixException("La fecha del control no puede ser anterior a hoy");
        }
        if (usuarioId == null) {
            throw new DentrixException("El usuario que programa el control es obligatorio");
        }
        PresupuestoItem item = presupuestoDAO.buscarItem(presupuestoItemId)
                .orElseThrow(() -> new DentrixException(
                        "Item del presupuesto no encontrado: " + presupuestoItemId));
        if (!"EJECUTADO".equals(item.getEstado())) {
            throw new DentrixException("El item " + presupuestoItemId + " esta " + item.getEstado()
                    + "; el control post exige un item EJECUTADO");
        }
        if (ejecucionDAO.controlPendiente(presupuestoItemId).isPresent()) {
            throw new DentrixException("El item " + presupuestoItemId
                    + " ya tiene un control post PENDIENTE");
        }
        ControlPost control = new ControlPost();
        control.setPresupuestoItemId(presupuestoItemId);
        control.setFechaProgramada(fecha);
        control.setEstado("PENDIENTE");
        control.setProfesionalId(usuarioId);
        try {
            ejecucionDAO.insertarControl(control);
        } catch (DentrixException e) {
            throw traducir(e);
        }
        return ejecucionDAO.controlPendiente(presupuestoItemId)
                .orElseThrow(() -> new DentrixException(
                        "No se pudo releer el control programado del item " + presupuestoItemId));
    }

    public void realizarControl(Long controlId, LocalDate fecha, String hallazgo, Integer profesionalId)
            throws DentrixException {
        if (controlId == null) {
            throw new DentrixException("El control a realizar es obligatorio");
        }
        if (fecha == null) {
            throw new DentrixException("La fecha del control realizado es obligatoria");
        }
        if (hallazgo == null || hallazgo.isBlank()) {
            throw new DentrixException("El hallazgo del control es obligatorio");
        }
        if (profesionalId == null) {
            throw new DentrixException("El profesional que realiza el control es obligatorio");
        }
        try {
            if (!ejecucionDAO.realizarControl(controlId, fecha, hallazgo.trim(), profesionalId)) {
                throw new DentrixException("El control " + controlId
                        + " no existe o ya no esta PENDIENTE");
            }
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public CierrePlan cerrarPlan(Long presupuestoId, String estado, String motivo,
                                 LocalDate fechaRecall, Integer usuarioId) throws DentrixException {
        if (presupuestoId == null) {
            throw new DentrixException("El plan a cerrar es obligatorio");
        }
        String estadoCierre = estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT);
        if (!ESTADOS_CIERRE.contains(estadoCierre)) {
            throw new DentrixException("El estado del cierre debe ser TERMINADO, ABANDONADO o DERIVADO");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new DentrixException("El motivo del cierre es obligatorio");
        }
        if ("TERMINADO".equals(estadoCierre) && fechaRecall == null) {
            throw new DentrixException("El cierre TERMINADO exige una fecha de recall");
        }
        if (usuarioId == null) {
            throw new DentrixException("El usuario que cierra el plan es obligatorio");
        }
        presupuestoDAO.buscarPorId(presupuestoId)
                .orElseThrow(() -> new DentrixException("Presupuesto no encontrado: " + presupuestoId));
        CierrePlan cierre = new CierrePlan();
        cierre.setPresupuestoId(presupuestoId);
        cierre.setEstado(estadoCierre);
        cierre.setMotivo(motivo.trim());
        cierre.setFecha(LocalDateTime.now());
        cierre.setUsuarioId(usuarioId);
        cierre.setFechaRecall(fechaRecall);
        try {
            ejecucionDAO.insertarCierre(cierre);
        } catch (DentrixException e) {
            throw traducir(e);
        }
        return ejecucionDAO.buscarCierre(presupuestoId)
                .orElseThrow(() -> new DentrixException(
                        "No se pudo releer el cierre del plan " + presupuestoId));
    }

    public List<EjecucionItem> sesiones(Long presupuestoItemId) throws DentrixException {
        if (presupuestoItemId == null) {
            throw new DentrixException("El item del presupuesto es obligatorio para listar sus sesiones");
        }
        return ejecucionDAO.listarEjecuciones(presupuestoItemId);
    }

    public Optional<Evolucion> evolucionDeSesion(Long ejecucionId) throws DentrixException {
        if (ejecucionId == null) {
            throw new DentrixException("La sesion es obligatoria para consultar su evolucion");
        }
        return ejecucionDAO.evolucionDeSesion(ejecucionId);
    }

    public List<Evolucion> evolucionesDeFicha(Long fichaId) throws DentrixException {
        if (fichaId == null) {
            throw new DentrixException("La ficha es obligatoria para consultar sus evoluciones");
        }
        return ejecucionDAO.listarEvoluciones(fichaId);
    }

    public List<ControlPost> controles(Long presupuestoItemId) throws DentrixException {
        if (presupuestoItemId == null) {
            throw new DentrixException("El item del presupuesto es obligatorio para listar sus controles");
        }
        return ejecucionDAO.listarControles(presupuestoItemId);
    }

    public Optional<CierrePlan> cierre(Long presupuestoId) throws DentrixException {
        if (presupuestoId == null) {
            throw new DentrixException("El presupuesto es obligatorio para consultar su cierre");
        }
        return ejecucionDAO.buscarCierre(presupuestoId);
    }

    public void descartarSesion(Long ejecucionId) throws DentrixException {
        if (ejecucionId == null) {
            throw new DentrixException("La sesion a descartar es obligatoria");
        }
        EjecucionItem sesion = buscarEjecucion(ejecucionId);
        if (!sesion.esBorrador()) {
            throw new DentrixException("Solo se descarta una sesion BORRADOR; la sesion " + ejecucionId
                    + " esta " + sesion.getEstado());
        }
        try {
            if (!ejecucionDAO.eliminarEjecucion(ejecucionId)) {
                throw new DentrixException("La sesion " + ejecucionId + " no pudo descartarse");
            }
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    private EjecucionItem buscarEjecucion(Long id) throws DentrixException {
        return ejecucionDAO.buscarEjecucion(id)
                .orElseThrow(() -> new DentrixException("Sesion ejecutada no encontrada: " + id));
    }

    private Long fichaDeItem(Long presupuestoItemId) throws DentrixException {
        PresupuestoItem item = presupuestoDAO.buscarItem(presupuestoItemId)
                .orElseThrow(() -> new DentrixException(
                        "Item del presupuesto no encontrado: " + presupuestoItemId));
        Presupuesto presupuesto = presupuestoDAO.buscarPorId(item.getPresupuestoId())
                .orElseThrow(() -> new DentrixException(
                        "Presupuesto no encontrado: " + item.getPresupuestoId()));
        if (presupuesto.getFichaId() == null) {
            throw new DentrixException("El presupuesto " + presupuesto.getId()
                    + " no tiene ficha clinica asociada");
        }
        return presupuesto.getFichaId();
    }

    private static int ordenFase(String fase) {
        int indice = fase == null ? -1 : ORDEN_FASE.indexOf(fase.trim().toUpperCase(Locale.ROOT));
        return indice < 0 ? ORDEN_FASE.size() : indice;
    }

    private DentrixException traducir(DentrixException e) {
        String mensaje = mensajeCompleto(e);
        String estado = sqlState(e);
        if (mensaje.contains("R7")) {
            return new DentrixException("El item exige presupuesto ACEPTADO y consentimiento firmado (R7).", e);
        }
        if (mensaje.contains("R9")) {
            return new DentrixException("La sesion o la evolucion ya esta firmada y no se edita (R9).", e);
        }
        if (mensaje.contains("R17")) {
            return new DentrixException("El plan tiene items sin cerrar; no se cierra TERMINADO (R17).", e);
        }
        if (mensaje.contains("R22")) {
            return new DentrixException("La sesion ya no admite cambios (R22): " + primeraLinea(mensaje), e);
        }
        if ("23505".equals(estado)) {
            return new DentrixException("La operacion genera un registro duplicado en la ejecucion.", e);
        }
        if ("23514".equals(estado)) {
            return new DentrixException("Los datos de la ejecucion no cumplen una regla de validacion.", e);
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
