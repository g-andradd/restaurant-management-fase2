package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.domain.exception.UserNotFoundException;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserByIdUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void returnsResultWhenFound() {
        var useCase = new GetUserByIdUseCase(userRepository);
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "hashed-pw", null);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        var result = useCase.execute(user.getId());

        assertThat(result.id()).isEqualTo(user.getId());
        assertThat(result.email()).isEqualTo("ana@example.com");
    }

    @Test
    void throwsWhenNotFound() {
        var useCase = new GetUserByIdUseCase(userRepository);
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(UserNotFoundException.class);
    }
}
