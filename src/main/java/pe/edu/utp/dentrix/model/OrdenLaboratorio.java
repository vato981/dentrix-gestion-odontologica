package pe.edu.utp.dentrix.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class OrdenLaboratorio {

    private Long id;
    private Long presupuestoItemId;
    private Long ejecucionItemId;
    private String laboratorio;
    private String trabajo;
    private String material;
    private String color;
    private LocalDateTime fechaSolicitud;
    private LocalDate fechaPrometida;
    private String estado;
    private BigDecimal costo;
    private Long ordenOrigenId;
    private String motivoRechazo;
    private String motivoAnulacion;

    public OrdenLaboratorio() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPresupuestoItemId() {
        return presupuestoItemId;
    }

    public void setPresupuestoItemId(Long presupuestoItemId) {
        this.presupuestoItemId = presupuestoItemId;
    }

    public Long getEjecucionItemId() {
        return ejecucionItemId;
    }

    public void setEjecucionItemId(Long ejecucionItemId) {
        this.ejecucionItemId = ejecucionItemId;
    }

    public String getLaboratorio() {
        return laboratorio;
    }

    public void setLaboratorio(String laboratorio) {
        this.laboratorio = laboratorio;
    }

    public String getTrabajo() {
        return trabajo;
    }

    public void setTrabajo(String trabajo) {
        this.trabajo = trabajo;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public LocalDate getFechaPrometida() {
        return fechaPrometida;
    }

    public void setFechaPrometida(LocalDate fechaPrometida) {
        this.fechaPrometida = fechaPrometida;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }

    public Long getOrdenOrigenId() {
        return ordenOrigenId;
    }

    public void setOrdenOrigenId(Long ordenOrigenId) {
        this.ordenOrigenId = ordenOrigenId;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public boolean esSolicitada() {
        return "SOLICITADA".equals(estado);
    }

    public boolean esRecibida() {
        return "RECIBIDA".equals(estado);
    }

    public boolean esConforme() {
        return "CONFORME".equals(estado);
    }

    public boolean esInstalada() {
        return "INSTALADA".equals(estado);
    }

    public boolean estaAbierta() {
        return esSolicitada() || esRecibida() || esConforme();
    }
}
