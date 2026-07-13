package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.UpdateUserTypeCommand;
import br.com.fiap.restaurant.application.dto.UserTypeResult;
import br.com.fiap.restaurant.domain.exception.UserTypeNameAlreadyExistsException;
import br.com.fiap.restaurant.domain.exception.UserTypeNotFoundException;
import br.com.fiap.restaurant.domain.model.UserType;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;

/**
 * Renames/updates a {@link UserType}, checking name uniqueness excluding
 * itself (409 on conflict with another type's name).
 */
public class UpdateUserTypeUseCase {

    private final UserTypeRepository userTypeRepository;

    public UpdateUserTypeUseCase(UserTypeRepository userTypeRepository) {
        this.userTypeRepository = userTypeRepository;
    }

    public UserTypeResult execute(UpdateUserTypeCommand command) {
        UserType userType = userTypeRepository.findById(command.id())
                .orElseThrow(() -> new UserTypeNotFoundException(command.id()));

        if (userTypeRepository.existsByNomeAndIdNot(command.nome(), command.id())) {
            throw new UserTypeNameAlreadyExistsException(command.nome());
        }

        userType.renomear(command.nome());
        userType.definirPodeSerDono(command.podeSerDono());
        UserType saved = userTypeRepository.save(userType);
        return UserTypeResult.from(saved);
    }
}
