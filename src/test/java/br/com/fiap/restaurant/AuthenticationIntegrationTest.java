package br.com.fiap.restaurant;

import br.com.fiap.restaurant.application.dto.CreateUserCommand;
import br.com.fiap.restaurant.application.usecase.CreateUserUseCase;
import br.com.fiap.restaurant.infrastructure.security.JwtProperties;
import br.com.fiap.restaurant.infrastructure.security.JwtTokenProvider;
import br.com.fiap.restaurant.infrastructure.web.dto.LoginRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.LoginResponse;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthenticationIntegrationTest {

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

    @Test
    void protectedEndpointReturns401WithoutToken() throws Exception {
        var created = createUserUseCase.execute(
                new CreateUserCommand("Ana Silva", "ana@example.com", "ana.silva", "senha123", null));

        assertProblemDetailUnauthorized(mockMvc.perform(get("/api/v1/users/{id}", created.id())));
    }

    @Test
    void protectedEndpointReturns401WithGarbageToken() throws Exception {
        var created = createUserUseCase.execute(
                new CreateUserCommand("Bruno Souza", "bruno@example.com", "bruno.souza", "senha123", null));

        assertProblemDetailUnauthorized(mockMvc.perform(get("/api/v1/users/{id}", created.id())
                .header("Authorization", "Bearer this.is.not.a.valid.jwt")));
    }

    @Test
    void protectedEndpointReturns401WithExpiredToken() throws Exception {
        var created = createUserUseCase.execute(
                new CreateUserCommand("Carla Dias", "carla@example.com", "carla.dias", "senha123", null));

        // Same signing key as the running app, but expired the instant it's minted.
        var expiredTokenProvider = new JwtTokenProvider(new JwtProperties(jwtProperties.secret(), -1000L));
        String expiredToken = expiredTokenProvider.generateToken(created.id().toString(), Map.of());

        assertProblemDetailUnauthorized(mockMvc.perform(get("/api/v1/users/{id}", created.id())
                .header("Authorization", "Bearer " + expiredToken)));
    }

    @Test
    void protectedEndpointReturns200WithValidBearerToken() throws Exception {
        var created = createUserUseCase.execute(
                new CreateUserCommand("Diego Melo", "diego@example.com", "diego.melo", "senha123", null));

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
