package br.com.fiap.restaurant.infrastructure.web.controller;

import br.com.fiap.restaurant.application.dto.PageResult;
import br.com.fiap.restaurant.application.dto.UserResult;
import br.com.fiap.restaurant.application.port.TokenProvider;
import br.com.fiap.restaurant.application.usecase.CreateUserUseCase;
import br.com.fiap.restaurant.application.usecase.DeleteUserUseCase;
import br.com.fiap.restaurant.application.usecase.GetUserByIdUseCase;
import br.com.fiap.restaurant.application.usecase.ListUsersUseCase;
import br.com.fiap.restaurant.application.usecase.UpdateUserUseCase;
import br.com.fiap.restaurant.domain.exception.EmailAlreadyExistsException;
import br.com.fiap.restaurant.domain.exception.UserNotFoundException;
import br.com.fiap.restaurant.infrastructure.security.ProblemDetailAccessDeniedHandler;
import br.com.fiap.restaurant.infrastructure.security.ProblemDetailAuthenticationEntryPoint;
import br.com.fiap.restaurant.infrastructure.security.SecurityConfig;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateUserRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.UpdateUserRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, ProblemDetailAuthenticationEntryPoint.class, ProblemDetailAccessDeniedHandler.class})
@ActiveProfiles("test")
@WithMockUser
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateUserUseCase createUserUseCase;

    @MockBean
    private GetUserByIdUseCase getUserByIdUseCase;

    @MockBean
    private ListUsersUseCase listUsersUseCase;

    @MockBean
    private UpdateUserUseCase updateUserUseCase;

    @MockBean
    private DeleteUserUseCase deleteUserUseCase;

    private static UserResult sampleResult(UUID id) {
        LocalDateTime now = LocalDateTime.now();
        return new UserResult(id, "Ana Silva", "ana@example.com", "ana.silva", null, now, now);
    }

    @Test
    void createReturns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(createUserUseCase.execute(any())).thenReturn(sampleResult(id));

        var request = new CreateUserRequest("Ana Silva", "ana@example.com", "ana.silva", "senha123", null);

        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void createReturns400OnBlankNome() throws Exception {
        var request = new CreateUserRequest(" ", "ana@example.com", "ana.silva", "senha123", null);

        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errors.nome").exists());
    }

    @Test
    void createReturns409OnDuplicateEmail() throws Exception {
        when(createUserUseCase.execute(any())).thenThrow(new EmailAlreadyExistsException("ana@example.com"));

        var request = new CreateUserRequest("Ana Silva", "ana@example.com", "ana.silva", "senha123", null);

        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void getByIdReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(getUserByIdUseCase.execute(id)).thenReturn(sampleResult(id));

        mockMvc.perform(get("/api/v1/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getByIdReturns404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(getUserByIdUseCase.execute(id)).thenThrow(new UserNotFoundException(id));

        mockMvc.perform(get("/api/v1/users/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void listReturns200WithPagedContent() throws Exception {
        UUID id = UUID.randomUUID();
        var pageResult = PageResult.of(List.of(sampleResult(id)), 0, 20, 1);
        when(listUsersUseCase.execute(any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/users").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void updateReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(updateUserUseCase.execute(any())).thenReturn(sampleResult(id));

        var request = new UpdateUserRequest("Ana Silva", "ana@example.com", "ana.silva", null, null);

        mockMvc.perform(put("/api/v1/users/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void deleteReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/users/{id}", id))
                .andExpect(status().isNoContent());
    }
}
