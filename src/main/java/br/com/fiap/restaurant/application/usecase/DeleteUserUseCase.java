package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.domain.exception.UserNotFoundException;
import br.com.fiap.restaurant.domain.exception.UserOwnsRestaurantsException;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;
import br.com.fiap.restaurant.domain.repository.UserRepository;

import java.util.UUID;

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
