package pe.edu.utp.dentrix.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Presupuesto {

    private Long id;
    private Integer pacienteId;
    private String pacienteNombre;
    private Long fichaId;
    private Long presupuestoAnteriorId;
    private Integer version;
    private String estado;
    private String moneda;
    private LocalDateTime fechaEmision;
    private Integer vigenciaDias;
    private LocalDate vigenciaHasta;
    private BigDecimal subtotal;
    private BigDecimal descuento;
    private BigDecimal total;
    private String descuentoMotivo;
    private Integer descuentoAutorizadoPor;
    private String motivoRechazo;
    private Integer creadoPor;

    public Presupuesto() {
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

    public String getPacienteNombre() {
        return pacienteNombre;
    }

    public void setPacienteNombre(String pacienteNombre) {
        this.pacienteNombre = pacienteNombre;
    }

    public Long getFichaId() {
        return fichaId;
    }

    public void setFichaId(Long fichaId) {
        this.fichaId = fichaId;
    }

    public Long getPresupuestoAnteriorId() {
        return presupuestoAnteriorId;
    }

    public void setPresupuestoAnteriorId(Long presupuestoAnteriorId) {
        this.presupuestoAnteriorId = presupuestoAnteriorId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public Integer getVigenciaDias() {
        return vigenciaDias;
    }

    public void setVigenciaDias(Integer vigenciaDias) {
        this.vigenciaDias = vigenciaDias;
    }

    public LocalDate getVigenciaHasta() {
        return vigenciaHasta;
    }

    public void setVigenciaHasta(LocalDate vigenciaHasta) {
        this.vigenciaHasta = vigenciaHasta;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getDescuentoMotivo() {
        return descuentoMotivo;
    }

    public void setDescuentoMotivo(String descuentoMotivo) {
        this.descuentoMotivo = descuentoMotivo;
    }

    public Integer getDescuentoAutorizadoPor() {
        return descuentoAutorizadoPor;
    }

    public void setDescuentoAutorizadoPor(Integer descuentoAutorizadoPor) {
        this.descuentoAutorizadoPor = descuentoAutorizadoPor;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public Integer getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(Integer creadoPor) {
        this.creadoPor = creadoPor;
    }

    public boolean esBorrador() {
        return "BORRADOR".equals(estado);
    }

    public boolean estaAceptado() {
        return "ACEPTADO".equals(estado);
    }
}
