package com.orchestrate.api.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
    return build(
        HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission to perform this action");
  }

  @ExceptionHandler(SlugAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleSlugExists(SlugAlreadyExistsException ex) {
    return build(HttpStatus.CONFLICT, "SLUG_ALREADY_EXISTS", ex.getMessage());
  }

  @ExceptionHandler(OrganizationNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleOrgNotFound(OrganizationNotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, "ORGANIZATION_NOT_FOUND", ex.getMessage());
  }

  @ExceptionHandler(MembershipNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleMembershipNotFound(MembershipNotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, "MEMBERSHIP_NOT_FOUND", ex.getMessage());
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", ex.getMessage());
  }

  @ExceptionHandler(MembershipAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleMembershipAlreadyExists(
      MembershipAlreadyExistsException ex) {
    return build(HttpStatus.CONFLICT, "MEMBERSHIP_ALREADY_EXISTS", ex.getMessage());
  }

  @ExceptionHandler(LastOwnerException.class)
  public ResponseEntity<ErrorResponse> handleLastOwner(LastOwnerException ex) {
    return build(HttpStatus.FORBIDDEN, "LAST_OWNER_REQUIRED", ex.getMessage());
  }

  @ExceptionHandler(OwnerActionRequiredException.class)
  public ResponseEntity<ErrorResponse> handleOwnerActionRequired(OwnerActionRequiredException ex) {
    return build(HttpStatus.FORBIDDEN, "OWNER_ACTION_REQUIRED", ex.getMessage());
  }

  private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message) {
    return ResponseEntity.status(status).body(new ErrorResponse(code, message));
  }
}
