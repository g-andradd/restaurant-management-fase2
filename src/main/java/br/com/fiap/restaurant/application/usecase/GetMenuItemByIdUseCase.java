package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.MenuItemResult;
import br.com.fiap.restaurant.domain.exception.MenuItemNotFoundException;
import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.domain.model.MenuItem;
import br.com.fiap.restaurant.domain.repository.MenuItemRepository;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;

import java.util.UUID;

public class GetMenuItemByIdUseCase {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    public GetMenuItemByIdUseCase(MenuItemRepository menuItemRepository, RestaurantRepository restaurantRepository) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
    }

    public MenuItemResult execute(UUID restaurantId, UUID id) {
        if (restaurantRepository.findById(restaurantId).isEmpty()) {
            throw new RestaurantNotFoundException(restaurantId);
        }

        // Deliberately the SAME exception whether the item doesn't exist at
        // all or belongs to a different restaurant - see
        // MenuItemNotFoundException's Javadoc for why (anti-leak trap).
        MenuItem menuItem = menuItemRepository.findById(id)
                .filter(item -> item.getRestaurantId().equals(restaurantId))
                .orElseThrow(() -> new MenuItemNotFoundException(id));

        return MenuItemResult.from(menuItem);
    }
}
