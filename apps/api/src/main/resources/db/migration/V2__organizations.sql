-- Phase 1c: organizations + membership-based RBAC.
-- Schema is authoritative; JPA runs with ddl-auto=validate, so entities must match exactly.

CREATE TABLE organizations (
    id         UUID         PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    slug       VARCHAR(50)  NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE organization_memberships (
    id              UUID        PRIMARY KEY,
    organization_id UUID        NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'APPROVER')),
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    UNIQUE (organization_id, user_id)
);
-- No standalone index on organization_id: UNIQUE(organization_id, user_id) already gives a
-- composite btree index with organization_id as the leading column, which serves "all members
-- of org X" lookups directly — a separate single-column index would be pure write overhead.
CREATE INDEX idx_organization_memberships_user_id ON organization_memberships (user_id);
