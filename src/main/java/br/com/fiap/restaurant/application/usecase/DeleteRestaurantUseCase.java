package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.exception.NotRestaurantOwnerException;
import br.com.fiap.restaurant.application.port.AuthenticatedUserProvider;
import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;

import java.util.UUID;

public class DeleteRestaurantUseCase {

    private final RestaurantRepository restaurantRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public DeleteRestaurantUseCase(RestaurantRepository restaurantRepository,
                                    AuthenticatedUserProvider authenticatedUserProvider) {
        this.restaurantRepository = restaurantRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public void execute(UUID id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException(id));

        UUID callerId = authenticatedUserProvider.getCurrentUserId();
        if (!restaurant.getOwnerId().equals(callerId)) {
            throw new NotRestaurantOwnerException(id, callerId);
        }

        restaurantRepository.deleteById(id);
    }
}
