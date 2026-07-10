package br.com.fiap.restaurant.domain.exception;

import java.util.UUID;

public class UserNotFoundException extends DomainException {

    private final UUID id;

    public UserNotFoundException(UUID id) {
        super("User not found: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
