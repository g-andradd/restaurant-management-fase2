package br.com.fiap.restaurant.infrastructure.web.dto;

import br.com.fiap.restaurant.domain.model.TipoCozinha;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record CreateRestaurantRequest(
        @NotBlank String nome,
        @NotBlank String endereco,
        @NotNull TipoCozinha tipoCozinha,
        @NotNull LocalTime horarioAbertura,
        @NotNull LocalTime horarioFechamento,
        @NotNull UUID ownerId) {
}
