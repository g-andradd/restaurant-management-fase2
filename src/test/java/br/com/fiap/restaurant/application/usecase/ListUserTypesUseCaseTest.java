package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.PageQuery;
import br.com.fiap.restaurant.domain.model.UserType;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUserTypesUseCaseTest {

    @Mock
    private UserTypeRepository userTypeRepository;

    @Test
    void assemblesPageResultFromRepositoryData() {
        var useCase = new ListUserTypesUseCase(userTypeRepository);
        UserType dono = UserType.create("Dono de Restaurante", true);
        UserType cliente = UserType.create("Cliente", false);

        when(userTypeRepository.findAll(0, 20)).thenReturn(List.of(dono, cliente));
        when(userTypeRepository.count()).thenReturn(2L);

        var result = useCase.execute(new PageQuery(0, 20));

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);
    }
}
