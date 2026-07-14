"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Field, FieldLabel, FieldError, FieldGroup } from "@/components/ui/field";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { ServerErrorBanner } from "@/components/shared/server-error-banner";
import { OAuthButtons } from "@/components/auth/oauth-buttons";
import { ApiError } from "@/lib/api/client";
import { login } from "@/lib/api/auth";
import { mapAuthError } from "@/lib/auth/errors";
import { loginSchema, type LoginValues } from "@/lib/auth/validation";

export function LoginForm() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [serverError, setServerError] = useState<string | null>(null);

  const form = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "", password: "" },
  });

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      queryClient.setQueryData(["auth", "me"], data);
      router.push("/");
    },
    onError: (err) => {
      setServerError(
        err instanceof ApiError
          ? mapAuthError(err.code)
          : "Something went wrong. Please try again."
      );
    },
  });

  const onSubmit = form.handleSubmit((values) => {
    setServerError(null);
    mutation.mutate(values);
  });

  return (
    <Card className="w-full max-w-sm">
      <CardHeader>
        <CardTitle>Log in</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <form onSubmit={onSubmit} noValidate>
          <FieldGroup>
            {serverError && <ServerErrorBanner message={serverError} />}
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
                autoComplete="current-password"
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
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Logging in…" : "Log in"}
            </Button>
          </FieldGroup>
        </form>

        <Separator />
        <OAuthButtons />

        <div className="flex flex-col gap-1 text-sm text-muted-foreground">
          <span>
            Don&apos;t have an account?{" "}
            <Link href="/signup" className="underline underline-offset-4">
              Sign up
            </Link>
          </span>
          <Link href="/reset-password/request" className="underline underline-offset-4">
            Forgot your password?
          </Link>
        </div>
      </CardContent>
    </Card>
  );
}
