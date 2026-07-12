package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.UpdateRestaurantCommand;
import br.com.fiap.restaurant.application.exception.NotRestaurantOwnerException;
import br.com.fiap.restaurant.application.port.AuthenticatedUserProvider;
import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.domain.model.HorarioFuncionamento;
import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.domain.model.TipoCozinha;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class UpdateRestaurantUseCaseTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    private UpdateRestaurantUseCase useCase() {
        return new UpdateRestaurantUseCase(restaurantRepository, userRepository, authenticatedUserProvider);
    }

    private static Restaurant existingRestaurant(UUID ownerId) {
        return Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA,
                new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(22, 0)), ownerId);
    }

    @Test
    void updatesWhenCallerIsOwner() {
        UUID ownerId = UUID.randomUUID();
        Restaurant restaurant = existingRestaurant(ownerId);
        User owner = User.create("Ana Silva", "ana@example.com", "ana.silva", "hash", null, UUID.randomUUID());
        var command = new UpdateRestaurantCommand(restaurant.getId(), "Cantina da Ana II", "Rua B, 200",
                TipoCozinha.JAPONESA, LocalTime.of(9, 0), LocalTime.of(23, 0));

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        var result = useCase().execute(command);

        assertThat(result.nome()).isEqualTo("Cantina da Ana II");
        assertThat(result.tipoCozinha()).isEqualTo(TipoCozinha.JAPONESA);
    }

    @Test
    void throwsWhenRestaurantNotFound() {
        UUID id = UUID.randomUUID();
        var command = new UpdateRestaurantCommand(id, "Cantina da Ana II", "Rua B, 200",
                TipoCozinha.JAPONESA, LocalTime.of(9, 0), LocalTime.of(23, 0));
        when(restaurantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(command)).isInstanceOf(RestaurantNotFoundException.class);
    }

    @Test
    void rejectsWhenCallerIsNotTheOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        Restaurant restaurant = existingRestaurant(ownerId);
        var command = new UpdateRestaurantCommand(restaurant.getId(), "Cantina da Ana II", "Rua B, 200",
                TipoCozinha.JAPONESA, LocalTime.of(9, 0), LocalTime.of(23, 0));

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(callerId);

        assertThatThrownBy(() -> useCase().execute(command)).isInstanceOf(NotRestaurantOwnerException.class);
        verify(restaurantRepository, never()).save(any());
    }
}
