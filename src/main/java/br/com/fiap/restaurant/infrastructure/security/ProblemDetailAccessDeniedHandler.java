package br.com.fiap.restaurant.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * AccessDeniedException is thrown inside the Spring Security filter chain and
 * caught there by ExceptionTranslationFilter, which delegates to this
 * handler - entirely within the servlet filter layer, before the request
 * ever reaches DispatcherServlet. That's why this is a standalone
 * AccessDeniedHandler rather than a {@code @RestControllerAdvice} method:
 * {@code @ExceptionHandler} only intercepts exceptions thrown during normal
 * Spring MVC dispatch, which this never reaches. (No role-based rule exists
 * yet in M02 to actually trigger this, but the handler is wired now for
 * when M03 adds one.)
 */
@Component
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private static final URI TYPE_FORBIDDEN = URI.create("urn:problem-type:forbidden");

    private final ObjectMapper objectMapper;

    public ProblemDetailAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource");
        problemDetail.setTitle("Forbidden");
        problemDetail.setType(TYPE_FORBIDDEN);

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problemDetail);
    }
}
