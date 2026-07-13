package br.com.fiap.restaurant.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "user_types")
public class UserTypeJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "can_own_restaurant", nullable = false)
    private boolean canOwnRestaurant;

    protected UserTypeJpaEntity() {
        // required by JPA
    }

    public UserTypeJpaEntity(UUID id, String nome, boolean canOwnRestaurant) {
        this.id = id;
        this.nome = nome;
        this.canOwnRestaurant = canOwnRestaurant;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public boolean isCanOwnRestaurant() {
        return canOwnRestaurant;
    }
}
