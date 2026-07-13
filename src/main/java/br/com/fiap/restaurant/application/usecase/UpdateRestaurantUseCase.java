package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.RestaurantOwnerResult;
import br.com.fiap.restaurant.application.dto.RestaurantResult;
import br.com.fiap.restaurant.application.dto.UpdateRestaurantCommand;
import br.com.fiap.restaurant.application.exception.NotRestaurantOwnerException;
import br.com.fiap.restaurant.application.port.AuthenticatedUserProvider;
import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.domain.exception.UserNotFoundException;
import br.com.fiap.restaurant.domain.model.HorarioFuncionamento;
import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;
import br.com.fiap.restaurant.domain.repository.UserRepository;

import java.util.UUID;

/**
 * Updates a {@link Restaurant}'s mutable fields, owner-only. Owner
 * reassignment is out of scope in Phase 2 - mirrors
 * {@code Restaurant.atualizarDados}'s immutable-owner invariant, so
 * {@code command} carries no owner field to change.
 */
public class UpdateRestaurantUseCase {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public UpdateRestaurantUseCase(RestaurantRepository restaurantRepository, UserRepository userRepository,
                                    AuthenticatedUserProvider authenticatedUserProvider) {
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public RestaurantResult execute(UpdateRestaurantCommand command) {
        Restaurant restaurant = restaurantRepository.findById(command.id())
                .orElseThrow(() -> new RestaurantNotFoundException(command.id()));

        UUID callerId = authenticatedUserProvider.getCurrentUserId();
        if (!restaurant.getOwnerId().equals(callerId)) {
            throw new NotRestaurantOwnerException(command.id(), callerId);
        }

        HorarioFuncionamento horario = new HorarioFuncionamento(command.horarioAbertura(), command.horarioFechamento());
        restaurant.atualizarDados(command.nome(), command.endereco(), command.tipoCozinha(), horario);
        Restaurant saved = restaurantRepository.save(restaurant);

        User owner = userRepository.findById(saved.getOwnerId())
                .orElseThrow(() -> new UserNotFoundException(saved.getOwnerId()));
        return RestaurantResult.from(saved, RestaurantOwnerResult.from(owner));
    }
}
