package br.com.fiap.restaurant.infrastructure.web.mapper;

import br.com.fiap.restaurant.application.dto.CreateUserCommand;
import br.com.fiap.restaurant.application.dto.PageResult;
import br.com.fiap.restaurant.application.dto.UpdateUserCommand;
import br.com.fiap.restaurant.application.dto.UserResult;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateUserRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.PageResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.UpdateUserRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.UserResponse;

import java.util.UUID;

public final class UserWebMapper {

    private UserWebMapper() {
    }

    public static CreateUserCommand toCommand(CreateUserRequest request) {
        return new CreateUserCommand(request.nome(), request.email(), request.login(),
                request.senha(), request.endereco(), request.userTypeId());
    }

    public static UpdateUserCommand toCommand(UUID id, UpdateUserRequest request) {
        return new UpdateUserCommand(id, request.nome(), request.email(), request.login(),
                request.senha(), request.endereco(), request.userTypeId());
    }

    public static UserResponse toResponse(UserResult result) {
        return new UserResponse(result.id(), result.nome(), result.email(), result.login(),
                result.endereco(), UserTypeWebMapper.toResponse(result.userType()),
                result.dataCriacao(), result.dataUltimaAlteracao());
    }

    public static PageResponse<UserResponse> toPageResponse(PageResult<UserResult> pageResult) {
        return new PageResponse<>(
                pageResult.content().stream().map(UserWebMapper::toResponse).toList(),
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages());
    }
}
