package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.CreateUserCommand;
import br.com.fiap.restaurant.application.dto.UserResult;
import br.com.fiap.restaurant.application.port.PasswordEncoder;
import br.com.fiap.restaurant.domain.exception.EmailAlreadyExistsException;
import br.com.fiap.restaurant.domain.exception.InvalidUserTypeReferenceException;
import br.com.fiap.restaurant.domain.exception.LoginAlreadyExistsException;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.model.UserType;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTypeRepository userTypeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private CreateUserUseCase useCase;

    @Test
    void createsUserWithEncodedPassword() {
        useCase = new CreateUserUseCase(userRepository, userTypeRepository, passwordEncoder);
        UUID userTypeId = UUID.randomUUID();
        var command = new CreateUserCommand("Ana Silva", "ana@example.com", "ana.silva", "plain-pw", null, userTypeId);

        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(userRepository.existsByLogin("ana.silva")).thenReturn(false);
        when(userTypeRepository.findById(userTypeId)).thenReturn(Optional.of(UserType.reconstitute(userTypeId, "Cliente", false)));
        when(passwordEncoder.encode("plain-pw")).thenReturn("hashed-pw");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResult result = useCase.execute(command);

        assertThat(result.nome()).isEqualTo("Ana Silva");
        assertThat(result.email()).isEqualTo("ana@example.com");
        assertThat(result.userType().nome()).isEqualTo("Cliente");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getSenhaHash()).isEqualTo("hashed-pw");
        assertThat(captor.getValue().getUserTypeId()).isEqualTo(userTypeId);
    }

    @Test
    void rejectsDuplicateEmail() {
        useCase = new CreateUserUseCase(userRepository, userTypeRepository, passwordEncoder);
        var command = new CreateUserCommand("Ana Silva", "ana@example.com", "ana.silva", "plain-pw", null, UUID.randomUUID());

        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateLogin() {
        useCase = new CreateUserUseCase(userRepository, userTypeRepository, passwordEncoder);
        var command = new CreateUserCommand("Ana Silva", "ana@example.com", "ana.silva", "plain-pw", null, UUID.randomUUID());

        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(userRepository.existsByLogin("ana.silva")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(LoginAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownUserTypeId() {
        useCase = new CreateUserUseCase(userRepository, userTypeRepository, passwordEncoder);
        UUID userTypeId = UUID.randomUUID();
        var command = new CreateUserCommand("Ana Silva", "ana@example.com", "ana.silva", "plain-pw", null, userTypeId);

        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(userRepository.existsByLogin("ana.silva")).thenReturn(false);
        when(userTypeRepository.findById(userTypeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(InvalidUserTypeReferenceException.class);
        verify(userRepository, never()).save(any());
    }
}
