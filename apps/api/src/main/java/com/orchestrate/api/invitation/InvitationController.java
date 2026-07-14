package com.orchestrate.api.invitation;

import com.orchestrate.api.auth.jwt.JwtService.AccessTokenPrincipal;
import com.orchestrate.api.invitation.dto.AcceptInvitationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Flat, non-org-scoped — the org is implied by the token itself, mirroring how
// /api/auth/verify-email is a flat token endpoint rather than nested under a resource path.
// No @PreAuthorize org check: the caller isn't a member of the org yet, only authenticated.
@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

  private final InvitationService invitationService;

  public InvitationController(InvitationService invitationService) {
    this.invitationService = invitationService;
  }

  @PostMapping("/accept")
  public ResponseEntity<Void> accept(
      @AuthenticationPrincipal AccessTokenPrincipal principal,
      @Valid @RequestBody AcceptInvitationRequest request) {
    invitationService.acceptInvitation(principal.userId(), principal.email(), request.token());
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}
