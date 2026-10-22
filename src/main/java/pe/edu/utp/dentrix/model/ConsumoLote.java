package pe.edu.utp.dentrix.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ConsumoLote {

    private Long id;
    private Long ejecucionItemId;
    private Long loteInsumoId;
    private String lote;
    private String insumoNombre;
    private BigDecimal cantidad;
    private LocalDateTime fecha;
    private Integer usuarioId;

    public ConsumoLote() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEjecucionItemId() {
        return ejecucionItemId;
    }

    public void setEjecucionItemId(Long ejecucionItemId) {
        this.ejecucionItemId = ejecucionItemId;
    }

    public Long getLoteInsumoId() {
        return loteInsumoId;
    }

    public void setLoteInsumoId(Long loteInsumoId) {
        this.loteInsumoId = loteInsumoId;
    }

    public String getLote() {
        return lote;
    }

    public void setLote(String lote) {
        this.lote = lote;
    }

    public String getInsumoNombre() {
        return insumoNombre;
    }

    public void setInsumoNombre(String insumoNombre) {
        this.insumoNombre = insumoNombre;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }
}
