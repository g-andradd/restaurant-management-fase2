package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.AuthenticateUserCommand;
import br.com.fiap.restaurant.application.exception.InvalidCredentialsException;
import br.com.fiap.restaurant.application.port.PasswordEncoder;
import br.com.fiap.restaurant.application.port.TokenProvider;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.model.UserType;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseTest {

    private static final UUID USER_TYPE_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTypeRepository userTypeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @Test
    void issuesTokenOnValidCredentials() {
        var useCase = new AuthenticateUserUseCase(userRepository, userTypeRepository, passwordEncoder, tokenProvider);
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "hashed-pw", null, USER_TYPE_ID);

        when(userRepository.findByLogin("ana.silva")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain-pw", "hashed-pw")).thenReturn(true);
        when(userTypeRepository.findById(USER_TYPE_ID)).thenReturn(Optional.of(UserType.reconstitute(USER_TYPE_ID, "Cliente")));
        when(tokenProvider.generateToken(eq(user.getId().toString()), anyMap())).thenReturn("signed-token");
        when(tokenProvider.getExpirationSeconds()).thenReturn(3600L);

        var result = useCase.execute(new AuthenticateUserCommand("ana.silva", "plain-pw"));

        assertThat(result.token()).isEqualTo("signed-token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresInSeconds()).isEqualTo(3600L);
        assertThat(result.subject()).isEqualTo(user.getId().toString());
    }

    @Test
    void embedsLoginAndUserTypeAsCustomClaims() {
        var useCase = new AuthenticateUserUseCase(userRepository, userTypeRepository, passwordEncoder, tokenProvider);
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "hashed-pw", null, USER_TYPE_ID);

        when(userRepository.findByLogin("ana.silva")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain-pw", "hashed-pw")).thenReturn(true);
        when(userTypeRepository.findById(USER_TYPE_ID)).thenReturn(Optional.of(UserType.reconstitute(USER_TYPE_ID, "Cliente")));
        when(tokenProvider.generateToken(any(), eq(Map.of("login", "ana.silva", "userType", "Cliente"))))
                .thenReturn("signed-token");

        useCase.execute(new AuthenticateUserCommand("ana.silva", "plain-pw"));
    }

    @Test
    void rejectsUnknownLogin() {
        var useCase = new AuthenticateUserUseCase(userRepository, userTypeRepository, passwordEncoder, tokenProvider);
        when(userRepository.findByLogin("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new AuthenticateUserCommand("nobody", "whatever")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsWrongPassword() {
        var useCase = new AuthenticateUserUseCase(userRepository, userTypeRepository, passwordEncoder, tokenProvider);
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "hashed-pw", null, USER_TYPE_ID);

        when(userRepository.findByLogin("ana.silva")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pw", "hashed-pw")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new AuthenticateUserCommand("ana.silva", "wrong-pw")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
