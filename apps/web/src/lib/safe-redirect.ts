/**
 * Validates that a `returnTo` value is a safe same-origin relative path before
 * using it in a client-side redirect, preventing an open-redirect via a
 * crafted `?returnTo=` query value.
 *
 * Resolves against the actual origin (rather than a string-prefix blocklist
 * like rejecting a leading "//") because the WHATWG URL spec normalizes a
 * leading backslash to a forward slash for special schemes (http/https) —
 * "/\evil.com" passes a "//" check but still resolves to a foreign origin,
 * and Next.js's router falls back to a real `location.assign` for any URL it
 * can't handle as a client-side route, completing an open redirect. Resolving
 * through the same URL parser the browser/router will actually use closes
 * this and any other normalization-based bypass by construction.
 */
export function isSafeReturnTo(
  value: string | undefined | null
): value is string {
  if (!value) return false;
  try {
    return new URL(value, window.location.origin).origin === window.location.origin;
  } catch {
    return false;
  }
}
