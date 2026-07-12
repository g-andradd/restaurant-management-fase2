package br.com.fiap.restaurant.domain.repository;

import br.com.fiap.restaurant.domain.model.Restaurant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantRepository {

    Restaurant save(Restaurant restaurant);

    Optional<Restaurant> findById(UUID id);

    List<Restaurant> findAll(int page, int size);

    long count();

    boolean existsByOwnerId(UUID ownerId);

    void deleteById(UUID id);
}
