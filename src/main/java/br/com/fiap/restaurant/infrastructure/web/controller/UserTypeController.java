package br.com.fiap.restaurant.infrastructure.web.controller;

import br.com.fiap.restaurant.application.dto.PageQuery;
import br.com.fiap.restaurant.application.usecase.CreateUserTypeUseCase;
import br.com.fiap.restaurant.application.usecase.DeleteUserTypeUseCase;
import br.com.fiap.restaurant.application.usecase.GetUserTypeByIdUseCase;
import br.com.fiap.restaurant.application.usecase.ListUserTypesUseCase;
import br.com.fiap.restaurant.application.usecase.UpdateUserTypeUseCase;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateUserTypeRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.PageResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.UpdateUserTypeRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.UserTypeResponse;
import br.com.fiap.restaurant.infrastructure.web.mapper.UserTypeWebMapper;
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
@RequestMapping("/api/v1/user-types")
@Tag(name = "User Types")
public class UserTypeController {

    private final CreateUserTypeUseCase createUserTypeUseCase;
    private final GetUserTypeByIdUseCase getUserTypeByIdUseCase;
    private final ListUserTypesUseCase listUserTypesUseCase;
    private final UpdateUserTypeUseCase updateUserTypeUseCase;
    private final DeleteUserTypeUseCase deleteUserTypeUseCase;

    public UserTypeController(CreateUserTypeUseCase createUserTypeUseCase,
                               GetUserTypeByIdUseCase getUserTypeByIdUseCase,
                               ListUserTypesUseCase listUserTypesUseCase,
                               UpdateUserTypeUseCase updateUserTypeUseCase,
                               DeleteUserTypeUseCase deleteUserTypeUseCase) {
        this.createUserTypeUseCase = createUserTypeUseCase;
        this.getUserTypeByIdUseCase = getUserTypeByIdUseCase;
        this.listUserTypesUseCase = listUserTypesUseCase;
        this.updateUserTypeUseCase = updateUserTypeUseCase;
        this.deleteUserTypeUseCase = deleteUserTypeUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new user type")
    public ResponseEntity<UserTypeResponse> create(@Valid @RequestBody CreateUserTypeRequest request) {
        UserTypeResponse response = UserTypeWebMapper.toResponse(
                createUserTypeUseCase.execute(UserTypeWebMapper.toCommand(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user type by id")
    public ResponseEntity<UserTypeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(UserTypeWebMapper.toResponse(getUserTypeByIdUseCase.execute(id)));
    }

    @GetMapping
    @Operation(summary = "List user types, paginated")
    public ResponseEntity<PageResponse<UserTypeResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(UserTypeWebMapper.toPageResponse(listUserTypesUseCase.execute(new PageQuery(page, size))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user type")
    public ResponseEntity<UserTypeResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody UpdateUserTypeRequest request) {
        UserTypeResponse response = UserTypeWebMapper.toResponse(
                updateUserTypeUseCase.execute(UserTypeWebMapper.toCommand(id, request)));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user type")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteUserTypeUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
