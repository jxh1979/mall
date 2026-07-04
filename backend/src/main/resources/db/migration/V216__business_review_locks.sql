-- V216 经营复盘锁定落库（fix/review-lock-persist）
-- 口径：后台「经营分析」页「锁定复盘快照」原为纯前端本地态（local.management_review_ready），
--       刷新/换人/审计追责均查不到。用户裁决=真落库。本表沉淀每次锁定为权威审计凭据。
-- 租户隔离：project_id 由调用方 token 派生（currentProjectScopeQuietly，V214 范式），
--       项目主管仅锁本综合体、仅见本综合体；平台管理员 scope 为空 → project_id 存 NULL 见全量。
--       与 staff_*(V214) 一致——平台级行用 NULL project_id，仓储 (:project_scope IS NULL OR project_id=:project_scope) 过滤。
CREATE TABLE IF NOT EXISTS business_review_locks (
    id                  VARCHAR(64)  PRIMARY KEY,
    project_id          VARCHAR(64),
    domain              VARCHAR(64)  NOT NULL,
    operator_note       VARCHAR(1000) NOT NULL,
    metric_count        INTEGER      NOT NULL DEFAULT 0,
    locked_by           VARCHAR(64),
    locked_at           TIMESTAMPTZ  NOT NULL,
    idempotency_key     VARCHAR(128) NOT NULL,
    CONSTRAINT uq_business_review_locks_idem UNIQUE (idempotency_key),
    CONSTRAINT chk_business_review_locks_metric_count CHECK (metric_count >= 0)
);

-- 租户隔离读通道谓词列（V214/analytics_dashboard_snapshots 同范式）。
CREATE INDEX IF NOT EXISTS idx_business_review_locks_project
    ON business_review_locks (project_id, locked_at DESC);

-- 复盘锁定权限：独立写权限码，不复用只读 view，不借道风控 manage（域语义不同）。
-- risk_level=high（管理层追责凭据），requires_approval=FALSE（日常经营复盘操作，非高危授权）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('analytics.review.lock', 'Lock business analytics review snapshot', 'analytics', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('analytics.review.lock')
ON CONFLICT (role_id, permission_code) DO NOTHING;
