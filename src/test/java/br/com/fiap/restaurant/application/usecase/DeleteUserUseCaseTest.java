package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.domain.exception.UserNotFoundException;
import br.com.fiap.restaurant.domain.exception.UserOwnsRestaurantsException;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Test
    void deletesWhenUserExists() {
        var useCase = new DeleteUserUseCase(userRepository, restaurantRepository);
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "hash", null, UUID.randomUUID());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(restaurantRepository.existsByOwnerId(user.getId())).thenReturn(false);

        useCase.execute(user.getId());

        verify(userRepository).deleteById(user.getId());
    }

    @Test
    void throwsWhenUserNotFound() {
        var useCase = new DeleteUserUseCase(userRepository, restaurantRepository);
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void rejectsDeletionWhenUserOwnsARestaurant() {
        var useCase = new DeleteUserUseCase(userRepository, restaurantRepository);
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "hash", null, UUID.randomUUID());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(restaurantRepository.existsByOwnerId(user.getId())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(user.getId())).isInstanceOf(UserOwnsRestaurantsException.class);
        verify(userRepository, never()).deleteById(any());
    }
}
