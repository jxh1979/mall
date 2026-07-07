-- V265 集中式数据清理工具（数据删除四层之第四层：Tier-D 集中清理入口）。
-- 用户反馈(2026-07-06)："只看到存档没看到删除"——既有删除藏在"每实体逐条存档→申请删除→他人审批"多步里太隐蔽，
--   本层给运营一个【集中入口】批量清理旧/测试/垃圾数据，仍带安全闸（预览 dry-run + 审批 + 禁自审 + 留痕）。
--
-- 安全边界（诚实）：本工具只清理【已存档（archived_at 非空）超过 N 天】的配置类实体行，绝不删活数据；
--   执行前逐行再校验"仍已存档 + 无活引用"，有活引用的行跳过不删（照 V255/V259/V260/V262 删除范式的引用闸）。
-- 覆盖范围随既有存档能力：entitlement_templates(V255)/merchants(V259)/ticket_types+ticket_price_rules(V260)/
--   bar_products+bar_product_categories+bar_combos(V262)。
--
-- 台账表 data_cleanup_jobs：平台级操作台账（global_reference），非租户数据边界；
--   记 谁发起/为何/预览命中/谁审批/删了几条，全程可追溯。
-- 状态机：dry_run（仅预览未删）->pending_approval（提交审批）->approved->executed（真删完）/rejected/cancelled。

CREATE TABLE IF NOT EXISTS data_cleanup_jobs (
    id             VARCHAR(64)  PRIMARY KEY,
    scope_type     VARCHAR(48)  NOT NULL,
    filter_params  JSONB        NOT NULL DEFAULT '{}'::jsonb,
    status         VARCHAR(24)  NOT NULL DEFAULT 'dry_run',
    preview_count  INTEGER      NOT NULL DEFAULT 0,
    preview_sample JSONB        NOT NULL DEFAULT '[]'::jsonb,
    reason         TEXT,
    requested_by   VARCHAR(64),
    requested_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    approved_by    VARCHAR(64),
    approved_at    TIMESTAMPTZ,
    executed_at    TIMESTAMPTZ,
    deleted_count  INTEGER,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_data_cleanup_scope_type') THEN
        ALTER TABLE data_cleanup_jobs
            ADD CONSTRAINT chk_data_cleanup_scope_type
            CHECK (scope_type IN (
                'archived_entitlement_templates',
                'archived_merchants',
                'archived_ticket_types',
                'archived_ticket_price_rules',
                'archived_bar_products',
                'archived_bar_product_categories',
                'archived_bar_combos'
            ));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_data_cleanup_status') THEN
        ALTER TABLE data_cleanup_jobs
            ADD CONSTRAINT chk_data_cleanup_status
            CHECK (status IN ('dry_run','pending_approval','approved','executed','rejected','cancelled'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_data_cleanup_jobs_status
    ON data_cleanup_jobs (status, created_at DESC);

-- ============ 权限码（2 个，domain=platform，requires_approval）============
-- 发起+预览 data.cleanup.request（high）≠ 审批+执行 data.cleanup.approve（critical），职责分离（照 supervisor_review）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('data.cleanup.request', '发起数据清理与预览', 'platform', 'high', TRUE, 'active'),
    ('data.cleanup.approve', '审批并执行数据清理', 'platform', 'critical', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

-- 新权限迁移必须同步授 role_platform_admin（V261 确立基线）。
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('data.cleanup.request', 'data.cleanup.approve')
ON CONFLICT (role_id, permission_code) DO NOTHING;
