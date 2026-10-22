package pe.edu.utp.dentrix.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Enmienda {

    private Long id;
    private Long presupuestoId;
    private Integer numero;
    private String tipo;
    private String motivo;
    private String estado;
    private BigDecimal descuentoAdicional;
    private String descuentoMotivo;
    private Integer descuentoAutorizadoPor;
    private String firmanteNombre;
    private String firmanteDocumento;
    private String firmanteRol;
    private LocalDateTime fechaFirma;
    private String firmaHash;
    private Integer firmadoPor;
    private Integer creadoPor;
    private LocalDateTime fechaCreacion;

    public Enmienda() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPresupuestoId() {
        return presupuestoId;
    }

    public void setPresupuestoId(Long presupuestoId) {
        this.presupuestoId = presupuestoId;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public BigDecimal getDescuentoAdicional() {
        return descuentoAdicional;
    }

    public void setDescuentoAdicional(BigDecimal descuentoAdicional) {
        this.descuentoAdicional = descuentoAdicional;
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

    public String getFirmanteNombre() {
        return firmanteNombre;
    }

    public void setFirmanteNombre(String firmanteNombre) {
        this.firmanteNombre = firmanteNombre;
    }

    public String getFirmanteDocumento() {
        return firmanteDocumento;
    }

    public void setFirmanteDocumento(String firmanteDocumento) {
        this.firmanteDocumento = firmanteDocumento;
    }

    public String getFirmanteRol() {
        return firmanteRol;
    }

    public void setFirmanteRol(String firmanteRol) {
        this.firmanteRol = firmanteRol;
    }

    public LocalDateTime getFechaFirma() {
        return fechaFirma;
    }

    public void setFechaFirma(LocalDateTime fechaFirma) {
        this.fechaFirma = fechaFirma;
    }

    public String getFirmaHash() {
        return firmaHash;
    }

    public void setFirmaHash(String firmaHash) {
        this.firmaHash = firmaHash;
    }

    public Integer getFirmadoPor() {
        return firmadoPor;
    }

    public void setFirmadoPor(Integer firmadoPor) {
        this.firmadoPor = firmadoPor;
    }

    public Integer getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(Integer creadoPor) {
        this.creadoPor = creadoPor;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public boolean esBorrador() {
        return "BORRADOR".equals(estado);
    }

    public boolean estaFirmada() {
        return "FIRMADA".equals(estado);
    }
}
