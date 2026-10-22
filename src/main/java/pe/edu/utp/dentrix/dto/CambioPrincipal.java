package pe.edu.utp.dentrix.dto;

import java.time.LocalDateTime;

public record CambioPrincipal(Long fichaId, String cie10Anterior, String cie10Nuevo, String motivo,
                              Integer usuarioId, LocalDateTime fecha) {
}
