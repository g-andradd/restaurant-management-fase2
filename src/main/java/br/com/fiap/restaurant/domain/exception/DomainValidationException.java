package br.com.fiap.restaurant.domain.exception;

/**
 * Thrown by domain constructors/validators when an aggregate or value
 * object invariant is violated (blank required fields, out-of-range
 * values, cross-field invariants like abertura/fechamento ordering).
 * Deliberately NOT {@link IllegalArgumentException}: that type is also
 * thrown by library code and by genuine programming errors (e.g.
 * java.util.UUID.fromString on a malformed value), so mapping it globally
 * to 400 would tell a client "your request is bad" when the server is
 * actually broken. This type exists so GlobalExceptionHandler can map
 * exactly "a domain invariant was violated" to 400, and nothing else.
 */
public class DomainValidationException extends DomainException {

    public DomainValidationException(String message) {
        super(message);
    }
}
