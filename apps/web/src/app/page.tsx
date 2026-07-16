import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Topbar } from "@/components/shared/topbar";

const FEATURES = [
  {
    title: "Trigger → AI classification",
    description:
      "An incoming event is classified by an AI node — a recommendation, not a decision.",
  },
  {
    title: "Human approval on consequential actions",
    description:
      "The AI recommends; a person decides before anything irreversible happens. Validation gates AI output before it can affect routing.",
  },
  {
    title: "Full execution audit trail",
    description:
      "Every trigger, classification, and approval is recorded — a transparent, reviewable history of what happened and why.",
  },
  {
    title: "Organizations & role-based access",
    description:
      "Multi-tenant organizations with OWNER/ADMIN/MEMBER/APPROVER roles and email invitations — shipped today.",
  },
];

export default function Home() {
  return (
    <div className="flex min-h-screen flex-col">
      <Topbar
        logoHref="/"
        nav={
          <Link
            href="#features"
            className="text-sm text-muted-foreground hover:text-foreground"
          >
            Features
          </Link>
        }
        actions={
          <>
            <Link
              href="/login"
              className={buttonVariants({ variant: "outline", size: "sm" })}
            >
              Log in
            </Link>
            <Link href="/signup" className={buttonVariants({ size: "sm" })}>
              Sign up
            </Link>
          </>
        }
      />

      <main className="flex-1">
        <section className="mx-auto max-w-3xl px-8 py-24 text-center">
          <p className="mb-5 font-mono text-xs tracking-[0.04em] text-muted-foreground">
            TRIGGER · AI CLASSIFICATION · HUMAN APPROVAL · AUDIT TRAIL
          </p>
          <h1 className="text-[clamp(34px,5vw,52px)] leading-[1.05] font-semibold tracking-[-0.025em] text-balance">
            Automate the routine. Keep a human in the loop for what matters.
          </h1>
          <p className="mx-auto mt-5 max-w-[56ch] text-[17px] leading-[1.6] text-muted-foreground">
            Orchestrate turns operational triggers into AI-assisted workflows —
            an AI node classifies, a person approves anything consequential,
            and every step is recorded from start to finish.
          </p>
          <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
            <Link href="/signup" className={buttonVariants({ size: "lg" })}>
              Get started
            </Link>
            <Link
              href="/login"
              className={buttonVariants({ variant: "outline", size: "lg" })}
            >
              Log in
            </Link>
          </div>
        </section>

        <section
          id="features"
          className="mx-auto max-w-5xl border-t border-border px-8 py-16"
        >
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {FEATURES.map((feature) => (
              <Card key={feature.title}>
                <CardHeader>
                  <CardTitle>{feature.title}</CardTitle>
                  <CardDescription>{feature.description}</CardDescription>
                </CardHeader>
              </Card>
            ))}
          </div>
        </section>
      </main>

      <footer className="border-t border-border px-8 py-6 text-center text-xs text-muted-foreground">
        © {new Date().getFullYear()} Orchestrate. A portfolio project.
      </footer>
    </div>
  );
}
