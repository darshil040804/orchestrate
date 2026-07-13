package com.orchestrate.api.org.dto;

import com.orchestrate.api.org.OrganizationRole;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull OrganizationRole role) {}
