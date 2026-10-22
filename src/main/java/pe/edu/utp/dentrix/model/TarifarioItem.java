package pe.edu.utp.dentrix.model;

import java.math.BigDecimal;

public class TarifarioItem {

    private Long id;
    private Long tarifarioId;
    private Integer tratamientoId;
    private String tratamientoNombre;
    private BigDecimal precio;

    public TarifarioItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTarifarioId() {
        return tarifarioId;
    }

    public void setTarifarioId(Long tarifarioId) {
        this.tarifarioId = tarifarioId;
    }

    public Integer getTratamientoId() {
        return tratamientoId;
    }

    public void setTratamientoId(Integer tratamientoId) {
        this.tratamientoId = tratamientoId;
    }

    public String getTratamientoNombre() {
        return tratamientoNombre;
    }

    public void setTratamientoNombre(String tratamientoNombre) {
        this.tratamientoNombre = tratamientoNombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }
}
