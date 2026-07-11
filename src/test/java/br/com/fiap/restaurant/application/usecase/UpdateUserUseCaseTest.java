package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.UpdateUserCommand;
import br.com.fiap.restaurant.application.port.PasswordEncoder;
import br.com.fiap.restaurant.domain.exception.EmailAlreadyExistsException;
import br.com.fiap.restaurant.domain.exception.InvalidUserTypeReferenceException;
import br.com.fiap.restaurant.domain.exception.LoginAlreadyExistsException;
import br.com.fiap.restaurant.domain.exception.UserNotFoundException;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.model.UserType;
import br.com.fiap.restaurant.domain.repository.UserRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateUserUseCaseTest {

    private static final UUID USER_TYPE_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTypeRepository userTypeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void updatesDataWithoutChangingPasswordWhenSenhaAbsent() {
        var useCase = new UpdateUserUseCase(userRepository, userTypeRepository, passwordEncoder);
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "old-hash", null, USER_TYPE_ID);
        var command = new UpdateUserCommand(user.getId(), "Ana Souza", "ana.souza@example.com",
                "ana.souza", null, "Rua B, 200", USER_TYPE_ID);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("ana.souza@example.com", user.getId())).thenReturn(false);
        when(userRepository.existsByLoginAndIdNot("ana.souza", user.getId())).thenReturn(false);
        when(userTypeRepository.findById(USER_TYPE_ID)).thenReturn(Optional.of(UserType.reconstitute(USER_TYPE_ID, "Cliente")));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(command);

        assertThat(result.nome()).isEqualTo("Ana Souza");
        assertThat(user.getSenhaHash()).isEqualTo("old-hash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void encodesNewPasswordWhenSenhaPresent() {
        var useCase = new UpdateUserUseCase(userRepository, userTypeRepository, passwordEncoder);
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "old-hash", null, USER_TYPE_ID);
        var command = new UpdateUserCommand(user.getId(), "Ana Silva", "ana@example.com",
                "ana.silva", "new-plain-pw", null, USER_TYPE_ID);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("ana@example.com", user.getId())).thenReturn(false);
        when(userRepository.existsByLoginAndIdNot("ana.silva", user.getId())).thenReturn(false);
        when(userTypeRepository.findById(USER_TYPE_ID)).thenReturn(Optional.of(UserType.reconstitute(USER_TYPE_ID, "Cliente")));
        when(passwordEncoder.encode("new-plain-pw")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command);

        assertThat(user.getSenhaHash()).isEqualTo("new-hash");
    }

    @Test
    void throwsWhenUserNotFound() {
        var useCase = new UpdateUserUseCase(userRepository, userTypeRepository, passwordEncoder);
        UUID id = UUID.randomUUID();
        var command = new UpdateUserCommand(id, "Ana Silva", "ana@example.com", "ana.silva", null, null, USER_TYPE_ID);

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void rejectsEmailOwnedByAnotherUser() {
        var useCase = new UpdateUserUseCase(userRepository, userTypeRepository, passwordEncoder);
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "old-hash", null, USER_TYPE_ID);
        var command = new UpdateUserCommand(user.getId(), "Ana Silva", "taken@example.com", "ana.silva", null, null, USER_TYPE_ID);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("taken@example.com", user.getId())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void rejectsLoginOwnedByAnotherUser() {
        var useCase = new UpdateUserUseCase(userRepository, userTypeRepository, passwordEncoder);
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "old-hash", null, USER_TYPE_ID);
        var command = new UpdateUserCommand(user.getId(), "Ana Silva", "ana@example.com", "taken.login", null, null, USER_TYPE_ID);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("ana@example.com", user.getId())).thenReturn(false);
        when(userRepository.existsByLoginAndIdNot("taken.login", user.getId())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(LoginAlreadyExistsException.class);
    }

    @Test
    void rejectsUnknownUserTypeId() {
        var useCase = new UpdateUserUseCase(userRepository, userTypeRepository, passwordEncoder);
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "old-hash", null, USER_TYPE_ID);
        UUID unknownTypeId = UUID.randomUUID();
        var command = new UpdateUserCommand(user.getId(), "Ana Silva", "ana@example.com", "ana.silva", null, null, unknownTypeId);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        lenient().when(userRepository.existsByEmailAndIdNot("ana@example.com", user.getId())).thenReturn(false);
        lenient().when(userRepository.existsByLoginAndIdNot("ana.silva", user.getId())).thenReturn(false);
        when(userTypeRepository.findById(unknownTypeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(InvalidUserTypeReferenceException.class);
    }
}
