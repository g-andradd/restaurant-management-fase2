package br.com.fiap.restaurant;

import br.com.fiap.restaurant.application.dto.CreateUserCommand;
import br.com.fiap.restaurant.application.usecase.CreateUserUseCase;
import br.com.fiap.restaurant.infrastructure.security.JwtProperties;
import br.com.fiap.restaurant.infrastructure.security.JwtTokenProvider;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateUserRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.LoginRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.LoginResponse;
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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

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
class AuthenticationIntegrationTest {

    private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CreateUserUseCase createUserUseCase;

    @Autowired
    private JwtProperties jwtProperties;

    private static void assertProblemDetailUnauthorized(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").exists());
    }

    /**
     * Regression test for the P0 bootstrap deadlock: a brand-new deployment
     * has no seeded user and SecurityConfig requires a Bearer token for
     * everything except POST /api/v1/auth/login and POST /api/v1/users. If
     * the latter weren't public, there would be no way to ever obtain a
     * first token. This test proves the whole flow works through HTTP
     * alone - it deliberately never calls createUserUseCase directly, since
     * that would bypass the filter chain and miss exactly this class of bug.
     */
    @Test
    void coldStartSelfRegistrationWorksThroughHttpAloneWithNoDirectSeeding() throws Exception {
        var createRequest = new CreateUserRequest(
                "Gabriela Reis", "gabriela@example.com", "gabriela.reis", "senha123", null, CLIENTE_ID);

        var createResult = mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        var createdUser = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), UserResponse.class);

        var loginRequest = new LoginRequest("gabriela.reis", "senha123");
        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), LoginResponse.class);

        mockMvc.perform(get("/api/v1/users/{id}", createdUser.id())
                        .header("Authorization", "Bearer " + loginResponse.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdUser.id().toString()));
    }

    @Test
    void protectedEndpointReturns401WithoutToken() throws Exception {
        var created = createUserUseCase.execute(
                new CreateUserCommand("Ana Silva", "ana@example.com", "ana.silva", "senha123", null, CLIENTE_ID));

        assertProblemDetailUnauthorized(mockMvc.perform(get("/api/v1/users/{id}", created.id())));
    }

    @Test
    void protectedEndpointReturns401WithGarbageToken() throws Exception {
        var created = createUserUseCase.execute(
                new CreateUserCommand("Bruno Souza", "bruno@example.com", "bruno.souza", "senha123", null, CLIENTE_ID));

        assertProblemDetailUnauthorized(mockMvc.perform(get("/api/v1/users/{id}", created.id())
                .header("Authorization", "Bearer this.is.not.a.valid.jwt")));
    }

    @Test
    void protectedEndpointReturns401WithExpiredToken() throws Exception {
        var created = createUserUseCase.execute(
                new CreateUserCommand("Carla Dias", "carla@example.com", "carla.dias", "senha123", null, CLIENTE_ID));

        // Same signing key as the running app, but expired the instant it's minted.
        var expiredTokenProvider = new JwtTokenProvider(new JwtProperties(jwtProperties.secret(), -1000L));
        String expiredToken = expiredTokenProvider.generateToken(created.id().toString(), Map.of());

        assertProblemDetailUnauthorized(mockMvc.perform(get("/api/v1/users/{id}", created.id())
                .header("Authorization", "Bearer " + expiredToken)));
    }

    @Test
    void updateWithoutTokenReturns401() throws Exception {
        var created = createUserUseCase.execute(
                new CreateUserCommand("Elisa Nunes", "elisa@example.com", "elisa.nunes", "senha123", null, CLIENTE_ID));

        var updateRequest = new UpdateUserRequest("Elisa N.", "elisa@example.com", "elisa.nunes", null, null, CLIENTE_ID);

        assertProblemDetailUnauthorized(mockMvc.perform(put("/api/v1/users/{id}", created.id())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(updateRequest))));
    }

    @Test
    void deleteWithoutTokenReturns401() throws Exception {
        var created = createUserUseCase.execute(
                new CreateUserCommand("Fabio Rocha", "fabio@example.com", "fabio.rocha", "senha123", null, CLIENTE_ID));

        assertProblemDetailUnauthorized(mockMvc.perform(delete("/api/v1/users/{id}", created.id())));
    }

    @Test
    void protectedEndpointReturns200WithValidBearerToken() throws Exception {
        var created = createUserUseCase.execute(
                new CreateUserCommand("Diego Melo", "diego@example.com", "diego.melo", "senha123", null, CLIENTE_ID));

        var loginRequest = new LoginRequest("diego.melo", "senha123");
        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), LoginResponse.class);

        mockMvc.perform(get("/api/v1/users/{id}", created.id())
                        .header("Authorization", "Bearer " + loginResponse.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id().toString()));
    }
}
