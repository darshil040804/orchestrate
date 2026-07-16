import type { ReactNode } from "react";
import { Logo } from "@/components/shared/logo";
import { ThemeControl } from "@/components/shared/theme-control";

export function Topbar({
  logoHref = "/",
  nav,
  actions,
}: {
  logoHref?: string;
  nav?: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <header className="sticky top-0 z-20 flex items-center justify-between gap-4 border-b border-border bg-background/80 px-8 py-3.5 backdrop-blur-md">
      <div className="flex items-center gap-8">
        <Logo href={logoHref} />
        {nav}
      </div>
      <div className="flex items-center gap-3">
        {actions}
        <ThemeControl />
      </div>
    </header>
  );
}
