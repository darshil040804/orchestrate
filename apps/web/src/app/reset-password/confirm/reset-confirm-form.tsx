"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import Link from "next/link";
import { Button, buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Field, FieldLabel, FieldError, FieldGroup } from "@/components/ui/field";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ServerErrorBanner } from "@/components/auth/server-error-banner";
import { ApiError } from "@/lib/api/client";
import { confirmPasswordReset } from "@/lib/api/auth";
import { mapAuthError } from "@/lib/auth/errors";
import { resetConfirmSchema, type ResetConfirmValues } from "@/lib/auth/validation";

export function ResetConfirmForm({ token }: { token?: string }) {
  const [serverErrorCode, setServerErrorCode] = useState<string | undefined>(
    undefined
  );
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const form = useForm<ResetConfirmValues>({
    resolver: zodResolver(resetConfirmSchema),
    defaultValues: { newPassword: "", confirmPassword: "" },
  });

  const mutation = useMutation({
    mutationFn: confirmPasswordReset,
    onSuccess: (data) => setSuccessMessage(data.message),
    onError: (err) => {
      setServerErrorCode(err instanceof ApiError ? err.code : undefined);
    },
  });

  if (!token) {
    return (
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Reset password</CardTitle>
        </CardHeader>
        <CardContent>
          <ServerErrorBanner message="This link is invalid." />
        </CardContent>
      </Card>
    );
  }

  if (successMessage) {
    return (
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Password updated</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <p>{successMessage}</p>
          <Link href="/login" className={buttonVariants({ variant: "outline" })}>
            Go to login
          </Link>
        </CardContent>
      </Card>
    );
  }

  const onSubmit = form.handleSubmit((values) => {
    setServerErrorCode(undefined);
    mutation.mutate({ token, newPassword: values.newPassword });
  });

  return (
    <Card className="w-full max-w-sm">
      <CardHeader>
        <CardTitle>Choose a new password</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <form onSubmit={onSubmit} noValidate>
          <FieldGroup>
            {serverErrorCode && (
              <div className="flex flex-col gap-2">
                <ServerErrorBanner message={mapAuthError(serverErrorCode)} />
                {serverErrorCode === "INVALID_TOKEN" && (
                  <Link
                    href="/reset-password/request"
                    className="text-sm underline underline-offset-4"
                  >
                    Request a new link
                  </Link>
                )}
              </div>
            )}
            <Field data-invalid={!!form.formState.errors.newPassword}>
              <FieldLabel htmlFor="newPassword">New password</FieldLabel>
              <Input
                id="newPassword"
                type="password"
                autoComplete="new-password"
                aria-invalid={!!form.formState.errors.newPassword}
                {...form.register("newPassword")}
              />
              <FieldError
                errors={
                  form.formState.errors.newPassword
                    ? [form.formState.errors.newPassword]
                    : undefined
                }
              />
            </Field>
            <Field data-invalid={!!form.formState.errors.confirmPassword}>
              <FieldLabel htmlFor="confirmPassword">Confirm new password</FieldLabel>
              <Input
                id="confirmPassword"
                type="password"
                autoComplete="new-password"
                aria-invalid={!!form.formState.errors.confirmPassword}
                {...form.register("confirmPassword")}
              />
              <FieldError
                errors={
                  form.formState.errors.confirmPassword
                    ? [form.formState.errors.confirmPassword]
                    : undefined
                }
              />
            </Field>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Updating…" : "Update password"}
            </Button>
          </FieldGroup>
        </form>
      </CardContent>
    </Card>
  );
}
