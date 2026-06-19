-- 第二阶段：交班 / 班结对账（shift close / cash drawer reconciliation，对照 PRD §10.1）。
-- 收银交班遗留项落库：按班次/收银员对各支付方式应收 vs 实点现金做交接对账，定位差异、明确责任。
--
-- 设计要点：
--   - 一个班次 = 一个收银员(cashier_user_id) 在一个吧台(bar_id) 的一段收银时间窗口 [opened_at, closed_at)。
--   - 开班 openShift：记备用金(opening_cash_cent)、收银员、班次开始；status=open。
--   - 班结 closeShift：按班次时间窗口汇总本收银员/吧台已结账单的 bar_bill_settlements 各支付方式
--       （cash/pos/aggregate_pay/bank_transfer/other → 归并 system_cash/system_pos/system_online_cent）、
--       退款（V079 refund，按 refunded_at 落窗口 → system_refund_cent）、
--       小费（V091 tip，结算账单的 tip_amount_cent → system_tip_cent，代收口径）→ 系统应收快照。
--     录入实点现金 declared_cash_cent → 现金差异 cash_diff_cent = declared_cash - (opening_cash + system_cash)。
--       注：现金应收侧含备用金；差异 = 实点 - (备用金 + 系统现金应收)，0 为平账，正为长款，负为短款。
--     差异绝对值超阈值(cash_diff_threshold_cent，默认 0=任何差异即标记) → needs_review=TRUE 进复核。
--   - 与日结(T7)协同：班结是日内交接（按收银员/吧台），日结是经营日整体汇总；
--       班结明细可供日结/对账下钻，二者口径不重复扣（班结只读聚合 bar_bill_settlements，不改账单/不二次过账）。
--   - CHECK 枚举；currentMerchantScope 隔离；金额 BIGINT 分。

CREATE TABLE IF NOT EXISTS bar_shifts (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    bar_id VARCHAR(64) NOT NULL,
    cashier_user_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'open',
    opened_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    opening_cash_cent BIGINT NOT NULL DEFAULT 0,        -- 备用金
    declared_cash_cent BIGINT,                          -- 班结实点现金（含备用金）
    -- 班次内系统应收快照（班结时按时间窗口聚合 bar_bill_settlements / refund / tip 落定）
    system_cash_cent BIGINT NOT NULL DEFAULT 0,         -- 现金应收（settle_channel='cash'）
    system_pos_cent BIGINT NOT NULL DEFAULT 0,          -- POS 应收（settle_channel='pos'）
    system_online_cent BIGINT NOT NULL DEFAULT 0,       -- 扫码/聚合/转账应收（aggregate_pay/bank_transfer/other）
    system_refund_cent BIGINT NOT NULL DEFAULT 0,       -- 窗口内退款（refunded_at 落窗口）
    system_tip_cent BIGINT NOT NULL DEFAULT 0,          -- 窗口内小费（结算账单 tip，代收）
    cash_diff_cent BIGINT,                              -- 现金差异 = declared_cash - (opening + system_cash)
    cash_diff_threshold_cent BIGINT NOT NULL DEFAULT 0, -- 差异复核阈值（绝对值超此值进复核）
    needs_review BOOLEAN NOT NULL DEFAULT FALSE,        -- 差异超阈值标记进复核
    handover_to_user_id VARCHAR(64),                    -- 交接下一班收银员
    note VARCHAR(1000),
    opened_by_user_id VARCHAR(64) NOT NULL,
    closed_by_user_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_bar_shifts_status CHECK (status IN ('open', 'closed')),
    CONSTRAINT chk_bar_shifts_opening_cash CHECK (opening_cash_cent >= 0),
    CONSTRAINT chk_bar_shifts_threshold CHECK (cash_diff_threshold_cent >= 0),
    -- 关闭一致性：closed 必须有 closed_at / 实点现金 / 差异 / 结班人；open 不得携带班结字段（防误聚合）。
    CONSTRAINT chk_bar_shifts_close CHECK (
        (status = 'closed' AND closed_at IS NOT NULL AND declared_cash_cent IS NOT NULL
            AND cash_diff_cent IS NOT NULL AND closed_by_user_id IS NOT NULL)
        OR (status = 'open' AND closed_at IS NULL AND declared_cash_cent IS NULL
            AND cash_diff_cent IS NULL AND closed_by_user_id IS NULL)
    )
);

-- 同一吧台同一收银员同时仅允许一个 open 班次（防重复开班 / 时间窗口重叠歧义）。
CREATE UNIQUE INDEX IF NOT EXISTS uq_bar_shifts_open_cashier_bar
    ON bar_shifts (bar_id, cashier_user_id)
    WHERE status = 'open';

CREATE INDEX IF NOT EXISTS idx_bar_shifts_bar ON bar_shifts (bar_id, opened_at DESC);
CREATE INDEX IF NOT EXISTS idx_bar_shifts_merchant ON bar_shifts (merchant_id) WHERE merchant_id IS NOT NULL;

-- ============================================================================
-- 权限点 seed（开班 medium；班结 high 需主管复核差异）
-- ============================================================================
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('bar.shift.manage', 'Open a bar cashier shift (record float / cashier / start)', 'bar', 'medium', FALSE, 'active'),
    ('bar.shift.close', 'Close a bar cashier shift and reconcile cash drawer (high-risk, supervisor review on diff)', 'bar', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('bar.shift.manage', 'bar.shift.close')
ON CONFLICT (role_id, permission_code) DO NOTHING;
