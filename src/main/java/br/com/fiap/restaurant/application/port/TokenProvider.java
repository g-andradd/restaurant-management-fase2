package br.com.fiap.restaurant.application.port;

import java.util.Map;

/**
 * The stateless boundary between the JWT library (an infrastructure
 * concern) and the application layer. {@link #validateAndGetSubject} throws
 * one exception uniformly whether the token is missing, expired, or
 * tampered with, so callers never need to distinguish those cases.
 */
public interface TokenProvider {

    String generateToken(String subject, Map<String, Object> claims);

    long getExpirationSeconds();

    /**
     * @throws br.com.fiap.restaurant.application.exception.InvalidTokenException if the token is missing, expired, or tampered with
     */
    String validateAndGetSubject(String token);
}
