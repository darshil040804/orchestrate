// Maps backend invitation error codes (GlobalExceptionHandler's {error,message}
// shape) to user-facing copy. Independently declared rather than reusing
// lib/org/errors.ts, mirroring that file's own precedent — copy can legitimately
// differ per context even for a shared code (e.g. MEMBERSHIP_ALREADY_EXISTS here
// is first-person, since it's the accepting user's own membership being checked).
const INVITATION_ERROR_MESSAGES: Record<string, string> = {
  INVITATION_NOT_FOUND: "This invitation could not be found.",
  INVITATION_EMAIL_MISMATCH: "This invitation was sent to a different email address.",
  INVALID_TOKEN: "This invitation link is invalid or has expired.",
  MEMBERSHIP_ALREADY_EXISTS: "You're already a member of this organization.",
  OWNER_ACTION_REQUIRED: "Only an organization owner can perform this action.",
  ACCESS_DENIED: "You do not have permission to perform this action.",
  VALIDATION_ERROR: "Please check your input and try again.",
  CONFLICT: "That change conflicts with another update. Please try again.",
};

export function mapInvitationError(code: string | undefined): string {
  if (code && code in INVITATION_ERROR_MESSAGES) {
    return INVITATION_ERROR_MESSAGES[code];
  }
  return "Something went wrong. Please try again.";
}
