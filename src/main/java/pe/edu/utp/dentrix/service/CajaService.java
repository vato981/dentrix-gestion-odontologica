package pe.edu.utp.dentrix.service;

import pe.edu.utp.dentrix.dao.CierreCajaDAO;
import pe.edu.utp.dentrix.dao.ComprobanteDAO;
import pe.edu.utp.dentrix.dao.FichaClinicaDAO;
import pe.edu.utp.dentrix.dao.PacienteDAO;
import pe.edu.utp.dentrix.dao.PagoDAO;
import pe.edu.utp.dentrix.dao.PresupuestoDAO;
import pe.edu.utp.dentrix.dao.TratamientoDAO;
import pe.edu.utp.dentrix.dto.Imputacion;
import pe.edu.utp.dentrix.dto.ItemFacturado;
import pe.edu.utp.dentrix.dto.LineaBoleta;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.CierreCaja;
import pe.edu.utp.dentrix.model.ComprobanteItem;
import pe.edu.utp.dentrix.model.FichaClinica;
import pe.edu.utp.dentrix.model.Pago;
import pe.edu.utp.dentrix.model.Presupuesto;
import pe.edu.utp.dentrix.model.PresupuestoItem;
import pe.edu.utp.dentrix.model.Tratamiento;
import pe.edu.utp.dentrix.util.PiezaFdi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CajaService {

    private static final Set<String> MODALIDADES = Set.of("EFECTIVO", "YAPE", "PLIN", "TARJETA");
    private static final Set<String> ESTADOS_COBRABLES =
            Set.of("ACEPTADO", "EN_EJECUCION", "EJECUTADO", "CONTROLADO");

    private final PagoDAO pagoDAO;
    private final CierreCajaDAO cierreCajaDAO;
    private final PacienteDAO pacienteDAO;
    private final TratamientoDAO tratamientoDAO;
    private final FichaClinicaDAO fichaClinicaDAO;
    private final PresupuestoDAO presupuestoDAO;
    private final ComprobanteDAO comprobanteDAO;
    private final TarifarioService tarifario;

    public CajaService(PagoDAO pagoDAO, CierreCajaDAO cierreCajaDAO, PacienteDAO pacienteDAO,
                       TratamientoDAO tratamientoDAO, FichaClinicaDAO fichaClinicaDAO,
                       PresupuestoDAO presupuestoDAO, TarifarioService tarifario,
                       ComprobanteDAO comprobanteDAO) {
        this.pagoDAO = pagoDAO;
        this.cierreCajaDAO = cierreCajaDAO;
        this.pacienteDAO = pacienteDAO;
        this.tratamientoDAO = tratamientoDAO;
        this.fichaClinicaDAO = fichaClinicaDAO;
        this.presupuestoDAO = presupuestoDAO;
        this.comprobanteDAO = comprobanteDAO;
        this.tarifario = tarifario;
    }

    // =========================================================================
    // MÉTODO PARA LA CAJA: Calcula el saldo real de cada item
    // =========================================================================
    public Map<Long, BigDecimal> obtenerSaldosCobrables(Long presupuestoId) throws DentrixException {
        if (presupuestoId == null) {
            throw new DentrixException("El presupuesto es obligatorio para listar items");
        }
        Map<Long, BigDecimal> saldos = new HashMap<>();
        List<PresupuestoItem> items = presupuestoDAO.listarItems(presupuestoId);
        
        for (PresupuestoItem item : items) {
            if (!ESTADOS_COBRABLES.contains(item.getEstado())) {
                continue;
            }
            if (presupuestoDAO.estaFacturado(item.getId())) {
                continue;
            }
            BigDecimal monto = item.getMontoNeto() == null ? item.getSubtotal() : item.getMontoNeto();
            if (monto == null) monto = BigDecimal.ZERO;
            
            BigDecimal pagado = presupuestoDAO.imputadoDeItem(item.getId());
            if (pagado == null) pagado = BigDecimal.ZERO;
            
            BigDecimal saldo = monto.subtract(pagado);
            if (saldo.signum() < 0) saldo = BigDecimal.ZERO; 
            
            saldos.put(item.getId(), saldo);
        }
        return saldos;
    }

    public Pago crearBorrador(Integer pacienteId, Long fichaId, Integer usuarioCajaId, Long cierreId)
            throws DentrixException {
        if (pacienteId == null) {
            throw new DentrixException("El paciente es obligatorio para crear el comprobante");
        }
        pacienteDAO.buscarPorId(pacienteId)
                .orElseThrow(() -> new DentrixException("Paciente no encontrado"));
        if (usuarioCajaId == null) {
            throw new DentrixException("El usuario de caja es obligatorio para crear el comprobante");
        }
        Pago pago = new Pago();
        pago.setNumero(null);
        pago.setTipo("BOLETA");
        pago.setEstado("BORRADOR");
        pago.setFecha(LocalDateTime.now());
        pago.setPacienteId(pacienteId);
        pago.setFichaId(fichaId);
        pago.setUsuarioCajaId(usuarioCajaId);
        pago.setCierreId(cierreId);
        pago.setModalidad("EFECTIVO");
        pago.setSubtotal(BigDecimal.ZERO);
        pago.setIgv(BigDecimal.ZERO);
        pago.setMonto(BigDecimal.ZERO);
        pago.setSaldoDeudor(BigDecimal.ZERO);
        pago.setEstadoPago("PENDIENTE");
        try {
            pago.setId(comprobanteDAO.crearBorrador(pago));
        } catch (DentrixException e) {
            throw traducir(e);
        }
        return pagoDAO.buscarPorId(pago.getId())
                .orElseThrow(() -> new DentrixException("No se pudo releer el borrador creado"));
    }

    public ComprobanteItem agregarItem(Long borradorId, Long presupuestoItemId, String descripcion,
                                       BigDecimal cantidad, BigDecimal precioUnitario)
            throws DentrixException {
        if (borradorId == null) {
            throw new DentrixException("El comprobante en borrador es obligatorio");
        }
        Pago documento = pagoDAO.buscarPorId(borradorId)
                .orElseThrow(() -> new DentrixException("Comprobante no encontrado: " + borradorId));
        if (!"BORRADOR".equals(documento.getEstado())) {
            throw new DentrixException("El comprobante " + borradorId + " esta " + documento.getEstado()
                    + "; solo se edita el detalle de un BORRADOR");
        }
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new DentrixException("La cantidad del item debe ser mayor a 0");
        }
        if (precioUnitario != null && precioUnitario.signum() < 0) {
            throw new DentrixException("El precio unitario del item no puede ser negativo");
        }
        String texto = descripcion == null ? "" : descripcion.trim();
        BigDecimal precio = precioUnitario;
        if (presupuestoItemId != null) {
            PresupuestoItem item = presupuestoDAO.buscarItem(presupuestoItemId)
                    .orElseThrow(() -> new DentrixException(
                            "Item del presupuesto no encontrado: " + presupuestoItemId));
            Presupuesto presupuesto = presupuestoDAO.buscarPorId(item.getPresupuestoId())
                    .orElseThrow(() -> new DentrixException(
                            "Presupuesto no encontrado: " + item.getPresupuestoId()));
            if (!"ACEPTADO".equals(presupuesto.getEstado())) {
                throw new DentrixException("El presupuesto " + presupuesto.getId() + " esta en "
                        + presupuesto.getEstado() + "; solo se cobran items de un presupuesto ACEPTADO");
            }
            if (presupuestoDAO.estaFacturado(item.getId())) {
                throw new DentrixException("El item " + item.getId() + " ya fue facturado");
            }
            if (texto.isEmpty()) {
                texto = item.getTratamientoNombre() == null || item.getTratamientoNombre().isBlank()
                        ? "Item " + item.getId() : item.getTratamientoNombre();
            }
            if (precio == null) {
                precio = item.getMontoNeto() == null ? item.getSubtotal() : item.getMontoNeto();
            }
        }
        if (texto.isEmpty()) {
            throw new DentrixException("La descripcion del item es obligatoria");
        }
        if (precio == null) {
            precio = BigDecimal.ZERO;
        }
        ComprobanteItem item = new ComprobanteItem();
        item.setPagoId(borradorId);
        item.setPresupuestoItemId(presupuestoItemId);
        item.setDescripcion(texto);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(precio);
        item.setAfectacionIgv("10");
        try {
            Long id = comprobanteDAO.insertarItem(item);
            for (ComprobanteItem guardado : comprobanteDAO.listarItems(borradorId)) {
                if (id.equals(guardado.getId())) {
                    return guardado;
                }
            }
            throw new DentrixException("No se pudo releer el item del comprobante");
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public Pago confirmar(Long borradorId, String modalidad, String operacionRef) throws DentrixException {
        if (borradorId == null) {
            throw new DentrixException("El comprobante en borrador es obligatorio");
        }
        Pago documento = pagoDAO.buscarPorId(borradorId)
                .orElseThrow(() -> new DentrixException("Comprobante no encontrado: " + borradorId));
        if (!"BORRADOR".equals(documento.getEstado())) {
            throw new DentrixException("El comprobante " + borradorId + " esta " + documento.getEstado()
                    + "; solo se confirma un BORRADOR");
        }
        if (comprobanteDAO.listarItems(borradorId).isEmpty()) {
            throw new DentrixException("El comprobante " + borradorId
                    + " no tiene items; no se puede confirmar");
        }
        String mod = validarModalidad(modalidad, operacionRef);
        documento.setNumero(comprobanteDAO.siguienteNumero("B001"));
        documento.setModalidad(mod);
        documento.setOperacionRef(operacionRef);
        documento.setEstado("EMITIDO");
        try {
            pagoDAO.actualizar(documento);
        } catch (DentrixException e) {
            throw traducir(e);
        }
        return pagoDAO.buscarPorId(borradorId)
                .orElseThrow(() -> new DentrixException("No se pudo releer el comprobante emitido"));
    }

    public void anular(Long pagoId, String motivo, Integer usuarioId) throws DentrixException {
        if (pagoId == null) {
            throw new DentrixException("El comprobante a anular es obligatorio");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new DentrixException("El motivo de anulacion es obligatorio");
        }
        Pago documento = pagoDAO.buscarPorId(pagoId)
                .orElseThrow(() -> new DentrixException("Comprobante no encontrado: " + pagoId));
        if (documento.esAbono()) {
            throw new DentrixException("El id " + pagoId
                    + " corresponde a un abono; solo se anulan documentos");
        }
        if ("ANULADO".equals(documento.getEstado())) {
            throw new DentrixException("El comprobante " + pagoId + " ya esta ANULADO");
        }
        try {
            comprobanteDAO.anular(pagoId, motivo.trim(), usuarioId);
        } catch (DentrixException e) {
            throw traducir(e);
        }
    }

    public List<ComprobanteItem> itemsComprobante(Long pagoId) throws DentrixException {
        if (pagoId == null) {
            throw new DentrixException("El comprobante es obligatorio");
        }
        return comprobanteDAO.listarItems(pagoId);
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
        List<LineaBoleta> lineasItem = new ArrayList<>();
        List<LineaBoleta> lineasDirectas = new ArrayList<>();
        for (LineaBoleta linea : lineas) {
            if (linea == null) {
                throw new DentrixException("Cada linea de la boleta es obligatoria");
            }
            if (linea.esItem()) {
                lineasItem.add(linea);
            } else {
                lineasDirectas.add(linea);
            }
        }
        if (!lineasItem.isEmpty() && !lineasDirectas.isEmpty()) {
            throw new DentrixException(
                    "No se puede mezclar items del presupuesto con venta directa en la misma boleta");
        }
        Long fichaFinal = fichaId;
        List<ComprobanteItem> detalles = new ArrayList<>();
        if (!lineasItem.isEmpty()) {
            Long presupuestoId = null;
            Presupuesto presupuesto = null;
            for (LineaBoleta linea : lineasItem) {
                PresupuestoItem item = presupuestoDAO.buscarItem(linea.presupuestoItemId())
                        .orElseThrow(() -> new DentrixException(
                                "Item del presupuesto no encontrado: " + linea.presupuestoItemId()));
                if (presupuestoId == null) {
                    presupuestoId = item.getPresupuestoId();
                    Long presupuestoIdLeido = presupuestoId;
                    presupuesto = presupuestoDAO.buscarPorId(presupuestoIdLeido)
                            .orElseThrow(() -> new DentrixException(
                                    "Presupuesto no encontrado: " + presupuestoIdLeido));
                    if (!presupuesto.estaAceptado()) {
                        throw new DentrixException("El presupuesto " + presupuestoId + " esta en "
                                + presupuesto.getEstado()
                                + "; solo se cobran items de un presupuesto ACEPTADO");
                    }
                    if (!pacienteId.equals(presupuesto.getPacienteId())) {
                        throw new DentrixException("El presupuesto " + presupuestoId + " pertenece al paciente "
                                + presupuesto.getPacienteId() + " y no al paciente " + pacienteId);
                    }
                    if (fichaFinal == null) {
                        fichaFinal = presupuesto.getFichaId();
                    }
                } else if (!presupuestoId.equals(item.getPresupuestoId())) {
                    throw new DentrixException("Todos los items de la boleta deben ser del mismo presupuesto; "
                            + "el item " + item.getId() + " es del presupuesto " + item.getPresupuestoId());
                }
                if (!ESTADOS_COBRABLES.contains(item.getEstado())) {
                    throw new DentrixException("El item " + item.getId() + " esta en estado " + item.getEstado()
                            + "; solo se cobra un item ACEPTADO, EN_EJECUCION, EJECUTADO o CONTROLADO");
                }
                if (presupuestoDAO.estaFacturado(item.getId())) {
                    throw new DentrixException("El item " + item.getId() + " ya fue facturado");
                }
                BigDecimal monto = item.getMontoNeto() == null ? item.getSubtotal() : item.getMontoNeto();
                if (monto == null) {
                    monto = BigDecimal.ZERO;
                }
                ComprobanteItem detalle = new ComprobanteItem();
                detalle.setPresupuestoItemId(item.getId());
                detalle.setDescripcion(item.getTratamientoNombre() == null
                        || item.getTratamientoNombre().isBlank()
                        ? "Item " + item.getId() : item.getTratamientoNombre());
                detalle.setCantidad(BigDecimal.ONE);
                detalle.setPrecioUnitario(monto.setScale(2, RoundingMode.HALF_UP));
                detalles.add(detalle);
            }
        } else {
            if (fichaFinal != null) {
                FichaClinica ficha = fichaClinicaDAO.buscarPorId(fichaFinal)
                        .orElseThrow(() -> new DentrixException("Ficha clinica no encontrada"));
                if (!"ACEPTADA".equals(ficha.getEstadoDerivacion())) {
                    throw new DentrixException("La ficha está en derivación " + ficha.getEstadoDerivacion()
                            + "; solo se factura con derivación ACEPTADA (RF-07)");
                }
            }
            for (LineaBoleta linea : lineasDirectas) {
                if (linea.tratamientoId() == null) {
                    throw new DentrixException("Cada linea de la boleta debe indicar el tratamiento");
                }
                if (linea.cantidad() == null || linea.cantidad() < 1) {
                    throw new DentrixException("La cantidad de cada linea debe ser al menos 1");
                }
                if (!PiezaFdi.esValida(linea.piezaFdi())) {
                    throw new DentrixException("La pieza FDI " + linea.piezaFdi()
                            + " no es valida: debe estar entre 11 y 48 (diente 1-8) "
                            + "o entre 51 y 85 (diente 1-5)");
                }
                BigDecimal precio = tarifario.precioVigente(linea.tratamientoId());
                ComprobanteItem detalle = new ComprobanteItem();
                detalle.setDescripcion(tratamientoDAO.buscarPorId(linea.tratamientoId())
                        .map(Tratamiento::getNombre)
                        .orElse("Tratamiento " + linea.tratamientoId()));
                detalle.setCantidad(BigDecimal.valueOf(linea.cantidad()));
                detalle.setPrecioUnitario(precio);
                detalles.add(detalle);
            }
        }
        Pago borrador = crearBorrador(pacienteId, fichaFinal, usuarioCajaId, cierreId);
        if (odontologoId != null) {
            Pago guardado = pagoDAO.buscarPorId(borrador.getId()).orElse(borrador);
            guardado.setOdontologoId(odontologoId);
            try {
                pagoDAO.actualizar(guardado);
            } catch (DentrixException e) {
                throw traducir(e);
            }
        }
        for (ComprobanteItem detalle : detalles) {
            agregarItem(borrador.getId(), detalle.getPresupuestoItemId(), detalle.getDescripcion(),
                    detalle.getCantidad(), detalle.getPrecioUnitario());
        }
        return confirmar(borrador.getId(), mod, operacionRef);
    }

    public Pago registrarAbono(Long documentoId, BigDecimal monto, String modalidad, String operacionRef,
                               Integer usuarioCajaId) throws DentrixException {
        Pago documento = pagoDAO.buscarPorId(documentoId)
                .orElseThrow(() -> new DentrixException("Documento de pago no encontrado"));
        if (documento.getPagoOrigenId() != null) {
            throw new DentrixException("El id " + documentoId
                    + " corresponde a un abono; el abono debe apuntar a la boleta (RF-08)");
        }
        if ("BORRADOR".equals(documento.getEstado()) || "ANULADO".equals(documento.getEstado())) {
            throw new DentrixException("El documento " + documentoId + " esta " + documento.getEstado()
                    + "; solo se abona sobre un comprobante EMITIDO");
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
        List<ItemFacturado> facturados = pagoDAO.listarItemsFacturados(documentoId);
        List<Imputacion> imputaciones = new ArrayList<>();
        BigDecimal restante = monto;
        for (ItemFacturado item : facturados) {
            if (restante.signum() <= 0) {
                break;
            }
            BigDecimal ya = presupuestoDAO.imputadoDeItem(item.presupuestoItemId());
            BigDecimal saldo = item.monto().subtract(ya == null ? BigDecimal.ZERO : ya);
            if (saldo.signum() <= 0) {
                continue;
            }
            BigDecimal asignado = restante.min(saldo);
            imputaciones.add(new Imputacion(item.presupuestoItemId(), asignado));
            restante = restante.subtract(asignado);
        }
        try {
            if (imputaciones.isEmpty()) {
                abono.setId((long) pagoDAO.insertar(abono));
            } else {
                abono.setId(pagoDAO.insertarConImputaciones(abono, imputaciones, usuarioCajaId));
            }
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
        List<Pago> boletas = new ArrayList<>(pagoDAO.listarBoletasPorFecha(fecha));
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
        cierre.setTolerancia(BigDecimal.ONE);
        cierre.setMotivoDiferencia(null);
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
        if (!"ABIERTO".equals(cierre.getEstado())) {
            throw new DentrixException("Solo se cambia el fondo de un cierre en estado ABIERTO; el estado actual es "
                    + cierre.getEstado());
        }
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
        return consolidar(cierreId, efectivoContado, null, null, usuarioId);
    }

    public CierreCaja consolidar(Long cierreId, BigDecimal efectivoContado, BigDecimal tolerancia,
                                 String motivoDiferencia, Integer usuarioId)
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
        BigDecimal tol = tolerancia != null ? tolerancia
                : (cierre.getTolerancia() == null ? BigDecimal.ONE : cierre.getTolerancia());
        if (tol.signum() < 0) {
            throw new DentrixException("La tolerancia del arqueo no puede ser negativa");
        }
        String motivo = motivoDiferencia == null ? null : motivoDiferencia.trim();
        TotalesCierre totales = calcularTotales(pagoDAO.listarPorCierre(cierreId));
        BigDecimal fondo = cierre.getFondoInicial() == null ? BigDecimal.ZERO : cierre.getFondoInicial();
        BigDecimal diferencia = efectivoContado.subtract(fondo).subtract(totales.efectivo());
        if (diferencia.abs().compareTo(tol) > 0 && (motivo == null || motivo.isEmpty())) {
            throw new DentrixException("P6: la diferencia S/ " + diferencia.toPlainString()
                    + " supera la tolerancia S/ " + tol.toPlainString() + "; registra el motivo del arqueo");
        }
        cierre.setEfectivoSistema(totales.efectivo());
        cierre.setBilleterasSistema(totales.billeteras());
        cierre.setTotalSistema(totales.total());
        cierre.setEfectivoContado(efectivoContado);
        cierre.setTolerancia(tol);
        cierre.setMotivoDiferencia(motivo == null || motivo.isEmpty() ? null : motivo);
        cierre.setEstado("CONSOLIDADO");
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

    static TotalesCierre calcularTotales(List<Pago> pagos) {
        BigDecimal efectivo = BigDecimal.ZERO;
        BigDecimal billeteras = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        if (pagos == null) {
            return new TotalesCierre(efectivo, billeteras, total);
        }
        for (Pago pago : pagos) {
            if (pago == null || pago.getPagoOrigenId() == null) {
                continue;
            }
            BigDecimal monto = pago.getMonto() == null ? BigDecimal.ZERO : pago.getMonto();
            total = total.add(monto);
            if ("EFECTIVO".equals(pago.getModalidad())) {
                efectivo = efectivo.add(monto);
            } else if ("YAPE".equals(pago.getModalidad()) || "PLIN".equals(pago.getModalidad())) {
                billeteras = billeteras.add(monto);
            }
        }
        return new TotalesCierre(efectivo, billeteras, total);
    }

    record TotalesCierre(BigDecimal efectivo, BigDecimal billeteras, BigDecimal total) {
    }

    private CierreCaja cierrePorId(Long cierreId) throws DentrixException {
        if (cierreId == null) {
            throw new DentrixException("El identificador del cierre de caja es obligatorio");
        }
        return cierreCajaDAO.buscarPorId(cierreId)
                .orElseThrow(() -> new DentrixException("Cierre de caja no encontrado: " + cierreId));
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
            if (mensaje.contains("pagos registrados")) {
                return new DentrixException("R5: el documento tiene pagos registrados; "
                        + "requiere nota de credito (fuera de alcance v3).", e);
            }
            if (mensaje.contains("P5:")) {
                return new DentrixException("Regla del comprobante (P5): " + mensaje, e);
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