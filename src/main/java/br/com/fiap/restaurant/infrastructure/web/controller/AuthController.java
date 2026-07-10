package br.com.fiap.restaurant.infrastructure.web.controller;

import br.com.fiap.restaurant.application.dto.AuthenticateUserCommand;
import br.com.fiap.restaurant.application.usecase.AuthenticateUserUseCase;
import br.com.fiap.restaurant.infrastructure.web.dto.LoginRequest;
import br.com.fiap.restaurant.infrastructure.web.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;

    public AuthController(AuthenticateUserUseCase authenticateUserUseCase) {
        this.authenticateUserUseCase = authenticateUserUseCase;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with login and password, receive a JWT access token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = authenticateUserUseCase.execute(new AuthenticateUserCommand(request.login(), request.senha()));
        return ResponseEntity.ok(new LoginResponse(result.token(), result.tokenType(), result.expiresInSeconds()));
    }
}
