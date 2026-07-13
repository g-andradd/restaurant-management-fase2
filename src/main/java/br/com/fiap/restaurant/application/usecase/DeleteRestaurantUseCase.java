package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.exception.NotRestaurantOwnerException;
import br.com.fiap.restaurant.application.port.AuthenticatedUserProvider;
import br.com.fiap.restaurant.application.port.TransactionRunner;
import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.domain.repository.MenuItemRepository;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;

import java.util.UUID;

public class DeleteRestaurantUseCase {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final TransactionRunner transactionRunner;

    public DeleteRestaurantUseCase(RestaurantRepository restaurantRepository, MenuItemRepository menuItemRepository,
                                    AuthenticatedUserProvider authenticatedUserProvider,
                                    TransactionRunner transactionRunner) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.transactionRunner = transactionRunner;
    }

    public void execute(UUID id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException(id));

        UUID callerId = authenticatedUserProvider.getCurrentUserId();
        if (!restaurant.getOwnerId().equals(callerId)) {
            throw new NotRestaurantOwnerException(id, callerId);
        }

        // Deliberate cascade, not a DB-level ON DELETE CASCADE: a MenuItem is
        // a composed child with no independent existence (unlike User/
        // UserType, which are blocked from deletion while referenced), so
        // deleting the restaurant deletes its items too. Order matters - the
        // FK forbids deleting the restaurant first. Wrapped in one
        // transaction so a failure between the two writes can't leave
        // menu items deleted but the restaurant still present, or vice versa.
        transactionRunner.run(() -> {
            menuItemRepository.deleteByRestaurantId(id);
            restaurantRepository.deleteById(id);
        });
    }
}
