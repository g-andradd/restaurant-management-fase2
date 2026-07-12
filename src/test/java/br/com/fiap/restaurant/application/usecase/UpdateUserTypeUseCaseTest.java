package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.UpdateUserTypeCommand;
import br.com.fiap.restaurant.domain.exception.UserTypeNameAlreadyExistsException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateUserTypeUseCaseTest {

    @Mock
    private UserTypeRepository userTypeRepository;

    @Test
    void updatesNome() {
        var useCase = new UpdateUserTypeUseCase(userTypeRepository);
        UserType userType = UserType.create("Cliente", false);

        when(userTypeRepository.findById(userType.getId())).thenReturn(Optional.of(userType));
        when(userTypeRepository.existsByNomeAndIdNot("Dono de Restaurante", userType.getId())).thenReturn(false);
        when(userTypeRepository.save(any(UserType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(new UpdateUserTypeCommand(userType.getId(), "Dono de Restaurante", false));

        assertThat(result.nome()).isEqualTo("Dono de Restaurante");
    }

    @Test
    void updatesPodeSerDono() {
        var useCase = new UpdateUserTypeUseCase(userTypeRepository);
        UserType userType = UserType.create("Cliente", false);

        when(userTypeRepository.findById(userType.getId())).thenReturn(Optional.of(userType));
        when(userTypeRepository.existsByNomeAndIdNot("Cliente", userType.getId())).thenReturn(false);
        when(userTypeRepository.save(any(UserType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(new UpdateUserTypeCommand(userType.getId(), "Cliente", true));

        assertThat(result.podeSerDono()).isTrue();
    }

    @Test
    void throwsWhenNotFound() {
        var useCase = new UpdateUserTypeUseCase(userTypeRepository);
        UUID id = UUID.randomUUID();
        when(userTypeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateUserTypeCommand(id, "Cliente", false)))
                .isInstanceOf(UserTypeNotFoundException.class);
    }

    @Test
    void rejectsNomeOwnedByAnotherType() {
        var useCase = new UpdateUserTypeUseCase(userTypeRepository);
        UserType userType = UserType.create("Cliente", false);

        when(userTypeRepository.findById(userType.getId())).thenReturn(Optional.of(userType));
        when(userTypeRepository.existsByNomeAndIdNot("Dono de Restaurante", userType.getId())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new UpdateUserTypeCommand(userType.getId(), "Dono de Restaurante", false)))
                .isInstanceOf(UserTypeNameAlreadyExistsException.class);
    }
}
