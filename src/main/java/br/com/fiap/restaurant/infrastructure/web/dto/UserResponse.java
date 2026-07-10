package br.com.fiap.restaurant.infrastructure.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(UUID id, String nome, String email, String login, String endereco,
                            LocalDateTime dataCriacao, LocalDateTime dataUltimaAlteracao) {
}
