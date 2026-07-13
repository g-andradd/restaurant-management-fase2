package br.com.fiap.restaurant.domain.repository;

import br.com.fiap.restaurant.domain.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    List<User> findAllById(Collection<UUID> ids);

    Optional<User> findByLogin(String login);

    List<User> findAll(int page, int size);

    long count();

    boolean existsByEmail(String email);

    boolean existsByLogin(String login);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByLoginAndIdNot(String login, UUID id);

    boolean existsByUserTypeId(UUID userTypeId);

    void deleteById(UUID id);
}
