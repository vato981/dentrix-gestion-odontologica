package pe.edu.utp.dentrix.model;

import java.time.LocalDate;

public class LoteInsumo {

    private Long id;
    private Integer insumoId;
    private String insumoNombre;
    private String numero;
    private LocalDate vence;

    public LoteInsumo() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getInsumoId() {
        return insumoId;
    }

    public void setInsumoId(Integer insumoId) {
        this.insumoId = insumoId;
    }

    public String getInsumoNombre() {
        return insumoNombre;
    }

    public void setInsumoNombre(String insumoNombre) {
        this.insumoNombre = insumoNombre;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDate getVence() {
        return vence;
    }

    public void setVence(LocalDate vence) {
        this.vence = vence;
    }

    public boolean estaVencido() {
        return vence != null && vence.isBefore(LocalDate.now());
    }
}
