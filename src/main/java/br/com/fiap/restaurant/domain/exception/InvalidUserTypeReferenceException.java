package br.com.fiap.restaurant.domain.exception;

import java.util.UUID;

/**
 * Thrown when a request body references a userTypeId that does not exist.
 * Deliberately distinct from {@link UserTypeNotFoundException}: that one
 * means "the URL's own target resource doesn't exist" (404); this one means
 * "an otherwise well-formed request references something that doesn't
 * exist" (422 - the request is syntactically valid but semantically
 * unprocessable).
 */
public class InvalidUserTypeReferenceException extends DomainException {

    private final UUID userTypeId;

    public InvalidUserTypeReferenceException(UUID userTypeId) {
        super("No such user type: " + userTypeId);
        this.userTypeId = userTypeId;
    }

    public UUID getUserTypeId() {
        return userTypeId;
    }
}
