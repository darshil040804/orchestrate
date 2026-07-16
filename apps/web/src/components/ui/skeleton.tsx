import { cn } from "@/lib/utils"

function Skeleton({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="skeleton"
      className={cn(
        "animate-[orchestrate-shimmer_1.6s_ease-in-out_infinite] rounded-md bg-[linear-gradient(90deg,var(--secondary),color-mix(in_srgb,var(--secondary)_55%,var(--border)),var(--secondary))] bg-[length:200%_100%] motion-reduce:animate-none",
        className
      )}
      {...props}
    />
  )
}

export { Skeleton }
