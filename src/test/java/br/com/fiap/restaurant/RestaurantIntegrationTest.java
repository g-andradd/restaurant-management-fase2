package br.com.fiap.restaurant;

import br.com.fiap.restaurant.domain.model.TipoCozinha;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateRestaurantRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateUserRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.LoginRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.LoginResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.RestaurantResponse;
import br.com.fiap.restaurant.infrastructure.web.dto.UpdateUserRequest;
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
class RestaurantIntegrationTest {

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

    private static CreateRestaurantRequest createRestaurantRequest(UUID ownerId) {
        return new CreateRestaurantRequest("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA,
                LocalTime.of(8, 0), LocalTime.of(22, 0), ownerId);
    }

    /**
     * Cold-start proof: creates a Dono user, logs in, and creates a restaurant
     * entirely through HTTP - never calling a use case or repository
     * directly, so it can't miss a wiring/filter-chain gap the way a
     * unit test would.
     */
    @Test
    void coldStartCreateRestaurantWorksThroughHttpAlone() throws Exception {
        var owner = signUp("Helena", DONO_DE_RESTAURANTE_ID);
        String token = login(owner.login());

        mockMvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRestaurantRequest(owner.id()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner.id").value(owner.id().toString()));
    }

    @Test
    void createForAnotherUserReturns403EndToEnd() throws Exception {
        var ownerA = signUp("Igor", DONO_DE_RESTAURANTE_ID);
        var ownerB = signUp("Julia", DONO_DE_RESTAURANTE_ID);
        String tokenA = login(ownerA.login());

        mockMvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRestaurantRequest(ownerB.id()))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void clienteCannotCreateRestaurantForADono() throws Exception {
        // Same class of bug as createForAnotherUserReturns403EndToEnd, from
        // the opposite angle: the caller's own type (Cliente, cannot own)
        // must not matter here - the identity check on ownerId fires first,
        // regardless of whether either party could otherwise own a restaurant.
        var cliente = signUp("Karen", CLIENTE_ID);
        var dono = signUp("Lucas", DONO_DE_RESTAURANTE_ID);
        String clienteToken = login(cliente.login());

        mockMvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRestaurantRequest(dono.id()))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void staleTokenClaimDoesNotGrantOwnership() throws Exception {
        // AC6 behavioural proof: the JWT's "userType" claim is a login-time
        // snapshot, never authoritative for authorization. Demote the user's
        // type via PUT /users/{id} (chosen over PUT /user-types/{id} so the
        // shared Cliente/Dono seed types are left untouched for other tests),
        // then reuse the OLD token - still carrying the stale "Dono" claim -
        // to attempt a restaurant creation. It must be rejected with 422
        // (business rule: this user's *current* type cannot own), not 201.
        var user = signUp("Marina", DONO_DE_RESTAURANTE_ID);
        String staleToken = login(user.login());

        var demoteRequest = new UpdateUserRequest(user.nome(), user.email(), user.login(), null, null, CLIENTE_ID);
        mockMvc.perform(put("/api/v1/users/{id}", user.id())
                        .header("Authorization", "Bearer " + staleToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(demoteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userType.id").value(CLIENTE_ID.toString()));

        mockMvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + staleToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRestaurantRequest(user.id()))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void userWithRestaurantCannotBeDeletedReturns409() throws Exception {
        var owner = signUp("Nadia", DONO_DE_RESTAURANTE_ID);
        String token = login(owner.login());

        mockMvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRestaurantRequest(owner.id()))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/users/{id}", owner.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void aberturaNotBeforeFechamentoReturns400NotServerError() throws Exception {
        // Regression, fixed in M05: HorarioFuncionamento's abertura/fechamento
        // invariant used to throw a plain IllegalArgumentException, which
        // GlobalExceptionHandler had no handler for - it fell through to the
        // generic 500 fallback even though the request itself was the
        // problem. Now it's DomainValidationException -> 400. Exercised
        // through the real use case (no mocks) so it actually proves the
        // production wiring, not just the handler-to-exception mapping.
        var owner = signUp("Rafaela", DONO_DE_RESTAURANTE_ID);
        String token = login(owner.login());
        var invalidRequest = new CreateRestaurantRequest("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA,
                LocalTime.of(22, 0), LocalTime.of(8, 0), owner.id());

        mockMvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getRestaurantIsOpenToAnyAuthenticatedUserNotJustTheOwner() throws Exception {
        var owner = signUp("Otavio", DONO_DE_RESTAURANTE_ID);
        String ownerToken = login(owner.login());

        var createResult = mockMvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRestaurantRequest(owner.id()))))
                .andExpect(status().isCreated())
                .andReturn();
        var createdRestaurant = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), RestaurantResponse.class);

        var otherUser = signUp("Paula", CLIENTE_ID);
        String otherToken = login(otherUser.login());

        mockMvc.perform(get("/api/v1/restaurants/{id}", createdRestaurant.id())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdRestaurant.id().toString()));

        mockMvc.perform(get("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk());
    }
}
