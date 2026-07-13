package br.com.fiap.restaurant.infrastructure.security;

import br.com.fiap.restaurant.application.exception.InvalidTokenException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    // Deliberately NOT testing tampering by flipping the signature's own
    // trailing character - that approach is flaky. An HS256 signature is 32
    // bytes -> 43 base64url characters (43*6 = 258 bits for 256 bits of
    // data), so the last character carries 2 padding bits that canonical
    // encoding always zeroes. That restricts the last character to one of 16
    // values (A E I M Q U Y c g k o s w 0 4 8), each of which shares its 4
    // significant bits with exactly one other character in the full alphabet
    // (e.g. 'Y' and 'a'). Flipping to a fixed replacement character is
    // therefore byte-identical - and so still valid - whenever the real
    // signature happens to already end in that character's pair: a ~1-in-16
    // (6.25%) flake, which is exactly what made the old
    // `tamperedTokenIsRejected` test intermittently fail. Tamper with the
    // PAYLOAD instead (see tamperedPayloadIsRejected below), or use a
    // completely different key (tokenSignedWithDifferentKeyIsRejected above)
    // - both are deterministic because they don't depend on which specific
    // trailing character a given signature happens to end with.

    @Test
    void tamperedPayloadIsRejected() {
        // The realistic attack this check exists to prevent: forge a claim
        // but keep the original (now mismatched) signature.
        String token = provider.generateToken(UUID.randomUUID().toString(), Map.of("login", "ana.silva"));
        String[] parts = token.split("\\.");

        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String forgedPayloadJson = payloadJson.replace("ana.silva", "attacker");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(forgedPayloadJson.getBytes(StandardCharsets.UTF_8));

        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatThrownBy(() -> provider.validateAndGetSubject(tampered))
                .isInstanceOf(InvalidTokenException.class);
    }
}
