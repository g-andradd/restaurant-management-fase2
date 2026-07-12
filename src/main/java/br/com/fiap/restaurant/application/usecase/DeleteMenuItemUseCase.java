package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.exception.NotRestaurantOwnerException;
import br.com.fiap.restaurant.application.port.AuthenticatedUserProvider;
import br.com.fiap.restaurant.domain.exception.MenuItemNotFoundException;
import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.domain.model.MenuItem;
import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.domain.repository.MenuItemRepository;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;

import java.util.UUID;

public class DeleteMenuItemUseCase {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public DeleteMenuItemUseCase(MenuItemRepository menuItemRepository, RestaurantRepository restaurantRepository,
                                  AuthenticatedUserProvider authenticatedUserProvider) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public void execute(UUID restaurantId, UUID id) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

        MenuItem menuItem = menuItemRepository.findById(id)
                .filter(item -> item.getRestaurantId().equals(restaurantId))
                .orElseThrow(() -> new MenuItemNotFoundException(id));

        UUID callerId = authenticatedUserProvider.getCurrentUserId();
        if (!restaurant.getOwnerId().equals(callerId)) {
            throw new NotRestaurantOwnerException(restaurantId, callerId);
        }

        menuItemRepository.deleteById(menuItem.getId());
    }
}
