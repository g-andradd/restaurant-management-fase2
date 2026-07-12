package br.com.fiap.restaurant.infrastructure.web.controller;

import br.com.fiap.restaurant.application.dto.MenuItemResult;
import br.com.fiap.restaurant.application.dto.PageResult;
import br.com.fiap.restaurant.application.exception.NotRestaurantOwnerException;
import br.com.fiap.restaurant.application.port.TokenProvider;
import br.com.fiap.restaurant.application.usecase.CreateMenuItemUseCase;
import br.com.fiap.restaurant.application.usecase.DeleteMenuItemUseCase;
import br.com.fiap.restaurant.application.usecase.GetMenuItemByIdUseCase;
import br.com.fiap.restaurant.application.usecase.ListMenuItemsUseCase;
import br.com.fiap.restaurant.application.usecase.UpdateMenuItemUseCase;
import br.com.fiap.restaurant.domain.exception.MenuItemNotFoundException;
import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.infrastructure.security.ProblemDetailAccessDeniedHandler;
import br.com.fiap.restaurant.infrastructure.security.ProblemDetailAuthenticationEntryPoint;
import br.com.fiap.restaurant.infrastructure.security.SecurityConfig;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateMenuItemRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.UpdateMenuItemRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MenuItemController.class)
@Import({SecurityConfig.class, ProblemDetailAuthenticationEntryPoint.class, ProblemDetailAccessDeniedHandler.class})
@ActiveProfiles("test")
@WithMockUser
class MenuItemControllerTest {

    private static final UUID RESTAURANT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private CreateMenuItemUseCase createMenuItemUseCase;

    @MockBean
    private GetMenuItemByIdUseCase getMenuItemByIdUseCase;

    @MockBean
    private ListMenuItemsUseCase listMenuItemsUseCase;

    @MockBean
    private UpdateMenuItemUseCase updateMenuItemUseCase;

    @MockBean
    private DeleteMenuItemUseCase deleteMenuItemUseCase;

    private static MenuItemResult sampleResult(UUID id) {
        LocalDateTime now = LocalDateTime.now();
        return new MenuItemResult(id, "Pizza Margherita", "Molho, mussarela", new BigDecimal("39.90"), true,
                null, RESTAURANT_ID, now, now);
    }

    private static CreateMenuItemRequest createRequest() {
        return new CreateMenuItemRequest("Pizza Margherita", "Molho, mussarela", new BigDecimal("39.90"), true, null);
    }

    private static UpdateMenuItemRequest updateRequest() {
        return new UpdateMenuItemRequest("Pizza Grande", "Nova descricao", new BigDecimal("45.00"), true, null);
    }

    @Test
    void createReturns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(createMenuItemUseCase.execute(any())).thenReturn(sampleResult(id));

