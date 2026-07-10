package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.UserResult;
import br.com.fiap.restaurant.domain.exception.UserNotFoundException;
import br.com.fiap.restaurant.domain.repository.UserRepository;

import java.util.UUID;

public class GetUserByIdUseCase {

    private final UserRepository userRepository;

    public GetUserByIdUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResult execute(UUID id) {
        return userRepository.findById(id)
                .map(UserResult::from)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
