package br.com.fiap.restaurant.application.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid login or password");
    }
}
