package br.com.fiap.restaurant.infrastructure.config;

import br.com.fiap.restaurant.application.port.PasswordEncoder;
import br.com.fiap.restaurant.application.port.TokenProvider;
import br.com.fiap.restaurant.application.usecase.AuthenticateUserUseCase;
import br.com.fiap.restaurant.application.usecase.CreateUserTypeUseCase;
import br.com.fiap.restaurant.application.usecase.CreateUserUseCase;
import br.com.fiap.restaurant.application.usecase.DeleteUserTypeUseCase;
import br.com.fiap.restaurant.application.usecase.DeleteUserUseCase;
import br.com.fiap.restaurant.application.usecase.GetUserByIdUseCase;
import br.com.fiap.restaurant.application.usecase.GetUserTypeByIdUseCase;
import br.com.fiap.restaurant.application.usecase.ListUserTypesUseCase;
import br.com.fiap.restaurant.application.usecase.ListUsersUseCase;
import br.com.fiap.restaurant.application.usecase.UpdateUserTypeUseCase;
import br.com.fiap.restaurant.application.usecase.UpdateUserUseCase;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires plain, framework-free application-layer use cases as beans. Use cases
 * themselves carry no Spring annotations so the application layer stays free
 * of framework dependencies; this class is the only place that knows they're
 * Spring beans.
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public CreateUserUseCase createUserUseCase(UserRepository userRepository, UserTypeRepository userTypeRepository,
                                                PasswordEncoder passwordEncoder) {
        return new CreateUserUseCase(userRepository, userTypeRepository, passwordEncoder);
    }

    @Bean
    public GetUserByIdUseCase getUserByIdUseCase(UserRepository userRepository, UserTypeRepository userTypeRepository) {
        return new GetUserByIdUseCase(userRepository, userTypeRepository);
    }

    @Bean
    public ListUsersUseCase listUsersUseCase(UserRepository userRepository, UserTypeRepository userTypeRepository) {
        return new ListUsersUseCase(userRepository, userTypeRepository);
    }

    @Bean
    public UpdateUserUseCase updateUserUseCase(UserRepository userRepository, UserTypeRepository userTypeRepository,
                                                PasswordEncoder passwordEncoder) {
        return new UpdateUserUseCase(userRepository, userTypeRepository, passwordEncoder);
    }

    @Bean
    public DeleteUserUseCase deleteUserUseCase(UserRepository userRepository) {
        return new DeleteUserUseCase(userRepository);
    }

    @Bean
    public AuthenticateUserUseCase authenticateUserUseCase(UserRepository userRepository,
                                                             UserTypeRepository userTypeRepository,
                                                             PasswordEncoder passwordEncoder,
                                                             TokenProvider tokenProvider) {
        return new AuthenticateUserUseCase(userRepository, userTypeRepository, passwordEncoder, tokenProvider);
    }

    @Bean
    public CreateUserTypeUseCase createUserTypeUseCase(UserTypeRepository userTypeRepository) {
        return new CreateUserTypeUseCase(userTypeRepository);
    }

    @Bean
    public GetUserTypeByIdUseCase getUserTypeByIdUseCase(UserTypeRepository userTypeRepository) {
        return new GetUserTypeByIdUseCase(userTypeRepository);
    }

    @Bean
    public ListUserTypesUseCase listUserTypesUseCase(UserTypeRepository userTypeRepository) {
        return new ListUserTypesUseCase(userTypeRepository);
    }

    @Bean
    public UpdateUserTypeUseCase updateUserTypeUseCase(UserTypeRepository userTypeRepository) {
        return new UpdateUserTypeUseCase(userTypeRepository);
    }

    @Bean
    public DeleteUserTypeUseCase deleteUserTypeUseCase(UserTypeRepository userTypeRepository,
                                                        UserRepository userRepository) {
        return new DeleteUserTypeUseCase(userTypeRepository, userRepository);
    }
}
