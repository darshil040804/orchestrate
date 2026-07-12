package com.orchestrate.api.auth;

import com.orchestrate.api.auth.AuthService.IssuedTokens;
import com.orchestrate.api.auth.dto.LoginRequest;
import com.orchestrate.api.auth.dto.MessageResponse;
import com.orchestrate.api.auth.dto.PasswordResetConfirm;
import com.orchestrate.api.auth.dto.PasswordResetRequest;
import com.orchestrate.api.auth.dto.SignupRequest;
import com.orchestrate.api.auth.dto.UserResponse;
import com.orchestrate.api.auth.jwt.JwtService.AccessTokenPrincipal;
import com.orchestrate.api.error.InvalidTokenException;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final CookieFactory cookieFactory;

  public AuthController(AuthService authService, CookieFactory cookieFactory) {
    this.authService = authService;
    this.cookieFactory = cookieFactory;
  }

  @PostMapping("/signup")
  public ResponseEntity<MessageResponse> signup(@Valid @RequestBody SignupRequest request) {
    authService.signup(request.email(), request.password());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new MessageResponse("Account created. Check your email to verify before logging in."));
  }

  @GetMapping("/verify-email")
  public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
    authService.verifyEmail(token);
    return ResponseEntity.ok(new MessageResponse("Email verified. You can now log in."));
  }

  @PostMapping("/login")
  public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
    IssuedTokens tokens = authService.login(request.email(), request.password());
    return withAuthCookies(tokens).body(UserResponse.from(tokens.user()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<UserResponse> refresh(
      @CookieValue(name = CookieFactory.REFRESH_TOKEN_COOKIE, required = false)
          String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new InvalidTokenException("Missing refresh token");
    }
    IssuedTokens tokens = authService.refresh(refreshToken);
    return withAuthCookies(tokens).body(UserResponse.from(tokens.user()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(name = CookieFactory.REFRESH_TOKEN_COOKIE, required = false)
          String refreshToken) {
    authService.logout(refreshToken);
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookieFactory.clearAccessCookie().toString())
        .header(HttpHeaders.SET_COOKIE, cookieFactory.clearRefreshCookie().toString())
        .build();
  }

  @PostMapping("/password-reset/request")
  public ResponseEntity<MessageResponse> requestPasswordReset(
      @Valid @RequestBody PasswordResetRequest request) {
    authService.requestPasswordReset(request.email());
    return ResponseEntity.ok(
        new MessageResponse("If an account exists for that email, a reset link has been sent."));
  }

  @PostMapping("/password-reset/confirm")
  public ResponseEntity<MessageResponse> confirmPasswordReset(
      @Valid @RequestBody PasswordResetConfirm request) {
    authService.confirmPasswordReset(request.token(), request.newPassword());
    return ResponseEntity.ok(new MessageResponse("Password updated. Please log in again."));
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AccessTokenPrincipal principal) {
    return ResponseEntity.ok(UserResponse.from(authService.requireUser(principal.userId())));
  }

  private ResponseEntity.BodyBuilder withAuthCookies(IssuedTokens tokens) {
    ResponseCookie access = cookieFactory.accessCookie(tokens.accessToken());
    ResponseCookie refresh = cookieFactory.refreshCookie(tokens.refreshToken());
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, access.toString())
        .header(HttpHeaders.SET_COOKIE, refresh.toString());
  }
}
