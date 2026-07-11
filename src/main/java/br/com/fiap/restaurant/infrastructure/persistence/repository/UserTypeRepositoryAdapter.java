package br.com.fiap.restaurant.infrastructure.persistence.repository;

import br.com.fiap.restaurant.domain.model.UserType;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;
import br.com.fiap.restaurant.infrastructure.persistence.entity.UserTypeJpaEntity;
import br.com.fiap.restaurant.infrastructure.persistence.mapper.UserTypeEntityMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserTypeRepositoryAdapter implements UserTypeRepository {

    private final UserTypeJpaRepository jpaRepository;

    public UserTypeRepositoryAdapter(UserTypeJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public UserType save(UserType userType) {
        UserTypeJpaEntity saved = jpaRepository.save(UserTypeEntityMapper.toEntity(userType));
        return UserTypeEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<UserType> findById(UUID id) {
        return jpaRepository.findById(id).map(UserTypeEntityMapper::toDomain);
    }

    @Override
    public List<UserType> findAllById(Collection<UUID> ids) {
        return jpaRepository.findAllById(ids).stream()
                .map(UserTypeEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<UserType> findAll(int page, int size) {
        return jpaRepository.findAll(PageRequest.of(page, size)).getContent().stream()
                .map(UserTypeEntityMapper::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public boolean existsByNome(String nome) {
        return jpaRepository.existsByNome(nome);
    }

    @Override
    public boolean existsByNomeAndIdNot(String nome, UUID id) {
        return jpaRepository.existsByNomeAndIdNot(nome, id);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
