package br.com.fiap.restaurant.infrastructure.persistence.mapper;

import br.com.fiap.restaurant.domain.model.HorarioFuncionamento;
import br.com.fiap.restaurant.domain.model.Restaurant;
import br.com.fiap.restaurant.infrastructure.persistence.entity.RestaurantJpaEntity;

public final class RestaurantEntityMapper {

    private RestaurantEntityMapper() {
    }

    public static RestaurantJpaEntity toEntity(Restaurant restaurant) {
        return new RestaurantJpaEntity(
                restaurant.getId(),
                restaurant.getNome(),
                restaurant.getEndereco(),
                restaurant.getTipoCozinha(),
                restaurant.getHorarioFuncionamento().getAbertura(),
                restaurant.getHorarioFuncionamento().getFechamento(),
                restaurant.getOwnerId(),
                restaurant.getDataCriacao(),
                restaurant.getDataUltimaAlteracao());
    }

    public static Restaurant toDomain(RestaurantJpaEntity entity) {
        return Restaurant.reconstitute(
                entity.getId(),
                entity.getNome(),
                entity.getEndereco(),
                entity.getTipoCozinha(),
                new HorarioFuncionamento(entity.getHorarioAbertura(), entity.getHorarioFechamento()),
                entity.getOwnerId(),
                entity.getDataCriacao(),
                entity.getDataUltimaAlteracao());
    }
}
