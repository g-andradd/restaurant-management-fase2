package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.PageQuery;
import br.com.fiap.restaurant.application.dto.PageResult;
import br.com.fiap.restaurant.application.dto.RestaurantOwnerResult;
import br.com.fiap.restaurant.application.dto.RestaurantResult;
import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;
import br.com.fiap.restaurant.domain.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ListRestaurantsUseCase {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    public ListRestaurantsUseCase(RestaurantRepository restaurantRepository, UserRepository userRepository) {
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
    }

    public PageResult<RestaurantResult> execute(PageQuery query) {
        List<Restaurant> restaurants = restaurantRepository.findAll(query.page(), query.size());

        List<UUID> distinctOwnerIds = restaurants.stream().map(Restaurant::getOwnerId).distinct().toList();
        Map<UUID, RestaurantOwnerResult> ownersById = userRepository.findAllById(distinctOwnerIds).stream()
                .map(RestaurantOwnerResult::from)
                .collect(Collectors.toMap(RestaurantOwnerResult::id, Function.identity()));

        List<RestaurantResult> content = restaurants.stream()
                .map(restaurant -> RestaurantResult.from(restaurant, ownersById.get(restaurant.getOwnerId())))
                .toList();

        long totalElements = restaurantRepository.count();
        return PageResult.of(content, query.page(), query.size(), totalElements);
    }
}
