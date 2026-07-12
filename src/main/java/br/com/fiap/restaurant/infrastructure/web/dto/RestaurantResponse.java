package br.com.fiap.restaurant.infrastructure.web.dto;

import br.com.fiap.restaurant.domain.model.TipoCozinha;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record RestaurantResponse(UUID id, String nome, String endereco, TipoCozinha tipoCozinha,
                                  LocalTime horarioAbertura, LocalTime horarioFechamento,
                                  RestaurantOwnerResponse owner, LocalDateTime dataCriacao,
                                  LocalDateTime dataUltimaAlteracao) {
}
