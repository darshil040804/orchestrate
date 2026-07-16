import type { Metadata } from "next";
import { Topbar } from "@/components/shared/topbar";
import { AcceptInvitationView } from "./accept-invitation-view";

export const metadata: Metadata = { title: "Accept invitation — Orchestrate" };

export default async function AcceptInvitationPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token } = await searchParams;

  return (
    <div className="flex min-h-screen flex-col">
      <Topbar logoHref="/" />
      <main className="flex flex-1 flex-col items-center justify-center p-8">
        <AcceptInvitationView token={token} />
      </main>
    </div>
  );
}
