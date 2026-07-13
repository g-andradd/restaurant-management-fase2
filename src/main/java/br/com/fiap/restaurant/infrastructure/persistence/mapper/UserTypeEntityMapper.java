package br.com.fiap.restaurant.infrastructure.persistence.mapper;

import br.com.fiap.restaurant.domain.model.UserType;
import br.com.fiap.restaurant.infrastructure.persistence.entity.UserTypeJpaEntity;

public final class UserTypeEntityMapper {

    private UserTypeEntityMapper() {
    }

    public static UserTypeJpaEntity toEntity(UserType userType) {
        return new UserTypeJpaEntity(userType.getId(), userType.getNome(), userType.podeSerDono());
    }

    public static UserType toDomain(UserTypeJpaEntity entity) {
        return UserType.reconstitute(entity.getId(), entity.getNome(), entity.isCanOwnRestaurant());
    }
}
