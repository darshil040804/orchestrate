package com.orchestrate.api.org;

import com.orchestrate.api.error.LastOwnerException;
import com.orchestrate.api.error.MembershipAlreadyExistsException;
import com.orchestrate.api.error.MembershipNotFoundException;
import com.orchestrate.api.error.OrganizationNotFoundException;
import com.orchestrate.api.error.OwnerActionRequiredException;
import com.orchestrate.api.error.SlugAlreadyExistsException;
import com.orchestrate.api.error.UserNotFoundException;
import com.orchestrate.api.user.User;
import com.orchestrate.api.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgService {

  private final OrganizationRepository organizations;
  private final OrganizationMembershipRepository memberships;
  private final UserRepository users;

  public OrgService(
      OrganizationRepository organizations,
      OrganizationMembershipRepository memberships,
      UserRepository users) {
    this.organizations = organizations;
    this.memberships = memberships;
    this.users = users;
  }

  @Transactional
  public Organization createOrganization(UUID creatorUserId, String name, String slug) {
    if (organizations.existsBySlug(slug)) {
      throw new SlugAlreadyExistsException(slug);
    }
    Organization org = organizations.save(new Organization(name, slug));
    memberships.save(
        new OrganizationMembership(org.getId(), creatorUserId, OrganizationRole.OWNER));
    return org;
  }

  @Transactional(readOnly = true)
  public List<OrganizationWithRoleEntry> listOrganizationsForUser(UUID userId) {
    List<OrganizationMembership> mine = memberships.findByUserId(userId);
    if (mine.isEmpty()) {
      return List.of();
    }
    Map<UUID, OrganizationRole> roleByOrgId =
        mine.stream()
            .collect(
                Collectors.toMap(
                    OrganizationMembership::getOrganizationId, OrganizationMembership::getRole));
    return organizations.findAllById(roleByOrgId.keySet()).stream()
        .map(org -> new OrganizationWithRoleEntry(org, roleByOrgId.get(org.getId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public Organization getOrganization(UUID orgId) {
    return organizations
        .findById(orgId)
        .orElseThrow(() -> new OrganizationNotFoundException(orgId));
  }

  @Transactional(readOnly = true)
  public List<MembershipEntry> listMembers(UUID orgId) {
    List<OrganizationMembership> orgMemberships = memberships.findByOrganizationId(orgId);
    Map<UUID, User> usersById =
        users
            .findAllById(orgMemberships.stream().map(OrganizationMembership::getUserId).toList())
            .stream()
            .collect(Collectors.toMap(User::getId, u -> u));
    return orgMemberships.stream()
        .map(m -> new MembershipEntry(m, usersById.get(m.getUserId())))
        .toList();
  }

  /**
   * Matrix: ADMIN may freely add MEMBER/APPROVER-role members. Adding someone as ADMIN or OWNER
   * requires the acting user to be OWNER.
   */
  @Transactional
  public void addMember(UUID orgId, UUID actingUserId, UUID targetUserId, OrganizationRole role) {
    OrganizationMembership acting = requireMembership(orgId, actingUserId);
    boolean actingIsOwner = acting.getRole() == OrganizationRole.OWNER;
    boolean touchesOwnerTier = role == OrganizationRole.ADMIN || role == OrganizationRole.OWNER;
    if (touchesOwnerTier && !actingIsOwner) {
      throw new OwnerActionRequiredException();
    }
    if (!users.existsById(targetUserId)) {
      throw new UserNotFoundException(targetUserId);
    }
    if (memberships.existsByOrganizationIdAndUserId(orgId, targetUserId)) {
      throw new MembershipAlreadyExistsException();
    }
    memberships.save(new OrganizationMembership(orgId, targetUserId, role));
  }

  /**
   * Matrix: ADMIN may freely set MEMBER/APPROVER roles on MEMBER/APPROVER targets. Anything that
   * (a) promotes a target to ADMIN or OWNER, or (b) changes the role of a target who is currently
   * ADMIN or OWNER (demotion included), requires the acting user to be OWNER. Applied literally,
   * including to self-targeting: an ADMIN acting on their own membership is still an ADMIN-tier
   * target, so the same owner-only gate applies (no self-service carve-out — "leave org" is
   * explicitly out of scope for this task).
   */
  @Transactional
  public void updateMemberRole(
      UUID orgId, UUID actingUserId, UUID targetUserId, OrganizationRole newRole) {
    OrganizationMembership acting = requireMembership(orgId, actingUserId);
    OrganizationMembership target = requireMembership(orgId, targetUserId);

    boolean actingIsOwner = acting.getRole() == OrganizationRole.OWNER;
    boolean touchesOwnerTier =
        newRole == OrganizationRole.ADMIN
            || newRole == OrganizationRole.OWNER
            || target.getRole() == OrganizationRole.ADMIN
            || target.getRole() == OrganizationRole.OWNER;
    if (touchesOwnerTier && !actingIsOwner) {
      throw new OwnerActionRequiredException();
    }

    if (target.getRole() == OrganizationRole.OWNER && newRole != OrganizationRole.OWNER) {
      requireNotLastOwner(orgId);
    }

    target.setRole(newRole);
    memberships.save(target);
  }

  /**
   * Matrix: only OWNER may remove a target who is currently ADMIN or OWNER. ADMIN may remove
   * MEMBER/APPROVER targets freely. Same literal, no-self-carve-out reading as {@link
   * #updateMemberRole}.
   */
  @Transactional
  public void removeMember(UUID orgId, UUID actingUserId, UUID targetUserId) {
    OrganizationMembership acting = requireMembership(orgId, actingUserId);
    OrganizationMembership target = requireMembership(orgId, targetUserId);

    boolean actingIsOwner = acting.getRole() == OrganizationRole.OWNER;
    boolean targetIsOwnerOrAdmin =
        target.getRole() == OrganizationRole.OWNER || target.getRole() == OrganizationRole.ADMIN;
    if (targetIsOwnerOrAdmin && !actingIsOwner) {
      throw new OwnerActionRequiredException();
    }

    if (target.getRole() == OrganizationRole.OWNER) {
      requireNotLastOwner(orgId);
    }

    memberships.delete(target);
  }

  /**
   * Pessimistically locks every OWNER row for this org, then decides — not a plain unlocked COUNT.
   * A concurrent transaction trying to remove/demote a DIFFERENT owner of the same org blocks on
   * this same row lock until this transaction commits, so its own check reflects the post-commit
   * owner count instead of racing against a stale read. (Security review finding: with an unlocked
   * COUNT, two concurrent removeMember calls each targeting a different one of exactly two owners
   * could both observe count=2 and both proceed, leaving zero owners with no recovery path.)
   */
  private void requireNotLastOwner(UUID orgId) {
    List<OrganizationMembership> owners =
        memberships.findByOrganizationIdAndRole(orgId, OrganizationRole.OWNER);
    if (owners.size() <= 1) {
      throw new LastOwnerException();
    }
  }

  private OrganizationMembership requireMembership(UUID orgId, UUID userId) {
    return memberships
        .findByOrganizationIdAndUserId(orgId, userId)
        .orElseThrow(MembershipNotFoundException::new);
  }

  public record OrganizationWithRoleEntry(Organization organization, OrganizationRole role) {}

  public record MembershipEntry(OrganizationMembership membership, User user) {}
}
