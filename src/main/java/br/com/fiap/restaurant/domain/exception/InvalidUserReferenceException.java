package br.com.fiap.restaurant.domain.exception;

import java.util.UUID;

/**
 * Thrown when a request body references a userId (e.g. a restaurant's
 * ownerId) that does not exist. Distinct from {@link UserNotFoundException}:
 * that one means "the URL's own target resource doesn't exist" (404); this
 * one means "an otherwise well-formed request references something that
 * doesn't exist" (422). Same pattern as {@link InvalidUserTypeReferenceException}.
 */
public class InvalidUserReferenceException extends DomainException {

    private final UUID userId;

    public InvalidUserReferenceException(UUID userId) {
        super("No such user: " + userId);
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
