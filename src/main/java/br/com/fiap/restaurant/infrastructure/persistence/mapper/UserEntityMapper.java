package br.com.fiap.restaurant.infrastructure.persistence.mapper;

import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.infrastructure.persistence.entity.UserJpaEntity;

public final class UserEntityMapper {

    private UserEntityMapper() {
    }

    public static UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getLogin(),
                user.getSenhaHash(),
                user.getEndereco(),
                user.getDataCriacao(),
                user.getDataUltimaAlteracao());
    }

    public static User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                entity.getId(),
                entity.getNome(),
                entity.getEmail(),
                entity.getLogin(),
                entity.getSenhaHash(),
                entity.getEndereco(),
                entity.getDataCriacao(),
                entity.getDataUltimaAlteracao());
    }
}
