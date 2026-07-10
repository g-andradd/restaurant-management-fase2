package br.com.fiap.restaurant.application.dto;

public record AuthenticateUserCommand(String login, String senha) {
}
