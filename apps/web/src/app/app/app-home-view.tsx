"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { Building2Icon } from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { buttonVariants } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ServerErrorBanner } from "@/components/shared/server-error-banner";
import { Topbar } from "@/components/shared/topbar";
import { useRequireAuth } from "@/hooks/use-require-auth";
import { useOrgs } from "@/hooks/use-orgs";
import { logout } from "@/lib/api/auth";

export function AppHomeView() {
  const { data: user, isPending } = useRequireAuth();
  const queryClient = useQueryClient();
  const orgsQuery = useOrgs();

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSuccess: () => {
      queryClient.setQueryData(["auth", "me"], null);
    },
  });

  if (isPending || !user) {
    return <p className="p-8 text-muted-foreground">Loading…</p>;
  }

  const initial = user.email[0]?.toUpperCase() ?? "?";

  return (
    <>
      <Topbar
        logoHref="/app"
        nav={
          <Link
            href="/orgs"
            className="text-sm text-muted-foreground hover:text-foreground"
          >
            Organizations
          </Link>
        }
        actions={
          <DropdownMenu>
            <DropdownMenuTrigger className="cursor-pointer rounded-full outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background">
              <Avatar>
                <AvatarFallback>{initial}</AvatarFallback>
              </Avatar>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem
                onClick={() => logoutMutation.mutate()}
                disabled={logoutMutation.isPending}
              >
                {logoutMutation.isPending ? "Logging out…" : "Log out"}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        }
      />

      <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-6 px-8 py-12">
        <h1 className="text-xl font-semibold">Welcome back, {user.email}</h1>

        {orgsQuery.isPending && (
          <p className="text-muted-foreground">Loading…</p>
        )}

        {orgsQuery.isError && (
          <ServerErrorBanner message="Something went wrong. Please try again." />
        )}

        {orgsQuery.isSuccess && orgsQuery.data.length === 0 && (
          <Card>
            <CardContent className="flex flex-col items-center gap-3 py-8 text-center">
              <span className="flex size-11 items-center justify-center rounded-md bg-muted text-muted-foreground">
                <Building2Icon className="size-5" />
              </span>
              <div className="text-[15px] font-semibold">
                No organizations yet
              </div>
              <p className="max-w-[34ch] text-[13px] text-muted-foreground">
                Create your first workspace to start building automated
                workflows.
              </p>
              <Link href="/orgs" className={buttonVariants({ size: "sm" })}>
                Create organization
              </Link>
            </CardContent>
          </Card>
        )}

        {orgsQuery.isSuccess && orgsQuery.data.length > 0 && (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle>View organizations</CardTitle>
                <CardDescription>
                  You belong to {orgsQuery.data.length} organization
                  {orgsQuery.data.length === 1 ? "" : "s"}.
                </CardDescription>
              </CardHeader>
              <CardFooter>
                <Link
                  href="/orgs"
                  className={buttonVariants({ variant: "outline", size: "sm" })}
                >
                  View organizations
                </Link>
              </CardFooter>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>Create organization</CardTitle>
                <CardDescription>
                  Start a new workspace for your team.
                </CardDescription>
              </CardHeader>
              <CardFooter>
                <Link href="/orgs" className={buttonVariants({ size: "sm" })}>
                  Create organization
                </Link>
              </CardFooter>
            </Card>
          </div>
        )}
      </div>
    </>
  );
}
