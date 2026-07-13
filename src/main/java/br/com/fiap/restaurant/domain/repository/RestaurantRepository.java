package br.com.fiap.restaurant.domain.repository;

import br.com.fiap.restaurant.domain.model.Restaurant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence contract for {@link Restaurant}. {@link #existsByOwnerId} is
 * the contract {@code DeleteUserUseCase} relies on to block deleting a user
 * who still owns any restaurant with a 409, instead of letting the
 * database's foreign key surface as an unhandled 500.
 */
public interface RestaurantRepository {

    Restaurant save(Restaurant restaurant);

    Optional<Restaurant> findById(UUID id);

    List<Restaurant> findAll(int page, int size);

    long count();

    boolean existsByOwnerId(UUID ownerId);

    void deleteById(UUID id);
}
