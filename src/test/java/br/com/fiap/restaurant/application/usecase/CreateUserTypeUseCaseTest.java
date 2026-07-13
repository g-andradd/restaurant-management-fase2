package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.CreateUserTypeCommand;
import br.com.fiap.restaurant.domain.exception.UserTypeNameAlreadyExistsException;
import br.com.fiap.restaurant.domain.model.UserType;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserTypeUseCaseTest {

    @Mock
    private UserTypeRepository userTypeRepository;

    @Test
    void createsUserType() {
        var useCase = new CreateUserTypeUseCase(userTypeRepository);
        when(userTypeRepository.existsByNome("Cliente")).thenReturn(false);
        when(userTypeRepository.save(any(UserType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(new CreateUserTypeCommand("Cliente", false));

        assertThat(result.nome()).isEqualTo("Cliente");
        assertThat(result.podeSerDono()).isFalse();
    }

    @Test
    void createsUserTypeThatCanOwnRestaurants() {
        var useCase = new CreateUserTypeUseCase(userTypeRepository);
        when(userTypeRepository.existsByNome("Dono de Restaurante")).thenReturn(false);
        when(userTypeRepository.save(any(UserType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(new CreateUserTypeCommand("Dono de Restaurante", true));

        assertThat(result.podeSerDono()).isTrue();
    }

    @Test
    void rejectsDuplicateNome() {
        var useCase = new CreateUserTypeUseCase(userTypeRepository);
        when(userTypeRepository.existsByNome("Cliente")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new CreateUserTypeCommand("Cliente", false)))
                .isInstanceOf(UserTypeNameAlreadyExistsException.class);
        verify(userTypeRepository, never()).save(any());
    }
}
