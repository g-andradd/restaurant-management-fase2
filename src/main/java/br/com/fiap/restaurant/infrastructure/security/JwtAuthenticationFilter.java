package br.com.fiap.restaurant.infrastructure.security;

import br.com.fiap.restaurant.application.exception.InvalidTokenException;
import br.com.fiap.restaurant.application.port.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Extracts and validates a Bearer JWT per request. On
 * {@link InvalidTokenException}, deliberately clears the security context
 * and lets the chain continue rather than failing fast here - so a bad
 * token produces the exact same 401 (via {@code AuthorizationFilter} then
 * {@code ExceptionTranslationFilter} then
 * {@link ProblemDetailAuthenticationEntryPoint}) as no token at all. One
 * 401 path, not two.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;

    public JwtAuthenticationFilter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));

        if (token != null) {
            try {
                String subject = tokenProvider.validateAndGetSubject(token);
                var authentication = new UsernamePasswordAuthenticationToken(subject, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (InvalidTokenException ex) {
                // Missing/expired/tampered token: leave the SecurityContext empty
                // rather than fail the request here. AuthorizationFilter (further
                // down this same security filter chain) will then reject the
                // request for lack of authentication, and ExceptionTranslationFilter
                // routes that rejection to the AuthenticationEntryPoint - the same
                // 401 path as "no header at all".
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Returns the bearer token from the header, or {@code null} if the header
     * is absent, uses a different scheme, or has no token left after the
     * prefix (missing token, or only whitespace). Never throws: a malformed
     * Authorization header is always treated as "not authenticated" here,
     * never as a request-processing failure.
     */
    private static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }
        String trimmed = authorizationHeader.strip();
        if (!trimmed.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = trimmed.substring(BEARER_PREFIX.length()).strip();
        return token.isEmpty() ? null : token;
    }
}
