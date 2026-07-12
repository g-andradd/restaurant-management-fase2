package br.com.fiap.restaurant;

import br.com.fiap.restaurant.application.port.AuthenticatedUserProvider;
import br.com.fiap.restaurant.application.port.TransactionRunner;
import br.com.fiap.restaurant.application.usecase.DeleteRestaurantUseCase;
import br.com.fiap.restaurant.domain.model.HorarioFuncionamento;
import br.com.fiap.restaurant.domain.model.MenuItem;
import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.domain.model.TipoCozinha;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.repository.MenuItemRepository;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Proves Override B's atomicity claim end-to-end: if the SECOND write of the
 * restaurant -> menu-item cascade fails, the FIRST write (the bulk menu-item
 * delete) must be rolled back too, not left half-applied. Deliberately not a
 * @DataJpaTest: that annotation wraps every test in its own Spring-managed
 * transaction that always rolls back at the end regardless of what happens
 * inside, which would hide exactly the failure mode this test exists to
 * catch. @SpringBootTest (no test-level transaction) plus a real
 * TransactionRunner and a real MenuItemRepository is what makes this a
 * genuine proof rather than an artifact of the test harness.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class DeleteRestaurantCascadeRollbackTest {

    // Seeded by V3__create_user_types_table.sql.
    private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private TransactionRunner transactionRunner;

    @Test
    void aFailureMidCascadeRollsBackTheMenuItemDeletion() {
        User owner = User.create("Dono Rollback", "dono" + UUID.randomUUID() + "@example.com",
                "dono.rollback." + UUID.randomUUID(), "hash", null, CLIENTE_ID);
        userRepository.save(owner);

        Restaurant restaurant = Restaurant.create("Cantina do Rollback", "Rua A, 100", TipoCozinha.ITALIANA,
                new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(22, 0)), owner.getId());
        restaurantRepository.save(restaurant);

        menuItemRepository.save(MenuItem.create("Item Um", null, new BigDecimal("10.00"), false, null, restaurant.getId()));
        menuItemRepository.save(MenuItem.create("Item Dois", null, new BigDecimal("20.00"), false, null, restaurant.getId()));

        // Real MenuItemRepository + real TransactionRunner, but a
        // RestaurantRepository stub whose findById returns the real
        // restaurant (so the ownership pre-checks pass normally) while its
        // deleteById fails - simulating a crash between the cascade's two
        // writes.
        RestaurantRepository failingRestaurantRepository = mock(RestaurantRepository.class);
        when(failingRestaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        doThrow(new RuntimeException("simulated failure deleting the restaurant"))
                .when(failingRestaurantRepository).deleteById(restaurant.getId());

        AuthenticatedUserProvider callerIsOwner = owner::getId;

        var useCase = new DeleteRestaurantUseCase(failingRestaurantRepository, menuItemRepository,
                callerIsOwner, transactionRunner);

        assertThatThrownBy(() -> useCase.execute(restaurant.getId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("simulated failure deleting the restaurant");

        // The whole cascade is one transaction: the second write failing
        // must roll back the first write too, so the items are still there.
        List<MenuItem> stillThere = menuItemRepository.findByRestaurantId(restaurant.getId(), 0, 20);
        assertThat(stillThere).hasSize(2);
    }
}
