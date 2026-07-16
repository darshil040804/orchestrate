"use client"

import { useSyncExternalStore } from "react"
import { useTheme } from "next-themes"

import { cn } from "@/lib/utils"

const themes = ["light", "dark", "system"] as const

const emptySubscribe = () => () => undefined

export function ThemeControl({ className }: { className?: string }) {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false)
  const { theme, setTheme } = useTheme()

  return (
    <div
      aria-label="Color theme"
      className={cn(
        "inline-flex h-8 items-center rounded-lg border border-border bg-secondary p-[3px]",
        className
      )}
      role="group"
    >
      {themes.map((value) => {
        const selected = mounted && theme === value

        return (
          <button
            key={value}
            type="button"
            aria-pressed={selected}
            className={cn(
              "h-6 rounded-[5px] px-2 text-[11px] font-medium capitalize text-muted-foreground transition-colors duration-150 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring",
              selected && "bg-card text-foreground shadow-sm"
            )}
            onClick={() => setTheme(value)}
          >
            {value}
          </button>
        )
      })}
    </div>
  )
}
