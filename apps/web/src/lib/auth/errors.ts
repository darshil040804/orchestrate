// Maps backend auth error codes (the {error,message} JSON shape's `error` field,
// plus the two ?error= query codes the OAuth redirect can carry) to user-facing
// copy. Deliberately one flat map for both channels — same "code -> copy" concern,
// no collision risk since the backend already namespaces OAuth codes with "oauth_".
//
// Must NOT be used for GET /api/auth/me failures: that endpoint returns a plain
// 401 with no {error,message} body when unauthenticated, and it isn't a
// user-facing error in the first place — see useCurrentUser, which resolves it to
// `user: null` instead of surfacing banner text.
const AUTH_ERROR_MESSAGES: Record<string, string> = {
  EMAIL_ALREADY_EXISTS: "An account with this email already exists.",
  EMAIL_NOT_VERIFIED: "Please verify your email before logging in.",
  INVALID_CREDENTIALS: "Incorrect email or password.",
  INVALID_TOKEN: "This link is invalid or has expired.",
  VALIDATION_ERROR: "Please check your input and try again.",
  oauth_unverified_email:
    "We couldn't verify your email with that provider. Please try a different sign-in method.",
  oauth_failed: "Sign-in failed. Please try again.",
};

export function mapAuthError(code: string | undefined): string {
  if (code && code in AUTH_ERROR_MESSAGES) {
    return AUTH_ERROR_MESSAGES[code];
  }
  return "Something went wrong. Please try again.";
}
