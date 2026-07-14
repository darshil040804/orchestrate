"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ServerErrorBanner } from "@/components/auth/server-error-banner";
import { ApiError } from "@/lib/api/client";
import { verifyEmail } from "@/lib/api/auth";
import { mapAuthError } from "@/lib/auth/errors";

export function VerifyEmailView({ token }: { token?: string }) {
  const query = useQuery({
    queryKey: ["verify-email", token],
    queryFn: () => verifyEmail(token as string),
    enabled: !!token,
    retry: false,
  });

  return (
    <Card className="w-full max-w-sm">
      <CardHeader>
        <CardTitle>Email verification</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {!token && <ServerErrorBanner message="This link is invalid." />}

        {token && query.isPending && (
          <p className="text-muted-foreground">Verifying…</p>
        )}

        {token && query.isSuccess && (
          <>
            <p>{query.data.message}</p>
            <Link href="/login" className={buttonVariants({ variant: "outline" })}>
              Go to login
            </Link>
          </>
        )}

        {token && query.isError && (
          <ServerErrorBanner
            message={
              query.error instanceof ApiError
                ? mapAuthError(query.error.code)
                : "Something went wrong. Please try again."
            }
          />
        )}
      </CardContent>
    </Card>
  );
}
