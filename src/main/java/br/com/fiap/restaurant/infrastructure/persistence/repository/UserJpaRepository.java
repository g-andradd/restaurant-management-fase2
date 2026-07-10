package br.com.fiap.restaurant.infrastructure.persistence.repository;

import br.com.fiap.restaurant.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByLogin(String login);

    boolean existsByEmail(String email);

    boolean existsByLogin(String login);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByLoginAndIdNot(String login, UUID id);
}
