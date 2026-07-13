package br.com.fiap.restaurant.domain.repository;

import br.com.fiap.restaurant.domain.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence contract for {@link User}. The {@code existsBy...AndIdNot}
 * methods exist for uniqueness checks on update, excluding the record being
 * updated from the comparison; {@link #existsByUserTypeId} backs the "can't
 * delete a type still in use" 409 rule; {@link #findAllById} lets
 * {@code ListRestaurantsUseCase} batch-resolve distinct owners in one call
 * instead of one query per restaurant (N+1).
 */
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
