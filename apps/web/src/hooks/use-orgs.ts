import { useQuery } from "@tanstack/react-query";
import { listOrganizations } from "@/lib/api/orgs";

/**
 * Cross-cutting — used by both /orgs and /orgs/[orgId] (the detail page derives
 * its org header and the caller's own role from this same list response, since
 * GET /api/orgs/{orgId} has no role field; only the list endpoint carries it).
 *
 * Deliberately does NOT map 401 to null the way useCurrentUser does — that
 * mapping exists there because "not logged in" is an expected render state for
 * the dual-purpose landing page. These are protected pages with no such expected
 * state, so failures here should surface as a real error and let useRequireAuth
 * own the redirect decision.
 */
export function useOrgs() {
  return useQuery({
    queryKey: ["orgs"],
    queryFn: listOrganizations,
  });
}
