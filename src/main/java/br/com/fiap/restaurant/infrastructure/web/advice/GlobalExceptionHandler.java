package br.com.fiap.restaurant.infrastructure.web.advice;

import br.com.fiap.restaurant.application.exception.InvalidCredentialsException;
import br.com.fiap.restaurant.domain.exception.DomainException;
import br.com.fiap.restaurant.domain.exception.EmailAlreadyExistsException;
import br.com.fiap.restaurant.domain.exception.InvalidUserTypeReferenceException;
import br.com.fiap.restaurant.domain.exception.LoginAlreadyExistsException;
import br.com.fiap.restaurant.domain.exception.UserNotFoundException;
import br.com.fiap.restaurant.domain.exception.UserTypeInUseException;
import br.com.fiap.restaurant.domain.exception.UserTypeNameAlreadyExistsException;
import br.com.fiap.restaurant.domain.exception.UserTypeNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final URI TYPE_VALIDATION_ERROR = URI.create("urn:problem-type:validation-error");
    private static final URI TYPE_NOT_FOUND = URI.create("urn:problem-type:not-found");
    private static final URI TYPE_CONFLICT = URI.create("urn:problem-type:conflict");
    private static final URI TYPE_BUSINESS_RULE_VIOLATION = URI.create("urn:problem-type:business-rule-violation");
    private static final URI TYPE_UNAUTHORIZED = URI.create("urn:problem-type:unauthorized");
    private static final URI TYPE_INVALID_REFERENCE = URI.create("urn:problem-type:invalid-reference");
    private static final URI TYPE_INTERNAL_ERROR = URI.create("urn:problem-type:internal-error");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(TYPE_VALIDATION_ERROR);

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(TYPE_NOT_FOUND);
        return problemDetail;
    }

    @ExceptionHandler(UserTypeNotFoundException.class)
    public ProblemDetail handleUserTypeNotFound(UserTypeNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(TYPE_NOT_FOUND);
        return problemDetail;
    }

    @ExceptionHandler({EmailAlreadyExistsException.class, LoginAlreadyExistsException.class,
            UserTypeNameAlreadyExistsException.class, UserTypeInUseException.class})
    public ProblemDetail handleConflict(DomainException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Conflict");
        problemDetail.setType(TYPE_CONFLICT);
        return problemDetail;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problemDetail.setTitle("Unauthorized");
        problemDetail.setType(TYPE_UNAUTHORIZED);
        return problemDetail;
    }

    @ExceptionHandler(InvalidUserTypeReferenceException.class)
    public ProblemDetail handleInvalidUserTypeReference(InvalidUserTypeReferenceException ex) {
        // Deliberately its own handler, not the generic DomainException fallback
        // below: a non-existent UserType referenced from a request body is a
        // 422 (the request is well-formed but semantically unprocessable),
        // distinct from UserTypeNotFoundException's 404 (the URL's own
        // target is missing). Keeping this explicit means a future handler
        // reordering/addition can't silently change this status without
        // breaking a test - see the 404-vs-422 assertions in
        // UserControllerTest and UserTypeIntegrationTest.
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problemDetail.setTitle("Invalid Reference");
        problemDetail.setType(TYPE_INVALID_REFERENCE);
        return problemDetail;
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problemDetail.setTitle("Business Rule Violation");
        problemDetail.setType(TYPE_BUSINESS_RULE_VIOLATION);
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(TYPE_INTERNAL_ERROR);
        return problemDetail;
    }
}
