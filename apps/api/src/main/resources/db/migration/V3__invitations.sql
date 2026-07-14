CREATE TABLE org_invitations (
    id                 UUID         PRIMARY KEY,
    organization_id    UUID         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    invited_email      VARCHAR(320) NOT NULL,          -- app lowercases before persist
    role               VARCHAR(20)  NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'APPROVER')),
    token_hash         VARCHAR(64)  NOT NULL UNIQUE,
    invited_by_user_id UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at         TIMESTAMPTZ  NOT NULL,
    used_at            TIMESTAMPTZ,   -- set when accepted
    revoked_at         TIMESTAMPTZ,   -- set when cancelled, or superseded by a reissue
    created_at         TIMESTAMPTZ  NOT NULL
);

-- At most one ACTIVE pending invitation per (org, email) -- DB-level backstop for the app-level
-- reissue-revokes-old logic in InvitationService.createInvitation. Same "app pre-check + DB
-- constraint as defense-in-depth" shape as organization_memberships' UNIQUE(organization_id, user_id).
-- This partial index also directly serves the "list pending invitations for org X" query (its WHERE
-- clause matches the index predicate exactly), so no separate plain index on organization_id is added
-- -- same reasoning as V2's comment on organization_memberships.
CREATE UNIQUE INDEX idx_org_invitations_active_pending
    ON org_invitations (organization_id, invited_email)
    WHERE used_at IS NULL AND revoked_at IS NULL;
