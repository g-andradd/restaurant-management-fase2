package br.com.fiap.restaurant.infrastructure.web.controller;

import br.com.fiap.restaurant.application.dto.PageQuery;
import br.com.fiap.restaurant.application.usecase.CreateRestaurantUseCase;
import br.com.fiap.restaurant.application.usecase.DeleteRestaurantUseCase;
import br.com.fiap.restaurant.application.usecase.GetRestaurantByIdUseCase;
import br.com.fiap.restaurant.application.usecase.ListRestaurantsUseCase;
import br.com.fiap.restaurant.application.usecase.UpdateRestaurantUseCase;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateRestaurantRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.PageResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.RestaurantResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.UpdateRestaurantRequest;
import br.com.fiap.restaurant.infrastructure.web.mapper.RestaurantWebMapper;
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
@RequestMapping("/api/v1/restaurants")
@Tag(name = "Restaurants")
public class RestaurantController {

    private final CreateRestaurantUseCase createRestaurantUseCase;
    private final GetRestaurantByIdUseCase getRestaurantByIdUseCase;
    private final ListRestaurantsUseCase listRestaurantsUseCase;
    private final UpdateRestaurantUseCase updateRestaurantUseCase;
    private final DeleteRestaurantUseCase deleteRestaurantUseCase;

    public RestaurantController(CreateRestaurantUseCase createRestaurantUseCase,
                                 GetRestaurantByIdUseCase getRestaurantByIdUseCase,
                                 ListRestaurantsUseCase listRestaurantsUseCase,
                                 UpdateRestaurantUseCase updateRestaurantUseCase,
                                 DeleteRestaurantUseCase deleteRestaurantUseCase) {
        this.createRestaurantUseCase = createRestaurantUseCase;
        this.getRestaurantByIdUseCase = getRestaurantByIdUseCase;
        this.listRestaurantsUseCase = listRestaurantsUseCase;
        this.updateRestaurantUseCase = updateRestaurantUseCase;
        this.deleteRestaurantUseCase = deleteRestaurantUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new restaurant")
    public ResponseEntity<RestaurantResponse> create(@Valid @RequestBody CreateRestaurantRequest request) {
        RestaurantResponse response = RestaurantWebMapper.toResponse(
                createRestaurantUseCase.execute(RestaurantWebMapper.toCommand(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a restaurant by id")
    public ResponseEntity<RestaurantResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(RestaurantWebMapper.toResponse(getRestaurantByIdUseCase.execute(id)));
    }

    @GetMapping
    @Operation(summary = "List restaurants, paginated")
    public ResponseEntity<PageResponse<RestaurantResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(RestaurantWebMapper.toPageResponse(listRestaurantsUseCase.execute(new PageQuery(page, size))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a restaurant (owner only)")
    public ResponseEntity<RestaurantResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody UpdateRestaurantRequest request) {
        RestaurantResponse response = RestaurantWebMapper.toResponse(
                updateRestaurantUseCase.execute(RestaurantWebMapper.toCommand(id, request)));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a restaurant (owner only)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteRestaurantUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
