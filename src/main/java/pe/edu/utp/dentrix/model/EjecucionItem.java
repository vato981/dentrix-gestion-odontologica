package pe.edu.utp.dentrix.model;

import java.time.LocalDateTime;

public class EjecucionItem {

    private Long id;
    private Long presupuestoItemId;
    private Integer numeroSesion;
    private Integer sesionesPlanificadas;
    private LocalDateTime fecha;
    private Integer profesionalId;
    private String profesionalNombre;
    private String procedimiento;
    private Integer piezaFdi;
    private String superficies;
    private String observaciones;
    private String estado;
    private String motivoAnulacion;

    public EjecucionItem() {
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

    public Integer getNumeroSesion() {
        return numeroSesion;
    }

    public void setNumeroSesion(Integer numeroSesion) {
        this.numeroSesion = numeroSesion;
    }

    public Integer getSesionesPlanificadas() {
        return sesionesPlanificadas;
    }

    public void setSesionesPlanificadas(Integer sesionesPlanificadas) {
        this.sesionesPlanificadas = sesionesPlanificadas;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Integer getProfesionalId() {
        return profesionalId;
    }

    public void setProfesionalId(Integer profesionalId) {
        this.profesionalId = profesionalId;
    }

    public String getProfesionalNombre() {
        return profesionalNombre;
    }

    public void setProfesionalNombre(String profesionalNombre) {
        this.profesionalNombre = profesionalNombre;
    }

    public String getProcedimiento() {
        return procedimiento;
    }

    public void setProcedimiento(String procedimiento) {
        this.procedimiento = procedimiento;
    }

    public Integer getPiezaFdi() {
        return piezaFdi;
    }

    public void setPiezaFdi(Integer piezaFdi) {
        this.piezaFdi = piezaFdi;
    }

    public String getSuperficies() {
        return superficies;
    }

    public void setSuperficies(String superficies) {
        this.superficies = superficies;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public boolean esBorrador() {
        return "BORRADOR".equals(estado);
    }

    public boolean esFirmada() {
        return "FIRMADA".equals(estado);
    }
}
