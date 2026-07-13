package com.orchestrate.api.org.dto;

import com.orchestrate.api.org.OrganizationMembership;
import com.orchestrate.api.org.OrganizationRole;
import com.orchestrate.api.user.User;
import java.time.Instant;
import java.util.UUID;

public record MembershipResponse(
    UUID userId, String email, OrganizationRole role, Instant createdAt) {

  public static MembershipResponse from(OrganizationMembership membership, User user) {
    return new MembershipResponse(
        user.getId(), user.getEmail(), membership.getRole(), membership.getCreatedAt());
  }
}
