package br.com.fiap.restaurant.application.dto;

public record CreateUserCommand(String nome, String email, String login, String senha, String endereco) {
}
