package br.com.fiap.restaurant.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTypeTest {

    @Test
    void createGeneratesId() {
        UserType userType = UserType.create("Cliente");

        assertThat(userType.getId()).isNotNull();
        assertThat(userType.getNome()).isEqualTo("Cliente");
    }

    @Test
    void createRejectsBlankNome() {
        assertThatThrownBy(() -> UserType.create(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reconstituteRoundTripsExistingData() {
        UUID id = UUID.randomUUID();

        UserType userType = UserType.reconstitute(id, "Dono de Restaurante");

        assertThat(userType.getId()).isEqualTo(id);
        assertThat(userType.getNome()).isEqualTo("Dono de Restaurante");
    }

    @Test
    void renomearChangesNome() {
        UserType userType = UserType.create("Cliente");

        userType.renomear("Dono de Restaurante");

        assertThat(userType.getNome()).isEqualTo("Dono de Restaurante");
    }

    @Test
    void renomearRejectsBlankNome() {
        UserType userType = UserType.create("Cliente");

        assertThatThrownBy(() -> userType.renomear(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
