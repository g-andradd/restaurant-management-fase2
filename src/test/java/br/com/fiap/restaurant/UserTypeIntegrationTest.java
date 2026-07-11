package br.com.fiap.restaurant;

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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
class UserTypeIntegrationTest {

    private static final UUID DONO_DE_RESTAURANTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String loginName, String senha) throws Exception {
        var loginRequest = new LoginRequest(loginName, senha);
        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        var loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), LoginResponse.class);
        return loginResponse.accessToken();
    }

    @Test
    void selfRegistrationAsDonoDeRestauranteWorksEndToEnd() throws Exception {
        var createRequest = new CreateUserRequest("Helena Prado", "helena@example.com", "helena.prado",
                "senha123", null, DONO_DE_RESTAURANTE_ID);

        var createResult = mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userType.id").value(DONO_DE_RESTAURANTE_ID.toString()))
                .andExpect(jsonPath("$.userType.nome").value("Dono de Restaurante"))
                .andReturn();

        var createdUser = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), UserResponse.class);

        String token = login("helena.prado", "senha123");

        mockMvc.perform(get("/api/v1/users/{id}", createdUser.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userType.nome").value("Dono de Restaurante"));
    }

    @Test
    void reassigningAnExistingUsersTypeWorks() throws Exception {
        var createRequest = new CreateUserRequest("Igor Costa", "igor@example.com", "igor.costa",
                "senha123", null, CLIENTE_ID);

        var createResult = mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        var createdUser = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), UserResponse.class);

        String token = login("igor.costa", "senha123");

        var updateRequest = new UpdateUserRequest("Igor Costa", "igor@example.com", "igor.costa",
                null, null, DONO_DE_RESTAURANTE_ID);

        mockMvc.perform(put("/api/v1/users/{id}", createdUser.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userType.id").value(DONO_DE_RESTAURANTE_ID.toString()))
                .andExpect(jsonPath("$.userType.nome").value("Dono de Restaurante"));
    }

    @Test
    void publicSignupWithNonExistentUserTypeIdReturns422() throws Exception {
        // Contrast with a direct GET /user-types/{unknown-id}, which is 404:
        // here the same "no such type" condition is a reference inside a
        // POST /users body, so it must be 422 - asserted end-to-end through
        // the public signup endpoint, not just at the use-case/unit level.
        var createRequest = new CreateUserRequest("Karen Alves", "karen@example.com", "karen.alves",
                "senha123", null, UUID.randomUUID());

        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void deletingAnInUseUserTypeReturns409() throws Exception {
        var createRequest = new CreateUserRequest("Julia Reis", "julia@example.com", "julia.reis",
                "senha123", null, CLIENTE_ID);

        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        String token = login("julia.reis", "senha123");

        mockMvc.perform(delete("/api/v1/user-types/{id}", CLIENTE_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(409));
    }
}
