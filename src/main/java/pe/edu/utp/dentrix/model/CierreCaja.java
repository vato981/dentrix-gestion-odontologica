package pe.edu.utp.dentrix.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CierreCaja {

    private Long id;
    private LocalDate fecha;
    private String turno;
    private Integer cajeroId;
    private BigDecimal fondoInicial;
    private String historialFondos;
    private BigDecimal efectivoSistema;
    private BigDecimal billeterasSistema;
    private BigDecimal totalSistema;
    private BigDecimal efectivoContado;
    private BigDecimal diferencia;
    private BigDecimal tolerancia;
    private String motivoDiferencia;
    private String estado;
    private Integer consolidadoPor;
    private LocalDateTime consolidadoEn;
    private boolean actaGenerada;

    public CierreCaja() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }

    public Integer getCajeroId() {
        return cajeroId;
    }

    public void setCajeroId(Integer cajeroId) {
        this.cajeroId = cajeroId;
    }

    public BigDecimal getFondoInicial() {
        return fondoInicial;
    }

    public void setFondoInicial(BigDecimal fondoInicial) {
        this.fondoInicial = fondoInicial;
    }

    public String getHistorialFondos() {
        return historialFondos;
    }

    public void setHistorialFondos(String historialFondos) {
        this.historialFondos = historialFondos;
    }

    public BigDecimal getEfectivoSistema() {
        return efectivoSistema;
    }

    public void setEfectivoSistema(BigDecimal efectivoSistema) {
        this.efectivoSistema = efectivoSistema;
    }

    public BigDecimal getBilleterasSistema() {
        return billeterasSistema;
    }

    public void setBilleterasSistema(BigDecimal billeterasSistema) {
        this.billeterasSistema = billeterasSistema;
    }

    public BigDecimal getTotalSistema() {
        return totalSistema;
    }

    public void setTotalSistema(BigDecimal totalSistema) {
        this.totalSistema = totalSistema;
    }

    public BigDecimal getEfectivoContado() {
        return efectivoContado;
    }

    public void setEfectivoContado(BigDecimal efectivoContado) {
        this.efectivoContado = efectivoContado;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(BigDecimal diferencia) {
        this.diferencia = diferencia;
    }

    public BigDecimal getTolerancia() {
        return tolerancia;
    }

    public void setTolerancia(BigDecimal tolerancia) {
        this.tolerancia = tolerancia;
    }

    public String getMotivoDiferencia() {
        return motivoDiferencia;
    }

    public void setMotivoDiferencia(String motivoDiferencia) {
        this.motivoDiferencia = motivoDiferencia;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getConsolidadoPor() {
        return consolidadoPor;
    }

    public void setConsolidadoPor(Integer consolidadoPor) {
        this.consolidadoPor = consolidadoPor;
    }

    public LocalDateTime getConsolidadoEn() {
        return consolidadoEn;
    }

    public void setConsolidadoEn(LocalDateTime consolidadoEn) {
        this.consolidadoEn = consolidadoEn;
    }

    public boolean isActaGenerada() {
        return actaGenerada;
    }

    public void setActaGenerada(boolean actaGenerada) {
        this.actaGenerada = actaGenerada;
    }
}
