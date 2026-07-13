package br.com.fiap.restaurant.domain.exception;

public class EmailAlreadyExistsException extends DomainException {

    private final String email;

    public EmailAlreadyExistsException(String email) {
        super("Email already in use: " + email);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
