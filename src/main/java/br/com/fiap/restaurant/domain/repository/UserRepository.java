package br.com.fiap.restaurant.domain.repository;

import br.com.fiap.restaurant.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByLogin(String login);

    List<User> findAll(int page, int size);

    long count();

    boolean existsByEmail(String email);

    boolean existsByLogin(String login);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByLoginAndIdNot(String login, UUID id);

    void deleteById(UUID id);
}
