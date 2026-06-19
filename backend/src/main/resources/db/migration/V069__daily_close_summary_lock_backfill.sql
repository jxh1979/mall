-- R5 T7 日结锁定 + 补录审批 + 多源汇总（裁决⑤：锁定后硬拦截改影响金额 + 补录上级审批闭环 + 日结六源汇总）。
-- 规格：docs/r5-spec-T7-daily-close-lock-dev-design-20260618.md；口径：docs/r5-aggregation-metrics-draft-20260618.md §1.2。
--
-- ⚠️ 权威表收口结论（owner 收口点，影响 T7）：
--   规格文案默认 finance_ar_ap_items 为财务权威，但代码核验（grep src/）显示：
--     · finance_ar_ap_items（V002，UUID PK，business_entity_id/amount_cents 旧 schema）——**无任何 Java/测试/seed 引用，是孤儿/legacy 表**；
--     · ar_ap_items（V008，VARCHAR(64) PK，amount_cent/paid/unpaid/due_date/merchant_id）——
--       才是 FinanceService + JdbcFinanceRepository 实际读写的运行期权威台账。
--   T7 验收（GWT）要求锁定后硬拦截 createArApItem/registerArApPayment/退款、补录审批真正写金额变动；
--   这些逻辑全部跑在 ar_ap_items 上。若把 locked_day_close_id / ar_ap_adjustments.ar_ap_item_id 落到孤儿
--   finance_ar_ap_items，运行期代码读不到、特性不可用、GWT 全挂。
--   故本迁移把 locked_day_close_id 列与 ar_ap_adjustments 外键落到运行期权威表 ar_ap_items（VARCHAR PK），
--   这也正是规格顶部要求 owner 裁定的"两套 ar_ap 表权威收口"。本结论已在最终交付报告与 claims.json note 标注。

-- ── 1) 日结多源汇总（六源）────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS finance_daily_close_summaries (
    id VARCHAR(64) PRIMARY KEY,
    daily_close_id VARCHAR(64) NOT NULL REFERENCES finance_daily_closes(id) ON DELETE CASCADE,
    source_stream VARCHAR(40) NOT NULL,               -- 六源
    amount_cent BIGINT NOT NULL DEFAULT 0,
    refund_cent BIGINT NOT NULL DEFAULT 0,
    order_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_fdcs_source CHECK (source_stream IN
      ('official_ticket','channel_ticket','bar','amusement','supply_cost','thirdparty_reflow')),
    CONSTRAINT uq_fdcs_close_source UNIQUE (daily_close_id, source_stream)
);

CREATE INDEX IF NOT EXISTS idx_finance_daily_close_summaries_close
    ON finance_daily_close_summaries (daily_close_id);

-- ── 2) 锁定后补录审批 ──────────────────────────────────────────────────────
-- locked_day_close_id 落运行期权威表 ar_ap_items（见顶部收口结论）。
ALTER TABLE ar_ap_items
    ADD COLUMN IF NOT EXISTS locked_day_close_id VARCHAR(64) REFERENCES finance_daily_closes(id);

CREATE INDEX IF NOT EXISTS idx_ar_ap_items_locked_day_close
    ON ar_ap_items (locked_day_close_id);

CREATE TABLE IF NOT EXISTS ar_ap_adjustments (
    id VARCHAR(64) PRIMARY KEY,
    ar_ap_item_id VARCHAR(64) NOT NULL REFERENCES ar_ap_items(id),
    locked_day_close_id VARCHAR(64) NOT NULL REFERENCES finance_daily_closes(id),
    adjustment_amount_cent BIGINT NOT NULL,           -- 补录变动额（带符号）
    approval_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    requested_by VARCHAR(64) NOT NULL,
    approved_by VARCHAR(64),
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at TIMESTAMPTZ,
    CONSTRAINT chk_ar_ap_adj_approval CHECK (approval_status IN ('pending','approved','rejected'))
);

CREATE INDEX IF NOT EXISTS idx_ar_ap_adjustments_item
    ON ar_ap_adjustments (ar_ap_item_id);

-- ── 3) 对账差异类型收口（四类 CHECK，替换自由文本）──────────────────────────
-- difference_type 现状写入值核对（grep 代码/测试）：
--   FinanceServiceTests / FinanceJdbcIntegrationTests 向 finance_reconciliation_differences 写 'amount_mismatch'。
--   该值不在规格四类(amount/count/status/missing)内——若 CHECK 仅取四类，会使既有测试数据/迁移失败。
-- 处理：CHECK 取值集 = 规格四类 ∪ 现状遗留值 'amount_mismatch'（语义=amount），用幂等 DO 块加约束。
--   待财务复核后将历史 'amount_mismatch' 归一为 'amount' 再收紧 CHECK（留作后续迁移）。
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_frd_difference_type'
    ) THEN
        ALTER TABLE finance_reconciliation_differences
            ADD CONSTRAINT chk_frd_difference_type CHECK (difference_type IN (
                'amount', 'count', 'status', 'missing', 'amount_mismatch'
            ));
    END IF;
END $$;

-- ── 4) seed 权限（参照 V008 finance 权限 INSERT 模式）──────────────────────
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('finance.daily_close.manage', 'Manage finance daily close summaries', 'finance', 'high', TRUE, 'active'),
    ('finance.locked_backfill.request', 'Request locked AR/AP backfill adjustment', 'finance', 'high', TRUE, 'active'),
    ('finance.locked_backfill.approve', 'Approve locked AR/AP backfill adjustment', 'finance', 'high', TRUE, 'active')
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
        'finance.daily_close.manage',
        'finance.locked_backfill.request',
        'finance.locked_backfill.approve'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
