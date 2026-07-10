package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.PageQuery;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUsersUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void assemblesPageResultFromRepositoryData() {
        var useCase = new ListUsersUseCase(userRepository);
        User user1 = User.create("Ana Silva", "ana@example.com", "ana.silva", "hash1", null);
        User user2 = User.create("Bruno Souza", "bruno@example.com", "bruno.souza", "hash2", null);

        when(userRepository.findAll(0, 20)).thenReturn(List.of(user1, user2));
        when(userRepository.count()).thenReturn(2L);

        var result = useCase.execute(new PageQuery(0, 20));

        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);
    }
}
