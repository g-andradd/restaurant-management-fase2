package br.com.fiap.restaurant.infrastructure.security;

import br.com.fiap.restaurant.application.port.TokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * JWT-based stateless authentication: POST /api/v1/auth/login,
 * POST /api/v1/users (public self-registration - otherwise a freshly
 * started app has no way to create its first user, since a token can only
 * be obtained by logging in as a user that doesn't exist yet), GET on
 * /api/v1/user-types/** (lets that same anonymous signup discover valid
 * type ids), and the springdoc UI are open. Everything else requires a
 * valid Bearer token. No role-based authorization yet (that's M04+) -
 * JwtAuthenticationFilter only establishes an authenticated principal with
 * no authorities.
 */
@Configuration
public class SecurityConfig {

    private final TokenProvider tokenProvider;
    private final ProblemDetailAuthenticationEntryPoint authenticationEntryPoint;
    private final ProblemDetailAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(TokenProvider tokenProvider,
                           ProblemDetailAuthenticationEntryPoint authenticationEntryPoint,
                           ProblemDetailAccessDeniedHandler accessDeniedHandler) {
        this.tokenProvider = tokenProvider;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        // Public self-registration: must stay ABOVE any broader
                        // /api/v1/users/** rule (present or future), otherwise a
                        // wider rule declared first would shadow this one and
                        // either lock registration behind auth again or - worse -
                        // accidentally open the rest of /users. GET/PUT/DELETE on
                        // /api/v1/users/** stay authenticated via anyRequest() below.
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                        // Read-only: lets an anonymous signup discover valid user
                        // type ids without a token. Managing types (POST/PUT/
                        // DELETE) stays authenticated below via anyRequest().
                        .requestMatchers(HttpMethod.GET, "/api/v1/user-types", "/api/v1/user-types/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
