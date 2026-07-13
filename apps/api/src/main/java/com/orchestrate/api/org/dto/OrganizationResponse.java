package com.orchestrate.api.org.dto;

import com.orchestrate.api.org.Organization;
import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(UUID id, String name, String slug, Instant createdAt) {

  public static OrganizationResponse from(Organization org) {
    return new OrganizationResponse(org.getId(), org.getName(), org.getSlug(), org.getCreatedAt());
  }
}
