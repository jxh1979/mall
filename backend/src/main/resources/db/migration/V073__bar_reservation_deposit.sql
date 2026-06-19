-- R5 T16：订台定金闭环（bar_deposit_rules 固定额/比例 + 定金支付/到店抵扣/取消退没收时限）。
-- 依据：docs/r5-spec-T16-reservation-deposit-dev-design-20260618.md；二轮裁决。
-- 现状基准：bar_reservations 有 deposit_amount_cent(默认0)/deposit_status(V013, NOT NULL 无 CHECK)；V063 已加 deposit_rule。
-- deposit_status 现状写入值（已 grep 核对）：'not_required'(BarEntryService/CustomerService)、'paid'(测试 saveReservation)。
-- 取值集 = 现状∪规格 = {not_required, pending, paid, settled}，避免 CHECK 在既有/历史数据上失败。

-- 1) 定金规则表（后台可配：固定额 / 按低消比例；退没收免没收时限）
CREATE TABLE IF NOT EXISTS bar_deposit_rules (
    id VARCHAR(64) PRIMARY KEY,
    bar_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64),
    scope_type VARCHAR(32) NOT NULL DEFAULT 'bar',   -- bar / table_type / time_slot
    scope_id VARCHAR(64) NOT NULL,
    mode VARCHAR(32) NOT NULL,                        -- fixed / min_consumption_ratio（值最长 20 字符，预留余量）
    fixed_amount_cent BIGINT,                         -- mode=fixed 时取此固定金额
    ratio_bps INTEGER,                                -- mode=min_consumption_ratio 时基点（如 3000=30%）
    cancel_free_before_minutes INTEGER NOT NULL DEFAULT 0,  -- 取消免没收时限（分钟）
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_bar_deposit_mode CHECK (mode IN ('fixed', 'min_consumption_ratio')),
    CONSTRAINT chk_bar_deposit_scope_type CHECK (scope_type IN ('bar', 'table_type', 'time_slot')),
    CONSTRAINT chk_bar_deposit_fixed CHECK (fixed_amount_cent IS NULL OR fixed_amount_cent >= 0),
    CONSTRAINT chk_bar_deposit_ratio CHECK (ratio_bps IS NULL OR (ratio_bps >= 0 AND ratio_bps <= 100000)),
    CONSTRAINT chk_bar_deposit_free_minutes CHECK (cancel_free_before_minutes >= 0)
);

CREATE INDEX IF NOT EXISTS idx_bar_deposit_rules_scope
    ON bar_deposit_rules (bar_id, scope_type, scope_id, enabled);

-- 2) 订台补定金流转字段
ALTER TABLE bar_reservations
    ADD COLUMN IF NOT EXISTS deposit_intent_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS deposit_paid_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deposit_settlement VARCHAR(16);   -- deducted / refunded / forfeited

-- 3) CHECK 收口（幂等 DO 块；先核对既有数据再加，避免历史/测试数据触发约束失败）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_bar_reservations_deposit_settle'
    ) THEN
        ALTER TABLE bar_reservations
            ADD CONSTRAINT chk_bar_reservations_deposit_settle CHECK
            (deposit_settlement IS NULL OR deposit_settlement IN ('deducted', 'refunded', 'forfeited'));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_bar_reservations_deposit_status'
    ) THEN
        ALTER TABLE bar_reservations
            ADD CONSTRAINT chk_bar_reservations_deposit_status CHECK
            (deposit_status IN ('not_required', 'pending', 'paid', 'settled'));
    END IF;
END
$$;

-- 4) 权限：订台管理 + 定金规则管理
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('bar.reservation.manage', 'Manage bar reservation deposit lifecycle (pay/check-in/cancel)', 'bar', 'high', TRUE, 'active'),
    ('bar.deposit_rule.manage', 'Configure bar reservation deposit rules', 'bar', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('bar.reservation.manage', 'bar.deposit_rule.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
