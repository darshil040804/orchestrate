package com.orchestrate.api.auth.token;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, UUID> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  List<RefreshToken> findByUserId(UUID userId);

  /**
   * Revoke every still-active refresh token for a user (reuse-detection / password-reset response).
   */
  @Modifying
  @Query(
      "UPDATE RefreshToken t SET t.revokedAt = :now"
          + " WHERE t.userId = :userId AND t.revokedAt IS NULL")
  void revokeAllActiveForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
