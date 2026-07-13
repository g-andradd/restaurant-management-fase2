package br.com.fiap.restaurant.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.jwt.secret}/{@code app.jwt.expirationMs}; no logic.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMs) {
}
