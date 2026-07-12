package br.com.fiap.restaurant.application.exception;

import java.util.UUID;

/**
 * Thrown when the authenticated caller is not the owner of the restaurant
 * they're trying to modify (update/delete), or - per the create-time
 * ownership check - is trying to create a restaurant on behalf of someone
 * else. An authorization/process failure of the use case, not a Restaurant
 * aggregate invariant, so it lives here rather than in domain.exception -
 * same placement reasoning as InvalidCredentialsException.
 */
public class NotRestaurantOwnerException extends RuntimeException {

    public NotRestaurantOwnerException(UUID restaurantId, UUID callerId) {
        super("User " + callerId + " is not the owner of restaurant " + restaurantId);
    }

    public NotRestaurantOwnerException(String message) {
        super(message);
    }
}
