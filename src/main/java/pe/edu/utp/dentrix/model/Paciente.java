package pe.edu.utp.dentrix.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Paciente {

    private Integer id;
    private String codigoHc;
    private String dni;
    private String apellidos;
    private String nombres;
    private LocalDate fechaNac;
    private String celular;
    private String email;
    private String direccion;
    private String alergias;
    private String antecedentes;
    private String tutorNombre;
    private String tutorDocumento;
    private String tutorVinculo;
    private String pagadorNombre;
    private String pagadorDocumento;
    private LocalDate fechaApertura;
    private String estado;
    private Integer usuarioRegistroId;
    private LocalDateTime fechaRegistro;

    public Paciente() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodigoHc() {
        return codigoHc;
    }

    public void setCodigoHc(String codigoHc) {
        this.codigoHc = codigoHc;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public LocalDate getFechaNac() {
        return fechaNac;
    }

    public void setFechaNac(LocalDate fechaNac) {
        this.fechaNac = fechaNac;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getAlergias() {
        return alergias;
    }

    public void setAlergias(String alergias) {
        this.alergias = alergias;
    }

    public String getAntecedentes() {
        return antecedentes;
    }

    public void setAntecedentes(String antecedentes) {
        this.antecedentes = antecedentes;
    }

    public String getTutorNombre() {
        return tutorNombre;
    }

    public void setTutorNombre(String tutorNombre) {
        this.tutorNombre = tutorNombre;
    }

    public String getTutorDocumento() {
        return tutorDocumento;
    }

    public void setTutorDocumento(String tutorDocumento) {
        this.tutorDocumento = tutorDocumento;
    }

    public String getTutorVinculo() {
        return tutorVinculo;
    }

    public void setTutorVinculo(String tutorVinculo) {
        this.tutorVinculo = tutorVinculo;
    }

    public String getPagadorNombre() {
        return pagadorNombre;
    }

    public void setPagadorNombre(String pagadorNombre) {
        this.pagadorNombre = pagadorNombre;
    }

    public String getPagadorDocumento() {
        return pagadorDocumento;
    }

    public void setPagadorDocumento(String pagadorDocumento) {
        this.pagadorDocumento = pagadorDocumento;
    }

    public LocalDate getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDate fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getUsuarioRegistroId() {
        return usuarioRegistroId;
    }

    public void setUsuarioRegistroId(Integer usuarioRegistroId) {
        this.usuarioRegistroId = usuarioRegistroId;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String nombreCompleto() {
        return apellidos + ", " + nombres;
    }
}
