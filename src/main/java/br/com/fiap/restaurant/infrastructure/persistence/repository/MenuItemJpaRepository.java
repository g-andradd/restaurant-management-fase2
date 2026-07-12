package br.com.fiap.restaurant.infrastructure.persistence.repository;

import br.com.fiap.restaurant.infrastructure.persistence.entity.MenuItemJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MenuItemJpaRepository extends JpaRepository<MenuItemJpaEntity, UUID> {

    Page<MenuItemJpaEntity> findByRestaurantId(UUID restaurantId, Pageable pageable);

    long countByRestaurantId(UUID restaurantId);

    // Explicit single bulk DELETE statement - Spring Data's default derived
    // deleteByX loads every matching row then removes them one at a time.
    @Modifying
    @Query("delete from MenuItemJpaEntity m where m.restaurantId = :restaurantId")
    void deleteByRestaurantId(@Param("restaurantId") UUID restaurantId);
}
