package br.com.fiap.restaurant.domain.repository;

import br.com.fiap.restaurant.domain.model.MenuItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence contract for {@link MenuItem}. {@link #deleteByRestaurantId}
 * exists specifically so {@code DeleteRestaurantUseCase} can cascade-delete
 * a restaurant's items explicitly and transactionally before deleting the
 * restaurant itself - it is not a generic bulk-delete utility.
 */
public interface MenuItemRepository {

    MenuItem save(MenuItem menuItem);

    Optional<MenuItem> findById(UUID id);

    List<MenuItem> findByRestaurantId(UUID restaurantId, int page, int size);

    long countByRestaurantId(UUID restaurantId);

    void deleteById(UUID id);

    void deleteByRestaurantId(UUID restaurantId);
}
