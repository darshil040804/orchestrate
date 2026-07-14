// Maps backend org error codes (GlobalExceptionHandler's {error,message} shape)
// to user-facing copy. Mirrors lib/auth/errors.ts's exact pattern.
const ORG_ERROR_MESSAGES: Record<string, string> = {
  ACCESS_DENIED: "You do not have permission to perform this action.",
  SLUG_ALREADY_EXISTS: "That slug is already taken. Try a different one.",
  ORGANIZATION_NOT_FOUND: "This organization could not be found.",
  MEMBERSHIP_NOT_FOUND: "This member could not be found.",
  USER_NOT_FOUND: "That user could not be found.",
  MEMBERSHIP_ALREADY_EXISTS: "That user is already a member of this organization.",
  LAST_OWNER_REQUIRED: "An organization must always have at least one owner.",
  OWNER_ACTION_REQUIRED: "Only an organization owner can perform this action.",
  VALIDATION_ERROR: "Please check your input and try again.",
  CONFLICT: "That change conflicts with another update. Please try again.",
};

export function mapOrgError(code: string | undefined): string {
  if (code && code in ORG_ERROR_MESSAGES) {
    return ORG_ERROR_MESSAGES[code];
  }
  return "Something went wrong. Please try again.";
}
