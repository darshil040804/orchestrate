package com.orchestrate.api.invitation.dto;

import com.orchestrate.api.invitation.Invitation;
import com.orchestrate.api.org.OrganizationRole;
import com.orchestrate.api.user.User;
import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
    UUID id,
    UUID organizationId,
    String invitedEmail,
    OrganizationRole role,
    UUID invitedByUserId,
    String invitedByEmail,
    Instant expiresAt,
    Instant createdAt,
    boolean expired) {

  public static InvitationResponse from(Invitation invitation, User invitedBy) {
    return new InvitationResponse(
        invitation.getId(),
        invitation.getOrganizationId(),
        invitation.getInvitedEmail(),
        invitation.getRole(),
        invitation.getInvitedByUserId(),
        invitedBy.getEmail(),
        invitation.getExpiresAt(),
        invitation.getCreatedAt(),
        Instant.now().isAfter(invitation.getExpiresAt()));
  }
}
