package com.orchestrate.api.invitation;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Extends {@link JpaRepository} (not plain {@code CrudRepository}, unlike the auth token
 * repositories) because {@link InvitationService#createInvitation} needs {@code saveAndFlush} to
 * force a superseded invitation's revoke-UPDATE to hit the DB before the reissued invitation's
 * INSERT is attempted — see that method for why.
 */
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

  Optional<Invitation> findByOrganizationIdAndInvitedEmailAndUsedAtIsNullAndRevokedAtIsNull(
      UUID organizationId, String invitedEmail);

  List<Invitation> findByOrganizationIdAndUsedAtIsNullAndRevokedAtIsNull(UUID organizationId);

  /**
   * Pessimistic write-lock variants used by {@link InvitationService#acceptInvitation} and {@link
   * InvitationService#revokeInvitation} immediately before their {@code isUsable()}/{@code
   * isPending()} checks. Closes a TOCTOU race: without a lock, a concurrent accept and revoke on
   * the same row can each do a blind full-column UPDATE from a stale in-memory read, silently
   * overwriting the other's {@code used_at}/{@code revoked_at} write. Same pattern as {@code
   * OrganizationMembershipRepository.findByOrganizationIdAndRole}'s lock for the Phase 1c
   * last-owner race — the second transaction blocks on this {@code SELECT ... FOR UPDATE} until the
   * first commits, then re-reads the post-commit row state before deciding.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from Invitation i where i.id = :id")
  Optional<Invitation> findByIdForUpdate(@Param("id") UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from Invitation i where i.tokenHash = :tokenHash")
  Optional<Invitation> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
