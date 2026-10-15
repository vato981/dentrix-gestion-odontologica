package pe.edu.utp.dentrix.model;

import java.math.BigDecimal;

public class Tratamiento {

    private Integer id;
    private String codigo;
    private String nombre;
    private String especialidad;
    private String calculo;
    private BigDecimal precio;
    private String estado;

    public Tratamiento() {
    }

    public Tratamiento(String codigo, String nombre, String especialidad, String calculo, BigDecimal precio) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.especialidad = especialidad;
        this.calculo = calculo;
        this.precio = precio;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getCalculo() {
        return calculo;
    }

    public void setCalculo(String calculo) {
        this.calculo = calculo;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
