import { z } from "zod";

// Mirrors the backend's @MaxUtf8Bytes(72) constraint (BCrypt truncates/misbehaves
// past 72 UTF-8 bytes) — zod has no built-in byte-length check.
const passwordSchema = z
  .string()
  .min(8, { error: "Password must be at least 8 characters" })
  .refine((value) => new TextEncoder().encode(value).length <= 72, {
    error: "Password must be at most 72 bytes",
  });

export const signupSchema = z
  .object({
    email: z.email({ error: "Enter a valid email address" }),
    password: passwordSchema,
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    error: "Passwords do not match",
    path: ["confirmPassword"],
  });
export type SignupValues = z.infer<typeof signupSchema>;

// Login intentionally has no length/byte rule on password, matching the backend's
// LoginRequest DTO exactly (@NotBlank only) — a weaker client rule here would
// reject legitimate existing passwords that predate any future policy change.
export const loginSchema = z.object({
  email: z.email({ error: "Enter a valid email address" }),
  password: z.string().min(1, { error: "Password is required" }),
});
export type LoginValues = z.infer<typeof loginSchema>;

export const resetRequestSchema = z.object({
  email: z.email({ error: "Enter a valid email address" }),
});
export type ResetRequestValues = z.infer<typeof resetRequestSchema>;

export const resetConfirmSchema = z
  .object({
    newPassword: passwordSchema,
    confirmPassword: z.string(),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    error: "Passwords do not match",
    path: ["confirmPassword"],
  });
export type ResetConfirmValues = z.infer<typeof resetConfirmSchema>;
