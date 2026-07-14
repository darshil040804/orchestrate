import type { Metadata } from "next";
import { VerifyEmailView } from "./verify-email-view";

export const metadata: Metadata = { title: "Verify email — Orchestrate" };

export default async function VerifyEmailPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token } = await searchParams;

  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24">
      <VerifyEmailView token={token} />
    </main>
  );
}
