import type { Metadata } from "next";
import { LoginForm } from "./login-form";

export const metadata: Metadata = { title: "Log in — Orchestrate" };

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ returnTo?: string; error?: string }>;
}) {
  const { returnTo, error } = await searchParams;

  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24">
      <LoginForm returnTo={returnTo} oauthError={error} />
    </main>
  );
}
