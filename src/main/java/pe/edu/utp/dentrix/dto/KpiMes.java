package pe.edu.utp.dentrix.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record KpiMes(
        LocalDate desde,
        LocalDate hasta,
        long citas,
        long atendidas,
        long faltas,
        BigDecimal noShowPct,
        BigDecimal recaudado,
        BigDecimal efectivo,
        BigDecimal yape,
        BigDecimal plin,
        BigDecimal tarjeta
) {
}
