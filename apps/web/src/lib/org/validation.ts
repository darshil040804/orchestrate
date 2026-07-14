import { z } from "zod";

// Mirrors CreateOrganizationRequest exactly (name: @NotBlank @Size(max=255),
// slug: @NotBlank @Size(min=3,max=50) @Pattern(regexp="^[a-z0-9]+(-[a-z0-9]+)*$")).
export const createOrgSchema = z.object({
  name: z
    .string()
    .min(1, { error: "Name is required" })
    .max(255, { error: "Name must be at most 255 characters" }),
  slug: z
    .string()
    .min(3, { error: "Slug must be at least 3 characters" })
    .max(50, { error: "Slug must be at most 50 characters" })
    .regex(/^[a-z0-9]+(-[a-z0-9]+)*$/, {
      error:
        "Slug must be lowercase letters/digits, single hyphens, no leading/trailing hyphen",
    }),
});
export type CreateOrgValues = z.infer<typeof createOrgSchema>;
