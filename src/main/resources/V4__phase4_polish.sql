-- ============================================================
-- V4__phase4_polish.sql — Phase 4: Roles Polish
-- Idempotent script for Role Audit, Record Shares, and Workspace Settings
-- ============================================================

-- 1. Add JSONB settings column to workspaces table
ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS settings JSONB DEFAULT '{}'::jsonb NOT NULL;

-- 2. Role Audit Table
CREATE TABLE IF NOT EXISTS role_audits (
    id                BIGSERIAL   PRIMARY KEY,
    workspace_id      BIGINT      NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    actor_id          BIGINT      REFERENCES auth_accounts(id) ON DELETE SET NULL, -- who made the change
    target_account_id BIGINT      NOT NULL REFERENCES auth_accounts(id) ON DELETE CASCADE, -- whose role changed
    old_role          VARCHAR(32), -- can be null if it's a new assignment
    new_role          VARCHAR(32), -- can be null if role was removed (membership deleted)
    reason            VARCHAR(500), -- optional reason given or auto-generated
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_role_audits_workspace ON role_audits(workspace_id);
CREATE INDEX IF NOT EXISTS idx_role_audits_target    ON role_audits(target_account_id);

-- 3. Record Shares Table (for Guest access)
CREATE TABLE IF NOT EXISTS record_shares (
    id                      BIGSERIAL   PRIMARY KEY,
    workspace_id            BIGINT      NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    resource_type           VARCHAR(32) NOT NULL, -- e.g., 'OPPORTUNITY'
    resource_id             UUID        NOT NULL, -- polymorphic reference to the actual record
    shared_with_account_id  BIGINT      NOT NULL REFERENCES auth_accounts(id) ON DELETE CASCADE,
    permission              VARCHAR(32) NOT NULL DEFAULT 'READ', -- 'READ', 'COMMENT'
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_record_share UNIQUE (workspace_id, resource_type, resource_id, shared_with_account_id)
);

CREATE INDEX IF NOT EXISTS idx_record_shares_account ON record_shares(shared_with_account_id);
CREATE INDEX IF NOT EXISTS idx_record_shares_resource ON record_shares(resource_type, resource_id);
