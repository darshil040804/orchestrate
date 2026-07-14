"use client";

import { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ServerErrorBanner } from "@/components/shared/server-error-banner";
import { useRequireAuth } from "@/hooks/use-require-auth";
import { useOrgs } from "@/hooks/use-orgs";
import { CreateOrgForm } from "./create-org-form";

export function OrgsView() {
  const { data: user, isPending: userPending } = useRequireAuth();
  const orgsQuery = useOrgs();
  const [showCreateForm, setShowCreateForm] = useState(false);

  if (userPending || !user) {
    return <p className="text-muted-foreground">Loading…</p>;
  }

  return (
    <div className="flex w-full max-w-sm flex-col gap-4">
      <Card className="w-full">
        <CardHeader>
          <CardTitle>Organizations</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {orgsQuery.isPending && (
            <p className="text-muted-foreground">Loading…</p>
          )}

          {orgsQuery.isError && (
            <ServerErrorBanner message="Something went wrong. Please try again." />
          )}

          {orgsQuery.isSuccess && orgsQuery.data.length === 0 && (
            <p className="text-muted-foreground">
              You don&apos;t belong to any organizations yet.
            </p>
          )}

          {orgsQuery.isSuccess && orgsQuery.data.length > 0 && (
            <ul className="flex flex-col gap-2">
              {orgsQuery.data.map((org) => (
                <li key={org.id}>
                  <Link
                    href={`/orgs/${org.id}`}
                    className="flex items-center justify-between rounded-lg border border-border px-3 py-2 hover:bg-muted"
                  >
                    <span className="flex flex-col">
                      <span className="font-medium">{org.name}</span>
                      <span className="text-sm text-muted-foreground">
                        {org.slug}
                      </span>
                    </span>
                    <span className="text-xs text-muted-foreground">
                      {org.role}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          )}

          <Button
            variant="outline"
            onClick={() => setShowCreateForm((v) => !v)}
          >
            {showCreateForm ? "Cancel" : "+ New org"}
          </Button>
        </CardContent>
      </Card>

      {showCreateForm && <CreateOrgForm />}
    </div>
  );
}
