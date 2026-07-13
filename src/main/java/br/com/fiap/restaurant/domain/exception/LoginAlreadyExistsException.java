package br.com.fiap.restaurant.domain.exception;

public class LoginAlreadyExistsException extends DomainException {

    private final String login;

    public LoginAlreadyExistsException(String login) {
        super("Login already in use: " + login);
        this.login = login;
    }

    public String getLogin() {
        return login;
    }
}
