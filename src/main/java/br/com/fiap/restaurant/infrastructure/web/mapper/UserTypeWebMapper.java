package br.com.fiap.restaurant.infrastructure.web.mapper;

import br.com.fiap.restaurant.application.dto.CreateUserTypeCommand;
import br.com.fiap.restaurant.application.dto.PageResult;
import br.com.fiap.restaurant.application.dto.UpdateUserTypeCommand;
import br.com.fiap.restaurant.application.dto.UserTypeResult;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateUserTypeRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.PageResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.UpdateUserTypeRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.UserTypeResponse;

import java.util.UUID;

public final class UserTypeWebMapper {

    private UserTypeWebMapper() {
    }

    public static CreateUserTypeCommand toCommand(CreateUserTypeRequest request) {
        return new CreateUserTypeCommand(request.nome());
    }

    public static UpdateUserTypeCommand toCommand(UUID id, UpdateUserTypeRequest request) {
        return new UpdateUserTypeCommand(id, request.nome());
    }

    public static UserTypeResponse toResponse(UserTypeResult result) {
        return new UserTypeResponse(result.id(), result.nome());
    }

    public static PageResponse<UserTypeResponse> toPageResponse(PageResult<UserTypeResult> pageResult) {
        return new PageResponse<>(
                pageResult.content().stream().map(UserTypeWebMapper::toResponse).toList(),
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages());
    }
}
