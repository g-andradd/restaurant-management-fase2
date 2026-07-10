package br.com.fiap.restaurant.infrastructure.web.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
