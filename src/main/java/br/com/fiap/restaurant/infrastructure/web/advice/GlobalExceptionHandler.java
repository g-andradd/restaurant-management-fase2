package br.com.fiap.restaurant.infrastructure.web.advice;

import br.com.fiap.restaurant.application.exception.InvalidCredentialsException;
import br.com.fiap.restaurant.application.exception.NotRestaurantOwnerException;
import br.com.fiap.restaurant.domain.exception.DomainException;
import br.com.fiap.restaurant.domain.exception.DomainValidationException;
import br.com.fiap.restaurant.domain.exception.EmailAlreadyExistsException;
import br.com.fiap.restaurant.domain.exception.InvalidUserReferenceException;
import br.com.fiap.restaurant.domain.exception.InvalidUserTypeReferenceException;
import br.com.fiap.restaurant.domain.exception.LoginAlreadyExistsException;
import br.com.fiap.restaurant.domain.exception.MenuItemNotFoundException;
import br.com.fiap.restaurant.domain.exception.RestaurantNotFoundException;
import br.com.fiap.restaurant.domain.exception.UserCannotOwnRestaurantException;
import br.com.fiap.restaurant.domain.exception.UserNotFoundException;
import br.com.fiap.restaurant.domain.exception.UserOwnsRestaurantsException;
import br.com.fiap.restaurant.domain.exception.UserTypeInUseException;
import br.com.fiap.restaurant.domain.exception.UserTypeNameAlreadyExistsException;
import br.com.fiap.restaurant.domain.exception.UserTypeNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
    private static final URI TYPE_FORBIDDEN = URI.create("urn:problem-type:forbidden");
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex) {
        // Thrown by Jackson during @RequestBody deserialization - e.g. an
        // unknown enum literal for tipoCozinha - before the controller
        // method or Bean Validation ever runs. Without this handler it
        // falls through to the generic 500 fallback below.
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body");
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(TYPE_VALIDATION_ERROR);
        return problemDetail;
    }

    @ExceptionHandler({UserNotFoundException.class, UserTypeNotFoundException.class, RestaurantNotFoundException.class,
            MenuItemNotFoundException.class})
    public ProblemDetail handleNotFound(DomainException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(TYPE_NOT_FOUND);
        return problemDetail;
    }

    @ExceptionHandler({EmailAlreadyExistsException.class, LoginAlreadyExistsException.class,
            UserTypeNameAlreadyExistsException.class, UserTypeInUseException.class, UserOwnsRestaurantsException.class})
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

    @ExceptionHandler(NotRestaurantOwnerException.class)
    public ProblemDetail handleNotRestaurantOwner(NotRestaurantOwnerException ex) {
        // The first use case (from a normal @RestController method) to ever
        // emit 403 - distinct from ProblemDetailAccessDeniedHandler, which
        // only handles Spring Security's own AccessDeniedException thrown
        // inside the filter chain (nothing throws that one today, since
        // there's no @PreAuthorize/role-based rule anywhere).
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problemDetail.setTitle("Forbidden");
        problemDetail.setType(TYPE_FORBIDDEN);
        return problemDetail;
    }

    @ExceptionHandler(DomainValidationException.class)
    public ProblemDetail handleDomainValidation(DomainValidationException ex) {
        // A domain invariant was violated (blank required field, preco <= 0,
        // abertura not before fechamento, etc.) - the request itself is bad,
        // so 400. Deliberately its own type/handler rather than a generic
        // IllegalArgumentException handler: IllegalArgumentException is also
        // thrown by library code and genuine bugs, and mapping THAT to 400
        // globally would tell the client "your request is bad" for a server
        // problem. Only DomainValidationException means "bad request" here.
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(TYPE_VALIDATION_ERROR);
        return problemDetail;
    }

    @ExceptionHandler({InvalidUserTypeReferenceException.class, InvalidUserReferenceException.class})
    public ProblemDetail handleInvalidReference(DomainException ex) {
        // Deliberately its own handler, not the generic DomainException fallback
        // below: a non-existent reference (UserType or User) inside a request
        // body is 422 (the request is well-formed but semantically
        // unprocessable), distinct from the *NotFoundException family's 404
        // (the URL's own target is missing). Keeping this explicit means a
        // future handler reordering/addition can't silently change this status
        // without breaking a test.
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problemDetail.setTitle("Invalid Reference");
        problemDetail.setType(TYPE_INVALID_REFERENCE);
        return problemDetail;
    }

    @ExceptionHandler(UserCannotOwnRestaurantException.class)
    public ProblemDetail handleUserCannotOwnRestaurant(UserCannotOwnRestaurantException ex) {
        // Own dedicated handler rather than the generic DomainException
        // fallback: this status is explicitly required by the M04 status
        // contract, so it must not depend on a catch-all that a future
        // handler addition could silently reorder around.
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problemDetail.setTitle("Business Rule Violation");
        problemDetail.setType(TYPE_BUSINESS_RULE_VIOLATION);
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
