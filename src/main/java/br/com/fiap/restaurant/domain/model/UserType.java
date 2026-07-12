package br.com.fiap.restaurant.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class UserType {

    private final UUID id;
    private String nome;
    private boolean podeSerDono;

    private UserType(UUID id, String nome, boolean podeSerDono) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        validarNome(nome);
        this.nome = nome;
        this.podeSerDono = podeSerDono;
    }

    public static UserType create(String nome, boolean podeSerDono) {
        return new UserType(UUID.randomUUID(), nome, podeSerDono);
    }

    public static UserType reconstitute(UUID id, String nome, boolean podeSerDono) {
        return new UserType(id, nome, podeSerDono);
    }

    public void renomear(String nome) {
        validarNome(nome);
        this.nome = nome;
    }

    public void definirPodeSerDono(boolean podeSerDono) {
        this.podeSerDono = podeSerDono;
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

    public boolean podeSerDono() {
        return podeSerDono;
    }
}
