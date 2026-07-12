package br.com.fiap.restaurant.application.port;

import java.util.UUID;

public interface AuthenticatedUserProvider {

    UUID getCurrentUserId();
}
