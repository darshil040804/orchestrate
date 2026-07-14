import type { Metadata } from "next";
import { OrgsView } from "./orgs-view";

export const metadata: Metadata = { title: "Organizations — Orchestrate" };

export default function OrgsPage() {
  return (
    <main className="flex min-h-screen flex-col items-center gap-4 p-24">
      <OrgsView />
    </main>
  );
}
