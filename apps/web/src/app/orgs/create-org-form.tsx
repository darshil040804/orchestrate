"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Field, FieldLabel, FieldError, FieldGroup } from "@/components/ui/field";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ServerErrorBanner } from "@/components/shared/server-error-banner";
import { ApiError } from "@/lib/api/client";
import { createOrganization, type OrganizationWithRole } from "@/lib/api/orgs";
import { mapOrgError } from "@/lib/org/errors";
import { createOrgSchema, type CreateOrgValues } from "@/lib/org/validation";

export function CreateOrgForm() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [serverErrorCode, setServerErrorCode] = useState<string | undefined>(
    undefined
  );

  const form = useForm<CreateOrgValues>({
    resolver: zodResolver(createOrgSchema),
    defaultValues: { name: "", slug: "" },
  });

  const mutation = useMutation({
    mutationFn: createOrganization,
    onSuccess: (org) => {
      // Creator is always OWNER (OrgService.createOrganization) — seed the cache
      // synchronously so the detail page's own useOrgs() read (which mounts
      // after this navigation, replacing this component) finds the org
      // immediately instead of rendering the stale pre-creation list first.
      queryClient.setQueryData<OrganizationWithRole[]>(["orgs"], (old) => [
        ...(old ?? []),
        { ...org, role: "OWNER" },
      ]);
      queryClient.invalidateQueries({ queryKey: ["orgs"] });
      router.push(`/orgs/${org.id}`);
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
    <Card className="w-full max-w-sm">
      <CardHeader>
        <CardTitle>New organization</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={onSubmit} noValidate>
          <FieldGroup>
            {serverErrorCode && (
              <ServerErrorBanner message={mapOrgError(serverErrorCode)} />
            )}
            <Field data-invalid={!!form.formState.errors.name}>
              <FieldLabel htmlFor="name">Name</FieldLabel>
              <Input
                id="name"
                autoComplete="off"
                aria-invalid={!!form.formState.errors.name}
                {...form.register("name")}
              />
              <FieldError
                errors={
                  form.formState.errors.name
                    ? [form.formState.errors.name]
                    : undefined
                }
              />
            </Field>
            <Field data-invalid={!!form.formState.errors.slug}>
              <FieldLabel htmlFor="slug">Slug</FieldLabel>
              <Input
                id="slug"
                autoComplete="off"
                placeholder="acme-inc"
                aria-invalid={!!form.formState.errors.slug}
                {...form.register("slug")}
              />
              <FieldError
                errors={
                  form.formState.errors.slug
                    ? [form.formState.errors.slug]
                    : undefined
                }
              />
            </Field>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Creating…" : "Create organization"}
            </Button>
          </FieldGroup>
        </form>
      </CardContent>
    </Card>
  );
}
