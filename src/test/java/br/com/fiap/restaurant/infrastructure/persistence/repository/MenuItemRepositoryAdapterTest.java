package br.com.fiap.restaurant.infrastructure.persistence.repository;

import br.com.fiap.restaurant.domain.model.HorarioFuncionamento;
import br.com.fiap.restaurant.domain.model.MenuItem;
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

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
class MenuItemRepositoryAdapterTest {

    // Seeded by V3__create_user_types_table.sql - guaranteed to exist so the
    // FK on users.user_type_id is satisfied.
    private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static final HorarioFuncionamento HORARIO = new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(22, 0));

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MenuItemJpaRepository menuItemJpaRepository;

    @Autowired
    private RestaurantJpaRepository restaurantJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private MenuItemRepositoryAdapter adapter() {
        return new MenuItemRepositoryAdapter(menuItemJpaRepository);
    }

    private UUID persistRestaurant() {
        User owner = User.create("Ana Silva", "ana" + UUID.randomUUID() + "@example.com",
                "ana.silva." + UUID.randomUUID(), "hash", null, CLIENTE_ID);
        new UserRepositoryAdapter(userJpaRepository).save(owner);

        Restaurant restaurant = Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA,
                HORARIO, owner.getId());
        new RestaurantRepositoryAdapter(restaurantJpaRepository).save(restaurant);
        return restaurant.getId();
    }

    @Test
    void savesAndFindsById() {
        var adapter = adapter();
        UUID restaurantId = persistRestaurant();
        MenuItem menuItem = MenuItem.create("Pizza", "Descricao", new BigDecimal("39.90"), true, "/p.jpg", restaurantId);

        adapter.save(menuItem);
        Optional<MenuItem> found = adapter.findById(menuItem.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getNome()).isEqualTo("Pizza");
        assertThat(found.get().getPreco()).isEqualByComparingTo("39.90");
        assertThat(found.get().getRestaurantId()).isEqualTo(restaurantId);
    }

    @Test
    void updatesExistingMenuItem() {
        var adapter = adapter();
        UUID restaurantId = persistRestaurant();
        MenuItem menuItem = MenuItem.create("Pizza", "Descricao", new BigDecimal("39.90"), true, null, restaurantId);
        adapter.save(menuItem);

        menuItem.atualizarDados("Pizza Grande", "Nova descricao", new BigDecimal("45.00"), false, "/novo.jpg");
        adapter.save(menuItem);

        MenuItem reloaded = adapter.findById(menuItem.getId()).orElseThrow();
        assertThat(reloaded.getNome()).isEqualTo("Pizza Grande");
        assertThat(reloaded.getPreco()).isEqualByComparingTo("45.00");
        assertThat(reloaded.getFotoPath()).isEqualTo("/novo.jpg");
    }

    @Test
    void deletesById() {
        var adapter = adapter();
        UUID restaurantId = persistRestaurant();
        MenuItem menuItem = MenuItem.create("Pizza", null, new BigDecimal("39.90"), true, null, restaurantId);
        adapter.save(menuItem);

        adapter.deleteById(menuItem.getId());

        assertThat(adapter.findById(menuItem.getId())).isEmpty();
    }

    @Test
    void findByRestaurantIdAndCountSupportPagination() {
        var adapter = adapter();
        UUID restaurantId = persistRestaurant();
        adapter.save(MenuItem.create("Item Um", null, new BigDecimal("10.00"), false, null, restaurantId));
        adapter.save(MenuItem.create("Item Dois", null, new BigDecimal("20.00"), false, null, restaurantId));
        adapter.save(MenuItem.create("Item Tres", null, new BigDecimal("30.00"), false, null, restaurantId));

        List<MenuItem> firstPage = adapter.findByRestaurantId(restaurantId, 0, 2);

        assertThat(firstPage).hasSize(2);
        assertThat(adapter.countByRestaurantId(restaurantId)).isEqualTo(3);
    }

    @Test
    void findByRestaurantIdDoesNotReturnItemsFromOtherRestaurants() {
        var adapter = adapter();
        UUID restaurantIdA = persistRestaurant();
        UUID restaurantIdB = persistRestaurant();
        adapter.save(MenuItem.create("Item A", null, new BigDecimal("10.00"), false, null, restaurantIdA));
        adapter.save(MenuItem.create("Item B", null, new BigDecimal("20.00"), false, null, restaurantIdB));

        List<MenuItem> itemsOfA = adapter.findByRestaurantId(restaurantIdA, 0, 20);

        assertThat(itemsOfA).hasSize(1).extracting(MenuItem::getNome).containsExactly("Item A");
    }

    @Test
    void deleteByRestaurantIdRemovesAllItemsInOneCall() {
        var adapter = adapter();
        UUID restaurantId = persistRestaurant();
        adapter.save(MenuItem.create("Item Um", null, new BigDecimal("10.00"), false, null, restaurantId));
        adapter.save(MenuItem.create("Item Dois", null, new BigDecimal("20.00"), false, null, restaurantId));

        adapter.deleteByRestaurantId(restaurantId);

        assertThat(adapter.findByRestaurantId(restaurantId, 0, 20)).isEmpty();
        assertThat(adapter.countByRestaurantId(restaurantId)).isZero();
    }
}
