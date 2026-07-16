"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { MoreHorizontal } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ServerErrorBanner } from "@/components/shared/server-error-banner";
import { ApiError } from "@/lib/api/client";
import { revokeInvitation, type Invitation } from "@/lib/api/invitations";
import type { OrganizationRole } from "@/lib/api/orgs";
import { mapInvitationError } from "@/lib/invitation/errors";

export function InvitationRow({
  orgId,
  invitation,
  myRole,
}: {
  orgId: string;
  invitation: Invitation;
  myRole: OrganizationRole;
}) {
  const queryClient = useQueryClient();
  const [revokeDialogOpen, setRevokeDialogOpen] = useState(false);
  const [errorCode, setErrorCode] = useState<string | undefined>(undefined);

  const revokeMutation = useMutation({
    mutationFn: () => revokeInvitation(orgId, invitation.id),
    onSuccess: () => {
      setErrorCode(undefined);
      setRevokeDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ["orgs", orgId, "invitations"] });
    },
    onError: (err) => setErrorCode(err instanceof ApiError ? err.code : undefined),
  });

  // UX-only prediction of InvitationService's matrix, not authorization — the
  // mutation above keeps its own onError regardless of what's disabled here.
  // No last-owner concept applies to invitations (that's a membership-only
  // invariant), unlike member-row.tsx's equivalent.
  const iAmOwner = myRole === "OWNER";
  const targetTouchesOwnerTier =
    invitation.role === "OWNER" || invitation.role === "ADMIN";
  const canManage = iAmOwner || (myRole === "ADMIN" && !targetTouchesOwnerTier);

  const revokeDisabled = !canManage || revokeMutation.isPending;
  const revokeTitle = !canManage
    ? "Only an owner can revoke this invitation"
    : undefined;

  return (
    <li className="flex flex-col gap-2 rounded-lg border border-border px-3 py-2">
      <div className="flex items-center justify-between gap-2">
        <span className="flex flex-col">
          <span className="font-medium">{invitation.invitedEmail}</span>
          <span className="text-xs text-muted-foreground">
            Invited by {invitation.invitedByEmail}
          </span>
        </span>
        <div className="flex items-center gap-2">
          <Badge variant={invitation.role === "OWNER" || invitation.role === "ADMIN" ? "default" : "secondary"}>
            {invitation.role}
          </Badge>
          <Badge variant={invitation.expired ? "destructive" : "warning"}>
            {invitation.expired ? "Expired" : "Pending"}
          </Badge>

          <DropdownMenu>
            <DropdownMenuTrigger
              render={
                <Button variant="outline" size="icon" aria-label="Invitation actions" />
              }
            >
              <MoreHorizontal />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem
                variant="destructive"
                disabled={revokeDisabled}
                title={revokeTitle}
                onClick={() => setRevokeDialogOpen(true)}
              >
                Revoke invitation
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
      {errorCode && <ServerErrorBanner message={mapInvitationError(errorCode)} />}

      <AlertDialog open={revokeDialogOpen} onOpenChange={setRevokeDialogOpen}>
        <AlertDialogContent>
          <AlertDialogTitle>Revoke this invitation?</AlertDialogTitle>
          <AlertDialogDescription>The invite to {invitation.invitedEmail} will stop working immediately. This can&apos;t be undone.</AlertDialogDescription>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={revokeMutation.isPending}
              onClick={() => revokeMutation.mutate()}
            >
              {revokeMutation.isPending ? "Revoking…" : "Revoke invitation"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </li>
  );
}
