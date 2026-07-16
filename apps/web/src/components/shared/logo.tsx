import Image from "next/image"
import Link from "next/link"

import { cn } from "@/lib/utils"

type LogoProps = {
  compact?: boolean
  href?: string
  className?: string
  priority?: boolean
}

export function Logo({
  compact = false,
  href,
  className,
  priority = false,
}: LogoProps) {
  const content = (
    <>
      <Image
        src="/orchestrate-mark.svg"
        alt=""
        width={32}
        height={32}
        priority={priority}
        aria-hidden="true"
      />
      {!compact && (
        <span className="text-[15px] font-semibold tracking-[-0.02em] text-foreground">
          Orchestrate
        </span>
      )}
    </>
  )

  const classes = cn("inline-flex items-center gap-2.5", className)

  if (href) {
    return (
      <Link href={href} className={classes} aria-label={compact ? "Orchestrate" : undefined}>
        {content}
      </Link>
    )
  }

  return (
    <span className={classes} aria-label={compact ? "Orchestrate" : undefined}>
      {content}
    </span>
  )
}
