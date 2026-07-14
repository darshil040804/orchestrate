import { apiFetch } from "@/lib/api/client";

export type OrganizationRole = "OWNER" | "ADMIN" | "MEMBER" | "APPROVER";

export type Organization = {
  id: string;
  name: string;
  slug: string;
  createdAt: string;
};

export type OrganizationWithRole = Organization & {
  role: OrganizationRole;
};

export type Membership = {
  userId: string;
  email: string;
  role: OrganizationRole;
  createdAt: string;
};

export function listOrganizations() {
  return apiFetch<OrganizationWithRole[]>("/api/orgs", { authenticated: true });
}

export function createOrganization(input: { name: string; slug: string }) {
  return apiFetch<Organization>("/api/orgs", {
    method: "POST",
    body: input,
    authenticated: true,
  });
}

export function listMembers(orgId: string) {
  return apiFetch<Membership[]>(`/api/orgs/${orgId}/members`, {
    authenticated: true,
  });
}

export function updateMemberRole(
  orgId: string,
  userId: string,
  role: OrganizationRole
) {
  return apiFetch<void>(`/api/orgs/${orgId}/members/${userId}`, {
    method: "PATCH",
    body: { role },
    authenticated: true,
  });
}

export function removeMember(orgId: string, userId: string) {
  return apiFetch<void>(`/api/orgs/${orgId}/members/${userId}`, {
    method: "DELETE",
    authenticated: true,
  });
}
