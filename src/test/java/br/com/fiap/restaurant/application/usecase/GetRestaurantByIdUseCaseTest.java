package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.domain.exception.UserNotFoundException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRestaurantByIdUseCaseTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void returnsResultWhenFound() {
        var useCase = new GetRestaurantByIdUseCase(restaurantRepository, userRepository);
        UUID ownerId = UUID.randomUUID();
        User owner = User.create("Ana Silva", "ana@example.com", "ana.silva", "hash", null, UUID.randomUUID());
        Restaurant restaurant = Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA,
                new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(22, 0)), owner.getId());

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        var result = useCase.execute(restaurant.getId());

        assertThat(result.nome()).isEqualTo("Cantina da Ana");
        assertThat(result.owner().id()).isEqualTo(owner.getId());
    }

    @Test
    void throwsWhenRestaurantNotFound() {
        var useCase = new GetRestaurantByIdUseCase(restaurantRepository, userRepository);
        UUID id = UUID.randomUUID();
        when(restaurantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(RestaurantNotFoundException.class);
    }

    @Test
    void throwsWhenOwnerNoLongerExists() {
        var useCase = new GetRestaurantByIdUseCase(restaurantRepository, userRepository);
        UUID ownerId = UUID.randomUUID();
        Restaurant restaurant = Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA,
                new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(22, 0)), ownerId);

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(restaurant.getId())).isInstanceOf(UserNotFoundException.class);
    }
}
