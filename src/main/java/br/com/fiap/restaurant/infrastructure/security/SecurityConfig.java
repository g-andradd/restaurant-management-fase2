package br.com.fiap.restaurant.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Permits all requests for now: no JWT/login endpoint exists yet (that's M02).
 * Spring Boot still auto-configures a default in-memory user (the generated
 * Basic-auth password logged on startup) since no UserDetailsService bean is
 * defined here, but that user is never challenged while every request is
 * permitAll() — it becomes relevant once M02 replaces this filter chain.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }
}
