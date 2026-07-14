package com.orchestrate.api.invitation;

import com.orchestrate.api.org.OrganizationRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "org_invitations")
public class Invitation {

  @Id @UuidGenerator private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "invited_email", nullable = false)
  private String invitedEmail;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrganizationRole role;

  @Column(name = "token_hash", nullable = false, unique = true)
  private String tokenHash;

  @Column(name = "invited_by_user_id", nullable = false)
  private UUID invitedByUserId;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Invitation() {}

  public Invitation(
      UUID organizationId,
      String invitedEmail,
      OrganizationRole role,
      String tokenHash,
      UUID invitedByUserId,
      Instant expiresAt) {
    this.organizationId = organizationId;
    this.invitedEmail = invitedEmail;
    this.role = role;
    this.tokenHash = tokenHash;
    this.invitedByUserId = invitedByUserId;
    this.expiresAt = expiresAt;
  }

  /** Accept-time check: expiry matters here. */
  public boolean isUsable(Instant now) {
    return usedAt == null && revokedAt == null && now.isBefore(expiresAt);
  }

  /**
   * Revoke-eligibility / listing filter. Deliberately expiry-agnostic — an admin can see and revoke
   * an expired-but-unresolved invite (helps decide whether to reissue), and revocation is a state
   * transition (used vs. revoked), not a wall-clock check.
   */
  public boolean isPending() {
    return usedAt == null && revokedAt == null;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getInvitedEmail() {
    return invitedEmail;
  }

  public OrganizationRole getRole() {
    return role;
  }

  public UUID getInvitedByUserId() {
    return invitedByUserId;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setUsedAt(Instant usedAt) {
    this.usedAt = usedAt;
  }

  public void setRevokedAt(Instant revokedAt) {
    this.revokedAt = revokedAt;
  }
}
