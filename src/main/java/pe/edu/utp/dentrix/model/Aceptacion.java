package pe.edu.utp.dentrix.model;

import java.time.LocalDateTime;

public class Aceptacion {

    private Long id;
    private Long presupuestoId;
    private String tipo;
    private String firmanteNombre;
    private String firmanteDocumento;
    private String firmanteRol;
    private LocalDateTime fecha;
    private String firmaHash;
    private String alternativasOfrecidas;
    private String riesgosInformados;
    private Integer registradoPor;

    public Aceptacion() {
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
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

    public String getFirmanteRol() {
        return firmanteRol;
    }

    public void setFirmanteRol(String firmanteRol) {
        this.firmanteRol = firmanteRol;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getFirmaHash() {
        return firmaHash;
    }

    public void setFirmaHash(String firmaHash) {
        this.firmaHash = firmaHash;
    }

    public String getAlternativasOfrecidas() {
        return alternativasOfrecidas;
    }

    public void setAlternativasOfrecidas(String alternativasOfrecidas) {
        this.alternativasOfrecidas = alternativasOfrecidas;
    }

    public String getRiesgosInformados() {
        return riesgosInformados;
    }

    public void setRiesgosInformados(String riesgosInformados) {
        this.riesgosInformados = riesgosInformados;
    }

    public Integer getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(Integer registradoPor) {
        this.registradoPor = registradoPor;
    }

    public boolean esTotal() {
        return "TOTAL".equals(tipo);
    }
}
