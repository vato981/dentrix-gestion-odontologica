package pe.edu.utp.dentrix.dto;

import java.math.BigDecimal;

public record ProductividadOdontologo(
        Integer usuarioId,
        String nombre,
        long atendidas,
        long faltas,
        BigDecimal recaudado
) {
}
