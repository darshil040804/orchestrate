package com.orchestrate.api.auth;

import com.orchestrate.api.auth.jwt.JwtService;
import com.orchestrate.api.auth.token.EmailVerificationToken;
import com.orchestrate.api.auth.token.EmailVerificationTokenRepository;
import com.orchestrate.api.auth.token.PasswordResetToken;
import com.orchestrate.api.auth.token.PasswordResetTokenRepository;
import com.orchestrate.api.auth.token.RefreshToken;
import com.orchestrate.api.auth.token.RefreshTokenService;
import com.orchestrate.api.auth.token.TokenHasher;
import com.orchestrate.api.config.AppProperties;
import com.orchestrate.api.error.EmailAlreadyExistsException;
import com.orchestrate.api.error.EmailNotVerifiedException;
import com.orchestrate.api.error.InvalidTokenException;
import com.orchestrate.api.user.User;
import com.orchestrate.api.user.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orchestrates the email/password auth flows. Custom code is limited to our own business logic; */
@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final EmailVerificationTokenRepository verificationTokens;
  private final PasswordResetTokenRepository resetTokens;
  private final TokenHasher tokenHasher;
  private final EmailLinkLogger emailLinkLogger;
  private final AppProperties props;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager,
      JwtService jwtService,
      RefreshTokenService refreshTokenService,
      EmailVerificationTokenRepository verificationTokens,
      PasswordResetTokenRepository resetTokens,
      TokenHasher tokenHasher,
      EmailLinkLogger emailLinkLogger,
      AppProperties props) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
    this.verificationTokens = verificationTokens;
    this.resetTokens = resetTokens;
    this.tokenHasher = tokenHasher;
    this.emailLinkLogger = emailLinkLogger;
    this.props = props;
  }

  @Transactional
  public void signup(String rawEmail, String rawPassword) {
    String email = normalize(rawEmail);
    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyExistsException();
    }
    User user = userRepository.save(new User(email, passwordEncoder.encode(rawPassword)));
    issueVerificationToken(user);
  }

  @Transactional
  public void verifyEmail(String rawToken) {
    EmailVerificationToken token =
        verificationTokens
            .findByTokenHash(tokenHasher.hash(rawToken))
            .orElseThrow(() -> new InvalidTokenException("Unknown verification token"));
    if (!token.isUsable(Instant.now())) {
      throw new InvalidTokenException("Verification token is expired or already used");
    }
    User user =
        userRepository
            .findById(token.getUserId())
            .orElseThrow(() -> new InvalidTokenException("Unknown verification token"));
    user.setEmailVerified(true);
    token.setUsedAt(Instant.now());
    userRepository.save(user);
    verificationTokens.save(token);
  }

  /**
   * Authenticate credentials via Spring Security, then require a verified email before issuing
   * tokens.
   */
  @Transactional
  public IssuedTokens login(String rawEmail, String rawPassword) {
    String email = normalize(rawEmail);
    // Throws BadCredentialsException (→ 401) on unknown user or wrong password.
    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, rawPassword));

    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Authenticated user missing: " + email));
    if (!user.isEmailVerified()) {
      throw new EmailNotVerifiedException();
    }
    return issueTokens(user);
  }

  /**
   * Find-or-create a user for an OAuth login whose email the provider has already verified (Google
   * via the OIDC {@code email_verified} claim, GitHub via {@link
   * com.orchestrate.api.auth.oauth.GitHubOAuth2UserService}'s verified-email lookup), then issue
   * tokens via the exact same path as password login.
   *
   * <p>Auto-links by email: if a User already exists for this email — whether it was created via
   * password signup or a different OAuth provider — that existing account is reused. No explicit
   * linking step, no rejection.
   */
  @Transactional
  public OAuthLoginResult loginOrSignupViaOAuth(String rawEmail) {
    String email = normalize(rawEmail);
    User user = userRepository.findByEmail(email).orElse(null);
    boolean newAccount = user == null;
    if (user == null) {
      user = new User(email, null);
      user.setEmailVerified(true);
      user = userRepository.save(user);
    } else if (!user.isEmailVerified()) {
      user.setEmailVerified(true);
      user = userRepository.save(user);
    }
    return new OAuthLoginResult(issueTokens(user), newAccount);
  }

  @Transactional
  public IssuedTokens refresh(String rawRefreshToken) {
    RefreshToken current = refreshTokenService.verifyActive(rawRefreshToken);
    User user =
        userRepository
            .findById(current.getUserId())
            .orElseThrow(() -> new InvalidTokenException("Unknown refresh token"));
    String newRefreshRaw = refreshTokenService.rotate(current);
    String accessToken = jwtService.issueAccessToken(user);
    return new IssuedTokens(accessToken, newRefreshRaw, user);
  }

  @Transactional
  public void logout(String rawRefreshToken) {
    if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
      refreshTokenService.revoke(rawRefreshToken);
    }
  }

  /** Always succeeds from the caller's view — never reveals whether the email exists. */
  @Transactional
  public void requestPasswordReset(String rawEmail) {
    userRepository
        .findByEmail(normalize(rawEmail))
        .ifPresent(
            user -> {
              String raw = tokenHasher.generateRawToken();
              resetTokens.save(
                  new PasswordResetToken(
                      user.getId(),
                      tokenHasher.hash(raw),
                      Instant.now().plus(props.tokens().passwordResetTtl())));
              emailLinkLogger.sendPasswordResetLink(user.getEmail(), raw);
            });
  }

  @Transactional
  public void confirmPasswordReset(String rawToken, String newPassword) {
    PasswordResetToken token =
        resetTokens
            .findByTokenHash(tokenHasher.hash(rawToken))
            .orElseThrow(() -> new InvalidTokenException("Unknown reset token"));
    if (!token.isUsable(Instant.now())) {
      throw new InvalidTokenException("Reset token is expired or already used");
    }
    User user =
        userRepository
            .findById(token.getUserId())
            .orElseThrow(() -> new InvalidTokenException("Unknown reset token"));

    user.setPasswordHash(passwordEncoder.encode(newPassword));
    token.setUsedAt(Instant.now());
    userRepository.save(user);
    resetTokens.save(token);
    // Force re-login everywhere after a password change.
    refreshTokenService.revokeAllForUser(user.getId());
  }

  @Transactional(readOnly = true)
  public User requireUser(UUID id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new IllegalStateException("Authenticated user missing: " + id));
  }

  private IssuedTokens issueTokens(User user) {
    String accessToken = jwtService.issueAccessToken(user);
    String refreshToken = refreshTokenService.issue(user.getId());
    return new IssuedTokens(accessToken, refreshToken, user);
  }

  private void issueVerificationToken(User user) {
    String raw = tokenHasher.generateRawToken();
    verificationTokens.save(
        new EmailVerificationToken(
            user.getId(),
            tokenHasher.hash(raw),
            Instant.now().plus(props.tokens().emailVerificationTtl())));
    emailLinkLogger.sendVerificationLink(user.getEmail(), raw);
  }

  private String normalize(String email) {
    return email.trim().toLowerCase();
  }

  /** Bundle of freshly-issued credentials the controller turns into cookies. */
  public record IssuedTokens(String accessToken, String refreshToken, User user) {}

  /** Result of an OAuth login: the issued tokens, plus whether a new account was just created. */
  public record OAuthLoginResult(IssuedTokens tokens, boolean newAccount) {}
}
