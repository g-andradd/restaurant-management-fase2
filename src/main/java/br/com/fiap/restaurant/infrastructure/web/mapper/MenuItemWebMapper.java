package br.com.fiap.restaurant.infrastructure.web.mapper;

import br.com.fiap.restaurant.application.dto.CreateMenuItemCommand;
import br.com.fiap.restaurant.application.dto.MenuItemResult;
import br.com.fiap.restaurant.application.dto.PageResult;
import br.com.fiap.restaurant.application.dto.UpdateMenuItemCommand;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateMenuItemRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.MenuItemResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.PageResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.UpdateMenuItemRequest;

import java.util.UUID;

public final class MenuItemWebMapper {

    private MenuItemWebMapper() {
    }

    public static CreateMenuItemCommand toCommand(UUID restaurantId, CreateMenuItemRequest request) {
        return new CreateMenuItemCommand(restaurantId, request.nome(), request.descricao(), request.preco(),
                request.disponivelSomenteNoLocal(), request.fotoPath());
    }

    public static UpdateMenuItemCommand toCommand(UUID restaurantId, UUID id, UpdateMenuItemRequest request) {
        return new UpdateMenuItemCommand(restaurantId, id, request.nome(), request.descricao(), request.preco(),
                request.disponivelSomenteNoLocal(), request.fotoPath());
    }

    public static MenuItemResponse toResponse(MenuItemResult result) {
        return new MenuItemResponse(result.id(), result.nome(), result.descricao(), result.preco(),
                result.disponivelSomenteNoLocal(), result.fotoPath(), result.restaurantId(),
                result.dataCriacao(), result.dataUltimaAlteracao());
    }

    public static PageResponse<MenuItemResponse> toPageResponse(PageResult<MenuItemResult> pageResult) {
        return new PageResponse<>(
                pageResult.content().stream().map(MenuItemWebMapper::toResponse).toList(),
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages());
    }
}
