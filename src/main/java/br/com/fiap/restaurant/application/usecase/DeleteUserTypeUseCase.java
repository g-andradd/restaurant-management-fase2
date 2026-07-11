package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.domain.exception.UserTypeInUseException;
import br.com.fiap.restaurant.domain.exception.UserTypeNotFoundException;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;

import java.util.UUID;

public class DeleteUserTypeUseCase {

    private final UserTypeRepository userTypeRepository;
    private final UserRepository userRepository;

    public DeleteUserTypeUseCase(UserTypeRepository userTypeRepository, UserRepository userRepository) {
        this.userTypeRepository = userTypeRepository;
        this.userRepository = userRepository;
    }

    public void execute(UUID id) {
        if (userTypeRepository.findById(id).isEmpty()) {
            throw new UserTypeNotFoundException(id);
        }
        if (userRepository.existsByUserTypeId(id)) {
            throw new UserTypeInUseException(id);
        }
        userTypeRepository.deleteById(id);
    }
}
