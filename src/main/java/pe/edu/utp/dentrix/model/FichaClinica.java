package pe.edu.utp.dentrix.model;

import java.time.LocalDateTime;

public class FichaClinica {

    private Long id;
    private Integer pacienteId;
    private Long citaId;
    private Integer odontologoId;
    private LocalDateTime fecha;
    private String motivoConsulta;
    private String examenClinico;
    private String prescripcion;
    private String planIndicaciones;
    private String estadoDerivacion;
    private boolean derivadoACaja;
    private LocalDateTime fechaDerivacion;

    public FichaClinica() {
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

    public Long getCitaId() {
        return citaId;
    }

    public void setCitaId(Long citaId) {
        this.citaId = citaId;
    }

    public Integer getOdontologoId() {
        return odontologoId;
    }

    public void setOdontologoId(Integer odontologoId) {
        this.odontologoId = odontologoId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getMotivoConsulta() {
        return motivoConsulta;
    }

    public void setMotivoConsulta(String motivoConsulta) {
        this.motivoConsulta = motivoConsulta;
    }

    public String getExamenClinico() {
        return examenClinico;
    }

    public void setExamenClinico(String examenClinico) {
        this.examenClinico = examenClinico;
    }

    public String getPrescripcion() {
        return prescripcion;
    }

    public void setPrescripcion(String prescripcion) {
        this.prescripcion = prescripcion;
    }

    public String getPlanIndicaciones() {
        return planIndicaciones;
    }

    public void setPlanIndicaciones(String planIndicaciones) {
        this.planIndicaciones = planIndicaciones;
    }

    public String getEstadoDerivacion() {
        return estadoDerivacion;
    }

    public void setEstadoDerivacion(String estadoDerivacion) {
        this.estadoDerivacion = estadoDerivacion;
    }

    public boolean isDerivadoACaja() {
        return derivadoACaja;
    }

    public void setDerivadoACaja(boolean derivadoACaja) {
        this.derivadoACaja = derivadoACaja;
    }

    public LocalDateTime getFechaDerivacion() {
        return fechaDerivacion;
    }

    public void setFechaDerivacion(LocalDateTime fechaDerivacion) {
        this.fechaDerivacion = fechaDerivacion;
    }

    public boolean esCobrable() {
        return "ACEPTADA".equals(estadoDerivacion);
    }
}
