# Orchestrate design system

This documents what's actually shipped in `apps/web` today — read from the real source (`globals.css`
and `components/ui/*`), not aspirational. If something below stops matching the code, the code wins;
update this file instead.

## Colors

Every semantic role is a CSS custom property in `src/app/globals.css`, defined as `oklch(...)` in
`:root` (light) and re-declared in `.dark`. The `@theme inline { --color-x: var(--x); ... }` block
aliases each one into a Tailwind theme key (`--color-primary`, etc.) — that indirection is what lets
utilities like `bg-primary` resolve differently under `.dark` at all, since Tailwind's `@theme` values
are otherwise static at build time. The same pattern covers elevation: `--shadow-sm`/`--shadow-md` are
aliased from `--shadow-color-sm`/`--shadow-color-md` (named distinctly to avoid a self-referential
`--shadow-sm: var(--shadow-sm)`), which is why `<Card>` and other components can just use the stock
`shadow-sm` Tailwind class and get the right elevation per theme.

| Token | Light | Dark |
|---|---|---|
| `background` | `#ffffff` | `#0c0d10` |
| `foreground` | `#17181b` | `#f1f2f4` |
| `card` / `popover` | `#ffffff` | `#16171a` |
| `card-foreground` / `popover-foreground` | `#17181b` | `#f1f2f4` |
| `primary` | `#3366e2` | `#5085fb` |
| `primary-foreground` | `#fafafa` | `#fafafa` |
| `secondary` / `muted` / `accent` | `#f3f4f6` | `#222428` |
| `secondary-foreground` / `accent-foreground` | `#17181b` | `#f1f2f4` |
| `muted-foreground` | `#656971` | `#9ba0a9` |
| `destructive` | `#e40017` | `#ea3c3f` |
| `destructive-foreground` | `#fafafa` | `#fafafa` |
| `success` | `#0e9254` | `#2fa465` |
| `warning` | `#d28500` | `#df911a` |
| `border` / `input` | `#e4e5e8` | `#27292d` / `#2c2e32` |
| `ring` | `#3366e2` | `#5085fb` |

`success`/`warning` back `Badge`'s `success`/`warning` variants (tinted `bg-x/10` + `text-x`, mirroring
the existing `destructive` tint pattern) — there's no dedicated status-badge component beyond that.

## Typography

Deliberately minimal today — there is no large display/heading scale in any shipped page yet:

- **Body default**: `text-sm` (14px) / `leading-[1.5]`, set once globally in `globals.css`'s
  `@layer base` (`body { @apply ... text-sm leading-[1.5] ... }`). Nearly every page inherits this
  rather than setting its own font size.
- **Card title** (`CardTitle`): `text-base` (16px) / `font-medium`. The largest text size actually in
  use anywhere in the shipped app.
- **Muted / helper text**: `text-xs` (12px) or `text-sm` (14px) + `text-muted-foreground` — used for
  timestamps, role labels, field descriptions, empty-state copy.
- **Interactive labels** (buttons, badges, form labels): `text-[13px]`–`text-[10px]` at
  `font-medium`/`font-semibold`, sized per-component (see below).
- **Monospace**: `--font-mono` (Geist Mono) is wired as a token but **not used in any shipped page
  today** — noted honestly rather than invented.

## Spacing & radius

- `--radius: 0.5rem` (8px), with `--radius-sm/md/lg/xl` derived as `-2px/+0/+2px/+4px` from it.
  Components route through these Tailwind classes (`rounded-md`, `rounded-lg`, `rounded-xl`) rather
  than hardcoding pixel radii — this was fixed this session; a few components previously had a
  hardcoded `rounded-[7px]` that didn't track `--radius` at all.
- `Card` padding is a local `--card-spacing` custom property: `--spacing(6)` (24px) by default,
  `--spacing(5)` (20px) at `size="sm"`.
- No other page-level spacing scale is formalized; layouts use ordinary Tailwind gap/padding
  utilities per page.

## Component conventions

