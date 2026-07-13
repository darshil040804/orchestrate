package com.orchestrate.api.org.dto;

import com.orchestrate.api.org.OrganizationRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddMemberRequest(@NotNull UUID userId, @NotNull OrganizationRole role) {}
