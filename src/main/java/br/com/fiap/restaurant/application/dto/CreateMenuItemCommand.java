package br.com.fiap.restaurant.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateMenuItemCommand(UUID restaurantId, String nome, String descricao, BigDecimal preco,
                                     boolean disponivelSomenteNoLocal, String fotoPath) {
}
