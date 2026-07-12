package br.com.fiap.restaurant.domain.exception;

import java.util.UUID;

public class UserCannotOwnRestaurantException extends DomainException {

    private final UUID userId;

    public UserCannotOwnRestaurantException(UUID userId) {
        super("User's type cannot own a restaurant: " + userId);
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
