package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.exception.NotRestaurantOwnerException;
import br.com.fiap.restaurant.application.port.AuthenticatedUserProvider;
import br.com.fiap.restaurant.application.port.TransactionRunner;
import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.domain.model.HorarioFuncionamento;
import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.domain.model.TipoCozinha;
import br.com.fiap.restaurant.domain.repository.MenuItemRepository;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteRestaurantUseCaseTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Mock
    private TransactionRunner transactionRunner;

    private DeleteRestaurantUseCase useCase() {
        return new DeleteRestaurantUseCase(restaurantRepository, menuItemRepository, authenticatedUserProvider,
                transactionRunner);
    }

    private static Restaurant existingRestaurant(UUID ownerId) {
        return Restaurant.create("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA,
                new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(22, 0)), ownerId);
    }

    private void stubRunnerToActuallyRun() {
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
    }

    @Test
    void deletesWhenCallerIsOwner() {
        stubRunnerToActuallyRun();
        UUID ownerId = UUID.randomUUID();
        Restaurant restaurant = existingRestaurant(ownerId);

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(ownerId);

        useCase().execute(restaurant.getId());

        verify(restaurantRepository).deleteById(restaurant.getId());
    }

    @Test
    void cascadesMenuItemDeletionBeforeRestaurantDeletionInsideOneTransaction() {
        stubRunnerToActuallyRun();
        UUID ownerId = UUID.randomUUID();
        Restaurant restaurant = existingRestaurant(ownerId);

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(ownerId);

        useCase().execute(restaurant.getId());

        verify(transactionRunner, times(1)).run(any());
        InOrder order = inOrder(menuItemRepository, restaurantRepository);
        order.verify(menuItemRepository).deleteByRestaurantId(restaurant.getId());
        order.verify(restaurantRepository).deleteById(restaurant.getId());
    }

    @Test
    void throwsWhenRestaurantNotFound() {
        UUID id = UUID.randomUUID();
        when(restaurantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(id)).isInstanceOf(RestaurantNotFoundException.class);
        verify(transactionRunner, never()).run(any());
    }

    @Test
    void rejectsWhenCallerIsNotTheOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        Restaurant restaurant = existingRestaurant(ownerId);

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(callerId);

        assertThatThrownBy(() -> useCase().execute(restaurant.getId())).isInstanceOf(NotRestaurantOwnerException.class);
        verify(restaurantRepository, never()).deleteById(any());
        verify(transactionRunner, never()).run(any());
    }
}
