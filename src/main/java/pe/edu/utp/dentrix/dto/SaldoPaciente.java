package pe.edu.utp.dentrix.dto;

import java.math.BigDecimal;

public record SaldoPaciente(Integer pacienteId, String codigoHc, String nombre, BigDecimal saldo, long documentos) {
}
