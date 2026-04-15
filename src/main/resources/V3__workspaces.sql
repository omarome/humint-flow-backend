-- ============================================================
-- V3__workspaces.sql — Phase 3: Multi-workspace schema
-- Idempotent: safe to run multiple times via spring.sql.init
-- ============================================================

-- 1. Workspaces table
CREATE TABLE IF NOT EXISTS workspaces (
    id          BIGSERIAL    PRIMARY KEY,
    slug        VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    created_by  BIGINT       REFERENCES auth_accounts(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 2. Per-workspace membership table (carries the workspace-scoped role)
CREATE TABLE IF NOT EXISTS workspace_memberships (
    id           BIGSERIAL   PRIMARY KEY,
    workspace_id BIGINT      NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    account_id   BIGINT      NOT NULL REFERENCES auth_accounts(id) ON DELETE CASCADE,
    role         VARCHAR(32) NOT NULL DEFAULT 'SALES_REP',
    joined_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_workspace_account UNIQUE (workspace_id, account_id)
);
CREATE INDEX IF NOT EXISTS idx_membership_account    ON workspace_memberships(account_id);
CREATE INDEX IF NOT EXISTS idx_membership_workspace  ON workspace_memberships(workspace_id);

-- 3. Add workspace_id FK column to CRM tables (nullable first)
ALTER TABLE organizations    ADD COLUMN IF NOT EXISTS workspace_id BIGINT REFERENCES workspaces(id);
ALTER TABLE contacts         ADD COLUMN IF NOT EXISTS workspace_id BIGINT REFERENCES workspaces(id);
ALTER TABLE opportunities    ADD COLUMN IF NOT EXISTS workspace_id BIGINT REFERENCES workspaces(id);
ALTER TABLE activities       ADD COLUMN IF NOT EXISTS workspace_id BIGINT REFERENCES workspaces(id);
ALTER TABLE automation_rules ADD COLUMN IF NOT EXISTS workspace_id BIGINT REFERENCES workspaces(id);
ALTER TABLE saved_views      ADD COLUMN IF NOT EXISTS workspace_id BIGINT REFERENCES workspaces(id);

-- 4. Seed Default Workspace (idempotent)
INSERT INTO workspaces (slug, name, created_at, updated_at)
    VALUES ('default', 'Default Workspace', now(), now())
    ON CONFLICT (slug) DO NOTHING;

-- 5. Back-fill all existing CRM rows to the Default Workspace
    UPDATE organizations    SET workspace_id = (SELECT id FROM workspaces WHERE slug = 'default') WHERE workspace_id IS NULL;
    UPDATE contacts         SET workspace_id = (SELECT id FROM workspaces WHERE slug = 'default') WHERE workspace_id IS NULL;
    UPDATE opportunities    SET workspace_id = (SELECT id FROM workspaces WHERE slug = 'default') WHERE workspace_id IS NULL;
    UPDATE activities       SET workspace_id = (SELECT id FROM workspaces WHERE slug = 'default') WHERE workspace_id IS NULL;
    UPDATE automation_rules SET workspace_id = (SELECT id FROM workspaces WHERE slug = 'default') WHERE workspace_id IS NULL;
    UPDATE saved_views      SET workspace_id = (SELECT id FROM workspaces WHERE slug = 'default') WHERE workspace_id IS NULL;

-- 6. Seed all existing auth_accounts into Default Workspace (preserve their global role)
INSERT INTO workspace_memberships (workspace_id, account_id, role, joined_at)
    SELECT w.id, a.id, a.role, now()
    FROM   workspaces w
    JOIN   auth_accounts a ON 1 = 1
    WHERE  w.slug = 'default'
    ON CONFLICT (workspace_id, account_id) DO NOTHING;

-- 7. Enforce NOT NULL on the mandatory CRM tables after back-fill
ALTER TABLE organizations  ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE contacts       ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE opportunities  ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE activities     ALTER COLUMN workspace_id SET NOT NULL;

-- automation_rules and saved_views stay nullable (workspace-scoped but not always present)

-- 8. Performance indexes for workspace-scoped queries
CREATE INDEX IF NOT EXISTS idx_org_workspace   ON organizations(workspace_id)  WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_con_workspace   ON contacts(workspace_id)       WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_opp_workspace   ON opportunities(workspace_id)  WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_act_workspace   ON activities(workspace_id);
