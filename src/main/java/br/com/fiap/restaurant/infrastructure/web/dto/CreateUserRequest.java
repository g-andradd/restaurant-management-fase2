package br.com.fiap.restaurant.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateUserRequest(
        @NotBlank String nome,
        @NotBlank @Email String email,
        @NotBlank String login,
        @NotBlank @Size(min = 8) String senha,
        String endereco,
        @NotNull UUID userTypeId) {
}
