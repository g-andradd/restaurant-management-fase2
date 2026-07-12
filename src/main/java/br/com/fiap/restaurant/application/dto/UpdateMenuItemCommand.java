package br.com.fiap.restaurant.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateMenuItemCommand(UUID restaurantId, UUID id, String nome, String descricao, BigDecimal preco,
                                     boolean disponivelSomenteNoLocal, String fotoPath) {
}
