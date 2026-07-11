package br.com.fiap.restaurant.application.dto;

import java.util.UUID;

public record CreateUserCommand(String nome, String email, String login, String senha, String endereco,
                                 UUID userTypeId) {
}
