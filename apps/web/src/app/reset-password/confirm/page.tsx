import type { Metadata } from "next";
import { Topbar } from "@/components/shared/topbar";
import { ResetConfirmForm } from "./reset-confirm-form";

export const metadata: Metadata = { title: "Reset password — Orchestrate" };

export default async function ResetConfirmPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token } = await searchParams;

  return (
    <div className="flex min-h-screen flex-col">
      <Topbar logoHref="/" />
      <main className="flex flex-1 flex-col items-center justify-center p-8">
        <ResetConfirmForm token={token} />
      </main>
    </div>
  );
}
