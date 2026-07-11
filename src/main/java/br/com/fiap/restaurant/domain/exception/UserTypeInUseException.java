package br.com.fiap.restaurant.domain.exception;

import java.util.UUID;

public class UserTypeInUseException extends DomainException {

    private final UUID id;

    public UserTypeInUseException(UUID id) {
        super("User type is still in use by at least one user: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
