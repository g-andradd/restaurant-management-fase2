package br.com.fiap.restaurant.infrastructure.persistence.repository;

import br.com.fiap.restaurant.domain.model.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
class UserTypeRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UserTypeJpaRepository userTypeJpaRepository;

    private UserTypeRepositoryAdapter adapter() {
        return new UserTypeRepositoryAdapter(userTypeJpaRepository);
    }

    @Test
    void savesAndFindsById() {
        var adapter = adapter();
        UserType userType = UserType.create("Fiscal");

        adapter.save(userType);
        Optional<UserType> found = adapter.findById(userType.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getNome()).isEqualTo("Fiscal");
    }

    @Test
    void existsByNome() {
        var adapter = adapter();
        UserType userType = UserType.create("Fiscal");
        adapter.save(userType);

        assertThat(adapter.existsByNome("Fiscal")).isTrue();
        assertThat(adapter.existsByNome("Inexistente")).isFalse();
    }

    @Test
    void existsByNomeAndIdNotExcludesOwnRecord() {
        var adapter = adapter();
        UserType userType = UserType.create("Fiscal");
        adapter.save(userType);

        assertThat(adapter.existsByNomeAndIdNot("Fiscal", userType.getId())).isFalse();
        assertThat(adapter.existsByNomeAndIdNot("Fiscal", UUID.randomUUID())).isTrue();
    }

    @Test
    void updatesExistingUserType() {
        var adapter = adapter();
        UserType userType = UserType.create("Fiscal");
        adapter.save(userType);

        userType.renomear("Auditor");
        adapter.save(userType);

        UserType reloaded = adapter.findById(userType.getId()).orElseThrow();
        assertThat(reloaded.getNome()).isEqualTo("Auditor");
    }

    @Test
    void deletesById() {
        var adapter = adapter();
        UserType userType = UserType.create("Fiscal");
        adapter.save(userType);

        adapter.deleteById(userType.getId());

        assertThat(adapter.findById(userType.getId())).isEmpty();
    }

    @Test
    void findAllByIdResolvesMultipleTypesInOneCall() {
        var adapter = adapter();
        UserType tipoA = UserType.create("Fiscal");
        UserType tipoB = UserType.create("Auditor");
        adapter.save(tipoA);
        adapter.save(tipoB);

        List<UserType> found = adapter.findAllById(List.of(tipoA.getId(), tipoB.getId()));

        assertThat(found).hasSize(2)
                .extracting(UserType::getNome)
                .containsExactlyInAnyOrder("Fiscal", "Auditor");
    }

    @Test
    void findAllAndCountSupportPagination() {
        var adapter = adapter();
        // V3's seed rows ("Dono de Restaurante", "Cliente") are already in the
        // table - they're inserted by Flyway once, outside this test's
        // transaction, so @DataJpaTest's per-test rollback doesn't hide them.
        long baseline = adapter.count();

        adapter.save(UserType.create("Tipo Um"));
        adapter.save(UserType.create("Tipo Dois"));
        adapter.save(UserType.create("Tipo Tres"));

        List<UserType> firstPage = adapter.findAll(0, 2);

        assertThat(firstPage).hasSize(2);
        assertThat(adapter.count()).isEqualTo(baseline + 3);
    }
}
