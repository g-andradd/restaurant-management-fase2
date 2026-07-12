package br.com.fiap.restaurant.infrastructure.persistence.mapper;

import br.com.fiap.restaurant.domain.model.MenuItem;
import br.com.fiap.restaurant.infrastructure.persistence.entity.MenuItemJpaEntity;

public final class MenuItemEntityMapper {

    private MenuItemEntityMapper() {
    }

    public static MenuItemJpaEntity toEntity(MenuItem menuItem) {
        return new MenuItemJpaEntity(
                menuItem.getId(),
                menuItem.getNome(),
                menuItem.getDescricao(),
                menuItem.getPreco(),
                menuItem.isDisponivelSomenteNoLocal(),
                menuItem.getFotoPath(),
                menuItem.getRestaurantId(),
                menuItem.getDataCriacao(),
                menuItem.getDataUltimaAlteracao());
    }

    public static MenuItem toDomain(MenuItemJpaEntity entity) {
        return MenuItem.reconstitute(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getPreco(),
                entity.isDisponivelSomenteNoLocal(),
                entity.getFotoPath(),
                entity.getRestaurantId(),
                entity.getDataCriacao(),
                entity.getDataUltimaAlteracao());
    }
}
