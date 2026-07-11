package br.com.fiap.restaurant.domain.exception;

public class UserTypeNameAlreadyExistsException extends DomainException {

    private final String nome;

    public UserTypeNameAlreadyExistsException(String nome) {
        super("User type name already in use: " + nome);
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }
}
