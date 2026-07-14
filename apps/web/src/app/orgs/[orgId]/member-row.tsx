"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { ServerErrorBanner } from "@/components/shared/server-error-banner";
import { ApiError } from "@/lib/api/client";
import {
  updateMemberRole,
  removeMember,
  type Membership,
  type OrganizationRole,
} from "@/lib/api/orgs";
import { mapOrgError } from "@/lib/org/errors";

const ROLE_OPTIONS: OrganizationRole[] = ["OWNER", "ADMIN", "MEMBER", "APPROVER"];

export function MemberRow({
  orgId,
  member,
  myRole,
  isLastOwner,
}: {
  orgId: string;
  member: Membership;
  myRole: OrganizationRole;
  /** true only when member.role === "OWNER" and the org has exactly 1 owner */
  isLastOwner: boolean;
}) {
  const queryClient = useQueryClient();
  const [confirmingRemove, setConfirmingRemove] = useState(false);
  const [errorCode, setErrorCode] = useState<string | undefined>(undefined);

  // Self-targeting is allowed server-side (no self-service carve-out), and a
  // self-demote/self-remove changes the top-level ["orgs"] cache too (it holds
  // the caller's own role, used by the page header) — invalidate both.
  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["orgs", orgId, "members"] });
    queryClient.invalidateQueries({ queryKey: ["orgs"] });
  };

  const roleMutation = useMutation({
    mutationFn: (role: OrganizationRole) =>
      updateMemberRole(orgId, member.userId, role),
    onSuccess: () => {
      setErrorCode(undefined);
      invalidate();
    },
    onError: (err) => setErrorCode(err instanceof ApiError ? err.code : undefined),
  });

  const removeMutation = useMutation({
    mutationFn: () => removeMember(orgId, member.userId),
    onSuccess: () => {
      setErrorCode(undefined);
      invalidate();
    },
    onError: (err) => setErrorCode(err instanceof ApiError ? err.code : undefined),
  });

  // UX-only prediction of OrgService's matrix, not authorization — every
  // mutation above keeps its own onError regardless of what's disabled here,
  // covering the race where another admin changes the target between page
  // load and click.
  const iAmOwner = myRole === "OWNER";
  const targetTouchesOwnerTier = member.role === "OWNER" || member.role === "ADMIN";
  const canManage = iAmOwner || (myRole === "ADMIN" && !targetTouchesOwnerTier);
  const targetIsSoleOwner = member.role === "OWNER" && isLastOwner;

  const selectDisabled =
    !canManage || targetIsSoleOwner || roleMutation.isPending;
  const removeDisabled = !canManage || targetIsSoleOwner || removeMutation.isPending;

  return (
    <li className="flex flex-col gap-2 rounded-lg border border-border px-3 py-2">
      <div className="flex items-center justify-between gap-2">
        <span className="font-medium">{member.email}</span>
        <div className="flex items-center gap-2">
          <select
            className="h-8 rounded-lg border border-input bg-transparent px-2 text-sm disabled:opacity-50"
            value={member.role}
            disabled={selectDisabled}
            title={
              !canManage
                ? "Only an owner can change this member's role"
                : targetIsSoleOwner
                  ? "An organization must always have at least one owner"
                  : undefined
            }
            onChange={(e) =>
              roleMutation.mutate(e.target.value as OrganizationRole)
            }
          >
            {ROLE_OPTIONS.map((role) => (
              // A non-owner ADMIN may freely set MEMBER/APPROVER, but promoting
              // to ADMIN/OWNER touches the owner tier regardless of the target's
              // current role — matches OrgService.updateMemberRole's
              // touchesOwnerTier predicate exactly (current role OR new role).
              <option
                key={role}
                value={role}
                disabled={!iAmOwner && (role === "OWNER" || role === "ADMIN")}
              >
                {role}
              </option>
            ))}
          </select>

          {!confirmingRemove ? (
            <Button
              variant="outline"
              size="sm"
              disabled={removeDisabled}
              title={
                !canManage
                  ? "Only an owner can remove this member"
                  : targetIsSoleOwner
                    ? "An organization must always have at least one owner"
                    : undefined
              }
              onClick={() => setConfirmingRemove(true)}
            >
              Remove
            </Button>
          ) : (
            <div className="flex items-center gap-1">
              <Button
                variant="destructive"
                size="sm"
                disabled={removeMutation.isPending}
                onClick={() => removeMutation.mutate()}
              >
                {removeMutation.isPending ? "Removing…" : "Confirm?"}
              </Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setConfirmingRemove(false)}
              >
                Cancel
              </Button>
            </div>
          )}
        </div>
      </div>
      {errorCode && <ServerErrorBanner message={mapOrgError(errorCode)} />}
    </li>
  );
}
