package br.com.fiap.restaurant.domain.repository;

import br.com.fiap.restaurant.domain.model.UserType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
