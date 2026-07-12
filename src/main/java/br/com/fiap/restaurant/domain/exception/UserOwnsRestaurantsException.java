package br.com.fiap.restaurant.domain.exception;

import java.util.UUID;

public class UserOwnsRestaurantsException extends DomainException {

    private final UUID userId;

    public UserOwnsRestaurantsException(UUID userId) {
        super("User still owns at least one restaurant: " + userId);
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
