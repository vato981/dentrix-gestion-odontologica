package pe.edu.utp.dentrix.model;

import java.math.BigDecimal;

public class EnmiendaItem {

    private Long id;
    private Long enmiendaId;
    private Long presupuestoItemId;
    private String accion;
    private String motivo;
    private String tratamientoNombre;
    private Integer piezaFdi;
    private BigDecimal monto;

    public EnmiendaItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEnmiendaId() {
        return enmiendaId;
    }

    public void setEnmiendaId(Long enmiendaId) {
        this.enmiendaId = enmiendaId;
    }

    public Long getPresupuestoItemId() {
        return presupuestoItemId;
    }

    public void setPresupuestoItemId(Long presupuestoItemId) {
        this.presupuestoItemId = presupuestoItemId;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getTratamientoNombre() {
        return tratamientoNombre;
    }

    public void setTratamientoNombre(String tratamientoNombre) {
        this.tratamientoNombre = tratamientoNombre;
    }

    public Integer getPiezaFdi() {
        return piezaFdi;
    }

    public void setPiezaFdi(Integer piezaFdi) {
        this.piezaFdi = piezaFdi;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }
}
