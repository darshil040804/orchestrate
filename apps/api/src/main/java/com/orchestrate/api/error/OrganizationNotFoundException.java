package com.orchestrate.api.error;

import java.util.UUID;

public class OrganizationNotFoundException extends RuntimeException {

  public OrganizationNotFoundException(UUID orgId) {
    super("Organization not found: " + orgId);
  }
}
