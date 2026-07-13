package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.AuthenticateUserCommand;
import br.com.fiap.restaurant.application.dto.AuthenticationResult;
import br.com.fiap.restaurant.application.exception.InvalidCredentialsException;
import br.com.fiap.restaurant.application.port.PasswordEncoder;
import br.com.fiap.restaurant.application.port.TokenProvider;
import br.com.fiap.restaurant.domain.exception.UserTypeNotFoundException;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.model.UserType;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;

import java.util.Map;

/**
 * The "userType" claim embedded in the issued token is a snapshot taken at
 * login time - it is NOT re-validated on every request (JwtAuthenticationFilter
 * never hits the database). If a user's type is reassigned after login, their
 * existing token keeps the OLD type name until it expires. Any
 * authorization/business decision that depends on the current type (notably
 * M04's "only a Dono de Restaurante may own a restaurant") MUST re-read the
 * user's type from the database via UserRepository/UserTypeRepository at the
 * time of the decision - never trust this claim for anything but display or
 * convenience. Treating it as authoritative is a correctness trap.
 */
public class AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public AuthenticateUserUseCase(UserRepository userRepository, UserTypeRepository userTypeRepository,
                                    PasswordEncoder passwordEncoder, TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.userTypeRepository = userTypeRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public AuthenticationResult execute(AuthenticateUserCommand command) {
        User user = userRepository.findByLogin(command.login())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(command.senha(), user.getSenhaHash())) {
            throw new InvalidCredentialsException();
        }

        UserType userType = userTypeRepository.findById(user.getUserTypeId())
                .orElseThrow(() -> new UserTypeNotFoundException(user.getUserTypeId()));

        Map<String, Object> claims = Map.of("login", user.getLogin(), "userType", userType.getNome());
        String subject = user.getId().toString();
        String token = tokenProvider.generateToken(subject, claims);

        return AuthenticationResult.bearer(token, tokenProvider.getExpirationSeconds(), subject);
    }
}
