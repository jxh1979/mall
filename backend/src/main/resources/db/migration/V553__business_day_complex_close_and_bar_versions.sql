-- 文件版本：V0.01
-- 第十八批第 1 条（ENUM:187、BSM:1682、DEC-BIZDAY-001）：营业日出厂起点上午 10:00、次日 07:00 闭店，
-- 起止时点按综合体（即 V483 游乐已在用的那一套）与每家酒吧分别配置，须同时配置上下限，调整须最高权限审批并留痕、只影响未来。
-- V201 按业务域直改即生效的旧口径删除，不做存量映射；已落库的 business_date、已锁日结、看板快照、台账 due_date 一律不回改。

-- ① 综合体营业日版本补闭店时点及上下限（ledger §G-3：不校验闭店时点与起点的关系）。
ALTER TABLE amusement_business_day_configs
    ADD COLUMN close_hour INTEGER NOT NULL DEFAULT 7,
    ADD COLUMN min_close_hour INTEGER NOT NULL DEFAULT 7,
    ADD COLUMN max_close_hour INTEGER NOT NULL DEFAULT 7;
ALTER TABLE amusement_business_day_configs
    ALTER COLUMN close_hour DROP DEFAULT,
    ALTER COLUMN min_close_hour DROP DEFAULT,
    ALTER COLUMN max_close_hour DROP DEFAULT,
    ADD CONSTRAINT chk_amusement_business_day_close_hour CHECK (0 <= min_close_hour AND min_close_hour <= close_hour
        AND close_hour <= max_close_hour AND max_close_hour <= 23);

-- ② 每家酒吧的营业日版本：列与约束照 V483；酒吧＝业态为酒吧的商户（V267），bar_id 即商户 id；
--    project_id 提交时取 merchants.project_id，供范围闸。
CREATE TABLE bar_business_day_configs (
    id VARCHAR(64) PRIMARY KEY,
    bar_id VARCHAR(64) NOT NULL REFERENCES merchants(id),
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    version VARCHAR(16) NOT NULL CHECK (version ~ '^V[0-9]+[.][0-9]{2}$'),
    cutoff_hour INTEGER NOT NULL,
    min_cutoff_hour INTEGER NOT NULL,
    max_cutoff_hour INTEGER NOT NULL,
    close_hour INTEGER NOT NULL,
    min_close_hour INTEGER NOT NULL,
    max_close_hour INTEGER NOT NULL,
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
    CHECK (0 <= min_close_hour AND min_close_hour <= close_hour
        AND close_hour <= max_close_hour AND max_close_hour <= 23),
    CHECK (effective_at > created_at),
    CHECK (status <> 'approved' OR (decided_at IS NOT NULL AND decided_at < effective_at)),
    UNIQUE (bar_id, version)
);
CREATE UNIQUE INDEX uq_bar_business_day_approved_start
    ON bar_business_day_configs(bar_id, effective_at) WHERE status = 'approved';
CREATE INDEX ix_bar_business_day_versions
    ON bar_business_day_configs(bar_id, effective_at DESC);

-- ③ 权限码（照 V483 只种权限码、不授角色）；综合体那一套不再只是游乐用，改名不改码。
INSERT INTO permissions(permission_code,permission_name,domain,risk_level,requires_approval,status)
VALUES ('bar.business_day.view','查看酒吧营业日版本','bar','medium',FALSE,'active'),
       ('bar.business_day.manage','提交酒吧营业日审批','bar','high',TRUE,'active')
ON CONFLICT(permission_code) DO NOTHING;
UPDATE permissions SET permission_name='查看综合体营业日版本' WHERE permission_code='amusement.business_day.view';
UPDATE permissions SET permission_name='提交综合体营业日审批' WHERE permission_code='amusement.business_day.manage';

-- ④ V201 按业务域的旧日切配置。
DROP TABLE business_day_cutoff_config;
