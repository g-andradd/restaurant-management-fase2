package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.exception.NotRestaurantOwnerException;
import br.com.fiap.restaurant.application.port.AuthenticatedUserProvider;
import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.domain.model.HorarioFuncionamento;
import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.domain.model.TipoCozinha;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteRestaurantUseCaseTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    private DeleteRestaurantUseCase useCase() {
        return new DeleteRestaurantUseCase(restaurantRepository, authenticatedUserProvider);
    }

    private static Restaurant existingRestaurant(UUID ownerId) {
        return Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA,
                new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(22, 0)), ownerId);
    }

    @Test
    void deletesWhenCallerIsOwner() {
        UUID ownerId = UUID.randomUUID();
        Restaurant restaurant = existingRestaurant(ownerId);

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(ownerId);

        useCase().execute(restaurant.getId());

        verify(restaurantRepository).deleteById(restaurant.getId());
    }

    @Test
    void throwsWhenRestaurantNotFound() {
        UUID id = UUID.randomUUID();
        when(restaurantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(id)).isInstanceOf(RestaurantNotFoundException.class);
    }

    @Test
    void rejectsWhenCallerIsNotTheOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        Restaurant restaurant = existingRestaurant(ownerId);

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(callerId);

        assertThatThrownBy(() -> useCase().execute(restaurant.getId())).isInstanceOf(NotRestaurantOwnerException.class);
        verify(restaurantRepository, never()).deleteById(any());
    }
}
