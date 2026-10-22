package pe.edu.utp.dentrix.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PlanTratamiento {

    private Long id;
    private Integer pacienteId;
    private Integer tratamientoId;
    private Long fichaId;
    private Long pagoId;
    private Long presupuestoItemId;
    private Integer piezaFdi;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private String estado;
    private LocalDateTime fecha;

    public PlanTratamiento() {
    }

    public PlanTratamiento(Integer pacienteId, Integer tratamientoId, Integer piezaFdi,
                           Integer cantidad, BigDecimal precioUnitario, String estado) {
        this.pacienteId = pacienteId;
        this.tratamientoId = tratamientoId;
        this.piezaFdi = piezaFdi;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Integer pacienteId) {
        this.pacienteId = pacienteId;
    }

    public Integer getTratamientoId() {
        return tratamientoId;
    }

    public void setTratamientoId(Integer tratamientoId) {
        this.tratamientoId = tratamientoId;
    }

    public Long getFichaId() {
        return fichaId;
    }

    public void setFichaId(Long fichaId) {
        this.fichaId = fichaId;
    }

    public Long getPagoId() {
        return pagoId;
    }

    public void setPagoId(Long pagoId) {
        this.pagoId = pagoId;
    }

    public Long getPresupuestoItemId() {
        return presupuestoItemId;
    }

    public void setPresupuestoItemId(Long presupuestoItemId) {
        this.presupuestoItemId = presupuestoItemId;
    }

    public Integer getPiezaFdi() {
        return piezaFdi;
    }

    public void setPiezaFdi(Integer piezaFdi) {
        this.piezaFdi = piezaFdi;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
