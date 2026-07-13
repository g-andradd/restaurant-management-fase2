package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.UserResult;
import br.com.fiap.restaurant.application.dto.UserTypeResult;
import br.com.fiap.restaurant.domain.exception.UserNotFoundException;
import br.com.fiap.restaurant.domain.exception.UserTypeNotFoundException;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;

import java.util.UUID;

/**
 * Fetches one {@link User} together with its resolved {@link UserType}
 * summary in a single call.
 */
public class GetUserByIdUseCase {

    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;

    public GetUserByIdUseCase(UserRepository userRepository, UserTypeRepository userTypeRepository) {
        this.userRepository = userRepository;
        this.userTypeRepository = userTypeRepository;
    }

    public UserResult execute(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        UserTypeResult userType = userTypeRepository.findById(user.getUserTypeId())
                .map(UserTypeResult::from)
                .orElseThrow(() -> new UserTypeNotFoundException(user.getUserTypeId()));
        return UserResult.from(user, userType);
    }
}
