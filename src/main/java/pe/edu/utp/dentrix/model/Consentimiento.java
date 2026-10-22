package pe.edu.utp.dentrix.model;

import java.time.LocalDateTime;

public class Consentimiento {

    private Long id;
    private Integer pacienteId;
    private Long presupuestoItemId;
    private Integer tratamientoId;
    private String procedimiento;
    private String riesgos;
    private String alternativas;
    private String estado;
    private String firmanteNombre;
    private String firmanteDocumento;
    private String representanteVinculo;
    private Integer profesionalId;
    private LocalDateTime fechaFirma;
    private String firmaHash;
    private String motivoRechazo;

    public Consentimiento() {
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

    public Long getPresupuestoItemId() {
        return presupuestoItemId;
    }

    public void setPresupuestoItemId(Long presupuestoItemId) {
        this.presupuestoItemId = presupuestoItemId;
    }

    public Integer getTratamientoId() {
        return tratamientoId;
    }

    public void setTratamientoId(Integer tratamientoId) {
        this.tratamientoId = tratamientoId;
    }

    public String getProcedimiento() {
        return procedimiento;
    }

    public void setProcedimiento(String procedimiento) {
        this.procedimiento = procedimiento;
    }

    public String getRiesgos() {
        return riesgos;
    }

    public void setRiesgos(String riesgos) {
        this.riesgos = riesgos;
    }

    public String getAlternativas() {
        return alternativas;
    }

    public void setAlternativas(String alternativas) {
        this.alternativas = alternativas;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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

    public String getRepresentanteVinculo() {
        return representanteVinculo;
    }

    public void setRepresentanteVinculo(String representanteVinculo) {
        this.representanteVinculo = representanteVinculo;
    }

    public Integer getProfesionalId() {
        return profesionalId;
    }

    public void setProfesionalId(Integer profesionalId) {
        this.profesionalId = profesionalId;
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

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public boolean estaFirmado() {
        return "FIRMADO".equals(estado);
    }
}
