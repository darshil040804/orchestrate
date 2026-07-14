package com.orchestrate.api.invitation;

import com.orchestrate.api.auth.jwt.JwtService.AccessTokenPrincipal;
import com.orchestrate.api.invitation.dto.CreateInvitationRequest;
import com.orchestrate.api.invitation.dto.InvitationResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Both create and list are gated hasAtLeastRole(ADMIN), not the isMember gate used for
// /members — who's been invited (and at what role) is admin-sensitive, unlike the roster.
@RestController
@RequestMapping("/api/orgs/{orgId}/invitations")
public class OrgInvitationController {

  private final InvitationService invitationService;

  public OrgInvitationController(InvitationService invitationService) {
    this.invitationService = invitationService;
  }

  @PostMapping
  @PreAuthorize(
      "@orgSecurity.hasAtLeastRole(#orgId, principal, T(com.orchestrate.api.org.OrganizationRole).ADMIN)")
  public ResponseEntity<Void> create(
      @AuthenticationPrincipal AccessTokenPrincipal principal,
      @PathVariable UUID orgId,
      @Valid @RequestBody CreateInvitationRequest request) {
    invitationService.createInvitation(orgId, principal.userId(), request.email(), request.role());
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @GetMapping
  @PreAuthorize(
      "@orgSecurity.hasAtLeastRole(#orgId, principal, T(com.orchestrate.api.org.OrganizationRole).ADMIN)")
  public ResponseEntity<List<InvitationResponse>> listPending(@PathVariable UUID orgId) {
    List<InvitationResponse> body =
        invitationService.listPending(orgId).stream()
            .map(e -> InvitationResponse.from(e.invitation(), e.invitedBy()))
            .toList();
    return ResponseEntity.ok(body);
  }

  @DeleteMapping("/{invitationId}")
  @PreAuthorize(
      "@orgSecurity.hasAtLeastRole(#orgId, principal, T(com.orchestrate.api.org.OrganizationRole).ADMIN)")
  public ResponseEntity<Void> revoke(
      @AuthenticationPrincipal AccessTokenPrincipal principal,
      @PathVariable UUID orgId,
      @PathVariable UUID invitationId) {
    invitationService.revokeInvitation(orgId, principal.userId(), invitationId);
    return ResponseEntity.noContent().build();
  }
}
