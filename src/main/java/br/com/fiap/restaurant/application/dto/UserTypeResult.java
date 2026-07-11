package br.com.fiap.restaurant.application.dto;

import br.com.fiap.restaurant.domain.model.UserType;

import java.util.UUID;

public record UserTypeResult(UUID id, String nome) {

    public static UserTypeResult from(UserType userType) {
        return new UserTypeResult(userType.getId(), userType.getNome());
    }
}
