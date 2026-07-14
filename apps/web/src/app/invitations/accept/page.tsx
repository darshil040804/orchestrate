import type { Metadata } from "next";
import { AcceptInvitationView } from "./accept-invitation-view";

export const metadata: Metadata = { title: "Accept invitation — Orchestrate" };

export default async function AcceptInvitationPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token } = await searchParams;

  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24">
      <AcceptInvitationView token={token} />
    </main>
  );
}
