package br.com.fiap.restaurant.infrastructure.persistence.repository;

import br.com.fiap.restaurant.infrastructure.persistence.entity.UserTypeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserTypeJpaRepository extends JpaRepository<UserTypeJpaEntity, UUID> {

    boolean existsByNome(String nome);

    boolean existsByNomeAndIdNot(String nome, UUID id);
}
