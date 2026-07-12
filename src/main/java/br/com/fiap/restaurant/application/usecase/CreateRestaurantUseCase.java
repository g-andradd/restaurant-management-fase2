package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.CreateRestaurantCommand;
import br.com.fiap.restaurant.application.dto.RestaurantOwnerResult;
import br.com.fiap.restaurant.application.dto.RestaurantResult;
import br.com.fiap.restaurant.application.exception.NotRestaurantOwnerException;
import br.com.fiap.restaurant.application.port.AuthenticatedUserProvider;
import br.com.fiap.restaurant.domain.exception.InvalidUserReferenceException;
import br.com.fiap.restaurant.domain.exception.UserCannotOwnRestaurantException;
import br.com.fiap.restaurant.domain.model.HorarioFuncionamento;
import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.model.UserType;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;

import java.util.UUID;

public class CreateRestaurantUseCase {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public CreateRestaurantUseCase(RestaurantRepository restaurantRepository, UserRepository userRepository,
                                    UserTypeRepository userTypeRepository,
                                    AuthenticatedUserProvider authenticatedUserProvider) {
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
        this.userTypeRepository = userTypeRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public RestaurantResult execute(CreateRestaurantCommand command) {
        User owner = userRepository.findById(command.ownerId())
                .orElseThrow(() -> new InvalidUserReferenceException(command.ownerId()));

        // P0: the ownerId must be the caller's own id. There is no admin role
        // in Phase 2, so there is no legitimate create-on-behalf-of case -
        // without this check, any authenticated user (even a Cliente) could
        // create a restaurant in someone else's name just by knowing their id.
        UUID callerId = authenticatedUserProvider.getCurrentUserId();
        if (!command.ownerId().equals(callerId)) {
            throw new NotRestaurantOwnerException(
                    "User " + callerId + " cannot create a restaurant on behalf of user " + command.ownerId());
        }

        // Not a *NotFoundException/404: users.user_type_id is NOT NULL with an
        // FK to user_types, so a User whose UserType can't be resolved means
        // the data itself is corrupt - unreachable in practice. A 404 here
        // would violate "404 only for the URL's own target"; 500 is the
        // honest answer for a state that should be structurally impossible.
        UserType ownerType = userTypeRepository.findById(owner.getUserTypeId())
                .orElseThrow(() -> new IllegalStateException(
                        "User " + owner.getId() + " references a non-existent UserType " + owner.getUserTypeId()));
        if (!ownerType.podeSerDono()) {
            throw new UserCannotOwnRestaurantException(command.ownerId());
        }

        HorarioFuncionamento horario = new HorarioFuncionamento(command.horarioAbertura(), command.horarioFechamento());
        Restaurant restaurant = Restaurant.create(command.nome(), command.endereco(), command.tipoCozinha(),
                horario, command.ownerId());
        Restaurant saved = restaurantRepository.save(restaurant);
        return RestaurantResult.from(saved, RestaurantOwnerResult.from(owner));
    }
}
