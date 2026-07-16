"use client";

import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Field, FieldLabel, FieldError, FieldGroup } from "@/components/ui/field";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ServerErrorBanner } from "@/components/shared/server-error-banner";
import { ApiError } from "@/lib/api/client";
import { createInvitation } from "@/lib/api/invitations";
import type { OrganizationRole } from "@/lib/api/orgs";
import { mapInvitationError } from "@/lib/invitation/errors";
import {
  createInvitationSchema,
  roleOptions,
  type CreateInvitationValues,
} from "@/lib/invitation/validation";

export function CreateInvitationForm({
  orgId,
  myRole,
}: {
  orgId: string;
  myRole: OrganizationRole;
}) {
  const queryClient = useQueryClient();
  const [serverErrorCode, setServerErrorCode] = useState<string | undefined>(
    undefined
  );
  const iAmOwner = myRole === "OWNER";

  const form = useForm<CreateInvitationValues>({
    resolver: zodResolver(createInvitationSchema),
    defaultValues: { email: "", role: "MEMBER" },
  });

  const mutation = useMutation({
    mutationFn: (values: CreateInvitationValues) =>
      createInvitation(orgId, values),
    onSuccess: () => {
      setServerErrorCode(undefined);
      queryClient.invalidateQueries({ queryKey: ["orgs", orgId, "invitations"] });
      form.reset({ email: "", role: "MEMBER" });
    },
    onError: (err) => {
      setServerErrorCode(err instanceof ApiError ? err.code : undefined);
    },
  });

  const onSubmit = form.handleSubmit((values) => {
    setServerErrorCode(undefined);
    mutation.mutate(values);
  });

  return (
    <form onSubmit={onSubmit} noValidate>
      <FieldGroup>
        {serverErrorCode && (
          <ServerErrorBanner message={mapInvitationError(serverErrorCode)} />
        )}
        <Field data-invalid={!!form.formState.errors.email}>
          <FieldLabel htmlFor="invite-email">Email</FieldLabel>
          <Input
            id="invite-email"
            type="email"
            autoComplete="off"
            aria-invalid={!!form.formState.errors.email}
            {...form.register("email")}
          />
          <FieldError
            errors={
              form.formState.errors.email
                ? [form.formState.errors.email]
                : undefined
            }
          />
        </Field>
        <Field data-invalid={!!form.formState.errors.role}>
          <FieldLabel htmlFor="invite-role">Role</FieldLabel>
          <Controller
            control={form.control}
            name="role"
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger id="invite-role" aria-invalid={!!form.formState.errors.role}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {roleOptions.map((role) => (
                    // Matches InvitationService.createInvitation's touchesOwnerTier
                    // predicate exactly: inviting as ADMIN/OWNER requires the actor
                    // to already be OWNER. No existing target role to consider here
                    // (unlike member-row.tsx's role-change select).
                    <SelectItem
                      key={role}
                      value={role}
                      disabled={!iAmOwner && (role === "OWNER" || role === "ADMIN")}
                    >
                      {role}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
          <FieldError
            errors={
              form.formState.errors.role ? [form.formState.errors.role] : undefined
            }
          />
        </Field>
        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? "Sending…" : "Send invitation"}
        </Button>
      </FieldGroup>
    </form>
  );
}
