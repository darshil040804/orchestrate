"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useCurrentUser } from "@/hooks/use-current-user";

/**
 * Guards a genuinely protected page (unlike `/`, which deliberately renders both
 * auth states inline for the OAuth-redirect-target requirement). Redirects to
 * /login once we know for certain the visitor isn't authenticated. Callers
 * should render a loading/empty state while `isPending || data === null` so
 * nothing protected flashes before the redirect effect fires.
 */
export function useRequireAuth() {
  const router = useRouter();
  const query = useCurrentUser();

  useEffect(() => {
    if (!query.isPending && query.data === null) {
      router.push("/login");
    }
  }, [query.isPending, query.data, router]);

  return query;
}
