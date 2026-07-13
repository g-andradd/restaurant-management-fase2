package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.domain.exception.UserNotFoundException;
import br.com.fiap.restaurant.domain.exception.UserOwnsRestaurantsException;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;
import br.com.fiap.restaurant.domain.repository.UserRepository;

import java.util.UUID;

/**
 * Deletes a {@code User}, blocked with a 409 if the user still owns any
 * restaurant ({@code existsByOwnerId}) - Phase 2 has no cascade and no
 * reassignment path for a user's restaurants, so this is a hard block, not
 * a warning. Checking explicitly here means the foreign key never has to
 * surface as an unhandled 500.
 */
public class DeleteUserUseCase {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;

    public DeleteUserUseCase(UserRepository userRepository, RestaurantRepository restaurantRepository) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
    }

    public void execute(UUID id) {
        if (userRepository.findById(id).isEmpty()) {
            throw new UserNotFoundException(id);
        }
        if (restaurantRepository.existsByOwnerId(id)) {
            throw new UserOwnsRestaurantsException(id);
        }
        userRepository.deleteById(id);
    }
}
