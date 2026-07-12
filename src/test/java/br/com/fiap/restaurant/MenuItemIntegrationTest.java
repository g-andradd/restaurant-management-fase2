package br.com.fiap.restaurant;

import br.com.fiap.restaurant.domain.model.TipoCozinha;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateMenuItemRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateRestaurantRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateUserRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.LoginRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.LoginResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.MenuItemResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.RestaurantResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.UpdateMenuItemRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class MenuItemIntegrationTest {

    // Seeded by V3__create_user_types_table.sql + V4's backfill: only the
    // Dono type has can_own_restaurant = TRUE.
    private static final UUID DONO_DE_RESTAURANTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UserResponse signUp(String nomePrefix, UUID userTypeId) throws Exception {
        int n = COUNTER.incrementAndGet();
        var request = new CreateUserRequest(nomePrefix + " " + n, nomePrefix.toLowerCase() + n + "@example.com",
                nomePrefix.toLowerCase() + "." + n, "senha123", null, userTypeId);

        var result = mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse.class);
    }

    private String login(String loginName) throws Exception {
        var loginRequest = new LoginRequest(loginName, "senha123");
        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        var loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), LoginResponse.class);
        return loginResponse.accessToken();
    }

    private RestaurantResponse createRestaurant(UUID ownerId, String token) throws Exception {
        var request = new CreateRestaurantRequest("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA,
                LocalTime.of(8, 0), LocalTime.of(22, 0), ownerId);
        var result = mockMvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), RestaurantResponse.class);
    }

    private static CreateMenuItemRequest createMenuItemRequest() {
        return new CreateMenuItemRequest("Pizza Margherita", "Molho, mussarela", new BigDecimal("39.90"), true, null);
    }

    /**
     * Cold-start proof: signup as Dono -> login -> create restaurant ->
     * create menu item, entirely through HTTP, never calling a use case or
     * repository directly.
     */
    @Test
    void coldStartCreateMenuItemWorksThroughHttpAlone() throws Exception {
        var owner = signUp("Sofia", DONO_DE_RESTAURANTE_ID);
        String token = login(owner.login());
        var restaurant = createRestaurant(owner.id(), token);

        mockMvc.perform(post("/api/v1/restaurants/{restaurantId}/menu-items", restaurant.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createMenuItemRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.restaurantId").value(restaurant.id().toString()));
    }

    @Test
    void createReturns403WhenCallerDoesNotOwnTheRestaurant() throws Exception {
        var ownerA = signUp("Tiago", DONO_DE_RESTAURANTE_ID);
        var ownerB = signUp("Ursula", DONO_DE_RESTAURANTE_ID);
        String tokenA = login(ownerA.login());
        var restaurantOfB = createRestaurant(ownerB.id(), login(ownerB.login()));

        mockMvc.perform(post("/api/v1/restaurants/{restaurantId}/menu-items", restaurantOfB.id())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createMenuItemRequest())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void crossRestaurantItemAccessReturns404NotForbiddenOnGetPutAndDelete() throws Exception {
        // THE P0 TRAP, end-to-end: two real restaurants owned by two
        // different Donos. An item created under restaurant A must not be
        // reachable (read, update, or delete) via restaurant B's path, even
        // though B is a real restaurant that genuinely exists.
        var ownerA = signUp("Vitor", DONO_DE_RESTAURANTE_ID);
        var ownerB = signUp("Wanda", DONO_DE_RESTAURANTE_ID);
        String tokenA = login(ownerA.login());
        String tokenB = login(ownerB.login());
        var restaurantA = createRestaurant(ownerA.id(), tokenA);
        var restaurantB = createRestaurant(ownerB.id(), tokenB);

        var createResult = mockMvc.perform(post("/api/v1/restaurants/{restaurantId}/menu-items", restaurantA.id())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createMenuItemRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        var itemOfA = objectMapper.readValue(createResult.getResponse().getContentAsString(), MenuItemResponse.class);

        // ownerB (who genuinely owns restaurantB) tries to reach A's item
        // through B's path - must 404, never 403, on all three operations.
        mockMvc.perform(get("/api/v1/restaurants/{restaurantId}/menu-items/{id}", restaurantB.id(), itemOfA.id())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404));

        var updateRequest = new UpdateMenuItemRequest("Hacked", null, new BigDecimal("1.00"), false, null);
        mockMvc.perform(put("/api/v1/restaurants/{restaurantId}/menu-items/{id}", restaurantB.id(), itemOfA.id())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(delete("/api/v1/restaurants/{restaurantId}/menu-items/{id}", restaurantB.id(), itemOfA.id())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404));

        // Sanity check: the item is untouched and still reachable via its
        // real restaurant.
        mockMvc.perform(get("/api/v1/restaurants/{restaurantId}/menu-items/{id}", restaurantA.id(), itemOfA.id())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Pizza Margherita"));
    }

    @Test
    void deletingARestaurantCascadesToItsMenuItems() throws Exception {
        var owner = signUp("Xavier", DONO_DE_RESTAURANTE_ID);
        String token = login(owner.login());
        var restaurant = createRestaurant(owner.id(), token);

        var item1Result = mockMvc.perform(post("/api/v1/restaurants/{restaurantId}/menu-items", restaurant.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createMenuItemRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        var item1 = objectMapper.readValue(item1Result.getResponse().getContentAsString(), MenuItemResponse.class);

        mockMvc.perform(post("/api/v1/restaurants/{restaurantId}/menu-items", restaurant.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createMenuItemRequest())))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/restaurants/{restaurantId}", restaurant.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // The restaurant itself is gone, so its own path 404s - and so does
        // the item that used to live under it (proving it was actually
        // deleted, not just orphaned).
        mockMvc.perform(get("/api/v1/restaurants/{restaurantId}/menu-items/{id}", restaurant.id(), item1.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMenuItemIsOpenToAnyAuthenticatedUserNotJustTheOwner() throws Exception {
        var owner = signUp("Yara", DONO_DE_RESTAURANTE_ID);
        String ownerToken = login(owner.login());
        var restaurant = createRestaurant(owner.id(), ownerToken);

        var createResult = mockMvc.perform(post("/api/v1/restaurants/{restaurantId}/menu-items", restaurant.id())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createMenuItemRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        var item = objectMapper.readValue(createResult.getResponse().getContentAsString(), MenuItemResponse.class);

        var otherUser = signUp("Zeca", CLIENTE_ID);
        String otherToken = login(otherUser.login());

        mockMvc.perform(get("/api/v1/restaurants/{restaurantId}/menu-items/{id}", restaurant.id(), item.id())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(item.id().toString()));

        mockMvc.perform(get("/api/v1/restaurants/{restaurantId}/menu-items", restaurant.id())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk());
    }
}