- **Button** (`components/ui/button.tsx`): heights — `sm` 32px, default 36px, `lg` 44px, `icon` 36px
  square (`icon-xs`/`icon-sm`/`icon-lg` also exist at 24/28/36px for denser contexts). Variants:
  `default` (solid primary), `outline`, `secondary`, `ghost`, `destructive`, `link`. `rounded-md`
  throughout.
- **Card** (`components/ui/card.tsx`): 1px `border-border`, `shadow-sm` at rest, `rounded-xl`, 24px
  padding (20px at `size="sm"`). `CardHeader`/`CardTitle`/`CardDescription`/`CardAction`/`CardContent`/
  `CardFooter` sub-parts.
- **Input** (`components/ui/input.tsx`) / **Select trigger** (`components/ui/select.tsx`): 40px height
  (`sm` Select trigger is 32px), `rounded-md`, `border-input`, 2px focus ring + offset.
- **Tabs** (`components/ui/tabs.tsx`): the `line` (underline) variant is the *default* — active tab
  gets a 2px `bg-primary` bottom indicator, not a filled pill. A `default` (filled-pill) variant also
  exists but isn't the default.
- **Select / DropdownMenu / AlertDialog content** (`select.tsx`, `dropdown-menu.tsx`,
  `alert-dialog.tsx`): share one popover surface convention — `bg-popover`, `border-border`,
  `rounded-lg` (`AlertDialog`'s outer surface is `rounded-xl`), and one ad hoc elevated shadow
  (`shadow-[0_12px_32px_rgb(0_0_0_/_12%)]`) reused identically across all three — a project
  convention, not a named CSS token.
- **Badge** (`components/ui/badge.tsx`): 23px tall, `rounded-full`, `text-[10px] font-semibold`.
  Variants: `default`, `secondary`, `destructive`, `success`, `warning`, `outline`, `ghost`, `link`.
  The tinted variants (`destructive`/`success`/`warning`) all use the same `bg-x/10 text-x
  dark:bg-x/20` pattern.
- **Avatar** (`components/ui/avatar.tsx`): default 36px, `sm` 24px, `lg` 40px, `rounded-full`.
  `AvatarFallback` renders initials on `bg-accent`.
- **Skeleton** (`components/ui/skeleton.tsx`): a 1.6s shimmer animation over a
  `--secondary`→`--border` gradient (`--secondary` and `--muted` are numerically identical today, so
  this reads the same as a `--muted`-based shimmer would).
- **Toast** (`components/ui/sonner.tsx`): theme is bound live to `next-themes`' `useTheme()` (verified
  end-to-end in both light/dark this session — `data-sonner-theme` flips correctly), not hardcoded
  and not omitted. Surface styled via `--popover`/`--border`/`--radius` CSS vars passed to Sonner.

## Marketing / page-level patterns

Introduced with the "/" marketing landing and "/app" authenticated home — new at the page level,
not (yet) formalized as reusable component variants:

- **Marketing hero**: a mono eyebrow line (`text-xs`, `tracking-[0.04em]`, `text-muted-foreground`),
  a headline at `text-[clamp(34px,5vw,52px)] leading-[1.05] font-semibold tracking-[-0.025em]`, and a
  subheading at `text-[17px] leading-[1.6] text-muted-foreground max-w-[56ch]`. This is the only place
  in the app with type larger than `CardTitle`'s 16px — it exists solely for the hero, not as a
  general "page title" utility yet.
- **Topbar** (`components/shared/topbar.tsx`): a new shared shell — sticky, `bg-background/80` +
  `backdrop-blur-md`, 1px `border-b`, `Logo` pinned left (configurable `logoHref`), `nav`/`actions`
  slots, `ThemeControl` always rendered last. Used by both `/` and `/app`.
- **Empty state** (used on `/app` when the user has zero organizations): a centered `Card` — an
  icon in a `size-11 rounded-md bg-muted` box, one `text-[15px] font-semibold` heading, one
  `text-[13px] text-muted-foreground max-w-[34ch]` sentence, one button. Deliberately no
  illustration and no more than one action.
