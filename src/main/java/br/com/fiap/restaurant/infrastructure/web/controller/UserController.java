package br.com.fiap.restaurant.infrastructure.web.controller;

import br.com.fiap.restaurant.application.dto.PageQuery;
import br.com.fiap.restaurant.application.usecase.CreateUserUseCase;
import br.com.fiap.restaurant.application.usecase.DeleteUserUseCase;
import br.com.fiap.restaurant.application.usecase.GetUserByIdUseCase;
import br.com.fiap.restaurant.application.usecase.ListUsersUseCase;
import br.com.fiap.restaurant.application.usecase.UpdateUserUseCase;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateUserRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.PageResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.UpdateUserRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.UserResponse;
import br.com.fiap.restaurant.infrastructure.web.mapper.UserWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;

    public UserController(CreateUserUseCase createUserUseCase, GetUserByIdUseCase getUserByIdUseCase,
                           ListUsersUseCase listUsersUseCase, UpdateUserUseCase updateUserUseCase,
                           DeleteUserUseCase deleteUserUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new user")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = UserWebMapper.toResponse(
                createUserUseCase.execute(UserWebMapper.toCommand(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by id")
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(UserWebMapper.toResponse(getUserByIdUseCase.execute(id)));
    }

    @GetMapping
    @Operation(summary = "List users, paginated")
    public ResponseEntity<PageResponse<UserResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(UserWebMapper.toPageResponse(listUsersUseCase.execute(new PageQuery(page, size))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        UserResponse response = UserWebMapper.toResponse(
                updateUserUseCase.execute(UserWebMapper.toCommand(id, request)));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteUserUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
