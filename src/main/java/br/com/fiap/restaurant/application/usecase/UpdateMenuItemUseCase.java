package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.MenuItemResult;
import br.com.fiap.restaurant.application.dto.UpdateMenuItemCommand;
import br.com.fiap.restaurant.application.exception.NotRestaurantOwnerException;
import br.com.fiap.restaurant.application.port.AuthenticatedUserProvider;
import br.com.fiap.restaurant.domain.exception.MenuItemNotFoundException;
import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.domain.model.MenuItem;
import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.domain.repository.MenuItemRepository;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;

import java.util.UUID;

/**
 * Updates a {@link MenuItem} scoped to a {@code restaurantId}, owner-only.
 * Item existence/match to the restaurant is checked <strong>before</strong>
 * the ownership check, unconditionally: reversing that order would let a
 * non-owner distinguish "item exists under another restaurant" (403) from
 * "item doesn't exist" (404) - the same anti-leak trap as
 * {@link GetMenuItemByIdUseCase}, applied to the write path.
 */
public class UpdateMenuItemUseCase {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public UpdateMenuItemUseCase(MenuItemRepository menuItemRepository, RestaurantRepository restaurantRepository,
                                  AuthenticatedUserProvider authenticatedUserProvider) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public MenuItemResult execute(UpdateMenuItemCommand command) {
        Restaurant restaurant = restaurantRepository.findById(command.restaurantId())
                .orElseThrow(() -> new RestaurantNotFoundException(command.restaurantId()));

        // Item existence/match is checked BEFORE the ownership check,
        // unconditionally - a mismatched item must 404 regardless of who's
        // calling, never leaking existence via a 403. See
        // MenuItemNotFoundException's Javadoc.
        MenuItem menuItem = menuItemRepository.findById(command.id())
                .filter(item -> item.getRestaurantId().equals(command.restaurantId()))
                .orElseThrow(() -> new MenuItemNotFoundException(command.id()));

        UUID callerId = authenticatedUserProvider.getCurrentUserId();
        if (!restaurant.getOwnerId().equals(callerId)) {
            throw new NotRestaurantOwnerException(command.restaurantId(), callerId);
        }

        menuItem.atualizarDados(command.nome(), command.descricao(), command.preco(),
                command.disponivelSomenteNoLocal(), command.fotoPath());
        MenuItem saved = menuItemRepository.save(menuItem);
        return MenuItemResult.from(saved);
    }
}
