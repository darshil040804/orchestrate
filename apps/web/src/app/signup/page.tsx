import type { Metadata } from "next";
import { Topbar } from "@/components/shared/topbar";
import { SignupForm } from "./signup-form";

export const metadata: Metadata = { title: "Sign up — Orchestrate" };

export default function SignupPage() {
  return (
    <div className="flex min-h-screen flex-col">
      <Topbar logoHref="/" />
      <main className="flex flex-1 flex-col items-center justify-center p-8">
        <SignupForm />
      </main>
    </div>
  );
}
