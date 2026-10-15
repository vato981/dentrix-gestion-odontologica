package pe.edu.utp.dentrix.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Pago {

    private Long id;
    private String numero;
    private String tipo;
    private String estado;
    private Long pagoOrigenId;
    private LocalDateTime fecha;
    private Integer pacienteId;
    private Long fichaId;
    private Integer odontologoId;
    private Integer usuarioCajaId;
    private Long cierreId;
    private String modalidad;
    private String operacionRef;
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal monto;
    private BigDecimal saldoDeudor;
    private String estadoPago;

    public Pago() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Long getPagoOrigenId() {
        return pagoOrigenId;
    }

    public void setPagoOrigenId(Long pagoOrigenId) {
        this.pagoOrigenId = pagoOrigenId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Integer getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Integer pacienteId) {
        this.pacienteId = pacienteId;
    }

    public Long getFichaId() {
        return fichaId;
    }

    public void setFichaId(Long fichaId) {
        this.fichaId = fichaId;
    }

    public Integer getOdontologoId() {
        return odontologoId;
    }

    public void setOdontologoId(Integer odontologoId) {
        this.odontologoId = odontologoId;
    }

    public Integer getUsuarioCajaId() {
        return usuarioCajaId;
    }

    public void setUsuarioCajaId(Integer usuarioCajaId) {
        this.usuarioCajaId = usuarioCajaId;
    }

    public Long getCierreId() {
        return cierreId;
    }

    public void setCierreId(Long cierreId) {
        this.cierreId = cierreId;
    }

    public String getModalidad() {
        return modalidad;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }

    public String getOperacionRef() {
        return operacionRef;
    }

    public void setOperacionRef(String operacionRef) {
        this.operacionRef = operacionRef;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getIgv() {
        return igv;
    }

    public void setIgv(BigDecimal igv) {
        this.igv = igv;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public BigDecimal getSaldoDeudor() {
        return saldoDeudor;
    }

    public void setSaldoDeudor(BigDecimal saldoDeudor) {
        this.saldoDeudor = saldoDeudor;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public boolean esAbono() {
        return pagoOrigenId != null;
    }
}
