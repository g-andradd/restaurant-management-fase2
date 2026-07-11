package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.PageQuery;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.model.UserType;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUsersUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTypeRepository userTypeRepository;

    @Test
    void assemblesPageResultFromRepositoryData() {
        var useCase = new ListUsersUseCase(userRepository, userTypeRepository);
        UUID donoId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        User user1 = User.create("Ana Silva", "ana@example.com", "ana.silva", "hash1", null, donoId);
        User user2 = User.create("Bruno Souza", "bruno@example.com", "bruno.souza", "hash2", null, clienteId);

        when(userRepository.findAll(0, 20)).thenReturn(List.of(user1, user2));
        when(userRepository.count()).thenReturn(2L);
        when(userTypeRepository.findAllById(any())).thenReturn(List.of(
                UserType.reconstitute(donoId, "Dono de Restaurante"),
                UserType.reconstitute(clienteId, "Cliente")));

        var result = useCase.execute(new PageQuery(0, 20));

        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);

        // N+1 guard: exactly one bulk resolution call, never a per-user lookup.
        verify(userTypeRepository, times(1)).findAllById(any());
    }
}
