import { useQuery } from "@tanstack/react-query";
import { ApiError } from "@/lib/api/client";
import { getMe, type UserResponse } from "@/lib/api/auth";

/**
 * The one cross-cutting auth-state hook — used by the landing page today, every
 * future protected page later. Resolves to `null` (not an error state) when the
 * visitor simply isn't logged in, since GET /api/auth/me returning 401 is the
 * normal, expected case for anonymous visitors, not a failure to surface.
 */
export function useCurrentUser() {
  return useQuery<UserResponse | null>({
    queryKey: ["auth", "me"],
    queryFn: async () => {
      try {
        return await getMe();
      } catch (err) {
        if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
          return null;
        }
        throw err;
      }
    },
    retry: false,
  });
}
