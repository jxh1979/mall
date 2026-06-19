-- 会员忠诚度全维度 第2部分（gap-analysis §7A，D12 定稿）：
--   1) 储值（预付卡 / 余额账户，P1）：余额账户 + 充值/消费/退卡/调整流水；充值绑 finance 支付意图，
--      支付成功才入账（预付资金合规，不凭空加余额）。冻结/解冻账户。
--   2) 付费会员（订阅制权益，P1）：会员档位（权益 jsonb）+ 会员订阅（开通/到期/续费/取消），开通绑支付意图。
--   3) 积分转赠（会员间转移，P2）：转赠流水 + 风控（日限额 / 日次数 / 冻结校验，防套现）。
-- 隔离键沿用综合体级 project_id（与 V060 customer_points_rules / V089 一致，非 merchant）；单综合体默认全局可见。
-- 资金相关（储值/付费）金额单位 分（_cent），与 finance_payment_intents.amount_cent 口径一致。

-- ============================================================================
-- 1. 储值：余额账户（每客每项目唯一）
-- ============================================================================
CREATE TABLE IF NOT EXISTS customer_stored_value_accounts (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    project_id VARCHAR(64),
    balance_cent BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_csva_balance_nonneg CHECK (balance_cent >= 0),
    CONSTRAINT chk_csva_status CHECK (status IN ('active','frozen','closed')),
    CONSTRAINT uq_csva_customer_project UNIQUE (customer_id, project_id)
);
CREATE INDEX IF NOT EXISTS idx_customer_stored_value_accounts_project
    ON customer_stored_value_accounts (project_id, customer_id);

-- ============================================================================
-- 2. 储值：余额变动流水（topup/consume/refund/adjust，amount_cent 带方向）
--    payment_intent_id 关联充值支付意图；idempotency_key 唯一防重复入账。
-- ============================================================================
CREATE TABLE IF NOT EXISTS customer_stored_value_transactions (
    id VARCHAR(64) PRIMARY KEY,
    account_id VARCHAR(64) NOT NULL REFERENCES customer_stored_value_accounts(id) ON DELETE CASCADE,
    customer_id VARCHAR(64) NOT NULL,
    txn_type VARCHAR(32) NOT NULL,
    amount_cent BIGINT NOT NULL,
    balance_after_cent BIGINT NOT NULL,
    payment_intent_id VARCHAR(64),
    reason VARCHAR(255),
    idempotency_key VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_csvt_type CHECK (txn_type IN ('topup','consume','refund','adjust')),
    CONSTRAINT chk_csvt_balance_after_nonneg CHECK (balance_after_cent >= 0),
    CONSTRAINT uq_csvt_idempotency UNIQUE (idempotency_key)
);
CREATE INDEX IF NOT EXISTS idx_customer_stored_value_txn_account
    ON customer_stored_value_transactions (account_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_customer_stored_value_txn_payment_intent
    ON customer_stored_value_transactions (payment_intent_id)
    WHERE payment_intent_id IS NOT NULL;

-- ============================================================================
-- 3. 付费会员：会员档位（权益 jsonb）
-- ============================================================================
CREATE TABLE IF NOT EXISTS customer_membership_plans (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64),
    plan_code VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    price_cent BIGINT NOT NULL,
    duration_days INTEGER NOT NULL,
    benefits JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_cmp_price_nonneg CHECK (price_cent >= 0),
    CONSTRAINT chk_cmp_duration_pos CHECK (duration_days > 0),
    CONSTRAINT chk_cmp_status CHECK (status IN ('active','inactive','archived')),
    CONSTRAINT uq_cmp_project_plan UNIQUE (project_id, plan_code)
);
CREATE INDEX IF NOT EXISTS idx_customer_membership_plans_project
    ON customer_membership_plans (project_id);

