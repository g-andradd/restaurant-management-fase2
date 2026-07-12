package br.com.fiap.restaurant.infrastructure.persistence.repository;

import br.com.fiap.restaurant.domain.model.MenuItem;
import br.com.fiap.restaurant.domain.repository.MenuItemRepository;
import br.com.fiap.restaurant.infrastructure.persistence.mapper.MenuItemEntityMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MenuItemRepositoryAdapter implements MenuItemRepository {

    private final MenuItemJpaRepository jpaRepository;

    public MenuItemRepositoryAdapter(MenuItemJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public MenuItem save(MenuItem menuItem) {
        var saved = jpaRepository.save(MenuItemEntityMapper.toEntity(menuItem));
        return MenuItemEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<MenuItem> findById(UUID id) {
        return jpaRepository.findById(id).map(MenuItemEntityMapper::toDomain);
    }

    @Override
    public List<MenuItem> findByRestaurantId(UUID restaurantId, int page, int size) {
        return jpaRepository.findByRestaurantId(restaurantId, PageRequest.of(page, size)).getContent().stream()
                .map(MenuItemEntityMapper::toDomain)
                .toList();
    }

    @Override
    public long countByRestaurantId(UUID restaurantId) {
        return jpaRepository.countByRestaurantId(restaurantId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteByRestaurantId(UUID restaurantId) {
        jpaRepository.deleteByRestaurantId(restaurantId);
    }
}
