package br.com.fiap.restaurant.domain.model;

import br.com.fiap.restaurant.domain.exception.DomainValidationException;

import java.util.Objects;
import java.util.UUID;

/**
 * A role a {@link User} can have. {@code podeSerDono} is the exact
 * capability flag {@code CreateRestaurantUseCase} keys restaurant
 * ownership on - never this type's {@code nome} or id, both of which are
 * mutable via this same class's CRUD. Accepted gap: {@link #definirPodeSerDono}
 * has no retroactive check - flipping the flag does not revisit users or
 * restaurants that already relied on its previous value.
 */
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
            throw new DomainValidationException("nome must not be blank");
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
