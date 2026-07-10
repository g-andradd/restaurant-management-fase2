package br.com.fiap.restaurant.application.dto;

import java.util.UUID;

public record UpdateUserCommand(UUID id, String nome, String email, String login, String senha, String endereco) {
}
