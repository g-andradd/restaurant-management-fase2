package br.com.fiap.restaurant.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordEncoderAdapterTest {

    private final BCryptPasswordEncoderAdapter adapter = new BCryptPasswordEncoderAdapter();

    @Test
    void encodesAndMatchesCorrectPassword() {
        String hash = adapter.encode("correct-horse-battery-staple");

        assertThat(hash).isNotBlank().isNotEqualTo("correct-horse-battery-staple");
        assertThat(adapter.matches("correct-horse-battery-staple", hash)).isTrue();
    }

    @Test
    void doesNotMatchWrongPassword() {
        String hash = adapter.encode("correct-horse-battery-staple");

        assertThat(adapter.matches("wrong-password", hash)).isFalse();
    }
}
