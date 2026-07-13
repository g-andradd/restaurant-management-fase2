package br.com.fiap.restaurant.infrastructure.security;

import br.com.fiap.restaurant.application.port.AuthenticatedUserProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Reads the same principal JwtAuthenticationFilter sets on the
 * SecurityContext: the token's subject, which is the user's immutable UUID
 * (never the mutable "userType" claim - see AuthenticateUserUseCase's
 * Javadoc on why that claim must never gate a decision).
 */
@Component
public class SpringSecurityAuthenticatedUserProvider implements AuthenticatedUserProvider {

    @Override
    public UUID getCurrentUserId() {
        String principal = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(principal);
    }
}
