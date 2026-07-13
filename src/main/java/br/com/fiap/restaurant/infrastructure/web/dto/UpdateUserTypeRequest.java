package br.com.fiap.restaurant.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserTypeRequest(@NotBlank String nome, boolean podeSerDono) {
}
