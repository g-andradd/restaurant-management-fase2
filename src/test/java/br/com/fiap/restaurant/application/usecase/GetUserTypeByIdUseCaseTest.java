package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.domain.exception.UserTypeNotFoundException;
import br.com.fiap.restaurant.domain.model.UserType;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;
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
class GetUserTypeByIdUseCaseTest {

    @Mock
    private UserTypeRepository userTypeRepository;

    @Test
    void returnsResultWhenFound() {
        var useCase = new GetUserTypeByIdUseCase(userTypeRepository);
        UserType userType = UserType.create("Cliente");
        when(userTypeRepository.findById(userType.getId())).thenReturn(Optional.of(userType));

        var result = useCase.execute(userType.getId());

        assertThat(result.nome()).isEqualTo("Cliente");
    }

    @Test
    void throwsWhenNotFound() {
        var useCase = new GetUserTypeByIdUseCase(userTypeRepository);
        UUID id = UUID.randomUUID();
        when(userTypeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(UserTypeNotFoundException.class);
    }
}
