package com.orchestrate.api.invitation;

import com.orchestrate.api.auth.EmailLinkLogger;
import com.orchestrate.api.auth.token.TokenHasher;
import com.orchestrate.api.config.AppProperties;
import com.orchestrate.api.error.InvalidTokenException;
import com.orchestrate.api.error.InvitationEmailMismatchException;
import com.orchestrate.api.error.InvitationNotFoundException;
import com.orchestrate.api.error.MembershipAlreadyExistsException;
import com.orchestrate.api.error.MembershipNotFoundException;
import com.orchestrate.api.error.OrganizationNotFoundException;
import com.orchestrate.api.error.OwnerActionRequiredException;
import com.orchestrate.api.org.Organization;
import com.orchestrate.api.org.OrganizationMembership;
import com.orchestrate.api.org.OrganizationMembershipRepository;
import com.orchestrate.api.org.OrganizationRepository;
import com.orchestrate.api.org.OrganizationRole;
import com.orchestrate.api.user.User;
import com.orchestrate.api.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvitationService {

  private final InvitationRepository invitations;
  private final OrganizationMembershipRepository memberships;
  private final OrganizationRepository organizations;
  private final UserRepository users;
  private final TokenHasher tokenHasher;
  private final EmailLinkLogger emailLinkLogger;
  private final AppProperties props;

  public InvitationService(
      InvitationRepository invitations,
      OrganizationMembershipRepository memberships,
      OrganizationRepository organizations,
      UserRepository users,
      TokenHasher tokenHasher,
      EmailLinkLogger emailLinkLogger,
      AppProperties props) {
    this.invitations = invitations;
    this.memberships = memberships;
    this.organizations = organizations;
    this.users = users;
    this.tokenHasher = tokenHasher;
    this.emailLinkLogger = emailLinkLogger;
    this.props = props;
  }

  /**
   * Matrix: same shape as {@code OrgService.addMember} — an invitation has no existing target role
   * to consider, so inviting as ADMIN/OWNER requires the acting user to be OWNER.
   *
   * <p>Reissue: an existing active pending invitation for the same (org, email) is revoked before
   * the new one is inserted — at most one active pending invitation per (org, email) at a time,
   * backed by a partial unique index (see V3__invitations.sql).
   */
  @Transactional
  public void createInvitation(
      UUID orgId, UUID actingUserId, String rawEmail, OrganizationRole role) {
    OrganizationMembership acting = requireMembership(orgId, actingUserId);
    boolean actingIsOwner = acting.getRole() == OrganizationRole.OWNER;
    boolean touchesOwnerTier = role == OrganizationRole.ADMIN || role == OrganizationRole.OWNER;
    if (touchesOwnerTier && !actingIsOwner) {
      throw new OwnerActionRequiredException();
    }

    String email = normalize(rawEmail);
    users
        .findByEmail(email)
        .ifPresent(
            existingUser -> {
              if (memberships.existsByOrganizationIdAndUserId(orgId, existingUser.getId())) {
                throw new MembershipAlreadyExistsException();
              }
            });

    Organization org =
        organizations.findById(orgId).orElseThrow(() -> new OrganizationNotFoundException(orgId));

    // Reissue: must flush the revoke before inserting the new row. Hibernate's default flush
    // order runs all pending INSERTs before UPDATEs in the same flush regardless of call order,
    // so a plain save() here (instead of saveAndFlush()) would attempt the new row's INSERT while
    // this row still shows revoked_at IS NULL in the DB, colliding with the partial unique index.
    invitations
        .findByOrganizationIdAndInvitedEmailAndUsedAtIsNullAndRevokedAtIsNull(orgId, email)
        .ifPresent(
            existing -> {
              existing.setRevokedAt(Instant.now());
              invitations.saveAndFlush(existing);
            });

    String raw = tokenHasher.generateRawToken();
    invitations.save(
        new Invitation(
            orgId,
            email,
            role,
            tokenHasher.hash(raw),
            actingUserId,
            Instant.now().plus(props.tokens().orgInvitationTtl())));
    emailLinkLogger.sendOrgInvitationLink(email, org.getName(), raw);
  }

  /**
   * Shows invitations regardless of wall-clock expiry (expiry is surfaced as a computed flag on
   * {@code InvitationResponse}, not filtered out) — an admin needs to see an expired-but-unresolved
   * invite to decide whether to reissue it.
   */
  @Transactional(readOnly = true)
  public List<InvitationEntry> listPending(UUID orgId) {
    List<Invitation> pending =
        invitations.findByOrganizationIdAndUsedAtIsNullAndRevokedAtIsNull(orgId);
    Map<UUID, User> usersById =
        users.findAllById(pending.stream().map(Invitation::getInvitedByUserId).toList()).stream()
            .collect(Collectors.toMap(User::getId, u -> u));
    return pending.stream()
        .map(
            invitation ->
                new InvitationEntry(invitation, usersById.get(invitation.getInvitedByUserId())))
        .toList();
  }

  /**
   * Matrix mirrors creation, but against the invitation's stored (invited) role rather than a
   * current membership role — an invitation hasn't become a membership yet, so the invited role is
   * the closest analog to "same matrix gating as removal."
   *
   * <p>Uses the pessimistic-write-lock finder (not plain {@code findById}) so a concurrent {@link
   * #acceptInvitation} on the same row blocks here until this transaction commits, then re-reads
   * the post-commit state instead of racing against a stale in-memory read.
   */
  @Transactional
  public void revokeInvitation(UUID orgId, UUID actingUserId, UUID invitationId) {
    OrganizationMembership acting = requireMembership(orgId, actingUserId);
    Invitation invitation =
        invitations
            .findByIdForUpdate(invitationId)
            .filter(i -> i.getOrganizationId().equals(orgId))
            .orElseThrow(InvitationNotFoundException::new);

    boolean actingIsOwner = acting.getRole() == OrganizationRole.OWNER;
    boolean touchesOwnerTier =
        invitation.getRole() == OrganizationRole.ADMIN
            || invitation.getRole() == OrganizationRole.OWNER;
    if (touchesOwnerTier && !actingIsOwner) {
      throw new OwnerActionRequiredException();
    }

    if (!invitation.isPending()) {
      throw new InvitationNotFoundException();
    }

    invitation.setRevokedAt(Instant.now());
    invitations.save(invitation);
  }

  /**
   * Uses the pessimistic-write-lock finder (not plain {@code findByTokenHash}) so a concurrent
   * {@link #revokeInvitation} on the same row blocks here until this transaction commits, then
   * re-reads the post-commit state instead of racing against a stale in-memory read — see {@link
   * InvitationRepository#findByTokenHashForUpdate}.
   */
  @Transactional
  public void acceptInvitation(UUID acceptingUserId, String acceptingUserEmail, String rawToken) {
    Invitation invitation =
        invitations
            .findByTokenHashForUpdate(tokenHasher.hash(rawToken))
            .orElseThrow(() -> new InvalidTokenException("Unknown invitation token"));
    if (!invitation.isUsable(Instant.now())) {
      throw new InvalidTokenException("Invitation is expired, already used, or revoked");
    }
    if (!normalize(acceptingUserEmail).equals(invitation.getInvitedEmail())) {
      throw new InvitationEmailMismatchException();
    }
    if (memberships.existsByOrganizationIdAndUserId(
        invitation.getOrganizationId(), acceptingUserId)) {
      throw new MembershipAlreadyExistsException();
    }

    memberships.save(
        new OrganizationMembership(
            invitation.getOrganizationId(), acceptingUserId, invitation.getRole()));
    invitation.setUsedAt(Instant.now());
    invitations.save(invitation);
  }

  private OrganizationMembership requireMembership(UUID orgId, UUID userId) {
    return memberships
        .findByOrganizationIdAndUserId(orgId, userId)
        .orElseThrow(MembershipNotFoundException::new);
  }

  private String normalize(String email) {
    return email.trim().toLowerCase();
  }

  public record InvitationEntry(Invitation invitation, User invitedBy) {}
}
