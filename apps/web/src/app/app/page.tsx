import type { Metadata } from "next";
import { AppHomeView } from "./app-home-view";

export const metadata: Metadata = { title: "Home — Orchestrate" };

export default function AppHomePage() {
  return (
    <main className="flex min-h-screen flex-col">
      <AppHomeView />
    </main>
  );
}
