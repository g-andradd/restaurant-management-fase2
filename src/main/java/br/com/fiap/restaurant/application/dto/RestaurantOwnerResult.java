package br.com.fiap.restaurant.application.dto;

import br.com.fiap.restaurant.domain.model.User;

import java.util.UUID;

public record RestaurantOwnerResult(UUID id, String nome) {

    public static RestaurantOwnerResult from(User user) {
        return new RestaurantOwnerResult(user.getId(), user.getNome());
    }
}
