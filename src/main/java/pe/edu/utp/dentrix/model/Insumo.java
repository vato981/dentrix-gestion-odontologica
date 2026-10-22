package pe.edu.utp.dentrix.model;

public class Insumo {

    private Integer id;
    private String codigo;
    private String nombre;
    private String unidad;
    private Boolean requiereLote;
    private String estado;

    public Insumo() {
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

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public Boolean getRequiereLote() {
        return requiereLote;
    }

    public void setRequiereLote(Boolean requiereLote) {
        this.requiereLote = requiereLote;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isRequiereLote() {
        return Boolean.TRUE.equals(requiereLote);
    }

    public boolean isActivo() {
        return "ACTIVO".equals(estado);
    }
}
