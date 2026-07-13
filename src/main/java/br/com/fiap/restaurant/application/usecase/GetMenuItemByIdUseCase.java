package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.MenuItemResult;
import br.com.fiap.restaurant.domain.exception.MenuItemNotFoundException;
import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.domain.model.MenuItem;
import br.com.fiap.restaurant.domain.repository.MenuItemRepository;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;

import java.util.UUID;

/**
 * Fetches one {@link MenuItem} scoped to a {@code restaurantId}. A
 * mismatched {@code restaurantId} - the item exists, but under a different
 * restaurant - yields the exact same 404 as the item not existing at all,
 * never a 403: a 403 would leak that the item exists somewhere else. See
 * {@link MenuItemNotFoundException}.
 */
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
