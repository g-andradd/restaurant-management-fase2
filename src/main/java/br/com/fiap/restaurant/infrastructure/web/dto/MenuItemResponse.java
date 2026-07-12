package br.com.fiap.restaurant.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MenuItemResponse(UUID id, String nome, String descricao, BigDecimal preco,
                                boolean disponivelSomenteNoLocal, String fotoPath, UUID restaurantId,
                                LocalDateTime dataCriacao, LocalDateTime dataUltimaAlteracao) {
}
