package br.com.fiap.restaurant.infrastructure.config;

import br.com.fiap.restaurant.application.port.AuthenticatedUserProvider;
import br.com.fiap.restaurant.application.port.PasswordEncoder;
import br.com.fiap.restaurant.application.port.TokenProvider;
import br.com.fiap.restaurant.application.port.TransactionRunner;
import br.com.fiap.restaurant.application.usecase.AuthenticateUserUseCase;
import br.com.fiap.restaurant.application.usecase.CreateMenuItemUseCase;
import br.com.fiap.restaurant.application.usecase.CreateRestaurantUseCase;
import br.com.fiap.restaurant.application.usecase.CreateUserTypeUseCase;
import br.com.fiap.restaurant.application.usecase.CreateUserUseCase;
import br.com.fiap.restaurant.application.usecase.DeleteMenuItemUseCase;
import br.com.fiap.restaurant.application.usecase.DeleteRestaurantUseCase;
import br.com.fiap.restaurant.application.usecase.DeleteUserTypeUseCase;
import br.com.fiap.restaurant.application.usecase.DeleteUserUseCase;
import br.com.fiap.restaurant.application.usecase.GetMenuItemByIdUseCase;
import br.com.fiap.restaurant.application.usecase.GetRestaurantByIdUseCase;
import br.com.fiap.restaurant.application.usecase.GetUserByIdUseCase;
import br.com.fiap.restaurant.application.usecase.GetUserTypeByIdUseCase;
import br.com.fiap.restaurant.application.usecase.ListMenuItemsUseCase;
import br.com.fiap.restaurant.application.usecase.ListRestaurantsUseCase;
import br.com.fiap.restaurant.application.usecase.ListUserTypesUseCase;
import br.com.fiap.restaurant.application.usecase.ListUsersUseCase;
import br.com.fiap.restaurant.application.usecase.UpdateMenuItemUseCase;
import br.com.fiap.restaurant.application.usecase.UpdateRestaurantUseCase;
import br.com.fiap.restaurant.application.usecase.UpdateUserTypeUseCase;
import br.com.fiap.restaurant.application.usecase.UpdateUserUseCase;
import br.com.fiap.restaurant.domain.repository.MenuItemRepository;
import br.com.fiap.restaurant.domain.repository.RestaurantRepository;
import br.com.fiap.restaurant.domain.repository.UserRepository;
import br.com.fiap.restaurant.domain.repository.UserTypeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires plain, framework-free application-layer use cases as beans. Use cases
 * themselves carry no Spring annotations so the application layer stays free
 * of framework dependencies; this class is the only place that knows they're
 * Spring beans.
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public CreateUserUseCase createUserUseCase(UserRepository userRepository, UserTypeRepository userTypeRepository,
                                                PasswordEncoder passwordEncoder) {
        return new CreateUserUseCase(userRepository, userTypeRepository, passwordEncoder);
    }

    @Bean
    public GetUserByIdUseCase getUserByIdUseCase(UserRepository userRepository, UserTypeRepository userTypeRepository) {
        return new GetUserByIdUseCase(userRepository, userTypeRepository);
    }

    @Bean
    public ListUsersUseCase listUsersUseCase(UserRepository userRepository, UserTypeRepository userTypeRepository) {
        return new ListUsersUseCase(userRepository, userTypeRepository);
    }

    @Bean
    public UpdateUserUseCase updateUserUseCase(UserRepository userRepository, UserTypeRepository userTypeRepository,
                                                PasswordEncoder passwordEncoder) {
        return new UpdateUserUseCase(userRepository, userTypeRepository, passwordEncoder);
    }

    @Bean
    public DeleteUserUseCase deleteUserUseCase(UserRepository userRepository, RestaurantRepository restaurantRepository) {
        return new DeleteUserUseCase(userRepository, restaurantRepository);
    }

    @Bean
    public AuthenticateUserUseCase authenticateUserUseCase(UserRepository userRepository,
                                                             UserTypeRepository userTypeRepository,
                                                             PasswordEncoder passwordEncoder,
                                                             TokenProvider tokenProvider) {
        return new AuthenticateUserUseCase(userRepository, userTypeRepository, passwordEncoder, tokenProvider);
    }

    @Bean
    public CreateUserTypeUseCase createUserTypeUseCase(UserTypeRepository userTypeRepository) {
        return new CreateUserTypeUseCase(userTypeRepository);
    }

    @Bean
    public GetUserTypeByIdUseCase getUserTypeByIdUseCase(UserTypeRepository userTypeRepository) {
        return new GetUserTypeByIdUseCase(userTypeRepository);
    }

    @Bean
    public ListUserTypesUseCase listUserTypesUseCase(UserTypeRepository userTypeRepository) {
        return new ListUserTypesUseCase(userTypeRepository);
    }

    @Bean
    public UpdateUserTypeUseCase updateUserTypeUseCase(UserTypeRepository userTypeRepository) {
        return new UpdateUserTypeUseCase(userTypeRepository);
    }

    @Bean
    public DeleteUserTypeUseCase deleteUserTypeUseCase(UserTypeRepository userTypeRepository,
                                                        UserRepository userRepository) {
        return new DeleteUserTypeUseCase(userTypeRepository, userRepository);
    }

    @Bean
    public CreateRestaurantUseCase createRestaurantUseCase(RestaurantRepository restaurantRepository,
                                                             UserRepository userRepository,
                                                             UserTypeRepository userTypeRepository,
                                                             AuthenticatedUserProvider authenticatedUserProvider) {
        return new CreateRestaurantUseCase(restaurantRepository, userRepository, userTypeRepository,
                authenticatedUserProvider);
    }

    @Bean
    public GetRestaurantByIdUseCase getRestaurantByIdUseCase(RestaurantRepository restaurantRepository,
                                                               UserRepository userRepository) {
        return new GetRestaurantByIdUseCase(restaurantRepository, userRepository);
    }

    @Bean
    public ListRestaurantsUseCase listRestaurantsUseCase(RestaurantRepository restaurantRepository,
                                                           UserRepository userRepository) {
        return new ListRestaurantsUseCase(restaurantRepository, userRepository);
    }

    @Bean
    public UpdateRestaurantUseCase updateRestaurantUseCase(RestaurantRepository restaurantRepository,
                                                             UserRepository userRepository,
                                                             AuthenticatedUserProvider authenticatedUserProvider) {
        return new UpdateRestaurantUseCase(restaurantRepository, userRepository, authenticatedUserProvider);
    }

    @Bean
    public DeleteRestaurantUseCase deleteRestaurantUseCase(RestaurantRepository restaurantRepository,
                                                             MenuItemRepository menuItemRepository,
                                                             AuthenticatedUserProvider authenticatedUserProvider,
                                                             TransactionRunner transactionRunner) {
        return new DeleteRestaurantUseCase(restaurantRepository, menuItemRepository, authenticatedUserProvider,
                transactionRunner);
    }

    @Bean
    public CreateMenuItemUseCase createMenuItemUseCase(MenuItemRepository menuItemRepository,
                                                         RestaurantRepository restaurantRepository,
                                                         AuthenticatedUserProvider authenticatedUserProvider) {
        return new CreateMenuItemUseCase(menuItemRepository, restaurantRepository, authenticatedUserProvider);
    }

    @Bean
    public GetMenuItemByIdUseCase getMenuItemByIdUseCase(MenuItemRepository menuItemRepository,
                                                           RestaurantRepository restaurantRepository) {
        return new GetMenuItemByIdUseCase(menuItemRepository, restaurantRepository);
    }

    @Bean
    public ListMenuItemsUseCase listMenuItemsUseCase(MenuItemRepository menuItemRepository,
                                                       RestaurantRepository restaurantRepository) {
        return new ListMenuItemsUseCase(menuItemRepository, restaurantRepository);
    }

    @Bean
    public UpdateMenuItemUseCase updateMenuItemUseCase(MenuItemRepository menuItemRepository,
                                                         RestaurantRepository restaurantRepository,
                                                         AuthenticatedUserProvider authenticatedUserProvider) {
        return new UpdateMenuItemUseCase(menuItemRepository, restaurantRepository, authenticatedUserProvider);
    }

    @Bean
    public DeleteMenuItemUseCase deleteMenuItemUseCase(MenuItemRepository menuItemRepository,
                                                         RestaurantRepository restaurantRepository,
                                                         AuthenticatedUserProvider authenticatedUserProvider) {
        return new DeleteMenuItemUseCase(menuItemRepository, restaurantRepository, authenticatedUserProvider);
    }
}
