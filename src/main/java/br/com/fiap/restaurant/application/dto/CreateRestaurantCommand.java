package br.com.fiap.restaurant.application.dto;

import br.com.fiap.restaurant.domain.model.TipoCozinha;

import java.time.LocalTime;
import java.util.UUID;

public record CreateRestaurantCommand(String nome, String endereco, TipoCozinha tipoCozinha,
                                       LocalTime horarioAbertura, LocalTime horarioFechamento, UUID ownerId) {
}
