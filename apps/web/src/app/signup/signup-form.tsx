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
import { signup } from "@/lib/api/auth";
import { mapAuthError } from "@/lib/auth/errors";
import { signupSchema, type SignupValues } from "@/lib/auth/validation";

export function SignupForm() {
  const [serverErrorCode, setServerErrorCode] = useState<string | undefined>(
    undefined
  );
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const form = useForm<SignupValues>({
    resolver: zodResolver(signupSchema),
    defaultValues: { email: "", password: "", confirmPassword: "" },
  });

  const mutation = useMutation({
    mutationFn: signup,
    onSuccess: (data) => setSuccessMessage(data.message),
    onError: (err) => {
      setServerErrorCode(err instanceof ApiError ? err.code : undefined);
    },
  });

  const onSubmit = form.handleSubmit((values) => {
    setServerErrorCode(undefined);
    mutation.mutate({ email: values.email, password: values.password });
  });

  if (successMessage) {
    return (
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Check your email</CardTitle>
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

  return (
    <Card className="w-full max-w-sm">
      <CardHeader>
        <CardTitle>Sign up</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <form onSubmit={onSubmit} noValidate>
          <FieldGroup>
            {serverErrorCode && (
              <div className="flex flex-col gap-2">
                <ServerErrorBanner message={mapAuthError(serverErrorCode)} />
                {serverErrorCode === "EMAIL_ALREADY_EXISTS" && (
                  <Link href="/login" className="text-sm underline underline-offset-4">
                    Log in instead
                  </Link>
                )}
              </div>
            )}
            <Field data-invalid={!!form.formState.errors.email}>
              <FieldLabel htmlFor="email">Email</FieldLabel>
              <Input
                id="email"
                type="email"
                autoComplete="email"
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
            <Field data-invalid={!!form.formState.errors.password}>
              <FieldLabel htmlFor="password">Password</FieldLabel>
              <Input
                id="password"
                type="password"
                autoComplete="new-password"
                aria-invalid={!!form.formState.errors.password}
                {...form.register("password")}
              />
              <FieldError
                errors={
                  form.formState.errors.password
                    ? [form.formState.errors.password]
                    : undefined
                }
              />
            </Field>
            <Field data-invalid={!!form.formState.errors.confirmPassword}>
              <FieldLabel htmlFor="confirmPassword">Confirm password</FieldLabel>
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
              {mutation.isPending ? "Signing up…" : "Sign up"}
            </Button>
          </FieldGroup>
        </form>

        <p className="text-sm text-muted-foreground">
          Already have an account?{" "}
          <Link href="/login" className="underline underline-offset-4">
            Log in
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}
