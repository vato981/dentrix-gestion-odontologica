package pe.edu.utp.dentrix.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Tarifario {

    private Long id;
    private Integer version;
    private String moneda;
    private LocalDate vigenciaDesde;
    private LocalDate vigenciaHasta;
    private String estado;
    private String motivo;
    private Integer creadoPor;
    private Integer publicadoPor;
    private LocalDateTime fechaPublicacion;

    public Tarifario() {
    }

    public Tarifario(Integer version, String moneda, LocalDate vigenciaDesde, String motivo, Integer creadoPor) {
        this.version = version;
        this.moneda = moneda;
        this.vigenciaDesde = vigenciaDesde;
        this.motivo = motivo;
        this.creadoPor = creadoPor;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public LocalDate getVigenciaDesde() {
        return vigenciaDesde;
    }

    public void setVigenciaDesde(LocalDate vigenciaDesde) {
        this.vigenciaDesde = vigenciaDesde;
    }

    public LocalDate getVigenciaHasta() {
        return vigenciaHasta;
    }

    public void setVigenciaHasta(LocalDate vigenciaHasta) {
        this.vigenciaHasta = vigenciaHasta;
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

    public Integer getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(Integer creadoPor) {
        this.creadoPor = creadoPor;
    }

    public Integer getPublicadoPor() {
        return publicadoPor;
    }

    public void setPublicadoPor(Integer publicadoPor) {
        this.publicadoPor = publicadoPor;
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(LocalDateTime fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }

    public boolean esBorrador() {
        return "BORRADOR".equals(estado);
    }
}
