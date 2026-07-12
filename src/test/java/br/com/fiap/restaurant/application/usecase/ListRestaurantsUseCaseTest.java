package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.PageQuery;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListRestaurantsUseCaseTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void assemblesPageResultFromRepositoryData() {
        var useCase = new ListRestaurantsUseCase(restaurantRepository, userRepository);
        User owner1 = User.create("Ana Silva", "ana@example.com", "ana.silva", "hash1", null, UUID.randomUUID());
        User owner2 = User.create("Bruno Souza", "bruno@example.com", "bruno.souza", "hash2", null, UUID.randomUUID());
        var horario = new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(22, 0));
        Restaurant restaurant1 = Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA, horario, owner1.getId());
        Restaurant restaurant2 = Restaurant.create("Sushi do Bruno", "Rua B, 200", TipoCozinha.JAPONESA, horario, owner2.getId());

        when(restaurantRepository.findAll(0, 20)).thenReturn(List.of(restaurant1, restaurant2));
        when(restaurantRepository.count()).thenReturn(2L);
        when(userRepository.findAllById(any())).thenReturn(List.of(owner1, owner2));

        var result = useCase.execute(new PageQuery(0, 20));

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);

        // N+1 guard: exactly one bulk resolution call, never a per-restaurant lookup.
        verify(userRepository, times(1)).findAllById(any());
    }
}