-- ============================================================================
-- 4. 付费会员：会员订阅（开通/到期/续费/取消，开通绑支付意图）
-- ============================================================================
CREATE TABLE IF NOT EXISTS customer_memberships (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    plan_id VARCHAR(64) NOT NULL REFERENCES customer_membership_plans(id),
    project_id VARCHAR(64),
    start_at TIMESTAMPTZ NOT NULL,
    expire_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    payment_intent_id VARCHAR(64),
    idempotency_key VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_cm_status CHECK (status IN ('active','expired','cancelled')),
    CONSTRAINT chk_cm_window CHECK (expire_at >= start_at),
    CONSTRAINT uq_cm_idempotency UNIQUE (idempotency_key)
);
CREATE INDEX IF NOT EXISTS idx_customer_memberships_customer
    ON customer_memberships (customer_id, status);
CREATE INDEX IF NOT EXISTS idx_customer_memberships_expire
    ON customer_memberships (status, expire_at)
    WHERE status = 'active';

-- ============================================================================
-- 5. 积分转赠（会员间转移）+ 风控字段（日限额 / 次数，防套现）
-- ============================================================================
CREATE TABLE IF NOT EXISTS customer_points_transfers (
    id VARCHAR(64) PRIMARY KEY,
    from_customer_id VARCHAR(64) NOT NULL REFERENCES customers(id),
    to_customer_id VARCHAR(64) NOT NULL REFERENCES customers(id),
    points BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'completed',
    reason VARCHAR(255),
    idempotency_key VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_cpt_points_pos CHECK (points > 0),
    CONSTRAINT chk_cpt_status CHECK (status IN ('completed','rejected','reversed')),
    CONSTRAINT chk_cpt_distinct CHECK (from_customer_id <> to_customer_id),
    CONSTRAINT uq_cpt_idempotency UNIQUE (idempotency_key)
);
CREATE INDEX IF NOT EXISTS idx_customer_points_transfers_from
    ON customer_points_transfers (from_customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_customer_points_transfers_to
    ON customer_points_transfers (to_customer_id, created_at DESC);

-- 积分转赠风控配置（日限额 / 日次数 上限，防套现）。projectId 可空（综合体级全局）。
CREATE TABLE IF NOT EXISTS customer_points_transfer_limits (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64),
    daily_points_limit BIGINT NOT NULL DEFAULT 0,
    daily_count_limit INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_cptl_points_nonneg CHECK (daily_points_limit >= 0),
    CONSTRAINT chk_cptl_count_nonneg CHECK (daily_count_limit >= 0),
    CONSTRAINT chk_cptl_status CHECK (status IN ('active','inactive')),
    CONSTRAINT uq_cptl_project UNIQUE (project_id)
);

-- ============================================================================
-- 6. 权限 seed：储值充值/消费/退卡/调整、会员档位与开通、积分转赠（后台高风险动作要审批）
-- ============================================================================
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('customer.stored_value.view', 'View stored-value accounts/transactions', 'customer', 'low', FALSE, 'active'),
    ('customer.stored_value.topup', 'Stored-value top-up (bind payment intent)', 'customer', 'high', TRUE, 'active'),
    ('customer.stored_value.consume', 'Stored-value consume (deduct balance)', 'customer', 'medium', FALSE, 'active'),
    ('customer.stored_value.refund', 'Stored-value refund/close (return balance)', 'customer', 'high', TRUE, 'active'),
    ('customer.stored_value.freeze', 'Freeze/unfreeze stored-value accounts', 'customer', 'high', TRUE, 'active'),
    ('customer.membership.view', 'View paid memberships/plans', 'customer', 'low', FALSE, 'active'),
    ('customer.membership.manage', 'Manage paid membership plans (benefits/price)', 'customer', 'high', TRUE, 'active'),
    ('customer.membership.subscribe', 'Subscribe paid membership (bind payment intent)', 'customer', 'medium', FALSE, 'active'),
    ('customer.points.transfer', 'Transfer member points (risk-controlled)', 'customer', 'medium', FALSE, 'active'),
    ('customer.points.transfer_limit', 'Manage points-transfer risk limits', 'customer', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN (
        'customer.stored_value.view',
        'customer.stored_value.topup',
        'customer.stored_value.consume',
        'customer.stored_value.refund',
        'customer.stored_value.freeze',
        'customer.membership.view',
        'customer.membership.manage',
        'customer.membership.subscribe',
        'customer.points.transfer',
        'customer.points.transfer_limit'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
