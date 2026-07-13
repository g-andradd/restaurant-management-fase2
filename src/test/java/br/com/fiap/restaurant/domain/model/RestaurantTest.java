package br.com.fiap.restaurant.domain.model;

import br.com.fiap.restaurant.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestaurantTest {

    private static final HorarioFuncionamento HORARIO = new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(22, 0));

    @Test
    void createGeneratesIdAndTimestamps() {
        UUID ownerId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA, HORARIO, ownerId);

        assertThat(restaurant.getId()).isNotNull();
        assertThat(restaurant.getNome()).isEqualTo("Cantina da Ana");
        assertThat(restaurant.getEndereco()).isEqualTo("Rua A, 100");
        assertThat(restaurant.getTipoCozinha()).isEqualTo(TipoCozinha.ITALIANA);
        assertThat(restaurant.getHorarioFuncionamento()).isEqualTo(HORARIO);
        assertThat(restaurant.getOwnerId()).isEqualTo(ownerId);
        assertThat(restaurant.getDataCriacao()).isEqualTo(restaurant.getDataUltimaAlteracao());
    }

    @Test
    void createRejectsBlankNome() {
        assertThatThrownBy(() -> Restaurant.create(" ", "Rua A, 100", TipoCozinha.ITALIANA, HORARIO, UUID.randomUUID()))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void createRejectsBlankEndereco() {
        assertThatThrownBy(() -> Restaurant.create("Cantina da Ana", " ", TipoCozinha.ITALIANA, HORARIO, UUID.randomUUID()))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void reconstituteRoundTripsExistingData() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        LocalDateTime dataCriacao = LocalDateTime.now().minusDays(1);
        LocalDateTime dataUltimaAlteracao = LocalDateTime.now();

        Restaurant restaurant = Restaurant.reconstitute(id, "Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA,
                HORARIO, ownerId, dataCriacao, dataUltimaAlteracao);

        assertThat(restaurant.getId()).isEqualTo(id);
        assertThat(restaurant.getOwnerId()).isEqualTo(ownerId);
        assertThat(restaurant.getDataCriacao()).isEqualTo(dataCriacao);
        assertThat(restaurant.getDataUltimaAlteracao()).isEqualTo(dataUltimaAlteracao);
    }

    @Test
    void atualizarDadosChangesEditableFieldsButNotOwnerId() {
        UUID ownerId = UUID.randomUUID();
        Restaurant restaurant = Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA, HORARIO, ownerId);
        var novoHorario = new HorarioFuncionamento(LocalTime.of(9, 0), LocalTime.of(23, 0));

        restaurant.atualizarDados("Cantina da Ana II", "Rua B, 200", TipoCozinha.JAPONESA, novoHorario);

        assertThat(restaurant.getNome()).isEqualTo("Cantina da Ana II");
        assertThat(restaurant.getEndereco()).isEqualTo("Rua B, 200");
        assertThat(restaurant.getTipoCozinha()).isEqualTo(TipoCozinha.JAPONESA);
        assertThat(restaurant.getHorarioFuncionamento()).isEqualTo(novoHorario);
        assertThat(restaurant.getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    void atualizarDadosRejectsBlankNome() {
        Restaurant restaurant = Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA, HORARIO, UUID.randomUUID());

        assertThatThrownBy(() -> restaurant.atualizarDados(" ", "Rua B, 200", TipoCozinha.JAPONESA, HORARIO))
                .isInstanceOf(DomainValidationException.class);
    }
}
