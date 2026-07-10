package br.com.fiap.restaurant.application.port;

import java.util.Map;

public interface TokenProvider {

    String generateToken(String subject, Map<String, Object> claims);

    long getExpirationSeconds();

    /**
     * @throws br.com.fiap.restaurant.application.exception.InvalidTokenException if the token is missing, expired, or tampered with
     */
    String validateAndGetSubject(String token);
}
