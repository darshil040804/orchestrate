import type { Metadata } from "next";
import { SignupForm } from "./signup-form";

export const metadata: Metadata = { title: "Sign up — Orchestrate" };

export default function SignupPage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24">
      <SignupForm />
    </main>
  );
}
