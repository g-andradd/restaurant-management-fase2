package br.com.fiap.restaurant.application.dto;

import java.util.UUID;

public record UpdateUserTypeCommand(UUID id, String nome, boolean podeSerDono) {
}
