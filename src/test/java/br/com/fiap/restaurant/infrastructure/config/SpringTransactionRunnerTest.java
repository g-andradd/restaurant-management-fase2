package br.com.fiap.restaurant.infrastructure.config;

import br.com.fiap.restaurant.application.port.TransactionRunner;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves SpringTransactionRunner actually runs its action inside a real
 * database transaction, not merely `(action) -> action.run()`. That
 * pass-through would satisfy every mock-based unit test that only checks
 * the runner was invoked and in what order (see DeleteRestaurantUseCaseTest)
 * - only a real rollback, observed against a real Postgres, proves the
 * atomicity guarantee the port exists for.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SpringTransactionRunnerTest {

    // Seeded by V3__create_user_types_table.sql.
    private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TransactionRunner transactionRunner;

    @Autowired
    private UserRepository userRepository;

    private static User newUser(String prefix) {
        return User.create(prefix, prefix.toLowerCase() + UUID.randomUUID() + "@example.com",
                prefix.toLowerCase() + "." + UUID.randomUUID(), "hash", null, CLIENTE_ID);
    }

    @Test
    void rollsBackTheWriteWhenTheActionThrows() {
        User user = newUser("Rollback");

        assertThatThrownBy(() -> transactionRunner.run(() -> {
            userRepository.save(user);
            throw new RuntimeException("boom");
        })).isInstanceOf(RuntimeException.class).hasMessage("boom");

        assertThat(userRepository.findById(user.getId())).isEmpty();
    }

    @Test
    void commitsTheWriteWhenTheActionCompletesNormally() {
        // Contrast case: rules out a runner that always rolls back
        // regardless of outcome (which would also make the test above pass).
        User user = newUser("Commit");

        transactionRunner.run(() -> userRepository.save(user));

        assertThat(userRepository.findById(user.getId())).isPresent();
    }
}
