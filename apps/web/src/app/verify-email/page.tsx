import type { Metadata } from "next";
import { Topbar } from "@/components/shared/topbar";
import { VerifyEmailView } from "./verify-email-view";

export const metadata: Metadata = { title: "Verify email — Orchestrate" };

export default async function VerifyEmailPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token } = await searchParams;

  return (
    <div className="flex min-h-screen flex-col">
      <Topbar logoHref="/" />
      <main className="flex flex-1 flex-col items-center justify-center p-8">
        <VerifyEmailView token={token} />
      </main>
    </div>
  );
}
