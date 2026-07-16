import type { Metadata } from "next";
import { Topbar } from "@/components/shared/topbar";
import { ResetRequestForm } from "./reset-request-form";

export const metadata: Metadata = { title: "Reset password — Orchestrate" };

export default function ResetRequestPage() {
  return (
    <div className="flex min-h-screen flex-col">
      <Topbar logoHref="/" />
      <main className="flex flex-1 flex-col items-center justify-center p-8">
        <ResetRequestForm />
      </main>
    </div>
  );
}
