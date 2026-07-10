package br.com.fiap.restaurant.infrastructure.persistence.repository;

import br.com.fiap.restaurant.domain.exception.EmailAlreadyExistsException;
import br.com.fiap.restaurant.domain.exception.LoginAlreadyExistsException;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import br.com.fiap.restaurant.infrastructure.persistence.entity.UserJpaEntity;
import br.com.fiap.restaurant.infrastructure.persistence.mapper.UserEntityMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserEntityMapper.toEntity(user);
        try {
            UserJpaEntity saved = jpaRepository.save(entity);
            return UserEntityMapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            // Safety net for the small race window between the use case's existence
            // check and this write; the unique indexes are the real guarantee.
            if (jpaRepository.existsByEmailAndIdNot(user.getEmail(), user.getId())) {
                throw new EmailAlreadyExistsException(user.getEmail());
            }
            if (jpaRepository.existsByLoginAndIdNot(user.getLogin(), user.getId())) {
                throw new LoginAlreadyExistsException(user.getLogin());
            }
            throw ex;
        }
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(UserEntityMapper::toDomain);
    }

    @Override
    public List<User> findAll(int page, int size) {
        return jpaRepository.findAll(PageRequest.of(page, size)).getContent().stream()
                .map(UserEntityMapper::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByLogin(String login) {
        return jpaRepository.existsByLogin(login);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, UUID id) {
        return jpaRepository.existsByEmailAndIdNot(email, id);
    }

    @Override
    public boolean existsByLoginAndIdNot(String login, UUID id) {
        return jpaRepository.existsByLoginAndIdNot(login, id);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
