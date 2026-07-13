package br.com.fiap.restaurant.application.port;

import java.util.UUID;

/**
 * Resolves the current caller's identity fresh, from the current request's
 * security context, every call - never cached, never derived from a token
 * claim. This is the seam use cases call to learn "who is calling" for
 * ownership checks (e.g. {@code CreateRestaurantUseCase}'s self-ownership
 * check) while staying framework-free themselves.
 */
public interface AuthenticatedUserProvider {

    UUID getCurrentUserId();
}
