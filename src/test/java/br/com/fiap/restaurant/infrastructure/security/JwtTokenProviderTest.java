package br.com.fiap.restaurant.infrastructure.security;

import br.com.fiap.restaurant.application.exception.InvalidTokenException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "test-only-secret-not-for-prod-0123456789";

    private final JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, 3_600_000L));

    @Test
    void generateThenValidateRoundTripsSubject() {
        String subject = UUID.randomUUID().toString();

        String token = provider.generateToken(subject, Map.of("login", "ana.silva"));

        assertThat(provider.validateAndGetSubject(token)).isEqualTo(subject);
    }

    @Test
    void expiredTokenIsRejected() {
        var expiredProvider = new JwtTokenProvider(new JwtProperties(SECRET, -1000L));
        String token = expiredProvider.generateToken(UUID.randomUUID().toString(), Map.of());

        assertThatThrownBy(() -> expiredProvider.validateAndGetSubject(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tokenSignedWithDifferentKeyIsRejected() {
        var otherProvider = new JwtTokenProvider(new JwtProperties("different-secret-also-32-bytes-min", 3_600_000L));
        String token = otherProvider.generateToken(UUID.randomUUID().toString(), Map.of());

        assertThatThrownBy(() -> provider.validateAndGetSubject(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = provider.generateToken(UUID.randomUUID().toString(), Map.of());
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThatThrownBy(() -> provider.validateAndGetSubject(tampered))
                .isInstanceOf(InvalidTokenException.class);
    }
}
