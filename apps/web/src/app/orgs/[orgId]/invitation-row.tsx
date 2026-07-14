"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
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
  const [confirmingRevoke, setConfirmingRevoke] = useState(false);
  const [errorCode, setErrorCode] = useState<string | undefined>(undefined);

  const revokeMutation = useMutation({
    mutationFn: () => revokeInvitation(orgId, invitation.id),
    onSuccess: () => {
      setErrorCode(undefined);
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
          <span className="text-xs text-muted-foreground">{invitation.role}</span>
          <span className="text-xs text-muted-foreground">
            {invitation.expired ? "Expired" : "Pending"}
          </span>

          {!confirmingRevoke ? (
            <Button
              variant="outline"
              size="sm"
              disabled={revokeDisabled}
              title={
                !canManage
                  ? "Only an owner can revoke this invitation"
                  : undefined
              }
              onClick={() => setConfirmingRevoke(true)}
            >
              Revoke
            </Button>
          ) : (
            <div className="flex items-center gap-1">
              <Button
                variant="destructive"
                size="sm"
                disabled={revokeMutation.isPending}
                onClick={() => revokeMutation.mutate()}
              >
                {revokeMutation.isPending ? "Revoking…" : "Confirm?"}
              </Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setConfirmingRevoke(false)}
              >
                Cancel
              </Button>
            </div>
          )}
        </div>
      </div>
      {errorCode && <ServerErrorBanner message={mapInvitationError(errorCode)} />}
    </li>
  );
}
