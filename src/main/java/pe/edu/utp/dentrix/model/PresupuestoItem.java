package pe.edu.utp.dentrix.model;

import java.math.BigDecimal;

public class PresupuestoItem {
    private Long id;
    private Long presupuestoId;
    private Integer tratamientoId;
    private String tratamientoNombre;
    private Integer piezaFdi;
    private String fase;
    private Integer prioridad;
    private Integer secuencia;
    private Integer cantidad;
    private Integer sesionesPlanificadas;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private BigDecimal montoNeto;
    private Boolean precioCongelado;
    private String estado;
    private Boolean requiereConsentimiento;
    private String motivoRechazo;
    private Long enmiendaId;
    private BigDecimal saldoPendiente;

    public PresupuestoItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPresupuestoId() { return presupuestoId; }
    public void setPresupuestoId(Long presupuestoId) { this.presupuestoId = presupuestoId; }
    public Integer getTratamientoId() { return tratamientoId; }
    public void setTratamientoId(Integer tratamientoId) { this.tratamientoId = tratamientoId; }
    public String getTratamientoNombre() { return tratamientoNombre; }
    public void setTratamientoNombre(String tratamientoNombre) { this.tratamientoNombre = tratamientoNombre; }
    public Integer getPiezaFdi() { return piezaFdi; }
    public void setPiezaFdi(Integer piezaFdi) { this.piezaFdi = piezaFdi; }
    public String getFase() { return fase; }
    public void setFase(String fase) { this.fase = fase; }
    public Integer getPrioridad() { return prioridad; }
    public void setPrioridad(Integer prioridad) { this.prioridad = prioridad; }
    public Integer getSecuencia() { return secuencia; }
    public void setSecuencia(Integer secuencia) { this.secuencia = secuencia; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public Integer getSesionesPlanificadas() { return sesionesPlanificadas; }
    public void setSesionesPlanificadas(Integer sesionesPlanificadas) { this.sesionesPlanificadas = sesionesPlanificadas; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getMontoNeto() { return montoNeto; }
    public void setMontoNeto(BigDecimal montoNeto) { this.montoNeto = montoNeto; }
    public Boolean getPrecioCongelado() { return precioCongelado; }
    public void setPrecioCongelado(Boolean precioCongelado) { this.precioCongelado = precioCongelado; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Boolean getRequiereConsentimiento() { return requiereConsentimiento; }
    public void setRequiereConsentimiento(Boolean requiereConsentimiento) { this.requiereConsentimiento = requiereConsentimiento; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }
    public Long getEnmiendaId() { return enmiendaId; }
    public void setEnmiendaId(Long enmiendaId) { this.enmiendaId = enmiendaId; }
    public BigDecimal getSaldoPendiente() { return saldoPendiente; }
    public void setSaldoPendiente(BigDecimal saldoPendiente) { this.saldoPendiente = saldoPendiente; }
    public boolean isPrecioCongelado() { return Boolean.TRUE.equals(precioCongelado); }
    public boolean isRequiereConsentimiento() { return Boolean.TRUE.equals(requiereConsentimiento); }
}