        mockMvc.perform(post("/api/v1/restaurants/{restaurantId}/menu-items", RESTAURANT_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.restaurantId").value(RESTAURANT_ID.toString()));
    }

    @Test
    void createReturns400OnBlankNome() throws Exception {
        var request = new CreateMenuItemRequest(" ", null, new BigDecimal("10.00"), false, null);

        mockMvc.perform(post("/api/v1/restaurants/{restaurantId}/menu-items", RESTAURANT_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errors.nome").exists());
    }

    @Test
    void createReturns400OnNonPositivePreco() throws Exception {
        var request = new CreateMenuItemRequest("Pizza", null, BigDecimal.ZERO, false, null);

        mockMvc.perform(post("/api/v1/restaurants/{restaurantId}/menu-items", RESTAURANT_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errors.preco").exists());
    }

    @Test
    void createReturns404OnUnknownRestaurantId() throws Exception {
        when(createMenuItemUseCase.execute(any())).thenThrow(new RestaurantNotFoundException(RESTAURANT_ID));

        mockMvc.perform(post("/api/v1/restaurants/{restaurantId}/menu-items", RESTAURANT_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void createReturns403WhenCallerIsNotTheRestaurantOwner() throws Exception {
        when(createMenuItemUseCase.execute(any()))
                .thenThrow(new NotRestaurantOwnerException(RESTAURANT_ID, UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/restaurants/{restaurantId}/menu-items", RESTAURANT_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getByIdReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(getMenuItemByIdUseCase.execute(RESTAURANT_ID, id)).thenReturn(sampleResult(id));

        mockMvc.perform(get("/api/v1/restaurants/{restaurantId}/menu-items/{id}", RESTAURANT_ID, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getByIdReturns404OnUnknownRestaurantId() throws Exception {
        UUID id = UUID.randomUUID();
        when(getMenuItemByIdUseCase.execute(RESTAURANT_ID, id)).thenThrow(new RestaurantNotFoundException(RESTAURANT_ID));

        mockMvc.perform(get("/api/v1/restaurants/{restaurantId}/menu-items/{id}", RESTAURANT_ID, id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void getByIdReturns404WhenItemBelongsToAnotherRestaurantNeverForbidden() throws Exception {
        // THE P0 TRAP, asserted at the HTTP layer: an item that exists but
        // under a different restaurant must 404, never 403 (which would leak
        // that the item exists somewhere).
        UUID id = UUID.randomUUID();
        when(getMenuItemByIdUseCase.execute(RESTAURANT_ID, id)).thenThrow(new MenuItemNotFoundException(id));

        mockMvc.perform(get("/api/v1/restaurants/{restaurantId}/menu-items/{id}", RESTAURANT_ID, id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listReturns200WithPagedContent() throws Exception {
        UUID id = UUID.randomUUID();
        var pageResult = PageResult.of(List.of(sampleResult(id)), 0, 20, 1);
        when(listMenuItemsUseCase.execute(eq(RESTAURANT_ID), any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/restaurants/{restaurantId}/menu-items", RESTAURANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()));
    }

    @Test
    void listReturns404OnUnknownRestaurantId() throws Exception {
        when(listMenuItemsUseCase.execute(eq(RESTAURANT_ID), any())).thenThrow(new RestaurantNotFoundException(RESTAURANT_ID));

        mockMvc.perform(get("/api/v1/restaurants/{restaurantId}/menu-items", RESTAURANT_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void updateReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(updateMenuItemUseCase.execute(any())).thenReturn(sampleResult(id));

        mockMvc.perform(put("/api/v1/restaurants/{restaurantId}/menu-items/{id}", RESTAURANT_ID, id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void updateReturns404WhenItemBelongsToAnotherRestaurantNeverForbidden() throws Exception {
        UUID id = UUID.randomUUID();
        when(updateMenuItemUseCase.execute(any())).thenThrow(new MenuItemNotFoundException(id));

        mockMvc.perform(put("/api/v1/restaurants/{restaurantId}/menu-items/{id}", RESTAURANT_ID, id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateReturns403WhenCallerIsNotTheRestaurantOwner() throws Exception {
        UUID id = UUID.randomUUID();
        when(updateMenuItemUseCase.execute(any()))
                .thenThrow(new NotRestaurantOwnerException(RESTAURANT_ID, UUID.randomUUID()));

        mockMvc.perform(put("/api/v1/restaurants/{restaurantId}/menu-items/{id}", RESTAURANT_ID, id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void deleteReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/restaurants/{restaurantId}/menu-items/{id}", RESTAURANT_ID, id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns404WhenItemBelongsToAnotherRestaurantNeverForbidden() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new MenuItemNotFoundException(id)).when(deleteMenuItemUseCase).execute(RESTAURANT_ID, id);

        mockMvc.perform(delete("/api/v1/restaurants/{restaurantId}/menu-items/{id}", RESTAURANT_ID, id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteReturns403WhenCallerIsNotTheRestaurantOwner() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new NotRestaurantOwnerException(RESTAURANT_ID, UUID.randomUUID()))
                .when(deleteMenuItemUseCase).execute(RESTAURANT_ID, id);

        mockMvc.perform(delete("/api/v1/restaurants/{restaurantId}/menu-items/{id}", RESTAURANT_ID, id))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403));
    }
}
