package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.PageQuery;
import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.domain.model.HorarioFuncionamento;
import br.com.fiap.restaurant.domain.model.MenuItem;
import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.domain.model.TipoCozinha;
import br.com.fiap.restaurant.domain.repository.MenuItemRepository;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListMenuItemsUseCaseTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    private ListMenuItemsUseCase useCase() {
        return new ListMenuItemsUseCase(menuItemRepository, restaurantRepository);
    }

    private static Restaurant existingRestaurant() {
        return Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA,
                new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(22, 0)), UUID.randomUUID());
    }

    @Test
    void assemblesPageResultFromRepositoryData() {
        Restaurant restaurant = existingRestaurant();
        MenuItem item1 = MenuItem.create("Pizza", null, new BigDecimal("39.90"), true, null, restaurant.getId());
        MenuItem item2 = MenuItem.create("Refrigerante", null, new BigDecimal("8.00"), false, null, restaurant.getId());

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByRestaurantId(restaurant.getId(), 0, 20)).thenReturn(List.of(item1, item2));
        when(menuItemRepository.countByRestaurantId(restaurant.getId())).thenReturn(2L);

        var result = useCase().execute(restaurant.getId(), new PageQuery(0, 20));

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void throwsWhenRestaurantNotFound() {
        UUID restaurantId = UUID.randomUUID();
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(restaurantId, new PageQuery(0, 20)))
                .isInstanceOf(RestaurantNotFoundException.class);
    }

    @Test
    void emptyRestaurantReturnsEmptyPageNotAnError() {
        Restaurant restaurant = existingRestaurant();
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByRestaurantId(restaurant.getId(), 0, 20)).thenReturn(List.of());
        when(menuItemRepository.countByRestaurantId(restaurant.getId())).thenReturn(0L);

        var result = useCase().execute(restaurant.getId(), new PageQuery(0, 20));

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }
}
