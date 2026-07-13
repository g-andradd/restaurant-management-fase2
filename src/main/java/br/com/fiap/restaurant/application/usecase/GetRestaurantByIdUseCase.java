package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.RestaurantOwnerResult;
import br.com.fiap.restaurant.application.dto.RestaurantResult;
import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.domain.exception.UserNotFoundException;
import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;
import br.com.fiap.restaurant.domain.repository.UserRepository;

import java.util.UUID;

public class GetRestaurantByIdUseCase {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    public GetRestaurantByIdUseCase(RestaurantRepository restaurantRepository, UserRepository userRepository) {
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
    }

    public RestaurantResult execute(UUID id) {
        Restaurant restaurant = restaurantRepository.findById(id).orElseThrow(() -> new RestaurantNotFoundException(id));
        User owner = userRepository.findById(restaurant.getOwnerId())
                .orElseThrow(() -> new UserNotFoundException(restaurant.getOwnerId()));
        return RestaurantResult.from(restaurant, RestaurantOwnerResult.from(owner));
    }
}
