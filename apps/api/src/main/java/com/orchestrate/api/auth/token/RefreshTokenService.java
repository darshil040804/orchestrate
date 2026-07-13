package com.orchestrate.api.auth.token;

import com.orchestrate.api.config.AppProperties;
import com.orchestrate.api.error.InvalidTokenException;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues, verifies, rotates, and revokes opaque refresh tokens. Rotation is one-time-use: each
 * {@link #rotate} mints a new token and revokes the old one. Presenting an already-revoked token is
 * treated as theft — {@link #verifyActive} revokes every active token for that user.
 */
@Service
public class RefreshTokenService {

  private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

  private final RefreshTokenRepository repository;
  private final RefreshTokenReuseHandler reuseHandler;
  private final TokenHasher hasher;
  private final AppProperties props;

  public RefreshTokenService(
      RefreshTokenRepository repository,
      RefreshTokenReuseHandler reuseHandler,
      TokenHasher hasher,
      AppProperties props) {
    this.repository = repository;
    this.reuseHandler = reuseHandler;
    this.hasher = hasher;
    this.props = props;
  }

  /** Create a new refresh token for a user and return the raw value (to be set as a cookie). */
  @Transactional
  public String issue(UUID userId) {
    String raw = hasher.generateRawToken();
    Instant expiresAt = Instant.now().plus(props.jwt().refreshTtl());
    repository.save(new RefreshToken(userId, hasher.hash(raw), expiresAt));
    return raw;
  }

  /**
   * Look up an active token by its raw value. Throws {@link InvalidTokenException} if unknown,
   * expired, or revoked; a revoked hit triggers reuse-detection (revoke all of the user's tokens).
   *
   * <p>// TODO(security-followup): reuse-detection has a concurrent-replay race under READ
   * COMMITTED isolation — two simultaneous refresh calls on the same token can both read
   * revokedAt==null before either commits, minting two valid children without triggering reuse
   * detection. Needs pessimistic locking (@Lock(PESSIMISTIC_WRITE) on the token lookup) or
   * optimistic locking (@Version + retry-on-conflict) before this is closed against concurrent
   * attackers. Tracked as a separate follow-up, not part of this PR.
   */
  @Transactional
  public RefreshToken verifyActive(String rawToken) {
    RefreshToken token =
        repository
            .findByTokenHash(hasher.hash(rawToken))
            .orElseThrow(() -> new InvalidTokenException("Unknown refresh token"));

    if (token.isRevoked()) {
      log.warn(
          "Refresh token reuse detected for user {} — revoking all sessions", token.getUserId());
      // Commit the revoke in its own transaction: the throw below rolls back THIS transaction,
      // which would otherwise undo the revoke.
      reuseHandler.revokeAllForUser(token.getUserId());
      throw new InvalidTokenException("Refresh token has been revoked");
    }
    if (token.isExpired(Instant.now())) {
      throw new InvalidTokenException("Refresh token has expired");
    }
    return token;
  }

  /**
   * Rotate: mint a replacement token, revoke the current one, and link the lineage.
   *
   * <p>// TODO(security-followup): see the race-condition note on {@link #verifyActive} — this is
   * the write half of that unlocked read-then-write sequence.
   */
  @Transactional
  public String rotate(RefreshToken current) {
    String raw = hasher.generateRawToken();
    Instant now = Instant.now();
    RefreshToken replacement =
        repository.save(
            new RefreshToken(
                current.getUserId(), hasher.hash(raw), now.plus(props.jwt().refreshTtl())));

    current.setRevokedAt(now);
    current.setReplacedById(replacement.getId());
    repository.save(current);
    return raw;
  }

  /** Revoke a single token by raw value if present (logout). Idempotent. */
  @Transactional
  public void revoke(String rawToken) {
    repository
        .findByTokenHash(hasher.hash(rawToken))
        .ifPresent(
            token -> {
              if (!token.isRevoked()) {
                token.setRevokedAt(Instant.now());
                repository.save(token);
              }
            });
  }

  @Transactional
  public void revokeAllForUser(UUID userId) {
    repository.revokeAllActiveForUser(userId, Instant.now());
  }
}
