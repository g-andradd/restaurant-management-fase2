package br.com.fiap.restaurant.infrastructure.persistence.repository;

import br.com.fiap.restaurant.infrastructure.persistence.entity.RestaurantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RestaurantJpaRepository extends JpaRepository<RestaurantJpaEntity, UUID> {

    boolean existsByOwnerId(UUID ownerId);
}
