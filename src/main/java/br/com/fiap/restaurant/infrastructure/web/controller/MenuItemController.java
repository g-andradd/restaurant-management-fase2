package br.com.fiap.restaurant.infrastructure.web.controller;

import br.com.fiap.restaurant.application.dto.PageQuery;
import br.com.fiap.restaurant.application.usecase.CreateMenuItemUseCase;
import br.com.fiap.restaurant.application.usecase.DeleteMenuItemUseCase;
import br.com.fiap.restaurant.application.usecase.GetMenuItemByIdUseCase;
import br.com.fiap.restaurant.application.usecase.ListMenuItemsUseCase;
import br.com.fiap.restaurant.application.usecase.UpdateMenuItemUseCase;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateMenuItemRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.MenuItemResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.PageResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.UpdateMenuItemRequest;
import br.com.fiap.restaurant.infrastructure.web.mapper.MenuItemWebMapper;
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
@RequestMapping("/api/v1/restaurants/{restaurantId}/menu-items")
@Tag(name = "MenuItems")
public class MenuItemController {

    private final CreateMenuItemUseCase createMenuItemUseCase;
    private final GetMenuItemByIdUseCase getMenuItemByIdUseCase;
    private final ListMenuItemsUseCase listMenuItemsUseCase;
    private final UpdateMenuItemUseCase updateMenuItemUseCase;
    private final DeleteMenuItemUseCase deleteMenuItemUseCase;

    public MenuItemController(CreateMenuItemUseCase createMenuItemUseCase,
                               GetMenuItemByIdUseCase getMenuItemByIdUseCase,
                               ListMenuItemsUseCase listMenuItemsUseCase,
                               UpdateMenuItemUseCase updateMenuItemUseCase,
                               DeleteMenuItemUseCase deleteMenuItemUseCase) {
        this.createMenuItemUseCase = createMenuItemUseCase;
        this.getMenuItemByIdUseCase = getMenuItemByIdUseCase;
        this.listMenuItemsUseCase = listMenuItemsUseCase;
        this.updateMenuItemUseCase = updateMenuItemUseCase;
        this.deleteMenuItemUseCase = deleteMenuItemUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a menu item for a restaurant (owner only)")
    public ResponseEntity<MenuItemResponse> create(@PathVariable UUID restaurantId,
                                                     @Valid @RequestBody CreateMenuItemRequest request) {
        MenuItemResponse response = MenuItemWebMapper.toResponse(
                createMenuItemUseCase.execute(MenuItemWebMapper.toCommand(restaurantId, request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a menu item by id")
    public ResponseEntity<MenuItemResponse> getById(@PathVariable UUID restaurantId, @PathVariable UUID id) {
        return ResponseEntity.ok(MenuItemWebMapper.toResponse(getMenuItemByIdUseCase.execute(restaurantId, id)));
    }

    @GetMapping
    @Operation(summary = "List a restaurant's menu items, paginated")
    public ResponseEntity<PageResponse<MenuItemResponse>> list(
            @PathVariable UUID restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(MenuItemWebMapper.toPageResponse(
                listMenuItemsUseCase.execute(restaurantId, new PageQuery(page, size))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a menu item (owner only)")
    public ResponseEntity<MenuItemResponse> update(@PathVariable UUID restaurantId, @PathVariable UUID id,
                                                     @Valid @RequestBody UpdateMenuItemRequest request) {
        MenuItemResponse response = MenuItemWebMapper.toResponse(
                updateMenuItemUseCase.execute(MenuItemWebMapper.toCommand(restaurantId, id, request)));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a menu item (owner only)")
    public ResponseEntity<Void> delete(@PathVariable UUID restaurantId, @PathVariable UUID id) {
        deleteMenuItemUseCase.execute(restaurantId, id);
        return ResponseEntity.noContent().build();
    }
}
