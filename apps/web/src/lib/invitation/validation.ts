import { z } from "zod";

export const roleOptions = ["OWNER", "ADMIN", "MEMBER", "APPROVER"] as const;

// Mirrors CreateInvitationRequest exactly (@Email @NotBlank email, @NotNull role).
export const createInvitationSchema = z.object({
  email: z.email({ error: "Enter a valid email address" }),
  role: z.enum(roleOptions, { error: "Select a role" }),
});
export type CreateInvitationValues = z.infer<typeof createInvitationSchema>;
