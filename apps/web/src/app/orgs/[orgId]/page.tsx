import type { Metadata } from "next";
import { OrgDetailView } from "./org-detail-view";

export const metadata: Metadata = { title: "Organization — Orchestrate" };

export default async function OrgDetailPage({
  params,
}: {
  params: Promise<{ orgId: string }>;
}) {
  const { orgId } = await params;

  return (
    <main className="flex min-h-screen flex-col items-center gap-4 p-24">
      <OrgDetailView orgId={orgId} />
    </main>
  );
}
