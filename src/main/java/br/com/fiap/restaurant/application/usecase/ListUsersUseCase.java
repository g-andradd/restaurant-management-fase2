package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.PageQuery;
import br.com.fiap.restaurant.application.dto.PageResult;
import br.com.fiap.restaurant.application.dto.UserResult;
import br.com.fiap.restaurant.application.dto.UserTypeResult;
import br.com.fiap.restaurant.domain.model.User;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ListUsersUseCase {

    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;

    public ListUsersUseCase(UserRepository userRepository, UserTypeRepository userTypeRepository) {
        this.userRepository = userRepository;
        this.userTypeRepository = userTypeRepository;
    }

    public PageResult<UserResult> execute(PageQuery query) {
        List<User> users = userRepository.findAll(query.page(), query.size());

        List<UUID> distinctUserTypeIds = users.stream().map(User::getUserTypeId).distinct().toList();
        Map<UUID, UserTypeResult> userTypesById = userTypeRepository.findAllById(distinctUserTypeIds).stream()
                .map(UserTypeResult::from)
                .collect(Collectors.toMap(UserTypeResult::id, Function.identity()));

        List<UserResult> content = users.stream()
                .map(user -> UserResult.from(user, userTypesById.get(user.getUserTypeId())))
                .toList();

        long totalElements = userRepository.count();
        return PageResult.of(content, query.page(), query.size(), totalElements);
    }
}
