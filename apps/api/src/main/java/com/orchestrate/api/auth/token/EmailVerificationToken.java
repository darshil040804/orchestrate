package com.orchestrate.api.auth.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {

  @Id @UuidGenerator private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "token_hash", nullable = false, unique = true)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected EmailVerificationToken() {}

  public EmailVerificationToken(UUID userId, String tokenHash, Instant expiresAt) {
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  public boolean isUsable(Instant now) {
    return usedAt == null && now.isBefore(expiresAt);
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUsedAt(Instant usedAt) {
    this.usedAt = usedAt;
  }
}
