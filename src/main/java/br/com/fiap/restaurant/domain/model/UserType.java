package br.com.fiap.restaurant.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class UserType {

    private final UUID id;
    private String nome;

    private UserType(UUID id, String nome) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        validarNome(nome);
        this.nome = nome;
    }

    public static UserType create(String nome) {
        return new UserType(UUID.randomUUID(), nome);
    }

    public static UserType reconstitute(UUID id, String nome) {
        return new UserType(id, nome);
    }

    public void renomear(String nome) {
        validarNome(nome);
        this.nome = nome;
    }

    private static void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome must not be blank");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}
