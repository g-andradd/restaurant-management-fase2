package br.com.fiap.restaurant.application.dto;

import br.com.fiap.restaurant.domain.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResult(UUID id, String nome, String email, String login, String endereco,
                          LocalDateTime dataCriacao, LocalDateTime dataUltimaAlteracao) {

    public static UserResult from(User user) {
        return new UserResult(user.getId(), user.getNome(), user.getEmail(), user.getLogin(),
                user.getEndereco(), user.getDataCriacao(), user.getDataUltimaAlteracao());
    }
}
