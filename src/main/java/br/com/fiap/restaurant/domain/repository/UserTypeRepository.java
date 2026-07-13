package br.com.fiap.restaurant.domain.repository;

import br.com.fiap.restaurant.domain.model.UserType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence contract for {@link UserType}. {@link #findAllById} exists
 * specifically so {@code ListUsersUseCase} can batch-resolve every distinct
 * type referenced by a page of users in one call, avoiding an N+1 query per
 * user (checked independently by {@code scripts/audit.sh} section 8).
 */
public interface UserTypeRepository {

    UserType save(UserType userType);

    Optional<UserType> findById(UUID id);

    List<UserType> findAllById(Collection<UUID> ids);

    List<UserType> findAll(int page, int size);

    long count();

    boolean existsByNome(String nome);

    boolean existsByNomeAndIdNot(String nome, UUID id);

    void deleteById(UUID id);
}
