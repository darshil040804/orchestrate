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
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
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
  const [removeDialogOpen, setRemoveDialogOpen] = useState(false);
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
      setRemoveDialogOpen(false);
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

  const changeRoleTitle = !canManage
    ? "Only an owner can change this member's role"
    : targetIsSoleOwner
      ? "An organization must always have at least one owner"
      : undefined;

  const removeTitle = !canManage
    ? "Only an owner can remove this member"
    : targetIsSoleOwner
      ? "An organization must always have at least one owner"
      : undefined;

  return (
    <li className="flex flex-col gap-2 rounded-lg border border-border px-3 py-2">
      <div className="flex items-center justify-between gap-2">
        <span className="font-medium">{member.email}</span>
        <div className="flex items-center gap-2">
          <Badge variant={member.role === "OWNER" || member.role === "ADMIN" ? "default" : "secondary"}>
            {member.role}
          </Badge>

          <DropdownMenu>
            <DropdownMenuTrigger
              render={
                <Button variant="outline" size="icon" aria-label="Member actions" />
              }
            >
              <MoreHorizontal />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuSub>
                <DropdownMenuSubTrigger
                  disabled={selectDisabled}
                  title={changeRoleTitle}
                >
                  Change role
                </DropdownMenuSubTrigger>
                <DropdownMenuSubContent>
                  <DropdownMenuRadioGroup
                    value={member.role}
                    onValueChange={(value) =>
                      roleMutation.mutate(value as OrganizationRole)
                    }
                  >
                    {ROLE_OPTIONS.map((role) => (
                      // A non-owner ADMIN may freely set MEMBER/APPROVER, but promoting
                      // to ADMIN/OWNER touches the owner tier regardless of the target's
                      // current role — matches OrgService.updateMemberRole's
                      // touchesOwnerTier predicate exactly (current role OR new role).
                      <DropdownMenuRadioItem
                        key={role}
                        value={role}
                        disabled={!iAmOwner && (role === "OWNER" || role === "ADMIN")}
                      >
                        {role}
                      </DropdownMenuRadioItem>
                    ))}
                  </DropdownMenuRadioGroup>
                </DropdownMenuSubContent>
              </DropdownMenuSub>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                variant="destructive"
                disabled={removeDisabled}
                title={removeTitle}
                onClick={() => setRemoveDialogOpen(true)}
              >
                Remove member
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
      {errorCode && <ServerErrorBanner message={mapOrgError(errorCode)} />}

      <AlertDialog open={removeDialogOpen} onOpenChange={setRemoveDialogOpen}>
        <AlertDialogContent>
          <AlertDialogTitle>Remove this member?</AlertDialogTitle>
          <AlertDialogDescription>
            {member.email} will lose access to this organization immediately.
          </AlertDialogDescription>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={removeMutation.isPending}
              onClick={() => removeMutation.mutate()}
            >
              {removeMutation.isPending ? "Removing…" : "Remove member"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </li>
  );
}
