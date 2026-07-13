package com.orchestrate.api.org;

import com.orchestrate.api.auth.jwt.JwtService.AccessTokenPrincipal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Per-request DB lookup of the caller's membership/role for a given org — referenced from
 * {@code @PreAuthorize} SpEL as {@code @orgSecurity}. Deliberately not JWT-embedded: role changes
 * take effect immediately, and a user can be scoped to multiple orgs simultaneously without any
 * "active org" / token-reissuance concept.
 */
@Component("orgSecurity")
public class OrgSecurity {

  private final OrganizationMembershipRepository memberships;

  public OrgSecurity(OrganizationMembershipRepository memberships) {
    this.memberships = memberships;
  }

  public boolean isMember(UUID orgId, AccessTokenPrincipal principal) {
    return principal != null
        && memberships.existsByOrganizationIdAndUserId(orgId, principal.userId());
  }

  public boolean hasAtLeastRole(
      UUID orgId, AccessTokenPrincipal principal, OrganizationRole minRole) {
    if (principal == null) {
      return false;
    }
    return memberships
        .findByOrganizationIdAndUserId(orgId, principal.userId())
        .map(m -> m.getRole().isAtLeast(minRole))
        .orElse(false);
  }
}
