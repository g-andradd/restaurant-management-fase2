package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.CreateMenuItemCommand;
import br.com.fiap.restaurant.application.dto.MenuItemResult;
import br.com.fiap.restaurant.application.exception.NotRestaurantOwnerException;
import br.com.fiap.restaurant.application.port.AuthenticatedUserProvider;
import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.domain.model.MenuItem;
import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.domain.repository.MenuItemRepository;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;

import java.util.UUID;

/**
 * Creates a {@link MenuItem} under a restaurant. Ownership is checked
 * before creation - only the restaurant's current owner may add items to
 * it.
 */
public class CreateMenuItemUseCase {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public CreateMenuItemUseCase(MenuItemRepository menuItemRepository, RestaurantRepository restaurantRepository,
                                  AuthenticatedUserProvider authenticatedUserProvider) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public MenuItemResult execute(CreateMenuItemCommand command) {
        Restaurant restaurant = restaurantRepository.findById(command.restaurantId())
                .orElseThrow(() -> new RestaurantNotFoundException(command.restaurantId()));

        UUID callerId = authenticatedUserProvider.getCurrentUserId();
        if (!restaurant.getOwnerId().equals(callerId)) {
            throw new NotRestaurantOwnerException(command.restaurantId(), callerId);
        }

        MenuItem menuItem = MenuItem.create(command.nome(), command.descricao(), command.preco(),
                command.disponivelSomenteNoLocal(), command.fotoPath(), command.restaurantId());
        MenuItem saved = menuItemRepository.save(menuItem);
        return MenuItemResult.from(saved);
    }
}
