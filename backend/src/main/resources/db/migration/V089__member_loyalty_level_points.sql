-- 会员忠诚度全维度 第1部分（gap-analysis §7A，D12 定稿 P1/P2）：
--   1) 等级降级/保级宽限：会员等级配置表(保级阈值/周期/宽限) + 账户加 周期起点/保级进度/宽限期；
--   2) 积分过期清零 / 冻结 / 解冻：账户加 frozen_points/status；明细加 expire_at/expired（FIFO 批次过期）；
--   3) 连续签到阶梯：customer_check_ins(customer_id+check_in_date 唯一/streak) + 阶梯奖励配置；
--   4) 生日加倍：customers 加 birth_date；生日加倍规则表 customer_birthday_rules。
-- 隔离键沿用综合体级 project_id（与 V060 customer_points_rules 一致，非 merchant）；单综合体默认全局可见。
-- 积分获得此前仅有兑换扣减、无获取路径；本期补 earn 路径（消费/签到/生日），故账户/明细补必要字段。

-- ============================================================================
-- 1. 会员等级配置表（保级阈值 / 考核周期 / 宽限期）
-- ============================================================================
CREATE TABLE IF NOT EXISTS customer_member_levels (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64),
    level_code VARCHAR(64) NOT NULL,
    level_name VARCHAR(160) NOT NULL,
    level_rank INTEGER NOT NULL,
    upgrade_threshold BIGINT NOT NULL DEFAULT 0,
    keep_threshold BIGINT NOT NULL DEFAULT 0,
    keep_period_days INTEGER NOT NULL DEFAULT 365,
    grace_days INTEGER NOT NULL DEFAULT 0,
    point_multiplier_basis_points INTEGER NOT NULL DEFAULT 10000,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_cml_status CHECK (status IN ('active','inactive','archived')),
    CONSTRAINT chk_cml_level_rank CHECK (level_rank >= 0),
    CONSTRAINT chk_cml_upgrade_threshold CHECK (upgrade_threshold >= 0),
    CONSTRAINT chk_cml_keep_threshold CHECK (keep_threshold >= 0),
    CONSTRAINT chk_cml_keep_period_days CHECK (keep_period_days > 0),
    CONSTRAINT chk_cml_grace_days CHECK (grace_days >= 0),
    CONSTRAINT chk_cml_multiplier CHECK (point_multiplier_basis_points > 0),
    CONSTRAINT uq_cml_project_level UNIQUE (project_id, level_code)
);
CREATE INDEX IF NOT EXISTS idx_customer_member_levels_project_rank
    ON customer_member_levels (project_id, level_rank);

-- ============================================================================
-- 2. 积分账户扩展：保级周期 / 宽限 / 冻结
-- ============================================================================
ALTER TABLE customer_points_accounts
    ADD COLUMN IF NOT EXISTS frozen_points BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS account_status VARCHAR(32) NOT NULL DEFAULT 'active',
    ADD COLUMN IF NOT EXISTS level_period_start TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS keep_threshold_progress BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS grace_until TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_check_in_date DATE,
    ADD COLUMN IF NOT EXISTS check_in_streak INTEGER NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_cpa_frozen_points_nonneg') THEN
        ALTER TABLE customer_points_accounts
            ADD CONSTRAINT chk_cpa_frozen_points_nonneg CHECK (frozen_points >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_cpa_account_status') THEN
        ALTER TABLE customer_points_accounts
            ADD CONSTRAINT chk_cpa_account_status CHECK (account_status IN ('active','frozen'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_cpa_keep_progress_nonneg') THEN
        ALTER TABLE customer_points_accounts
            ADD CONSTRAINT chk_cpa_keep_progress_nonneg CHECK (keep_threshold_progress >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_cpa_check_in_streak_nonneg') THEN
        ALTER TABLE customer_points_accounts
            ADD CONSTRAINT chk_cpa_check_in_streak_nonneg CHECK (check_in_streak >= 0);
    END IF;
END $$;

-- ============================================================================
-- 3. 积分明细扩展：批次过期 / 已过期标记（FIFO 批次过期清零）
-- ============================================================================
ALTER TABLE customer_points_ledger
    ADD COLUMN IF NOT EXISTS expire_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS remaining_points BIGINT,
    ADD COLUMN IF NOT EXISTS expired BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_customer_points_ledger_expire
    ON customer_points_ledger (customer_id, expire_at)
    WHERE expire_at IS NOT NULL AND expired = FALSE AND points_delta > 0;

-- ============================================================================
-- 4. 连续签到：签到记录表 + 阶梯奖励配置
-- ============================================================================
CREATE TABLE IF NOT EXISTS customer_check_ins (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    check_in_date DATE NOT NULL,
    streak INTEGER NOT NULL,
    awarded_points BIGINT NOT NULL DEFAULT 0,
    source_terminal VARCHAR(32) NOT NULL DEFAULT 'miniapp',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_customer_check_ins_customer_date UNIQUE (customer_id, check_in_date),
    CONSTRAINT chk_cci_streak CHECK (streak >= 1),
    CONSTRAINT chk_cci_awarded_points CHECK (awarded_points >= 0)
);
CREATE INDEX IF NOT EXISTS idx_customer_check_ins_customer_date
    ON customer_check_ins (customer_id, check_in_date DESC);

CREATE TABLE IF NOT EXISTS customer_check_in_tiers (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64),
    streak_days INTEGER NOT NULL,
    award_points BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_ccit_streak_days CHECK (streak_days >= 1),
    CONSTRAINT chk_ccit_award_points CHECK (award_points >= 0),
    CONSTRAINT chk_ccit_status CHECK (status IN ('active','inactive')),
    CONSTRAINT uq_ccit_project_streak UNIQUE (project_id, streak_days)
);
CREATE INDEX IF NOT EXISTS idx_customer_check_in_tiers_project_streak
    ON customer_check_in_tiers (project_id, streak_days);

-- ============================================================================
-- 5. 生日加倍：customers 加生日字段 + 生日加倍规则表
-- ============================================================================
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS birth_date DATE;

CREATE TABLE IF NOT EXISTS customer_birthday_rules (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64),
    rule_name VARCHAR(160) NOT NULL,
    window_type VARCHAR(32) NOT NULL,
    multiplier_basis_points INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_cbr_window_type CHECK (window_type IN ('birthday_day','birthday_month')),
    CONSTRAINT chk_cbr_multiplier CHECK (multiplier_basis_points > 10000),
    CONSTRAINT chk_cbr_status CHECK (status IN ('active','inactive')),
    CONSTRAINT uq_cbr_project_status UNIQUE (project_id, rule_name)
);
CREATE INDEX IF NOT EXISTS idx_customer_birthday_rules_project
    ON customer_birthday_rules (project_id) WHERE project_id IS NOT NULL;

-- ============================================================================
-- 6. 权限：会员忠诚度后台管理（等级配置 / 积分冻结解冻 / 等级评估作业 / 查询）
-- ============================================================================
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('customer.loyalty.view', 'View member loyalty (levels/points)', 'customer', 'low', FALSE, 'active'),
    ('customer.loyalty.manage', 'Manage member loyalty config (levels/tiers/birthday rules)', 'customer', 'high', TRUE, 'active'),
    ('customer.points.freeze', 'Freeze/unfreeze member points accounts', 'customer', 'high', TRUE, 'active'),
    ('customer.loyalty.evaluate', 'Run member level/points evaluation jobs', 'customer', 'high', TRUE, 'active')
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
        'customer.loyalty.view',
        'customer.loyalty.manage',
        'customer.points.freeze',
        'customer.loyalty.evaluate'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
