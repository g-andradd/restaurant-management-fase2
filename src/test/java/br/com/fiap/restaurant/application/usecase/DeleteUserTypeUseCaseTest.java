package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.domain.exception.UserTypeInUseException;
import br.com.fiap.restaurant.domain.exception.UserTypeNotFoundException;
import br.com.fiap.restaurant.domain.model.UserType;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;
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
class DeleteUserTypeUseCaseTest {

    @Mock
    private UserTypeRepository userTypeRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void deletesWhenNotInUse() {
        var useCase = new DeleteUserTypeUseCase(userTypeRepository, userRepository);
        UserType userType = UserType.create("Cliente");

        when(userTypeRepository.findById(userType.getId())).thenReturn(Optional.of(userType));
        when(userRepository.existsByUserTypeId(userType.getId())).thenReturn(false);

        useCase.execute(userType.getId());

        verify(userTypeRepository).deleteById(userType.getId());
    }

    @Test
    void throwsWhenNotFound() {
        var useCase = new DeleteUserTypeUseCase(userTypeRepository, userRepository);
        UUID id = UUID.randomUUID();
        when(userTypeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(UserTypeNotFoundException.class);
        verify(userTypeRepository, never()).deleteById(any());
    }

    @Test
    void rejectsDeletionWhenInUse() {
        var useCase = new DeleteUserTypeUseCase(userTypeRepository, userRepository);
        UserType userType = UserType.create("Cliente");

        when(userTypeRepository.findById(userType.getId())).thenReturn(Optional.of(userType));
        when(userRepository.existsByUserTypeId(userType.getId())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(userType.getId())).isInstanceOf(UserTypeInUseException.class);
        verify(userTypeRepository, never()).deleteById(any());
    }
}
