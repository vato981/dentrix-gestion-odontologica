package pe.edu.utp.dentrix.model;

public class DiagnosticoCie10 {

    private String codigo;
    private String descripcion;
    private String capitulo;
    private boolean esFrecuente;

    public DiagnosticoCie10() {
    }

    public DiagnosticoCie10(String codigo, String descripcion, String capitulo, boolean esFrecuente) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.capitulo = capitulo;
        this.esFrecuente = esFrecuente;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCapitulo() {
        return capitulo;
    }

    public void setCapitulo(String capitulo) {
        this.capitulo = capitulo;
    }

    public boolean isEsFrecuente() {
        return esFrecuente;
    }

    public void setEsFrecuente(boolean esFrecuente) {
        this.esFrecuente = esFrecuente;
    }
}
