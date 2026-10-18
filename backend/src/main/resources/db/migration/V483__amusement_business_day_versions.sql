-- 文件版本：V0.01
-- TK-17：无运营初值。真实综合体配置须经最高权限批准且只影响未来。
CREATE TABLE amusement_business_day_configs (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    version VARCHAR(16) NOT NULL CHECK (version ~ '^V[0-9]+[.][0-9]{2}$'),
    zone_id VARCHAR(80) NOT NULL,
    cutoff_hour INTEGER NOT NULL,
    min_cutoff_hour INTEGER NOT NULL,
    max_cutoff_hour INTEGER NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('pending_review','approved','rejected','expired')),
    review_id VARCHAR(64),
    reason TEXT NOT NULL,
    attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    decided_by VARCHAR(64),
    decided_at TIMESTAMPTZ,
    CHECK (0 <= min_cutoff_hour AND min_cutoff_hour <= cutoff_hour
        AND cutoff_hour <= max_cutoff_hour AND max_cutoff_hour <= 23),
    CHECK (effective_at > created_at),
    CHECK (status <> 'approved' OR (decided_at IS NOT NULL AND decided_at < effective_at)),
    UNIQUE (project_id, version)
);
CREATE UNIQUE INDEX uq_amusement_business_day_approved_start
    ON amusement_business_day_configs(project_id, effective_at) WHERE status = 'approved';
CREATE INDEX ix_amusement_business_day_versions
    ON amusement_business_day_configs(project_id, effective_at DESC);

INSERT INTO permissions(permission_code,permission_name,domain,risk_level,requires_approval,status)
VALUES ('amusement.business_day.view','查看综合体游乐营业日版本','amusement','medium',FALSE,'active'),
       ('amusement.business_day.manage','提交综合体游乐营业日审批','amusement','high',TRUE,'active')
ON CONFLICT(permission_code) DO NOTHING;
