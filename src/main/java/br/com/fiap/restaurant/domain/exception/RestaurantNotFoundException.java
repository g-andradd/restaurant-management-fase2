package br.com.fiap.restaurant.domain.exception;

import java.util.UUID;

public class RestaurantNotFoundException extends DomainException {

    private final UUID id;

    public RestaurantNotFoundException(UUID id) {
        super("Restaurant not found: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
