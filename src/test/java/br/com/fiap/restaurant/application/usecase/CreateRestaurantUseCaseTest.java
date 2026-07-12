package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.CreateRestaurantCommand;
import br.com.fiap.restaurant.application.exception.NotRestaurantOwnerException;
import br.com.fiap.restaurant.application.port.AuthenticatedUserProvider;
import br.com.fiap.restaurant.domain.exception.InvalidUserReferenceException;
import br.com.fiap.restaurant.domain.exception.UserCannotOwnRestaurantException;
import br.com.fiap.restaurant.domain.exception.UserTypeNotFoundException;
import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.domain.model.TipoCozinha;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.model.UserType;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateRestaurantUseCaseTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTypeRepository userTypeRepository;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    private CreateRestaurantUseCase useCase() {
        return new CreateRestaurantUseCase(restaurantRepository, userRepository, userTypeRepository, authenticatedUserProvider);
    }

    private CreateRestaurantCommand command(UUID ownerId) {
        return new CreateRestaurantCommand("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA,
                LocalTime.of(8, 0), LocalTime.of(22, 0), ownerId);
    }

    @Test
    void createsRestaurantWhenCallerIsOwnerAndTypeCanOwn() {
        UUID userTypeId = UUID.randomUUID();
        User owner = User.create("Ana Silva", "ana@example.com", "ana.silva", "hash", null, userTypeId);
        UUID ownerId = owner.getId();
        UserType donoType = UserType.reconstitute(userTypeId, "Dono de Restaurante", true);

        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(userTypeRepository.findById(userTypeId)).thenReturn(Optional.of(donoType));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase().execute(command(ownerId));

        assertThat(result.nome()).isEqualTo("Cantina da Ana");
        assertThat(result.owner().id()).isEqualTo(ownerId);
    }

    @Test
    void rejectsWhenOwnerIdIsNotTheCaller() {
        // P0: without this check, any authenticated user could create a
        // restaurant on behalf of someone else just by knowing their id -
        // there is no admin role/create-on-behalf-of case in Phase 2.
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        User owner = User.create("Ana Silva", "ana@example.com", "ana.silva", "hash", null, UUID.randomUUID());

        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(callerId);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> useCase().execute(command(ownerId)))
                .isInstanceOf(NotRestaurantOwnerException.class);
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownOwnerId() {
        UUID ownerId = UUID.randomUUID();
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(command(ownerId)))
                .isInstanceOf(InvalidUserReferenceException.class);
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void rejectsWhenOwnerUserTypeCannotOwnRestaurants() {
        // Capability-flag based check: even though the caller IS the owner
        // (identity check passes), a Cliente-typed user still cannot create
        // a restaurant - this must not be keyed on name/UUID string matching.
        UUID ownerId = UUID.randomUUID();
        UUID userTypeId = UUID.randomUUID();
        User owner = User.create("Ana Silva", "ana@example.com", "ana.silva", "hash", null, userTypeId);
        UserType clienteType = UserType.reconstitute(userTypeId, "Cliente", false);

        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(userTypeRepository.findById(userTypeId)).thenReturn(Optional.of(clienteType));

        assertThatThrownBy(() -> useCase().execute(command(ownerId)))
                .isInstanceOf(UserCannotOwnRestaurantException.class);
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void rejectsWhenOwnerUserTypeNoLongerExists() {
        UUID ownerId = UUID.randomUUID();
        UUID userTypeId = UUID.randomUUID();
        User owner = User.create("Ana Silva", "ana@example.com", "ana.silva", "hash", null, userTypeId);

        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(userTypeRepository.findById(userTypeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(command(ownerId)))
                .isInstanceOf(UserTypeNotFoundException.class);
    }
}
