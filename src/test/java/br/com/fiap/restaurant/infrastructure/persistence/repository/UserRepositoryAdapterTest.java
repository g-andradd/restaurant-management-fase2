package br.com.fiap.restaurant.infrastructure.persistence.repository;

import br.com.fiap.restaurant.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
class UserRepositoryAdapterTest {

    // Seeded by V3__create_user_types_table.sql - guaranteed to exist so the
    // FK on users.user_type_id is satisfied.
    private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UserJpaRepository userJpaRepository;

    private UserRepositoryAdapter adapter() {
        return new UserRepositoryAdapter(userJpaRepository);
    }

    @Test
    void savesAndFindsById() {
        var adapter = adapter();
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "hash", "Rua A, 100", CLIENTE_ID);

        adapter.save(user);
        Optional<User> found = adapter.findById(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("ana@example.com");
        assertThat(found.get().getUserTypeId()).isEqualTo(CLIENTE_ID);
    }

    @Test
    void existsByEmailAndLogin() {
        var adapter = adapter();
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "hash", null, CLIENTE_ID);
        adapter.save(user);

        assertThat(adapter.existsByEmail("ana@example.com")).isTrue();
        assertThat(adapter.existsByLogin("ana.silva")).isTrue();
        assertThat(adapter.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void existsByEmailAndIdNotExcludesOwnRecord() {
        var adapter = adapter();
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "hash", null, CLIENTE_ID);
        adapter.save(user);

        assertThat(adapter.existsByEmailAndIdNot("ana@example.com", user.getId())).isFalse();
        assertThat(adapter.existsByEmailAndIdNot("ana@example.com", UUID.randomUUID())).isTrue();
    }

    @Test
    void updatesExistingUser() {
        var adapter = adapter();
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "hash", null, CLIENTE_ID);
        adapter.save(user);

        user.atualizarDados("Ana Souza", "ana.souza@example.com", "ana.souza", "Rua B, 200", CLIENTE_ID);
        adapter.save(user);

        User reloaded = adapter.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getNome()).isEqualTo("Ana Souza");
        assertThat(reloaded.getEmail()).isEqualTo("ana.souza@example.com");
    }

    @Test
    void deletesById() {
        var adapter = adapter();
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "hash", null, CLIENTE_ID);
        adapter.save(user);

        adapter.deleteById(user.getId());

        assertThat(adapter.findById(user.getId())).isEmpty();
    }

    @Test
    void existsByUserTypeIdReflectsReferencingUsers() {
        var adapter = adapter();
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "hash", null, CLIENTE_ID);
        adapter.save(user);

        assertThat(adapter.existsByUserTypeId(CLIENTE_ID)).isTrue();
        assertThat(adapter.existsByUserTypeId(UUID.randomUUID())).isFalse();
    }

    @Test
    void findAllAndCountSupportPagination() {
        var adapter = adapter();
        adapter.save(User.create("User One", "one@example.com", "user.one", "hash", null, CLIENTE_ID));
        adapter.save(User.create("User Two", "two@example.com", "user.two", "hash", null, CLIENTE_ID));
        adapter.save(User.create("User Three", "three@example.com", "user.three", "hash", null, CLIENTE_ID));

        List<User> firstPage = adapter.findAll(0, 2);

        assertThat(firstPage).hasSize(2);
        assertThat(adapter.count()).isEqualTo(3);
    }
}
