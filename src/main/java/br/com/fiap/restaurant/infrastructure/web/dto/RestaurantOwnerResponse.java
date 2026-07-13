package br.com.fiap.restaurant.infrastructure.web.dto;

import java.util.UUID;

public record RestaurantOwnerResponse(UUID id, String nome) {
}
