package br.com.fiap.restaurant.infrastructure.web.controller;

import br.com.fiap.restaurant.application.dto.PageResult;
import br.com.fiap.restaurant.application.dto.RestaurantOwnerResult;
import br.com.fiap.restaurant.application.dto.RestaurantResult;
import br.com.fiap.restaurant.application.exception.NotRestaurantOwnerException;
import br.com.fiap.restaurant.application.port.TokenProvider;
import br.com.fiap.restaurant.application.usecase.CreateRestaurantUseCase;
import br.com.fiap.restaurant.application.usecase.DeleteRestaurantUseCase;
import br.com.fiap.restaurant.application.usecase.GetRestaurantByIdUseCase;
import br.com.fiap.restaurant.application.usecase.ListRestaurantsUseCase;
import br.com.fiap.restaurant.application.usecase.UpdateRestaurantUseCase;
import br.com.fiap.restaurant.domain.exception.DomainValidationException;
import br.com.fiap.restaurant.domain.exception.InvalidUserReferenceException;
import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.domain.exception.UserCannotOwnRestaurantException;
import br.com.fiap.restaurant.domain.model.TipoCozinha;
import br.com.fiap.restaurant.infrastructure.security.ProblemDetailAccessDeniedHandler;
import br.com.fiap.restaurant.infrastructure.security.ProblemDetailAuthenticationEntryPoint;
import br.com.fiap.restaurant.infrastructure.security.SecurityConfig;
import br.com.fiap.restaurant.infrastructure.web.dto.CreateRestaurantRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.UpdateRestaurantRequest;
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
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RestaurantController.class)
@Import({SecurityConfig.class, ProblemDetailAuthenticationEntryPoint.class, ProblemDetailAccessDeniedHandler.class})
@ActiveProfiles("test")
@WithMockUser
class RestaurantControllerTest {

    private static final UUID OWNER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private CreateRestaurantUseCase createRestaurantUseCase;

    @MockBean
    private GetRestaurantByIdUseCase getRestaurantByIdUseCase;

    @MockBean
    private ListRestaurantsUseCase listRestaurantsUseCase;

    @MockBean
    private UpdateRestaurantUseCase updateRestaurantUseCase;

    @MockBean
    private DeleteRestaurantUseCase deleteRestaurantUseCase;

    private static RestaurantResult sampleResult(UUID id) {
        LocalDateTime now = LocalDateTime.now();
        return new RestaurantResult(id, "Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA,
                LocalTime.of(8, 0), LocalTime.of(22, 0), new RestaurantOwnerResult(OWNER_ID, "Ana Silva"), now, now);
    }

    private static CreateRestaurantRequest createRequest() {
        return new CreateRestaurantRequest("Cantina da Ana", "Rua A, 100", TipoCozinha.ITALIANA,
                LocalTime.of(8, 0), LocalTime.of(22, 0), OWNER_ID);
    }

    private static UpdateRestaurantRequest updateRequest() {
        return new UpdateRestaurantRequest("Cantina da Ana II", "Rua B, 200", TipoCozinha.JAPONESA,
                LocalTime.of(9, 0), LocalTime.of(23, 0));
    }

    @Test
    void createReturns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(createRestaurantUseCase.execute(any())).thenReturn(sampleResult(id));

        mockMvc.perform(post("/api/v1/restaurants")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.owner.id").value(OWNER_ID.toString()));
    }

    @Test
    void createReturns400OnBlankNome() throws Exception {
        var request = new CreateRestaurantRequest(" ", "Rua A, 100", TipoCozinha.ITALIANA,
                LocalTime.of(8, 0), LocalTime.of(22, 0), OWNER_ID);

        mockMvc.perform(post("/api/v1/restaurants")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errors.nome").exists());
    }

    @Test
    void createReturns400OnUnknownTipoCozinhaLiteral() throws Exception {
        String malformedJson = """
                {"nome":"Cantina da Ana","endereco":"Rua A, 100","tipoCozinha":"NAO_EXISTE",
                "horarioAbertura":"08:00:00","horarioFechamento":"22:00:00","ownerId":"%s"}
                """.formatted(OWNER_ID);

        mockMvc.perform(post("/api/v1/restaurants")
                        .contentType("application/json")
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void createReturns400WhenAberturaIsNotBeforeFechamento() throws Exception {
        // Regression for the M04 gap fixed in M05: HorarioFuncionamento's
        // invariant now throws DomainValidationException, which
        // GlobalExceptionHandler maps to 400 (previously it fell through to
        // the generic 500 fallback, since plain IllegalArgumentException had
        // no handler). See RestaurantIntegrationTest for the end-to-end proof
        // through the real use case, not a mock.
        when(createRestaurantUseCase.execute(any()))
                .thenThrow(new DomainValidationException("abertura must be before fechamento"));

        mockMvc.perform(post("/api/v1/restaurants")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void createReturns403WhenOwnerIdIsNotTheCaller() throws Exception {
        when(createRestaurantUseCase.execute(any()))
                .thenThrow(new NotRestaurantOwnerException("caller is not the owner"));

        mockMvc.perform(post("/api/v1/restaurants")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void createReturns422OnUnknownOwnerId() throws Exception {
        when(createRestaurantUseCase.execute(any())).thenThrow(new InvalidUserReferenceException(OWNER_ID));

        mockMvc.perform(post("/api/v1/restaurants")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void createReturns422WhenOwnerUserTypeCannotOwnRestaurants() throws Exception {
        when(createRestaurantUseCase.execute(any())).thenThrow(new UserCannotOwnRestaurantException(OWNER_ID));

        mockMvc.perform(post("/api/v1/restaurants")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void getByIdReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(getRestaurantByIdUseCase.execute(id)).thenReturn(sampleResult(id));

        mockMvc.perform(get("/api/v1/restaurants/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getByIdReturns404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(getRestaurantByIdUseCase.execute(id)).thenThrow(new RestaurantNotFoundException(id));

        mockMvc.perform(get("/api/v1/restaurants/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void listReturns200WithPagedContent() throws Exception {
        UUID id = UUID.randomUUID();
        var pageResult = PageResult.of(List.of(sampleResult(id)), 0, 20, 1);
        when(listRestaurantsUseCase.execute(any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/restaurants").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void updateReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(updateRestaurantUseCase.execute(any())).thenReturn(sampleResult(id));

        mockMvc.perform(put("/api/v1/restaurants/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void updateReturns404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(updateRestaurantUseCase.execute(any())).thenThrow(new RestaurantNotFoundException(id));

        mockMvc.perform(put("/api/v1/restaurants/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void updateReturns403WhenCallerIsNotTheOwner() throws Exception {
        UUID id = UUID.randomUUID();
        when(updateRestaurantUseCase.execute(any())).thenThrow(new NotRestaurantOwnerException(id, UUID.randomUUID()));

        mockMvc.perform(put("/api/v1/restaurants/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void deleteReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/restaurants/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new RestaurantNotFoundException(id)).when(deleteRestaurantUseCase).execute(id);

        mockMvc.perform(delete("/api/v1/restaurants/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void deleteReturns403WhenCallerIsNotTheOwner() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new NotRestaurantOwnerException(id, UUID.randomUUID())).when(deleteRestaurantUseCase).execute(id);

        mockMvc.perform(delete("/api/v1/restaurants/{id}", id))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403));
    }
}
