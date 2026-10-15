package pe.edu.utp.dentrix.model;

import java.math.BigDecimal;
import java.time.LocalTime;

public class Configuracion {

    private Integer id;
    private String razonSocial;
    private String ruc;
    private String direccion;
    private String telefono;
    private String email;
    private String representanteLegal;
    private String medicoDirector;
    private String licenciaSanitaria;
    private String licenciaMunicipal;
    private LocalTime horaApertura;
    private LocalTime horaCierre;
    private LocalTime almuerzoInicio;
    private LocalTime almuerzoFin;
    private BigDecimal igvPorcentaje;
    private String serieBoleta;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRepresentanteLegal() {
        return representanteLegal;
    }

    public void setRepresentanteLegal(String representanteLegal) {
        this.representanteLegal = representanteLegal;
    }

    public String getMedicoDirector() {
        return medicoDirector;
    }

    public void setMedicoDirector(String medicoDirector) {
        this.medicoDirector = medicoDirector;
    }

    public String getLicenciaSanitaria() {
        return licenciaSanitaria;
    }

    public void setLicenciaSanitaria(String licenciaSanitaria) {
        this.licenciaSanitaria = licenciaSanitaria;
    }

    public String getLicenciaMunicipal() {
        return licenciaMunicipal;
    }

    public void setLicenciaMunicipal(String licenciaMunicipal) {
        this.licenciaMunicipal = licenciaMunicipal;
    }

    public LocalTime getHoraApertura() {
        return horaApertura;
    }

    public void setHoraApertura(LocalTime horaApertura) {
        this.horaApertura = horaApertura;
    }

    public LocalTime getHoraCierre() {
        return horaCierre;
    }

    public void setHoraCierre(LocalTime horaCierre) {
        this.horaCierre = horaCierre;
    }

    public LocalTime getAlmuerzoInicio() {
        return almuerzoInicio;
    }

    public void setAlmuerzoInicio(LocalTime almuerzoInicio) {
        this.almuerzoInicio = almuerzoInicio;
    }

    public LocalTime getAlmuerzoFin() {
        return almuerzoFin;
    }

    public void setAlmuerzoFin(LocalTime almuerzoFin) {
        this.almuerzoFin = almuerzoFin;
    }

    public BigDecimal getIgvPorcentaje() {
        return igvPorcentaje;
    }

    public void setIgvPorcentaje(BigDecimal igvPorcentaje) {
        this.igvPorcentaje = igvPorcentaje;
    }

    public String getSerieBoleta() {
        return serieBoleta;
    }

    public void setSerieBoleta(String serieBoleta) {
        this.serieBoleta = serieBoleta;
    }
}
