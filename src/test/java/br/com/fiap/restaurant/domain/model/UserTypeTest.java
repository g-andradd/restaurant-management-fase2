package br.com.fiap.restaurant.domain.model;

import br.com.fiap.restaurant.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTypeTest {

    @Test
    void createGeneratesId() {
        UserType userType = UserType.create("Cliente", false);

        assertThat(userType.getId()).isNotNull();
        assertThat(userType.getNome()).isEqualTo("Cliente");
        assertThat(userType.podeSerDono()).isFalse();
    }

    @Test
    void createCanMarkTypeAsAbleToOwnRestaurants() {
        UserType userType = UserType.create("Dono de Restaurante", true);

        assertThat(userType.podeSerDono()).isTrue();
    }

    @Test
    void createRejectsBlankNome() {
        assertThatThrownBy(() -> UserType.create(" ", false)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void reconstituteRoundTripsExistingData() {
        UUID id = UUID.randomUUID();

        UserType userType = UserType.reconstitute(id, "Dono de Restaurante", true);

        assertThat(userType.getId()).isEqualTo(id);
        assertThat(userType.getNome()).isEqualTo("Dono de Restaurante");
        assertThat(userType.podeSerDono()).isTrue();
    }

    @Test
    void renomearChangesNome() {
        UserType userType = UserType.create("Cliente", false);

        userType.renomear("Dono de Restaurante");

        assertThat(userType.getNome()).isEqualTo("Dono de Restaurante");
    }

    @Test
    void renomearRejectsBlankNome() {
        UserType userType = UserType.create("Cliente", false);

        assertThatThrownBy(() -> userType.renomear(" ")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void definirPodeSerDonoChangesFlag() {
        UserType userType = UserType.create("Cliente", false);

        userType.definirPodeSerDono(true);

        assertThat(userType.podeSerDono()).isTrue();
    }
}
