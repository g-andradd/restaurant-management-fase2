package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.PageQuery;
import br.com.fiap.restaurant.application.dto.PageResult;
import br.com.fiap.restaurant.application.dto.UserResult;
import br.com.fiap.restaurant.domain.repository.UserRepository;

import java.util.List;

public class ListUsersUseCase {

    private final UserRepository userRepository;

    public ListUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public PageResult<UserResult> execute(PageQuery query) {
        List<UserResult> content = userRepository.findAll(query.page(), query.size()).stream()
                .map(UserResult::from)
                .toList();
        long totalElements = userRepository.count();
        return PageResult.of(content, query.page(), query.size(), totalElements);
    }
}
