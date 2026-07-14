"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ServerErrorBanner } from "@/components/auth/server-error-banner";
import { useCurrentUser } from "@/hooks/use-current-user";
import { logout } from "@/lib/api/auth";
import { mapAuthError } from "@/lib/auth/errors";

export function LandingView({ oauthError }: { oauthError?: string }) {
  const { data: user, isPending } = useCurrentUser();
  const queryClient = useQueryClient();

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSuccess: () => {
      queryClient.setQueryData(["auth", "me"], null);
    },
  });

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 p-24">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Orchestrate</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {oauthError && <ServerErrorBanner message={mapAuthError(oauthError)} />}

          {isPending && <p className="text-muted-foreground">Loading…</p>}

          {!isPending && user && (
            <>
              <p>
                Logged in as <span className="font-medium">{user.email}</span>
              </p>
              <Button
                variant="outline"
                onClick={() => logoutMutation.mutate()}
                disabled={logoutMutation.isPending}
              >
                {logoutMutation.isPending ? "Logging out…" : "Log out"}
              </Button>
            </>
          )}

          {!isPending && !user && (
            <div className="flex flex-col gap-2">
              <Link href="/login" className="underline underline-offset-4">
                Log in
              </Link>
              <Link href="/signup" className="underline underline-offset-4">
                Sign up
              </Link>
            </div>
          )}
        </CardContent>
      </Card>
    </main>
  );
}
