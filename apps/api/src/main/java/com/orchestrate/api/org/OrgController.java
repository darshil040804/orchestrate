package com.orchestrate.api.org;

import com.orchestrate.api.auth.jwt.JwtService.AccessTokenPrincipal;
import com.orchestrate.api.org.dto.AddMemberRequest;
import com.orchestrate.api.org.dto.CreateOrganizationRequest;
import com.orchestrate.api.org.dto.MembershipResponse;
import com.orchestrate.api.org.dto.OrganizationResponse;
import com.orchestrate.api.org.dto.OrganizationWithRoleResponse;
import com.orchestrate.api.org.dto.UpdateRoleRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orgs")
public class OrgController {

  private final OrgService orgService;

  public OrgController(OrgService orgService) {
    this.orgService = orgService;
  }

  @PostMapping
  public ResponseEntity<OrganizationResponse> create(
      @AuthenticationPrincipal AccessTokenPrincipal principal,
      @Valid @RequestBody CreateOrganizationRequest request) {
    Organization org =
        orgService.createOrganization(principal.userId(), request.name(), request.slug());
    return ResponseEntity.status(HttpStatus.CREATED).body(OrganizationResponse.from(org));
  }

  @GetMapping
  public ResponseEntity<List<OrganizationWithRoleResponse>> listMine(
      @AuthenticationPrincipal AccessTokenPrincipal principal) {
    List<OrganizationWithRoleResponse> body =
        orgService.listOrganizationsForUser(principal.userId()).stream()
            .map(e -> OrganizationWithRoleResponse.from(e.organization(), e.role()))
            .toList();
    return ResponseEntity.ok(body);
  }

  @GetMapping("/{orgId}")
  @PreAuthorize("@orgSecurity.isMember(#orgId, principal)")
  public ResponseEntity<OrganizationResponse> get(@PathVariable UUID orgId) {
    return ResponseEntity.ok(OrganizationResponse.from(orgService.getOrganization(orgId)));
  }

  // Intentional design choice, not an oversight: gated on isMember (any role, including
  // MEMBER/APPROVER) rather than hasAtLeastRole(ADMIN) — every member can see the full roster,
  // including other members' emails. Matches common SaaS convention (Slack/GitHub/Notion).
  @GetMapping("/{orgId}/members")
  @PreAuthorize("@orgSecurity.isMember(#orgId, principal)")
  public ResponseEntity<List<MembershipResponse>> listMembers(@PathVariable UUID orgId) {
    List<MembershipResponse> body =
        orgService.listMembers(orgId).stream()
            .map(e -> MembershipResponse.from(e.membership(), e.user()))
            .toList();
    return ResponseEntity.ok(body);
  }

  @PostMapping("/{orgId}/members")
  @PreAuthorize(
      "@orgSecurity.hasAtLeastRole(#orgId, principal, T(com.orchestrate.api.org.OrganizationRole).ADMIN)")
  public ResponseEntity<Void> addMember(
      @AuthenticationPrincipal AccessTokenPrincipal principal,
      @PathVariable UUID orgId,
      @Valid @RequestBody AddMemberRequest request) {
    orgService.addMember(orgId, principal.userId(), request.userId(), request.role());
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PatchMapping("/{orgId}/members/{userId}")
  @PreAuthorize(
      "@orgSecurity.hasAtLeastRole(#orgId, principal, T(com.orchestrate.api.org.OrganizationRole).ADMIN)")
  public ResponseEntity<Void> updateMemberRole(
      @AuthenticationPrincipal AccessTokenPrincipal principal,
      @PathVariable UUID orgId,
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateRoleRequest request) {
    orgService.updateMemberRole(orgId, principal.userId(), userId, request.role());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{orgId}/members/{userId}")
  @PreAuthorize(
      "@orgSecurity.hasAtLeastRole(#orgId, principal, T(com.orchestrate.api.org.OrganizationRole).ADMIN)")
  public ResponseEntity<Void> removeMember(
      @AuthenticationPrincipal AccessTokenPrincipal principal,
      @PathVariable UUID orgId,
      @PathVariable UUID userId) {
    orgService.removeMember(orgId, principal.userId(), userId);
    return ResponseEntity.noContent().build();
  }
}
