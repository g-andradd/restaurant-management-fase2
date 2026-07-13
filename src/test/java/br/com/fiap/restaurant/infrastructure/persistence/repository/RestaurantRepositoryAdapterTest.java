package br.com.fiap.restaurant.infrastructure.persistence.repository;

import br.com.fiap.restaurant.domain.model.HorarioFuncionamento;
import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.domain.model.TipoCozinha;
import br.com.fiap.restaurant.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
class RestaurantRepositoryAdapterTest {

    // Seeded by V3__create_user_types_table.sql - guaranteed to exist so the
    // FK on users.user_type_id is satisfied.
    private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static final HorarioFuncionamento HORARIO = new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(22, 0));

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private RestaurantJpaRepository restaurantJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private RestaurantRepositoryAdapter adapter() {
        return new RestaurantRepositoryAdapter(restaurantJpaRepository);
    }

    private UUID persistOwner() {
        User owner = User.create("Ana Silva", "ana" + UUID.randomUUID() + "@example.com",
                "ana.silva." + UUID.randomUUID(), "hash", null, CLIENTE_ID);
        new UserRepositoryAdapter(userJpaRepository).save(owner);
        return owner.getId();
    }

    @Test
    void savesAndFindsById() {
        var adapter = adapter();
        UUID ownerId = persistOwner();
        Restaurant restaurant = Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA, HORARIO, ownerId);

        adapter.save(restaurant);
        Optional<Restaurant> found = adapter.findById(restaurant.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getNome()).isEqualTo("Cantina da Ana");
        assertThat(found.get().getOwnerId()).isEqualTo(ownerId);
        assertThat(found.get().getHorarioFuncionamento()).isEqualTo(HORARIO);
    }

    @Test
    void updatesExistingRestaurant() {
        var adapter = adapter();
        UUID ownerId = persistOwner();
        Restaurant restaurant = Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA, HORARIO, ownerId);
        adapter.save(restaurant);

        var novoHorario = new HorarioFuncionamento(LocalTime.of(9, 0), LocalTime.of(23, 0));
        restaurant.atualizarDados("Cantina da Ana II", "Rua B, 200", TipoCozinha.JAPONESA, novoHorario);
        adapter.save(restaurant);

        Restaurant reloaded = adapter.findById(restaurant.getId()).orElseThrow();
        assertThat(reloaded.getNome()).isEqualTo("Cantina da Ana II");
        assertThat(reloaded.getTipoCozinha()).isEqualTo(TipoCozinha.JAPONESA);
        assertThat(reloaded.getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    void deletesById() {
        var adapter = adapter();
        UUID ownerId = persistOwner();
        Restaurant restaurant = Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA, HORARIO, ownerId);
        adapter.save(restaurant);

        adapter.deleteById(restaurant.getId());

        assertThat(adapter.findById(restaurant.getId())).isEmpty();
    }

    @Test
    void existsByOwnerIdReflectsReferencingRestaurants() {
        var adapter = adapter();
        UUID ownerId = persistOwner();
        adapter.save(Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA, HORARIO, ownerId));

        assertThat(adapter.existsByOwnerId(ownerId)).isTrue();
        assertThat(adapter.existsByOwnerId(UUID.randomUUID())).isFalse();
    }

    @Test
    void findAllAndCountSupportPagination() {
        var adapter = adapter();
        UUID ownerId = persistOwner();
        adapter.save(Restaurant.create("Restaurante Um", "Rua A, 100", TipoCozinha.ITALIANA, HORARIO, ownerId));
        adapter.save(Restaurant.create("Restaurante Dois", "Rua B, 200", TipoCozinha.JAPONESA, HORARIO, ownerId));
        adapter.save(Restaurant.create("Restaurante Tres", "Rua C, 300", TipoCozinha.BRASILEIRA, HORARIO, ownerId));

        List<Restaurant> firstPage = adapter.findAll(0, 2);

        assertThat(firstPage).hasSize(2);
        assertThat(adapter.count()).isEqualTo(3);
    }
}
