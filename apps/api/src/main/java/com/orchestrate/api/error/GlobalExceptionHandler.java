package com.orchestrate.api.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .orElse("Validation failed");
    return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
  }

  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleEmailExists(EmailAlreadyExistsException ex) {
    return build(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", ex.getMessage());
  }

  @ExceptionHandler(EmailNotVerifiedException.class)
  public ResponseEntity<ErrorResponse> handleUnverified(EmailNotVerifiedException ex) {
    return build(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", ex.getMessage());
  }

  @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
  public ResponseEntity<ErrorResponse> handleBadCredentials(RuntimeException ex) {
    // Always use a generic message so we don't leak whether the email exists.
    return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
  }

  @ExceptionHandler(InvalidTokenException.class)
  public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
    return build(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", ex.getMessage());
  }

  private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message) {
    return ResponseEntity.status(status).body(new ErrorResponse(code, message));
  }
}
