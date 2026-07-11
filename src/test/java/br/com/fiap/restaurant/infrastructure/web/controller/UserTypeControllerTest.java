package br.com.fiap.restaurant.infrastructure.web.controller;

import br.com.fiap.restaurant.application.dto.PageResult;
import br.com.fiap.restaurant.application.dto.UserTypeResult;
import br.com.fiap.restaurant.application.port.TokenProvider;
import br.com.fiap.restaurant.application.usecase.CreateUserTypeUseCase;
import br.com.fiap.restaurant.application.usecase.DeleteUserTypeUseCase;
import br.com.fiap.restaurant.application.usecase.GetUserTypeByIdUseCase;
import br.com.fiap.restaurant.application.usecase.ListUserTypesUseCase;
import br.com.fiap.restaurant.application.usecase.UpdateUserTypeUseCase;
import br.com.fiap.restaurant.domain.exception.UserTypeInUseException;
import br.com.fiap.restaurant.domain.exception.UserTypeNotFoundException;
import br.com.fiap.restaurant.infrastructure.security.ProblemDetailAccessDeniedHandler;
import br.com.fiap.restaurant.infrastructure.security.ProblemDetailAuthenticationEntryPoint;
import br.com.fiap.restaurant.infrastructure.security.SecurityConfig;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateUserTypeRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.UpdateUserTypeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(UserTypeController.class)
@Import({SecurityConfig.class, ProblemDetailAuthenticationEntryPoint.class, ProblemDetailAccessDeniedHandler.class})
@ActiveProfiles("test")
@WithMockUser
class UserTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private CreateUserTypeUseCase createUserTypeUseCase;

    @MockBean
    private GetUserTypeByIdUseCase getUserTypeByIdUseCase;

    @MockBean
    private ListUserTypesUseCase listUserTypesUseCase;

    @MockBean
    private UpdateUserTypeUseCase updateUserTypeUseCase;

    @MockBean
    private DeleteUserTypeUseCase deleteUserTypeUseCase;

    @Test
    void createReturns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(createUserTypeUseCase.execute(any())).thenReturn(new UserTypeResult(id, "Cliente"));

        var request = new CreateUserTypeRequest("Cliente");

        mockMvc.perform(post("/api/v1/user-types")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.nome").value("Cliente"));
    }

    @Test
    void createReturns400OnBlankNome() throws Exception {
        var request = new CreateUserTypeRequest(" ");

        mockMvc.perform(post("/api/v1/user-types")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void getByIdReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(getUserTypeByIdUseCase.execute(id)).thenReturn(new UserTypeResult(id, "Dono de Restaurante"));

        mockMvc.perform(get("/api/v1/user-types/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Dono de Restaurante"));
    }

    @Test
    void getByIdReturns404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(getUserTypeByIdUseCase.execute(id)).thenThrow(new UserTypeNotFoundException(id));

        mockMvc.perform(get("/api/v1/user-types/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void listReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        var pageResult = PageResult.of(List.of(new UserTypeResult(id, "Cliente")), 0, 20, 1);
        when(listUserTypesUseCase.execute(any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/user-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()));
    }

    @Test
    void updateReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(updateUserTypeUseCase.execute(any())).thenReturn(new UserTypeResult(id, "Cliente"));

        var request = new UpdateUserTypeRequest("Cliente");

        mockMvc.perform(put("/api/v1/user-types/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/user-types/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns409WhenInUse() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new UserTypeInUseException(id)).when(deleteUserTypeUseCase).execute(id);

        mockMvc.perform(delete("/api/v1/user-types/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }
}
