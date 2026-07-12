package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.MenuItemResult;
import br.com.fiap.restaurant.application.dto.PageQuery;
import br.com.fiap.restaurant.application.dto.PageResult;
import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.domain.repository.MenuItemRepository;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;

import java.util.UUID;

public class ListMenuItemsUseCase {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    public ListMenuItemsUseCase(MenuItemRepository menuItemRepository, RestaurantRepository restaurantRepository) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
    }

    public PageResult<MenuItemResult> execute(UUID restaurantId, PageQuery query) {
        if (restaurantRepository.findById(restaurantId).isEmpty()) {
            throw new RestaurantNotFoundException(restaurantId);
        }

        var content = menuItemRepository.findByRestaurantId(restaurantId, query.page(), query.size()).stream()
                .map(MenuItemResult::from)
                .toList();
        long totalElements = menuItemRepository.countByRestaurantId(restaurantId);
        return PageResult.of(content, query.page(), query.size(), totalElements);
    }
}
