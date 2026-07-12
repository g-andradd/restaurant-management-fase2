package br.com.fiap.restaurant.domain.model;

import br.com.fiap.restaurant.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MenuItemTest {

    @Test
    void createGeneratesIdAndTimestamps() {
        UUID restaurantId = UUID.randomUUID();

        MenuItem menuItem = MenuItem.create("Pizza Margherita", "Molho de tomate, mussarela, manjericão",
                new BigDecimal("39.90"), true, "/photos/pizza.jpg", restaurantId);

        assertThat(menuItem.getId()).isNotNull();
        assertThat(menuItem.getNome()).isEqualTo("Pizza Margherita");
        assertThat(menuItem.getDescricao()).isEqualTo("Molho de tomate, mussarela, manjericão");
        assertThat(menuItem.getPreco()).isEqualByComparingTo("39.90");
        assertThat(menuItem.isDisponivelSomenteNoLocal()).isTrue();
        assertThat(menuItem.getFotoPath()).isEqualTo("/photos/pizza.jpg");
        assertThat(menuItem.getRestaurantId()).isEqualTo(restaurantId);
        assertThat(menuItem.getDataCriacao()).isEqualTo(menuItem.getDataUltimaAlteracao());
    }

    @Test
    void createAllowsNullDescricaoAndFotoPath() {
        MenuItem menuItem = MenuItem.create("Agua", null, new BigDecimal("5.00"), false, null, UUID.randomUUID());

        assertThat(menuItem.getDescricao()).isNull();
        assertThat(menuItem.getFotoPath()).isNull();
    }

    @Test
    void createRejectsBlankNome() {
        assertThatThrownBy(() -> MenuItem.create(" ", null, new BigDecimal("10.00"), false, null, UUID.randomUUID()))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void createRejectsZeroPreco() {
        assertThatThrownBy(() -> MenuItem.create("Pizza", null, BigDecimal.ZERO, false, null, UUID.randomUUID()))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void createRejectsNegativePreco() {
        assertThatThrownBy(() -> MenuItem.create("Pizza", null, new BigDecimal("-5.00"), false, null, UUID.randomUUID()))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void createAcceptsPrecoJustAboveZero() {
        MenuItem menuItem = MenuItem.create("Pizza", null, new BigDecimal("0.01"), false, null, UUID.randomUUID());

        assertThat(menuItem.getPreco()).isEqualByComparingTo("0.01");
    }

    @Test
    void reconstituteRoundTripsExistingData() {
        UUID id = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        LocalDateTime dataCriacao = LocalDateTime.now().minusDays(1);
        LocalDateTime dataUltimaAlteracao = LocalDateTime.now();

        MenuItem menuItem = MenuItem.reconstitute(id, "Pizza", "Descricao", new BigDecimal("20.00"), true,
                "/p.jpg", restaurantId, dataCriacao, dataUltimaAlteracao);

        assertThat(menuItem.getId()).isEqualTo(id);
        assertThat(menuItem.getRestaurantId()).isEqualTo(restaurantId);
        assertThat(menuItem.getDataCriacao()).isEqualTo(dataCriacao);
        assertThat(menuItem.getDataUltimaAlteracao()).isEqualTo(dataUltimaAlteracao);
    }

    @Test
    void atualizarDadosChangesEditableFieldsButNotRestaurantId() {
        UUID restaurantId = UUID.randomUUID();
        MenuItem menuItem = MenuItem.create("Pizza", "Descricao", new BigDecimal("20.00"), false, null, restaurantId);

        menuItem.atualizarDados("Pizza Grande", "Nova descricao", new BigDecimal("25.00"), true, "/p2.jpg");

        assertThat(menuItem.getNome()).isEqualTo("Pizza Grande");
        assertThat(menuItem.getDescricao()).isEqualTo("Nova descricao");
        assertThat(menuItem.getPreco()).isEqualByComparingTo("25.00");
        assertThat(menuItem.isDisponivelSomenteNoLocal()).isTrue();
        assertThat(menuItem.getFotoPath()).isEqualTo("/p2.jpg");
        assertThat(menuItem.getRestaurantId()).isEqualTo(restaurantId);
    }

    @Test
    void atualizarDadosRejectsZeroOrNegativePreco() {
        MenuItem menuItem = MenuItem.create("Pizza", null, new BigDecimal("20.00"), false, null, UUID.randomUUID());

        assertThatThrownBy(() -> menuItem.atualizarDados("Pizza", null, BigDecimal.ZERO, false, null))
                .isInstanceOf(DomainValidationException.class);
    }
}
