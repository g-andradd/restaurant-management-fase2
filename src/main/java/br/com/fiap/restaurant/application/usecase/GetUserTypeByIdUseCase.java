package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.UserTypeResult;
import br.com.fiap.restaurant.domain.exception.UserTypeNotFoundException;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;

import java.util.UUID;

public class GetUserTypeByIdUseCase {

    private final UserTypeRepository userTypeRepository;

    public GetUserTypeByIdUseCase(UserTypeRepository userTypeRepository) {
        this.userTypeRepository = userTypeRepository;
    }

    public UserTypeResult execute(UUID id) {
        return userTypeRepository.findById(id)
                .map(UserTypeResult::from)
                .orElseThrow(() -> new UserTypeNotFoundException(id));
    }
}
