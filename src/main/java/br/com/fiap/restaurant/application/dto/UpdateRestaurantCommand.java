package br.com.fiap.restaurant.application.dto;

import br.com.fiap.restaurant.domain.model.TipoCozinha;

import java.time.LocalTime;
import java.util.UUID;

public record UpdateRestaurantCommand(UUID id, String nome, String endereco, TipoCozinha tipoCozinha,
                                       LocalTime horarioAbertura, LocalTime horarioFechamento) {
}
