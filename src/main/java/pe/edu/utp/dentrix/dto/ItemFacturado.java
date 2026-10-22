package pe.edu.utp.dentrix.dto;

import java.math.BigDecimal;

public record ItemFacturado(Long presupuestoItemId, BigDecimal monto) {
}
