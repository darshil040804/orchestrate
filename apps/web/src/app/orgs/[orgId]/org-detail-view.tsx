"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ServerErrorBanner } from "@/components/shared/server-error-banner";
import { useRequireAuth } from "@/hooks/use-require-auth";
import { useOrgs } from "@/hooks/use-orgs";
import { listMembers } from "@/lib/api/orgs";
import { listInvitations } from "@/lib/api/invitations";
import { MemberRow } from "./member-row";
import { InvitationRow } from "./invitation-row";
import { CreateInvitationForm } from "./create-invitation-form";

export function OrgDetailView({ orgId }: { orgId: string }) {
  const { data: user, isPending: userPending } = useRequireAuth();
  const orgsQuery = useOrgs();
  const [tab, setTab] = useState<"members" | "invitations">("members");

  const myOrg = orgsQuery.data?.find((org) => org.id === orgId);
  const canManage = myOrg
    ? myOrg.role === "OWNER" || myOrg.role === "ADMIN"
    : false;

  const membersQuery = useQuery({
    queryKey: ["orgs", orgId, "members"],
    queryFn: () => listMembers(orgId),
    enabled: !!myOrg,
  });

  const invitationsQuery = useQuery({
    queryKey: ["orgs", orgId, "invitations"],
    queryFn: () => listInvitations(orgId),
    enabled: !!myOrg && canManage && tab === "invitations",
  });

  if (userPending || !user) {
    return <p className="text-muted-foreground">Loading…</p>;
  }

  if (orgsQuery.isPending) {
    return <p className="text-muted-foreground">Loading…</p>;
  }

  // Only treat "not in the list" as a real not-found once the list query has
  // settled — otherwise a hard refresh/direct visit briefly false-negatives
  // while useOrgs() is still fetching.
  if (!myOrg && !orgsQuery.isFetching) {
    return (
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Organization not found</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <p className="text-muted-foreground">
            This organization doesn&apos;t exist, or you&apos;re not a member of it.
          </p>
          <Link href="/orgs" className="underline underline-offset-4">
            Back to organizations
          </Link>
        </CardContent>
      </Card>
    );
  }

  if (!myOrg) {
    return <p className="text-muted-foreground">Loading…</p>;
  }

  const ownerCount =
    membersQuery.data?.filter((m) => m.role === "OWNER").length ?? 0;

  return (
    <div className="flex w-full max-w-sm flex-col gap-4">
      <Card className="w-full">
        <CardHeader>
          <CardTitle>{myOrg.name}</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-1">
          <p className="text-sm text-muted-foreground">{myOrg.slug}</p>
          <p className="flex items-center gap-2 text-sm text-muted-foreground">
            Your role: <Badge variant={myOrg.role === "OWNER" || myOrg.role === "ADMIN" ? "default" : "secondary"}>{myOrg.role}</Badge>
          </p>
          <Link href="/orgs" className="mt-2 text-sm underline underline-offset-4">
            Back to organizations
          </Link>
        </CardContent>
      </Card>

      <Tabs value={tab} onValueChange={(value) => setTab(value as "members" | "invitations")}>
        <TabsList>
          <TabsTrigger value="members">Members</TabsTrigger>
          {canManage && <TabsTrigger value="invitations">Invitations</TabsTrigger>}
        </TabsList>
      </Tabs>

      {tab === "members" && (
        <Card className="w-full">
          <CardHeader>
            <CardTitle>Members</CardTitle>
          </CardHeader>
          <CardContent>
            {membersQuery.isPending && (
              <p className="text-muted-foreground">Loading…</p>
            )}

            {membersQuery.isError && (
              <ServerErrorBanner message="Something went wrong. Please try again." />
            )}

            {membersQuery.isSuccess && !canManage && (
              <ul className="flex flex-col gap-2">
                {membersQuery.data.map((member) => (
                  <li
                    key={member.userId}
                    className="flex items-center justify-between rounded-lg border border-border px-3 py-2"
                  >
                    <span>{member.email}</span>
                    <Badge variant={member.role === "OWNER" || member.role === "ADMIN" ? "default" : "secondary"}>
                      {member.role}
                    </Badge>
                  </li>
                ))}
              </ul>
            )}

            {membersQuery.isSuccess && canManage && (
              <ul className="flex flex-col gap-2">
                {membersQuery.data.map((member) => (
                  <MemberRow
                    key={member.userId}
                    orgId={orgId}
                    member={member}
                    myRole={myOrg.role}
                    isLastOwner={member.role === "OWNER" && ownerCount <= 1}
                  />
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      )}

      {tab === "invitations" && canManage && (
        <Card className="w-full">
          <CardHeader>
            <CardTitle>Invitations</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <CreateInvitationForm orgId={orgId} myRole={myOrg.role} />

            {invitationsQuery.isPending && (
              <p className="text-muted-foreground">Loading…</p>
            )}

            {invitationsQuery.isError && (
              <ServerErrorBanner message="Something went wrong. Please try again." />
            )}

            {invitationsQuery.isSuccess && invitationsQuery.data.length === 0 && (
              <p className="text-muted-foreground">No pending invitations.</p>
            )}

            {invitationsQuery.isSuccess && invitationsQuery.data.length > 0 && (
              <ul className="flex flex-col gap-2">
                {invitationsQuery.data.map((invitation) => (
                  <InvitationRow
                    key={invitation.id}
                    orgId={orgId}
                    invitation={invitation}
                    myRole={myOrg.role}
                  />
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
