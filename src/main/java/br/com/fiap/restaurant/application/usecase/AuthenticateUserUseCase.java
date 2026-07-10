package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.AuthenticateUserCommand;
import br.com.fiap.restaurant.application.dto.AuthenticationResult;
import br.com.fiap.restaurant.application.exception.InvalidCredentialsException;
import br.com.fiap.restaurant.application.port.PasswordEncoder;
import br.com.fiap.restaurant.application.port.TokenProvider;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.repository.UserRepository;

import java.util.Map;

public class AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public AuthenticateUserUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                    TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public AuthenticationResult execute(AuthenticateUserCommand command) {
        User user = userRepository.findByLogin(command.login())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(command.senha(), user.getSenhaHash())) {
            throw new InvalidCredentialsException();
        }

        Map<String, Object> claims = Map.of("login", user.getLogin());
        String subject = user.getId().toString();
        String token = tokenProvider.generateToken(subject, claims);

        return AuthenticationResult.bearer(token, tokenProvider.getExpirationSeconds(), subject);
    }
}
