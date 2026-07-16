import type { Metadata } from "next";
import { Topbar } from "@/components/shared/topbar";
import { LoginForm } from "./login-form";

export const metadata: Metadata = { title: "Log in — Orchestrate" };

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ returnTo?: string; error?: string }>;
}) {
  const { returnTo, error } = await searchParams;

  return (
    <div className="flex min-h-screen flex-col">
      <Topbar logoHref="/" />
      <main className="flex flex-1 flex-col items-center justify-center p-8">
        <LoginForm returnTo={returnTo} oauthError={error} />
      </main>
    </div>
  );
}
