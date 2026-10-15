package pe.edu.utp.dentrix.dto;

import java.math.BigDecimal;

public record RankingTratamiento(String codigo, String nombre, long unidades, BigDecimal monto) {
}
