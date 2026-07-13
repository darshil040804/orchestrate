package com.orchestrate.api.org.dto;

import com.orchestrate.api.org.Organization;
import com.orchestrate.api.org.OrganizationRole;
import java.time.Instant;
import java.util.UUID;

public record OrganizationWithRoleResponse(
    UUID id, String name, String slug, OrganizationRole role, Instant createdAt) {

  public static OrganizationWithRoleResponse from(Organization org, OrganizationRole role) {
    return new OrganizationWithRoleResponse(
        org.getId(), org.getName(), org.getSlug(), role, org.getCreatedAt());
  }
}
