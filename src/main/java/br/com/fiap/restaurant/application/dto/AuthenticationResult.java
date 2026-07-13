package br.com.fiap.restaurant.application.dto;

public record AuthenticationResult(String token, String tokenType, long expiresInSeconds, String subject) {

    public static AuthenticationResult bearer(String token, long expiresInSeconds, String subject) {
        return new AuthenticationResult(token, "Bearer", expiresInSeconds, subject);
    }
}
