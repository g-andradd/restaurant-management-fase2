package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.CreateMenuItemCommand;
import br.com.fiap.restaurant.application.exception.NotRestaurantOwnerException;
import br.com.fiap.restaurant.application.port.AuthenticatedUserProvider;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateMenuItemUseCaseTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    private CreateMenuItemUseCase useCase() {
        return new CreateMenuItemUseCase(menuItemRepository, restaurantRepository, authenticatedUserProvider);
    }

    private static Restaurant existingRestaurant(UUID ownerId) {
        return Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA,
                new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(22, 0)), ownerId);
    }

    private static CreateMenuItemCommand command(UUID restaurantId) {
        return new CreateMenuItemCommand(restaurantId, "Pizza", "Descricao", new BigDecimal("39.90"), true, null);
    }

    @Test
    void createsMenuItemWhenCallerOwnsTheRestaurant() {
        UUID ownerId = UUID.randomUUID();
        Restaurant restaurant = existingRestaurant(ownerId);

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase().execute(command(restaurant.getId()));

        assertThat(result.nome()).isEqualTo("Pizza");
        assertThat(result.restaurantId()).isEqualTo(restaurant.getId());
    }

    @Test
    void rejectsUnknownRestaurantId() {
        UUID restaurantId = UUID.randomUUID();
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(command(restaurantId)))
                .isInstanceOf(RestaurantNotFoundException.class);
        verify(menuItemRepository, never()).save(any());
    }

    @Test
    void rejectsWhenCallerIsNotTheRestaurantOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        Restaurant restaurant = existingRestaurant(ownerId);

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(callerId);

        assertThatThrownBy(() -> useCase().execute(command(restaurant.getId())))
                .isInstanceOf(NotRestaurantOwnerException.class);
        verify(menuItemRepository, never()).save(any());
    }
}
