package br.com.fiap.restaurant.application.usecase;

import br.com.fiap.restaurant.application.dto.PageQuery;
import br.com.fiap.restaurant.application.dto.PageResult;
import br.com.fiap.restaurant.application.dto.UserTypeResult;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;

import java.util.List;

public class ListUserTypesUseCase {

    private final UserTypeRepository userTypeRepository;

    public ListUserTypesUseCase(UserTypeRepository userTypeRepository) {
        this.userTypeRepository = userTypeRepository;
    }

    public PageResult<UserTypeResult> execute(PageQuery query) {
        List<UserTypeResult> content = userTypeRepository.findAll(query.page(), query.size()).stream()
                .map(UserTypeResult::from)
                .toList();
        long totalElements = userTypeRepository.count();
        return PageResult.of(content, query.page(), query.size(), totalElements);
    }
}
