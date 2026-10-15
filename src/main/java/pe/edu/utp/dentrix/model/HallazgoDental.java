package pe.edu.utp.dentrix.model;

import java.time.LocalDateTime;

public class HallazgoDental {

    private Integer pacienteId;
    private Integer piezaFdi;
    private String estado;
    private String cara;
    private String observacion;
    private Long ultimaFichaId;
    private LocalDateTime fechaRegistro;

    public HallazgoDental() {
    }

    public HallazgoDental(Integer pacienteId, Integer piezaFdi, String estado, String observacion) {
        this.pacienteId = pacienteId;
        this.piezaFdi = piezaFdi;
        this.estado = estado;
        this.observacion = observacion;
    }

    public Integer getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Integer pacienteId) {
        this.pacienteId = pacienteId;
    }

    public Integer getPiezaFdi() {
        return piezaFdi;
    }

    public void setPiezaFdi(Integer piezaFdi) {
        this.piezaFdi = piezaFdi;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCara() {
        return cara;
    }

    public void setCara(String cara) {
        this.cara = cara;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Long getUltimaFichaId() {
        return ultimaFichaId;
    }

    public void setUltimaFichaId(Long ultimaFichaId) {
        this.ultimaFichaId = ultimaFichaId;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
