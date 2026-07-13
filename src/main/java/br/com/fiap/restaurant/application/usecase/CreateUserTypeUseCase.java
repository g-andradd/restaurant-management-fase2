package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.CreateUserTypeCommand;
import br.com.fiap.restaurant.application.dto.UserTypeResult;
import br.com.fiap.restaurant.domain.exception.UserTypeNameAlreadyExistsException;
import br.com.fiap.restaurant.domain.model.UserType;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;

/**
 * Creates a {@link UserType} after checking name uniqueness (409 on
 * conflict).
 */
public class CreateUserTypeUseCase {

    private final UserTypeRepository userTypeRepository;

    public CreateUserTypeUseCase(UserTypeRepository userTypeRepository) {
        this.userTypeRepository = userTypeRepository;
    }

    public UserTypeResult execute(CreateUserTypeCommand command) {
        if (userTypeRepository.existsByNome(command.nome())) {
            throw new UserTypeNameAlreadyExistsException(command.nome());
        }

        UserType userType = UserType.create(command.nome(), command.podeSerDono());
        UserType saved = userTypeRepository.save(userType);
        return UserTypeResult.from(saved);
    }
}
