package pe.edu.utp.dentrix.model;

import java.time.LocalDateTime;

public class ControlLaboratorio {

    private Long id;
    private Long ordenId;
    private LocalDateTime fecha;
    private String ajuste;
    private Boolean colorOk;
    private Boolean contactosOk;
    private Boolean conforme;
    private String observaciones;
    private Integer usuarioId;

    public ControlLaboratorio() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(Long ordenId) {
        this.ordenId = ordenId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getAjuste() {
        return ajuste;
    }

    public void setAjuste(String ajuste) {
        this.ajuste = ajuste;
    }

    public Boolean getColorOk() {
        return colorOk;
    }

    public void setColorOk(Boolean colorOk) {
        this.colorOk = colorOk;
    }

    public Boolean getContactosOk() {
        return contactosOk;
    }

    public void setContactosOk(Boolean contactosOk) {
        this.contactosOk = contactosOk;
    }

    public Boolean getConforme() {
        return conforme;
    }

    public void setConforme(Boolean conforme) {
        this.conforme = conforme;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public boolean isConforme() {
        return Boolean.TRUE.equals(conforme);
    }

    public boolean isColorOk() {
        return Boolean.TRUE.equals(colorOk);
    }

    public boolean isContactosOk() {
        return Boolean.TRUE.equals(contactosOk);
    }
}
