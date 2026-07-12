package br.com.fiap.restaurant.domain.exception;

import java.util.UUID;

/**
 * Thrown both when a menu item genuinely doesn't exist and when it exists
 * but under a different restaurantId than the one addressed in the URL.
 * Deliberately the SAME exception (and status) for both cases: telling a
 * caller "this item belongs to a different restaurant" would confirm the
 * item exists elsewhere, which is exactly the leak the nested-route
 * ownership trap is designed to prevent.
 */
public class MenuItemNotFoundException extends DomainException {

    private final UUID id;

    public MenuItemNotFoundException(UUID id) {
        super("Menu item not found: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
