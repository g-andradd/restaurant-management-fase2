package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.domain.exception.MenuItemNotFoundException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMenuItemByIdUseCaseTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    private GetMenuItemByIdUseCase useCase() {
        return new GetMenuItemByIdUseCase(menuItemRepository, restaurantRepository);
    }

    private static Restaurant existingRestaurant() {
        return Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA,
                new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(22, 0)), UUID.randomUUID());
    }

    @Test
    void returnsResultWhenItemBelongsToTheRestaurant() {
        Restaurant restaurant = existingRestaurant();
        MenuItem menuItem = MenuItem.create("Pizza", null, new BigDecimal("39.90"), true, null, restaurant.getId());

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findById(menuItem.getId())).thenReturn(Optional.of(menuItem));

        var result = useCase().execute(restaurant.getId(), menuItem.getId());

        assertThat(result.nome()).isEqualTo("Pizza");
    }

    @Test
    void throwsWhenRestaurantNotFound() {
        UUID restaurantId = UUID.randomUUID();
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(restaurantId, UUID.randomUUID()))
                .isInstanceOf(RestaurantNotFoundException.class);
    }

    @Test
    void throwsWhenItemDoesNotExist() {
        Restaurant restaurant = existingRestaurant();
        UUID itemId = UUID.randomUUID();
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(restaurant.getId(), itemId))
                .isInstanceOf(MenuItemNotFoundException.class);
    }

    @Test
    void itemBelongingToAnotherRestaurantReturnsNotFoundNeverForbidden() {
        // THE P0 TRAP: the item genuinely exists (under a different
        // restaurant), the restaurantId in the path also genuinely exists -
        // the only thing wrong is they don't belong together. Must be
        // MenuItemNotFoundException (404), never NotRestaurantOwnerException
        // (403) - a 403 would confirm the item exists somewhere.
        Restaurant pathRestaurant = existingRestaurant();
        Restaurant otherRestaurant = existingRestaurant();
        MenuItem itemOfOtherRestaurant = MenuItem.create("Pizza", null, new BigDecimal("39.90"), true, null,
                otherRestaurant.getId());

        when(restaurantRepository.findById(pathRestaurant.getId())).thenReturn(Optional.of(pathRestaurant));
        when(menuItemRepository.findById(itemOfOtherRestaurant.getId())).thenReturn(Optional.of(itemOfOtherRestaurant));

        assertThatThrownBy(() -> useCase().execute(pathRestaurant.getId(), itemOfOtherRestaurant.getId()))
                .isInstanceOf(MenuItemNotFoundException.class);
    }
}
