package br.com.fiap.restaurant.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * AuthenticationException is thrown inside the Spring Security filter chain
 * (by AuthorizationFilter, when authorizeHttpRequests rejects an
 * unauthenticated request) and caught there by ExceptionTranslationFilter,
 * which delegates to this entry point - entirely within the servlet filter
 * layer, before the request ever reaches DispatcherServlet. That's why this
 * is a standalone AuthenticationEntryPoint rather than a
 * {@code @RestControllerAdvice} method: {@code @ExceptionHandler} only
 * intercepts exceptions thrown during normal Spring MVC dispatch, which this
 * never reaches.
 */
@Component
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final URI TYPE_UNAUTHORIZED = URI.create("urn:problem-type:unauthorized");

    private final ObjectMapper objectMapper;

    public ProblemDetailAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Full authentication is required to access this resource");
        problemDetail.setTitle("Unauthorized");
        problemDetail.setType(TYPE_UNAUTHORIZED);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problemDetail);
    }
}
