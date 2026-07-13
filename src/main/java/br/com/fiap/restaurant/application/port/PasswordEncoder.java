package br.com.fiap.restaurant.application.port;

/**
 * One-way password hashing. {@link #matches} is the only comparison path -
 * nothing in this codebase should ever decode or compare raw hashes
 * directly.
 */
public interface PasswordEncoder {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
