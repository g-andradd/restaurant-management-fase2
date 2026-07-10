package br.com.fiap.restaurant.infrastructure.config;

import br.com.fiap.restaurant.application.port.PasswordEncoder;
import br.com.fiap.restaurant.application.usecase.CreateUserUseCase;
import br.com.fiap.restaurant.application.usecase.DeleteUserUseCase;
import br.com.fiap.restaurant.application.usecase.GetUserByIdUseCase;
import br.com.fiap.restaurant.application.usecase.ListUsersUseCase;
import br.com.fiap.restaurant.application.usecase.UpdateUserUseCase;
import br.com.fiap.restaurant.domain.repository.UserRepository;
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
    public CreateUserUseCase createUserUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return new CreateUserUseCase(userRepository, passwordEncoder);
    }

    @Bean
    public GetUserByIdUseCase getUserByIdUseCase(UserRepository userRepository) {
        return new GetUserByIdUseCase(userRepository);
    }

    @Bean
    public ListUsersUseCase listUsersUseCase(UserRepository userRepository) {
        return new ListUsersUseCase(userRepository);
    }

    @Bean
    public UpdateUserUseCase updateUserUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return new UpdateUserUseCase(userRepository, passwordEncoder);
    }

    @Bean
    public DeleteUserUseCase deleteUserUseCase(UserRepository userRepository) {
        return new DeleteUserUseCase(userRepository);
    }
}
