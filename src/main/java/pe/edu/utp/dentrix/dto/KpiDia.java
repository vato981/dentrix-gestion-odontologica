package pe.edu.utp.dentrix.dto;

import java.time.LocalDate;
import java.math.BigDecimal;

public record KpiDia(LocalDate fecha, long citas, long enSala, BigDecimal recaudado, long deudores) {
}
