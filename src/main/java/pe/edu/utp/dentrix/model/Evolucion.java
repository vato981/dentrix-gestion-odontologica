package pe.edu.utp.dentrix.model;

import java.time.LocalDateTime;

public class Evolucion {

    private Long id;
    private Long fichaId;
    private Long ejecucionItemId;
    private String texto;
    private String estado;
    private Integer profesionalId;
    private LocalDateTime fecha;
    private LocalDateTime fechaFirma;
    private String firmaHash;
    private Long enmiendaDe;
    private String motivoEnmienda;

    public Evolucion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFichaId() {
        return fichaId;
    }

    public void setFichaId(Long fichaId) {
        this.fichaId = fichaId;
    }

    public Long getEjecucionItemId() {
        return ejecucionItemId;
    }

    public void setEjecucionItemId(Long ejecucionItemId) {
        this.ejecucionItemId = ejecucionItemId;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getProfesionalId() {
        return profesionalId;
    }

    public void setProfesionalId(Integer profesionalId) {
        this.profesionalId = profesionalId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
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

    public Long getEnmiendaDe() {
        return enmiendaDe;
    }

    public void setEnmiendaDe(Long enmiendaDe) {
        this.enmiendaDe = enmiendaDe;
    }

    public String getMotivoEnmienda() {
        return motivoEnmienda;
    }

    public void setMotivoEnmienda(String motivoEnmienda) {
        this.motivoEnmienda = motivoEnmienda;
    }

    public boolean esFirmada() {
        return "FIRMADA".equals(estado);
    }
}
