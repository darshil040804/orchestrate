package com.orchestrate.api.auth.token;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Commits the reuse-detection revocation in its OWN transaction. This is a separate bean (not a
 * self-invoked method) so the {@code REQUIRES_NEW} proxy actually applies: the revoke must persist
 * even though the caller immediately throws {@link
 * com.orchestrate.api.error.InvalidTokenException}, which would otherwise roll back a shared
 * transaction and undo the revoke.
 */
@Component
public class RefreshTokenReuseHandler {

  private final RefreshTokenRepository repository;

  public RefreshTokenReuseHandler(RefreshTokenRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void revokeAllForUser(UUID userId) {
    repository.revokeAllActiveForUser(userId, Instant.now());
  }
}
