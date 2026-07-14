package com.orchestrate.api.invitation.dto;

import com.orchestrate.api.org.OrganizationRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInvitationRequest(
    @Email @NotBlank String email, @NotNull OrganizationRole role) {}
