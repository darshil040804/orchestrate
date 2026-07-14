import { apiFetch } from "@/lib/api/client";
import type { OrganizationRole } from "@/lib/api/orgs";

export type Invitation = {
  id: string;
  organizationId: string;
  invitedEmail: string;
  role: OrganizationRole;
  invitedByUserId: string;
  invitedByEmail: string;
  expiresAt: string;
  createdAt: string;
  expired: boolean;
};

export function createInvitation(
  orgId: string,
  input: { email: string; role: OrganizationRole }
) {
  return apiFetch<void>(`/api/orgs/${orgId}/invitations`, {
    method: "POST",
    body: input,
    authenticated: true,
  });
}

export function listInvitations(orgId: string) {
  return apiFetch<Invitation[]>(`/api/orgs/${orgId}/invitations`, {
    authenticated: true,
  });
}

export function revokeInvitation(orgId: string, invitationId: string) {
  return apiFetch<void>(`/api/orgs/${orgId}/invitations/${invitationId}`, {
    method: "DELETE",
    authenticated: true,
  });
}

export function acceptInvitation(token: string) {
  return apiFetch<void>("/api/invitations/accept", {
    method: "POST",
    body: { token },
    authenticated: true,
  });
}
