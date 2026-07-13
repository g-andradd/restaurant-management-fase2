package br.com.fiap.restaurant.infrastructure.web.mapper;

import br.com.fiap.restaurant.application.dto.CreateRestaurantCommand;
import br.com.fiap.restaurant.application.dto.PageResult;
import br.com.fiap.restaurant.application.dto.RestaurantResult;
import br.com.fiap.restaurant.application.dto.UpdateRestaurantCommand;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateRestaurantRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.PageResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.RestaurantOwnerResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.RestaurantResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.UpdateRestaurantRequest;

import java.util.UUID;

public final class RestaurantWebMapper {

    private RestaurantWebMapper() {
    }

    public static CreateRestaurantCommand toCommand(CreateRestaurantRequest request) {
        return new CreateRestaurantCommand(request.nome(), request.endereco(), request.tipoCozinha(),
                request.horarioAbertura(), request.horarioFechamento(), request.ownerId());
    }

    public static UpdateRestaurantCommand toCommand(UUID id, UpdateRestaurantRequest request) {
        return new UpdateRestaurantCommand(id, request.nome(), request.endereco(), request.tipoCozinha(),
                request.horarioAbertura(), request.horarioFechamento());
    }

    public static RestaurantResponse toResponse(RestaurantResult result) {
        return new RestaurantResponse(result.id(), result.nome(), result.endereco(), result.tipoCozinha(),
                result.horarioAbertura(), result.horarioFechamento(),
                new RestaurantOwnerResponse(result.owner().id(), result.owner().nome()),
                result.dataCriacao(), result.dataUltimaAlteracao());
    }

    public static PageResponse<RestaurantResponse> toPageResponse(PageResult<RestaurantResult> pageResult) {
        return new PageResponse<>(
                pageResult.content().stream().map(RestaurantWebMapper::toResponse).toList(),
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages());
    }
}
