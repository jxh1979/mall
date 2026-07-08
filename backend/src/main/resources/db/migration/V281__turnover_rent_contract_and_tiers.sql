-- 提成租金规则升级（2026-07-08 用户拍板）：挂合同 + 阶梯超额累进（无封顶）。保底(min_amount_cent)保留。
-- 封顶不再使用：新规则 max_amount_cent 恒为 NULL（现有 CHECK 允许 NULL，无需改约束）；旧单一费率规则保持兼容。
ALTER TABLE turnover_rent_rules ADD COLUMN IF NOT EXISTS contract_id varchar(64);
CREATE INDEX IF NOT EXISTS idx_turnover_rent_rules_contract ON turnover_rent_rules(contract_id);

-- 阶梯档位子表：超额累进，seq 升序，最高档 upper_bound_cent=NULL 表无上限。
CREATE TABLE turnover_rent_rule_tiers (
    id varchar(64) PRIMARY KEY,
    rule_id varchar(64) NOT NULL REFERENCES turnover_rent_rules(id) ON DELETE CASCADE,
    seq integer NOT NULL,
    upper_bound_cent bigint,
    rate_bps integer NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_trr_tier_rate CHECK (rate_bps >= 0 AND rate_bps <= 10000),
    CONSTRAINT chk_trr_tier_upper CHECK (upper_bound_cent IS NULL OR upper_bound_cent >= 0),
    CONSTRAINT uq_trr_tier_seq UNIQUE (rule_id, seq)
);
CREATE INDEX idx_trr_tiers_rule ON turnover_rent_rule_tiers(rule_id, seq);
GRANT SELECT, INSERT, UPDATE, DELETE ON turnover_rent_rule_tiers TO complex_app;
