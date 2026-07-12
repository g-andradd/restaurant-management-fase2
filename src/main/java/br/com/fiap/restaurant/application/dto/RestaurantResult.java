package br.com.fiap.restaurant.application.dto;

import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.domain.model.TipoCozinha;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record RestaurantResult(UUID id, String nome, String endereco, TipoCozinha tipoCozinha,
                                LocalTime horarioAbertura, LocalTime horarioFechamento,
                                RestaurantOwnerResult owner, LocalDateTime dataCriacao,
                                LocalDateTime dataUltimaAlteracao) {

    public static RestaurantResult from(Restaurant restaurant, RestaurantOwnerResult owner) {
        return new RestaurantResult(restaurant.getId(), restaurant.getNome(), restaurant.getEndereco(),
                restaurant.getTipoCozinha(), restaurant.getHorarioFuncionamento().getAbertura(),
                restaurant.getHorarioFuncionamento().getFechamento(), owner,
                restaurant.getDataCriacao(), restaurant.getDataUltimaAlteracao());
    }
}
