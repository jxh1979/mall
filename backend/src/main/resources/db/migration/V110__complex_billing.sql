-- P1#14 综合体计费引擎（finance-ar-ap §14）：营业额抽成 + 保证金台账 + CAM（公共区域维护费）分摊。
-- 现状证据：merchantbilling 仅费用类型字符串无引擎；finance SOURCE_TYPES 无 turnover_rent/cam/deposit。
-- 本迁移收口：
--   1) turnover_rent_rules：营业额抽成规则（扣率 + 保底/封顶 clamp），按规则×回流营业额生成应收。
--   2) deposit_ledger：保证金台账（收/退/抵扣/没收四动作 + 运行余额）。
--   3) cam_allocations：CAM 权重分摊（按商户权重分摊总额 → 每商户应收）。
--   4) finance SOURCE_TYPES 增 turnover_rent/cam/deposit（DB 层 ar_ap_items.source_type 无 CHECK，服务层枚举即可）。
-- 取数（只读跨域）：回流营业额来自 integration_order_syncs（第三方回流 order 金额，标准模型，只读不改其写）。

-- ── 1) 营业额抽成规则 ────────────────────────────────────────────────────────
-- rate_bps：扣率（基点 basis points，10000=100%）；min_amount_cent/max_amount_cent：保底/封顶（clamp，可空）。
CREATE TABLE IF NOT EXISTS turnover_rent_rules (
    id VARCHAR(64) PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    rate_bps INTEGER NOT NULL,                          -- 抽成扣率（基点）
    min_amount_cent BIGINT,                             -- 保底租金（分，可空）
    max_amount_cent BIGINT,                             -- 封顶租金（分，可空）
    effective_from DATE NOT NULL,
    effective_to DATE,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_turnover_rent_rate CHECK (rate_bps >= 0 AND rate_bps <= 10000),
    CONSTRAINT chk_turnover_rent_min CHECK (min_amount_cent IS NULL OR min_amount_cent >= 0),
    CONSTRAINT chk_turnover_rent_max CHECK (max_amount_cent IS NULL OR max_amount_cent >= 0),
    CONSTRAINT chk_turnover_rent_min_max
        CHECK (min_amount_cent IS NULL OR max_amount_cent IS NULL OR max_amount_cent >= min_amount_cent),
    CONSTRAINT chk_turnover_rent_status CHECK (status IN ('active', 'inactive'))
);

CREATE INDEX IF NOT EXISTS idx_turnover_rent_rules_merchant
    ON turnover_rent_rules (merchant_id, status, effective_from DESC);

-- ── 2) 保证金台账（四动作 + 余额）────────────────────────────────────────────
-- action：collect 收取 / refund 退还 / deduct 抵扣 / forfeit 没收。
-- amount_cent：本次动作金额（恒正）；balance_after_cent：本次动作后该商户保证金运行余额（分，>=0）。
CREATE TABLE IF NOT EXISTS deposit_ledger (
    id VARCHAR(64) PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,                        -- collect|refund|deduct|forfeit
    amount_cent BIGINT NOT NULL,
    balance_after_cent BIGINT NOT NULL,
    ar_ap_item_id VARCHAR(64) REFERENCES ar_ap_items(id), -- 抵扣/没收联动生成的 ar_ap_items（可空）
    reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_deposit_ledger_action CHECK (action IN ('collect', 'refund', 'deduct', 'forfeit')),
    CONSTRAINT chk_deposit_ledger_amount CHECK (amount_cent > 0),
    CONSTRAINT chk_deposit_ledger_balance CHECK (balance_after_cent >= 0)
);

CREATE INDEX IF NOT EXISTS idx_deposit_ledger_merchant_created
    ON deposit_ledger (merchant_id, created_at DESC);

-- ── 3) CAM（公共区域维护费）权重分摊 ─────────────────────────────────────────
-- 一次分摊批次（cam_allocation_batch_id）下，按各商户 weight 分摊 total_amount_cent，
-- 每商户得 allocated_amount_cent，并生成应收 ar_ap_items(source_type='cam')。
CREATE TABLE IF NOT EXISTS cam_allocations (
    id VARCHAR(64) PRIMARY KEY,
    allocation_batch_id VARCHAR(64) NOT NULL,           -- 同一次分摊批次的逻辑分组
    merchant_id VARCHAR(64) NOT NULL,
    period VARCHAR(32) NOT NULL,                         -- 计费周期（如 2026-06）
    total_amount_cent BIGINT NOT NULL,                  -- 本批次待分摊总额（分）
    weight_bps INTEGER NOT NULL,                         -- 本商户权重（基点，批次内合计应为 10000）
    allocated_amount_cent BIGINT NOT NULL,              -- 分摊到本商户的金额（分）
    ar_ap_item_id VARCHAR(64) REFERENCES ar_ap_items(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_cam_total CHECK (total_amount_cent > 0),
    CONSTRAINT chk_cam_weight CHECK (weight_bps >= 0 AND weight_bps <= 10000),
    CONSTRAINT chk_cam_allocated CHECK (allocated_amount_cent >= 0),
    CONSTRAINT uq_cam_allocations_batch_merchant UNIQUE (allocation_batch_id, merchant_id)
);

CREATE INDEX IF NOT EXISTS idx_cam_allocations_merchant_period
    ON cam_allocations (merchant_id, period);

-- ── 4) seed 权限（参照 V008/V069 finance 权限 INSERT 模式）────────────────────
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('finance.turnover_rent.bill', 'Bill merchant turnover-based rent', 'finance', 'high', TRUE, 'active'),
    ('finance.deposit.manage', 'Manage merchant deposit ledger', 'finance', 'high', TRUE, 'active'),
    ('finance.cam.allocate', 'Allocate common-area maintenance fee', 'finance', 'high', TRUE, 'active')
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
        'finance.turnover_rent.bill',
        'finance.deposit.manage',
        'finance.cam.allocate'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
