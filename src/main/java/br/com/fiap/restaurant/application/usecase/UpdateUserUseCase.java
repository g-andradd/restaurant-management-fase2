package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.UpdateUserCommand;
import br.com.fiap.restaurant.application.dto.UserResult;
import br.com.fiap.restaurant.application.port.PasswordEncoder;
import br.com.fiap.restaurant.domain.exception.EmailAlreadyExistsException;
import br.com.fiap.restaurant.domain.exception.LoginAlreadyExistsException;
import br.com.fiap.restaurant.domain.exception.UserNotFoundException;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.repository.UserRepository;

public class UpdateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UpdateUserUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
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

        user.atualizarDados(command.nome(), command.email(), command.login(), command.endereco());

        if (command.senha() != null && !command.senha().isBlank()) {
            user.alterarSenha(passwordEncoder.encode(command.senha()));
        }

        User saved = userRepository.save(user);
        return UserResult.from(saved);
    }
}
