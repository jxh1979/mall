-- 退款分级授权（审计 A2 升级，用户拍板「规则自动退 / 员工额度自助 / 超额复核」，适用所有退款）。
-- Stage A：仅建分级判定所需的两张主数据表（员工额度 + 自动退规则），不改任何退款行为（引擎默认无配置→全部走复核，最安全）。
-- 纯新建表（不会撞存量数据）。后续 Stage B 接入三域退款链 + ticket_refunds 发起人留痕（另起迁移）。

CREATE TABLE IF NOT EXISTS refund_authority_limits (
    id VARCHAR(64) PRIMARY KEY,
    subject_type VARCHAR(16) NOT NULL,      -- staff（按员工）| staff_level（按职级，预留）
    subject_id VARCHAR(64) NOT NULL,
    refund_domain VARCHAR(16) NOT NULL,     -- ticket | bar | finance | all
    limit_amount_cent BIGINT NOT NULL,      -- 该主体在该域的单笔退款自助额度（分）；金额≤此值可自助自动退
    project_id VARCHAR(64),                 -- 预留：综合体级；NULL=全局
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_refund_authority_subject_type CHECK (subject_type IN ('staff', 'staff_level')),
    CONSTRAINT chk_refund_authority_domain CHECK (refund_domain IN ('ticket', 'bar', 'finance', 'all')),
    CONSTRAINT chk_refund_authority_limit_nonneg CHECK (limit_amount_cent >= 0),
    CONSTRAINT chk_refund_authority_status CHECK (status IN ('active', 'inactive')),
    CONSTRAINT uq_refund_authority UNIQUE (subject_type, subject_id, refund_domain)
);
CREATE INDEX IF NOT EXISTS idx_refund_authority_lookup
    ON refund_authority_limits (subject_type, subject_id, refund_domain, status);

CREATE TABLE IF NOT EXISTS refund_auto_rules (
    id VARCHAR(64) PRIMARY KEY,
    refund_domain VARCHAR(16) NOT NULL,     -- ticket | bar | finance | all
    rule_type VARCHAR(40) NOT NULL,         -- expired_unused（到期未用）| unused（未使用）| ...
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    project_id VARCHAR(64),
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_refund_auto_rule_domain CHECK (refund_domain IN ('ticket', 'bar', 'finance', 'all')),
    CONSTRAINT chk_refund_auto_rule_status CHECK (status IN ('active', 'inactive')),
    CONSTRAINT uq_refund_auto_rule UNIQUE (refund_domain, rule_type)
);
