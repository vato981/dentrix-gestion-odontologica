package pe.edu.utp.dentrix.model;

public class FichaDiagnostico {

    private Long fichaId;
    private String cie10Codigo;
    private Integer piezaFdi;
    private boolean esPrincipal;
    private String observacion;

    public FichaDiagnostico() {
    }

    public FichaDiagnostico(Long fichaId, String cie10Codigo, Integer piezaFdi, boolean esPrincipal, String observacion) {
        this.fichaId = fichaId;
        this.cie10Codigo = cie10Codigo;
        this.piezaFdi = piezaFdi;
        this.esPrincipal = esPrincipal;
        this.observacion = observacion;
    }

    public Long getFichaId() {
        return fichaId;
    }

    public void setFichaId(Long fichaId) {
        this.fichaId = fichaId;
    }

    public String getCie10Codigo() {
        return cie10Codigo;
    }

    public void setCie10Codigo(String cie10Codigo) {
        this.cie10Codigo = cie10Codigo;
    }

    public Integer getPiezaFdi() {
        return piezaFdi;
    }

    public void setPiezaFdi(Integer piezaFdi) {
        this.piezaFdi = piezaFdi;
    }

    public boolean isEsPrincipal() {
        return esPrincipal;
    }

    public void setEsPrincipal(boolean esPrincipal) {
        this.esPrincipal = esPrincipal;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
