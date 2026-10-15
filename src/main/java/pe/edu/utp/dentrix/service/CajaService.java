package pe.edu.utp.dentrix.service;

import pe.edu.utp.dentrix.dao.CierreCajaDAO;
import pe.edu.utp.dentrix.dao.ConfiguracionDAO;
import pe.edu.utp.dentrix.dao.FichaClinicaDAO;
import pe.edu.utp.dentrix.dao.PacienteDAO;
import pe.edu.utp.dentrix.dao.PagoDAO;
import pe.edu.utp.dentrix.dao.PlanTratamientoDAO;
import pe.edu.utp.dentrix.dao.TratamientoDAO;
import pe.edu.utp.dentrix.dto.LineaBoleta;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.CierreCaja;
import pe.edu.utp.dentrix.model.Configuracion;
import pe.edu.utp.dentrix.model.FichaClinica;
import pe.edu.utp.dentrix.model.Paciente;
import pe.edu.utp.dentrix.model.Pago;
import pe.edu.utp.dentrix.model.PlanTratamiento;
import pe.edu.utp.dentrix.model.Tratamiento;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CajaService {

    private static final Set<String> MODALIDADES = Set.of("EFECTIVO", "YAPE", "PLIN", "TARJETA");

    private final PagoDAO pagoDAO;
    private final CierreCajaDAO cierreCajaDAO;
    private final PacienteDAO pacienteDAO;
    private final TratamientoDAO tratamientoDAO;
    private final FichaClinicaDAO fichaClinicaDAO;
    private final PlanTratamientoDAO planTratamientoDAO;
    private final ConfiguracionDAO configuracionDAO;

    public CajaService(PagoDAO pagoDAO, CierreCajaDAO cierreCajaDAO, PacienteDAO pacienteDAO,
                       TratamientoDAO tratamientoDAO, FichaClinicaDAO fichaClinicaDAO,
                       PlanTratamientoDAO planTratamientoDAO, ConfiguracionDAO configuracionDAO) {
        this.pagoDAO = pagoDAO;
        this.cierreCajaDAO = cierreCajaDAO;
        this.pacienteDAO = pacienteDAO;
        this.tratamientoDAO = tratamientoDAO;
        this.fichaClinicaDAO = fichaClinicaDAO;
        this.planTratamientoDAO = planTratamientoDAO;
        this.configuracionDAO = configuracionDAO;
    }

    public Pago emitirBoleta(Integer pacienteId, String modalidad, String operacionRef, Long fichaId,
                             Integer odontologoId, Integer usuarioCajaId, Long cierreId,
                             List<LineaBoleta> lineas) throws DentrixException {
        if (pacienteId == null) {
            throw new DentrixException("El paciente es obligatorio para emitir la boleta");
        }
        pacienteDAO.buscarPorId(pacienteId)
                .orElseThrow(() -> new DentrixException("Paciente no encontrado"));
        if (usuarioCajaId == null) {
            throw new DentrixException("El usuario de caja es obligatorio para emitir la boleta");
        }
        if (lineas == null || lineas.isEmpty()) {
            throw new DentrixException("La boleta debe incluir al menos un tratamiento");
        }
        String mod = validarModalidad(modalidad, operacionRef);
        if (fichaId != null) {
            FichaClinica ficha = fichaClinicaDAO.buscarPorId(fichaId)
                    .orElseThrow(() -> new DentrixException("Ficha clinica no encontrada"));
            if (!"ACEPTADA".equals(ficha.getEstadoDerivacion())) {
                throw new DentrixException("La ficha está en derivación " + ficha.getEstadoDerivacion()
                        + "; solo se factura con derivación ACEPTADA (RF-07)");
            }
        }
        List<Tratamiento> tarifario = new ArrayList<>();
        for (LineaBoleta linea : lineas) {
            if (linea.tratamientoId() == null) {
                throw new DentrixException("Cada linea de la boleta debe indicar el tratamiento");
            }
            if (linea.cantidad() == null || linea.cantidad() < 1) {
                throw new DentrixException("La cantidad de cada linea debe ser al menos 1");
            }
            if (linea.piezaFdi() != null && !esPiezaFdiValida(linea.piezaFdi())) {
                throw new DentrixException("La pieza FDI " + linea.piezaFdi()
                        + " no es valida: debe estar entre 11 y 48 y su segundo digito entre 1 y 8");
            }
            tarifario.add(tratamientoDAO.buscarPorId(linea.tratamientoId())
                    .orElseThrow(() -> new DentrixException(
                            "Tratamiento no encontrado en el tarifario: " + linea.tratamientoId())));
        }
        Pago pago = new Pago();
        pago.setNumero(siguienteNumeroBoleta());
        pago.setTipo("BOLETA");
        pago.setEstado("EMITIDO");
        pago.setFecha(LocalDateTime.now());
        pago.setPacienteId(pacienteId);
        pago.setFichaId(fichaId);
        pago.setOdontologoId(odontologoId);
        pago.setUsuarioCajaId(usuarioCajaId);
        pago.setCierreId(cierreId);
        pago.setModalidad(mod);
        pago.setOperacionRef(operacionRef);
        pago.setSubtotal(BigDecimal.ZERO);
        pago.setIgv(BigDecimal.ZERO);
        pago.setMonto(BigDecimal.ZERO);
        pago.setSaldoDeudor(BigDecimal.ZERO);
        pago.setEstadoPago("PENDIENTE");
        try {
            pago.setId((long) pagoDAO.insertar(pago));
        } catch (DentrixException e) {
            throw traducir(e);
        }
        for (int i = 0; i < lineas.size(); i++) {
            LineaBoleta linea = lineas.get(i);
            Tratamiento tratamiento = tarifario.get(i);
            PlanTratamiento detalle = new PlanTratamiento(pacienteId, linea.tratamientoId(),
                    linea.piezaFdi(), linea.cantidad(), tratamiento.getPrecio(), "COBRADO");
            detalle.setFichaId(fichaId);
            detalle.setPagoId(pago.getId());
            try {
                planTratamientoDAO.insertar(detalle);
            } catch (DentrixException e) {
                throw traducir(e);
            }
        }
        return pagoDAO.buscarPorId(pago.getId())
                .orElseThrow(() -> new DentrixException("No se pudo releer la boleta emitida"));
    }

    public Pago registrarAbono(Long documentoId, BigDecimal monto, String modalidad, String operacionRef,
                               Integer usuarioCajaId) throws DentrixException {
        Pago documento = pagoDAO.buscarPorId(documentoId)
                .orElseThrow(() -> new DentrixException("Documento de pago no encontrado"));
        if (documento.getPagoOrigenId() != null) {
            throw new DentrixException("El id " + documentoId
                    + " corresponde a un abono; el abono debe apuntar a la boleta (RF-08)");
        }
        if (usuarioCajaId == null) {
            throw new DentrixException("El usuario de caja es obligatorio para registrar el abono");
        }
        if (monto == null || monto.signum() <= 0) {
            throw new DentrixException("El monto del abono debe ser mayor a 0 (RF-08)");
        }
        String mod = validarModalidad(modalidad, operacionRef);
        if (!"PENDIENTE".equals(documento.getEstadoPago()) && !"PARCIAL".equals(documento.getEstadoPago())) {
            throw new DentrixException("El documento " + documento.getNumero()
                    + " no admite abonos porque su estado de pago es " + documento.getEstadoPago() + " (RF-08)");
        }
        if (monto.compareTo(documento.getSaldoDeudor()) > 0) {
            throw new DentrixException("RF-08: el abono excede el saldo pendiente (monto S/ " + monto
                    + " sobre saldo S/ " + documento.getSaldoDeudor() + ")");
        }
        Pago abono = new Pago();
        abono.setNumero(null);
        abono.setTipo("BOLETA");
        abono.setEstado("EMITIDO");
        abono.setPagoOrigenId(documentoId);
        abono.setFecha(LocalDateTime.now());
        abono.setPacienteId(documento.getPacienteId());
        abono.setUsuarioCajaId(usuarioCajaId);
        abono.setCierreId(documento.getCierreId());
        abono.setModalidad(mod);
        abono.setOperacionRef(operacionRef);
        abono.setSubtotal(BigDecimal.ZERO);
        abono.setIgv(BigDecimal.ZERO);
        abono.setMonto(monto);
        abono.setSaldoDeudor(BigDecimal.ZERO);
        abono.setEstadoPago("PENDIENTE");
        try {
            abono.setId((long) pagoDAO.insertar(abono));
        } catch (DentrixException e) {
            throw traducir(e);
        }
        return pagoDAO.buscarPorId(abono.getId())
                .orElseThrow(() -> new DentrixException("No se pudo releer el abono registrado"));
    }

    public List<Pago> boletasDelDia(LocalDate fecha) throws DentrixException {
        if (fecha == null) {
            throw new DentrixException("La fecha de las boletas es obligatoria");
        }
        List<Pago> boletas = new ArrayList<>();
        for (Pago pago : todosLosPagos()) {
            if (!pago.esAbono()
                    && "EMITIDO".equals(pago.getEstado())
                    && pago.getFecha() != null
                    && fecha.equals(pago.getFecha().toLocalDate())) {
                boletas.add(pago);
            }
        }
        boletas.sort(Comparator.comparing(Pago::getFecha).thenComparing(Pago::getId));
        return boletas;
    }

    public Pago buscarBoleta(String numero) throws DentrixException {
        if (numero == null || numero.isBlank()) {
            throw new DentrixException("El numero de la boleta es obligatorio");
        }
        return pagoDAO.buscarPorNumero(numero.trim())
                .orElseThrow(() -> new DentrixException("No existe la boleta " + numero));
    }

    public List<Pago> abonosDe(Long documentoId) throws DentrixException {
        return pagoDAO.listarAbonos(documentoId);
    }

    public List<Pago> documentosConSaldo() throws DentrixException {
        return pagoDAO.listarDocumentosConSaldo();
    }

    public CierreCaja abrirCierre(LocalDate fecha, String turno, Integer cajeroId,
                                  BigDecimal fondoInicial) throws DentrixException {
        if (fecha == null) {
            throw new DentrixException("La fecha del cierre es obligatoria");
        }
        if (cajeroId == null) {
            throw new DentrixException("El cajero del cierre es obligatorio");
        }
        String t = (turno == null || turno.isBlank()) ? "DIURNO" : turno.trim().toUpperCase(Locale.ROOT);
        BigDecimal fondo = fondoInicial == null ? BigDecimal.ZERO : fondoInicial;
        if (fondo.signum() < 0) {
            throw new DentrixException("El fondo inicial no puede ser negativo");
        }
        if (cierreCajaDAO.buscarPorFechaYTurno(fecha, t).isPresent()) {
            throw new DentrixException("Ya existe un cierre para esa fecha y turno");
        }
        CierreCaja cierre = new CierreCaja();
        cierre.setFecha(fecha);
        cierre.setTurno(t);
        cierre.setCajeroId(cajeroId);
        cierre.setFondoInicial(BigDecimal.ZERO);
        cierre.setHistorialFondos("[]");
        cierre.setEfectivoSistema(BigDecimal.ZERO);
        cierre.setBilleterasSistema(BigDecimal.ZERO);
        cierre.setTotalSistema(BigDecimal.ZERO);
        cierre.setEfectivoContado(null);
        cierre.setEstado("ABIERTO");
        cierre.setActaGenerada(false);
        try {
            cierreCajaDAO.insertar(cierre);
        } catch (DentrixException e) {
            throw traducir(e);
        }
        CierreCaja abierto = cierrePorId(cierre.getId());
        abierto.setFondoInicial(fondo);
        try {
            cierreCajaDAO.actualizar(abierto);
        } catch (DentrixException e) {
            throw traducir(e);
        }
        return cierrePorId(cierre.getId());
    }

    public CierreCaja buscarCierre(LocalDate fecha) throws DentrixException {
        if (fecha == null) {
            throw new DentrixException("La fecha del cierre es obligatoria");
        }
        return cierreCajaDAO.buscarPorFecha(fecha)
                .orElseThrow(() -> new DentrixException("Sin cierre de caja para esa fecha"));
    }

    public CierreCaja cambiarFondo(Long cierreId, BigDecimal nuevoFondo) throws DentrixException {
        CierreCaja cierre = cierrePorId(cierreId);
        if (nuevoFondo == null || nuevoFondo.signum() < 0) {
            throw new DentrixException("El fondo inicial no puede ser negativo");
        }
        cierre.setFondoInicial(nuevoFondo);
        try {
            cierreCajaDAO.actualizar(cierre);
        } catch (DentrixException e) {
            throw traducir(e);
        }
        return cierrePorId(cierreId);
    }

    public CierreCaja consolidar(Long cierreId, BigDecimal efectivoContado, Integer usuarioId)
            throws DentrixException {
        CierreCaja cierre = cierrePorId(cierreId);
        if (!"ABIERTO".equals(cierre.getEstado())) {
            throw new DentrixException("Solo se consolida un cierre en estado ABIERTO; el estado actual es "
                    + cierre.getEstado());
        }
        if (efectivoContado == null) {
            throw new DentrixException("El efectivo contado es obligatorio para consolidar el cierre");
        }
        if (usuarioId == null) {
            throw new DentrixException("El usuario que consolida el cierre es obligatorio");
        }
        cierre.setEstado("CONSOLIDADO");
        cierre.setEfectivoContado(efectivoContado);
        cierre.setConsolidadoPor(usuarioId);
        cierre.setConsolidadoEn(LocalDateTime.now());
        try {
            cierreCajaDAO.actualizar(cierre);
        } catch (DentrixException e) {
            throw traducir(e);
        }
        return cierrePorId(cierreId);
    }

    public void marcarActaGenerada(Long cierreId) throws DentrixException {
        CierreCaja cierre = cierrePorId(cierreId);
        if (!"CONSOLIDADO".equals(cierre.getEstado())) {
            throw new DentrixException("El acta solo se marca en un cierre CONSOLIDADO; el estado actual es "
                    + cierre.getEstado());
        }
        cierre.setActaGenerada(true);
        try {
            cierreCajaDAO.actualizar(cierre);
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    private String validarModalidad(String modalidad, String operacionRef) throws DentrixException {
        String mod = (modalidad == null || modalidad.isBlank())
                ? "EFECTIVO" : modalidad.trim().toUpperCase(Locale.ROOT);
        if (!MODALIDADES.contains(mod)) {
            throw new DentrixException("Modalidad de pago no valida: " + modalidad);
        }
        if (!"EFECTIVO".equals(mod) && (operacionRef == null || operacionRef.isBlank())) {
            throw new DentrixException("RF-08: Yape/Plin/Tarjeta requieren N° de operación");
        }
        return mod;
    }

    private String siguienteNumeroBoleta() throws DentrixException {
        String serie = configuracionDAO.obtener()
                .map(Configuracion::getSerieBoleta)
                .filter(s -> s != null && !s.isBlank())
                .orElse("B001")
                .trim();
        String prefijo = serie + "-";
        long maximo = 0L;
        for (Pago pago : todosLosPagos()) {
            if (pago.esAbono() || pago.getNumero() == null || !pago.getNumero().startsWith(prefijo)) {
                continue;
            }
            String sufijo = pago.getNumero().substring(prefijo.length());
            if (sufijo.matches("\\d+") && Long.parseLong(sufijo) > maximo) {
                maximo = Long.parseLong(sufijo);
            }
        }
        return serie + "-" + String.format(Locale.ROOT, "%06d", maximo + 1);
    }

    private List<Pago> todosLosPagos() throws DentrixException {
        List<Pago> pagos = new ArrayList<>();
        for (Paciente paciente : pacienteDAO.listarTodos()) {
            pagos.addAll(pagoDAO.listarPorPaciente(paciente.getId()));
        }
        return pagos;
    }

    private CierreCaja cierrePorId(Long cierreId) throws DentrixException {
        if (cierreId == null) {
            throw new DentrixException("El identificador del cierre de caja es obligatorio");
        }
        return cierreCajaDAO.listarTodos().stream()
                .filter(cierre -> cierreId.equals(cierre.getId()))
                .findFirst()
                .orElseThrow(() -> new DentrixException("Cierre de caja no encontrado: " + cierreId));
    }

    private boolean esPiezaFdiValida(Integer piezaFdi) {
        return piezaFdi >= 11 && piezaFdi <= 48 && piezaFdi % 10 >= 1 && piezaFdi % 10 <= 8;
    }

    private DentrixException traducir(DentrixException e) {
        if (e.getCause() instanceof SQLException sqle) {
            String estado = sqle.getSQLState();
            String mensaje = sqle.getMessage() == null ? "" : sqle.getMessage();
            if (mensaje.contains("RF-07")) {
                return new DentrixException(
                        "No se puede facturar la ficha: la derivacion no esta ACEPTADA (RF-07).", e);
            }
            if (mensaje.contains("RF-08")) {
                return new DentrixException("Regla de caja infringida (RF-08): " + mensaje, e);
            }
            if ("23505".equals(estado)) {
                return new DentrixException("Ya existe un comprobante con los mismos datos (duplicado).", e);
            }
            if ("23514".equals(estado)) {
                return new DentrixException("Los datos de caja no cumplen una regla de validacion.", e);
            }
        }
        return e;
    }
}
