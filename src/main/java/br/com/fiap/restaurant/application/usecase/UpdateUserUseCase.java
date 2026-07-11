package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.UpdateUserCommand;
import br.com.fiap.restaurant.application.dto.UserResult;
import br.com.fiap.restaurant.application.dto.UserTypeResult;
import br.com.fiap.restaurant.application.port.PasswordEncoder;
import br.com.fiap.restaurant.domain.exception.EmailAlreadyExistsException;
import br.com.fiap.restaurant.domain.exception.InvalidUserTypeReferenceException;
import br.com.fiap.restaurant.domain.exception.LoginAlreadyExistsException;
import br.com.fiap.restaurant.domain.exception.UserNotFoundException;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.model.UserType;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;

public class UpdateUserUseCase {

    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;
    private final PasswordEncoder passwordEncoder;

    public UpdateUserUseCase(UserRepository userRepository, UserTypeRepository userTypeRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userTypeRepository = userTypeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResult execute(UpdateUserCommand command) {
        User user = userRepository.findById(command.id())
                .orElseThrow(() -> new UserNotFoundException(command.id()));

        if (userRepository.existsByEmailAndIdNot(command.email(), command.id())) {
            throw new EmailAlreadyExistsException(command.email());
        }
        if (userRepository.existsByLoginAndIdNot(command.login(), command.id())) {
            throw new LoginAlreadyExistsException(command.login());
        }

        UserType userType = userTypeRepository.findById(command.userTypeId())
                .orElseThrow(() -> new InvalidUserTypeReferenceException(command.userTypeId()));

        user.atualizarDados(command.nome(), command.email(), command.login(), command.endereco(), command.userTypeId());

        if (command.senha() != null && !command.senha().isBlank()) {
            user.alterarSenha(passwordEncoder.encode(command.senha()));
        }

        User saved = userRepository.save(user);
        return UserResult.from(saved, UserTypeResult.from(userType));
    }
}
