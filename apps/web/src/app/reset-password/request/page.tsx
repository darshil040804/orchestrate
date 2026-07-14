import type { Metadata } from "next";
import { ResetRequestForm } from "./reset-request-form";

export const metadata: Metadata = { title: "Reset password — Orchestrate" };

export default function ResetRequestPage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24">
      <ResetRequestForm />
    </main>
  );
}
