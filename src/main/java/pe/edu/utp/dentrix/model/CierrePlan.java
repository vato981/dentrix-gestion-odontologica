package pe.edu.utp.dentrix.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CierrePlan {

    private Long id;
    private Long presupuestoId;
    private String estado;
    private String motivo;
    private LocalDateTime fecha;
    private Integer usuarioId;
    private LocalDate fechaRecall;

    public CierrePlan() {
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public LocalDate getFechaRecall() {
        return fechaRecall;
    }

    public void setFechaRecall(LocalDate fechaRecall) {
        this.fechaRecall = fechaRecall;
    }

    public boolean esTerminado() {
        return "TERMINADO".equals(estado);
    }
}
