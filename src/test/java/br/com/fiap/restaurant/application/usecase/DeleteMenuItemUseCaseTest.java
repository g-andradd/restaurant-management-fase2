package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.exception.NotRestaurantOwnerException;
import br.com.fiap.restaurant.application.port.AuthenticatedUserProvider;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteMenuItemUseCaseTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    private DeleteMenuItemUseCase useCase() {
        return new DeleteMenuItemUseCase(menuItemRepository, restaurantRepository, authenticatedUserProvider);
    }

    private static Restaurant existingRestaurant(UUID ownerId) {
        return Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA,
                new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(22, 0)), ownerId);
    }

    @Test
    void deletesWhenCallerOwnsTheRestaurantAndItemBelongsToIt() {
        UUID ownerId = UUID.randomUUID();
        Restaurant restaurant = existingRestaurant(ownerId);
        MenuItem menuItem = MenuItem.create("Pizza", null, new BigDecimal("39.90"), true, null, restaurant.getId());

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findById(menuItem.getId())).thenReturn(Optional.of(menuItem));
        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(ownerId);

        useCase().execute(restaurant.getId(), menuItem.getId());

        verify(menuItemRepository).deleteById(menuItem.getId());
    }

    @Test
    void throwsWhenRestaurantNotFound() {
        UUID restaurantId = UUID.randomUUID();
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(restaurantId, UUID.randomUUID()))
                .isInstanceOf(RestaurantNotFoundException.class);
    }

    @Test
    void itemBelongingToAnotherRestaurantReturnsNotFoundNeverForbidden() {
        UUID ownerId = UUID.randomUUID();
        Restaurant pathRestaurant = existingRestaurant(ownerId);
        Restaurant otherRestaurant = existingRestaurant(UUID.randomUUID());
        MenuItem itemOfOtherRestaurant = MenuItem.create("Pizza", null, new BigDecimal("39.90"), true, null,
                otherRestaurant.getId());

        when(restaurantRepository.findById(pathRestaurant.getId())).thenReturn(Optional.of(pathRestaurant));
        when(menuItemRepository.findById(itemOfOtherRestaurant.getId())).thenReturn(Optional.of(itemOfOtherRestaurant));

        assertThatThrownBy(() -> useCase().execute(pathRestaurant.getId(), itemOfOtherRestaurant.getId()))
                .isInstanceOf(MenuItemNotFoundException.class);
        verify(menuItemRepository, never()).deleteById(any());
    }

    @Test
    void throwsWhenItemDoesNotExist() {
        Restaurant restaurant = existingRestaurant(UUID.randomUUID());
        UUID itemId = UUID.randomUUID();
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(restaurant.getId(), itemId))
                .isInstanceOf(MenuItemNotFoundException.class);
    }

    @Test
    void rejectsWhenCallerIsNotTheRestaurantOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        Restaurant restaurant = existingRestaurant(ownerId);
        MenuItem menuItem = MenuItem.create("Pizza", null, new BigDecimal("39.90"), true, null, restaurant.getId());

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findById(menuItem.getId())).thenReturn(Optional.of(menuItem));
        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(callerId);

        assertThatThrownBy(() -> useCase().execute(restaurant.getId(), menuItem.getId()))
                .isInstanceOf(NotRestaurantOwnerException.class);
        verify(menuItemRepository, never()).deleteById(any());
    }
}
