package pe.edu.utp.dentrix.model;

import java.time.LocalDate;

public class ControlPost {

    private Long id;
    private Long presupuestoItemId;
    private LocalDate fechaProgramada;
    private LocalDate fechaRealizada;
    private String estado;
    private String hallazgo;
    private Integer profesionalId;
    private String motivoCancelacion;

    public ControlPost() {
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

    public LocalDate getFechaProgramada() {
        return fechaProgramada;
    }

    public void setFechaProgramada(LocalDate fechaProgramada) {
        this.fechaProgramada = fechaProgramada;
    }

    public LocalDate getFechaRealizada() {
        return fechaRealizada;
    }

    public void setFechaRealizada(LocalDate fechaRealizada) {
        this.fechaRealizada = fechaRealizada;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getHallazgo() {
        return hallazgo;
    }

    public void setHallazgo(String hallazgo) {
        this.hallazgo = hallazgo;
    }

    public Integer getProfesionalId() {
        return profesionalId;
    }

    public void setProfesionalId(Integer profesionalId) {
        this.profesionalId = profesionalId;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public void setMotivoCancelacion(String motivoCancelacion) {
        this.motivoCancelacion = motivoCancelacion;
    }

    public boolean estaPendiente() {
        return "PENDIENTE".equals(estado);
    }
}
