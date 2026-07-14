import type { Metadata } from "next";
import { ResetConfirmForm } from "./reset-confirm-form";

export const metadata: Metadata = { title: "Reset password — Orchestrate" };

export default async function ResetConfirmPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token } = await searchParams;

  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24">
      <ResetConfirmForm token={token} />
    </main>
  );
}
