"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ServerErrorBanner } from "@/components/shared/server-error-banner";
import { useCurrentUser } from "@/hooks/use-current-user";
import { ApiError } from "@/lib/api/client";
import { acceptInvitation } from "@/lib/api/invitations";
import { logout } from "@/lib/api/auth";
import { mapInvitationError } from "@/lib/invitation/errors";

export function AcceptInvitationView({ token }: { token?: string }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  // Called directly, not via useRequireAuth — this page has meaningful
  // content to show even when logged out (an explanation + login/signup
  // links), unlike /orgs, which has nothing to show and just redirects.
  const { data: user, isPending: userPending } = useCurrentUser();

  const acceptMutation = useMutation({
    mutationFn: () => acceptInvitation(token as string),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["orgs"] });
      router.push("/orgs");
    },
  });

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSuccess: () => {
      queryClient.setQueryData(["auth", "me"], null);
    },
  });

  if (!token) {
    return (
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Accept invitation</CardTitle>
        </CardHeader>
        <CardContent>
          <ServerErrorBanner message="This link is invalid." />
        </CardContent>
      </Card>
    );
  }

  if (userPending) {
    return (
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Accept invitation</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <Skeleton className="h-4 w-2/3" />
          <Skeleton className="h-9 w-full" />
        </CardContent>
      </Card>
    );
  }

  if (!user) {
    const returnTo = `/invitations/accept?token=${encodeURIComponent(token)}`;
    return (
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Accept invitation</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <p className="text-muted-foreground">
            You&apos;ve been invited to join an organization. Log in or sign up
            with the invited email address to accept.
          </p>
          <div className="flex flex-col gap-2">
            <Link
              href={`/login?returnTo=${encodeURIComponent(returnTo)}`}
              className="underline underline-offset-4"
            >
              Log in
            </Link>
            <Link href="/signup" className="underline underline-offset-4">
              Sign up
            </Link>
          </div>
        </CardContent>
      </Card>
    );
  }

  const errorCode =
    acceptMutation.error instanceof ApiError
      ? acceptMutation.error.code
      : undefined;

  return (
    <Card className="w-full max-w-sm">
      <CardHeader>
        <CardTitle>Accept invitation</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <p className="text-muted-foreground">
          Logged in as <span className="font-medium">{user.email}</span>
        </p>

        {acceptMutation.isError && errorCode === "INVITATION_EMAIL_MISMATCH" && (
          <div className="flex flex-col gap-2">
            <ServerErrorBanner
              message={`This invitation was sent to a different email address than the one you're logged in with (${user.email}).`}
            />
            <Button
              variant="outline"
              onClick={() => logoutMutation.mutate()}
              disabled={logoutMutation.isPending}
            >
              {logoutMutation.isPending ? "Logging out…" : "Log out"}
            </Button>
          </div>
        )}

        {acceptMutation.isError && errorCode === "MEMBERSHIP_ALREADY_EXISTS" && (
          <div className="flex flex-col gap-2">
            <ServerErrorBanner message={mapInvitationError(errorCode)} />
            <Link href="/orgs" className="underline underline-offset-4">
              Go to your organizations
            </Link>
          </div>
        )}

        {acceptMutation.isError &&
          errorCode !== "INVITATION_EMAIL_MISMATCH" &&
          errorCode !== "MEMBERSHIP_ALREADY_EXISTS" && (
            <ServerErrorBanner message={mapInvitationError(errorCode)} />
          )}

        {!acceptMutation.isError && (
          <Button
            onClick={() => acceptMutation.mutate()}
            disabled={acceptMutation.isPending}
          >
            {acceptMutation.isPending ? "Accepting…" : "Accept invitation"}
          </Button>
        )}
      </CardContent>
    </Card>
  );
}
