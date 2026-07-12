package br.com.fiap.restaurant.application.dto;

import br.com.fiap.restaurant.domain.model.MenuItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MenuItemResult(UUID id, String nome, String descricao, BigDecimal preco,
                              boolean disponivelSomenteNoLocal, String fotoPath, UUID restaurantId,
                              LocalDateTime dataCriacao, LocalDateTime dataUltimaAlteracao) {

    public static MenuItemResult from(MenuItem menuItem) {
        return new MenuItemResult(menuItem.getId(), menuItem.getNome(), menuItem.getDescricao(),
                menuItem.getPreco(), menuItem.isDisponivelSomenteNoLocal(), menuItem.getFotoPath(),
                menuItem.getRestaurantId(), menuItem.getDataCriacao(), menuItem.getDataUltimaAlteracao());
    }
}
