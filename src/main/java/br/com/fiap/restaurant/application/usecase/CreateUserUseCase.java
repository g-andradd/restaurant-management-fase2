package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.CreateUserCommand;
import br.com.fiap.restaurant.application.dto.UserResult;
import br.com.fiap.restaurant.application.dto.UserTypeResult;
import br.com.fiap.restaurant.application.port.PasswordEncoder;
import br.com.fiap.restaurant.domain.exception.EmailAlreadyExistsException;
import br.com.fiap.restaurant.domain.exception.InvalidUserTypeReferenceException;
import br.com.fiap.restaurant.domain.exception.LoginAlreadyExistsException;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.model.UserType;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;

public class CreateUserUseCase {

    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;
    private final PasswordEncoder passwordEncoder;

    public CreateUserUseCase(UserRepository userRepository, UserTypeRepository userTypeRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userTypeRepository = userTypeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResult execute(CreateUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyExistsException(command.email());
        }
        if (userRepository.existsByLogin(command.login())) {
            throw new LoginAlreadyExistsException(command.login());
        }

        UserType userType = userTypeRepository.findById(command.userTypeId())
                .orElseThrow(() -> new InvalidUserTypeReferenceException(command.userTypeId()));

        String senhaHash = passwordEncoder.encode(command.senha());
        User user = User.create(command.nome(), command.email(), command.login(), senhaHash,
                command.endereco(), command.userTypeId());
        User saved = userRepository.save(user);
        return UserResult.from(saved, UserTypeResult.from(userType));
    }
}
