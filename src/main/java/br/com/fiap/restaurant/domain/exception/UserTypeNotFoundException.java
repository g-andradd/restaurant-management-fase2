package br.com.fiap.restaurant.domain.exception;

import java.util.UUID;

public class UserTypeNotFoundException extends DomainException {

    private final UUID id;

    public UserTypeNotFoundException(UUID id) {
        super("User type not found: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
