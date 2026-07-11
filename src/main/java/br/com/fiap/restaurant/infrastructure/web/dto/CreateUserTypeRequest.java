package br.com.fiap.restaurant.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserTypeRequest(@NotBlank String nome) {
}
