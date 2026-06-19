-- P2#④ 综合体计费收口：CAM 年度 true-up（多退少补）+ 退款方向（refund_direction）统一维度。
-- 审计依据：
--   ① CAM 公摊（V110 cam_allocations）仅按周期权重预收，缺"年度 true-up"——年末实际发生额与已预收差额无多退少补闭环。
--   ② 退款方向语义散落、无统一 refund_direction 维度（finance_refunds V008 仅 refund_channel 字符串，无方向枚举）。
-- 本迁移收口：
--   1) cam_allocations 增 settlement_period（结算归属期，区分预收周期与年度 true-up 期）+ trued_up（是否已纳入某次 true-up）标记。
--   2) 新建 cam_trueup_runs / cam_trueup_lines：一次年度（或任意期间）true-up 批次 = 已预收合计 vs 实际应摊 → 逐户差额 →
--      差额生成 source_type='cam' 的应收（少收，补）/应付（多收，退）调整台账（ar_ap_items）。
--   3) finance_refunds 增 refund_direction（original 原路退回 / cash 现金 / third_party 第三方代退 / mixed 混合），
--      统一退款方向维度；CHECK 收口枚举；历史行回填默认 original（最常见原路）。
--   4) seed 权限 finance.cam.trueup（沿用 V110 finance 权限 INSERT 模式）。

-- ── 1) cam_allocations 增 true-up 关联列 ────────────────────────────────────────
-- settlement_period：本分摊行的结算归属期（默认沿用 period，便于年度 true-up 按归属期归集已预收）。
-- trued_up：本预收行是否已被某次 true-up 批次纳入对账（防重复 true-up）。
ALTER TABLE cam_allocations
    ADD COLUMN IF NOT EXISTS settlement_period VARCHAR(32);

UPDATE cam_allocations
   SET settlement_period = period
 WHERE settlement_period IS NULL;

ALTER TABLE cam_allocations
    ADD COLUMN IF NOT EXISTS trued_up BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_cam_allocations_merchant_settlement
    ON cam_allocations (merchant_id, settlement_period);

-- ── 2) CAM 年度 true-up 批次 + 逐户差额行 ───────────────────────────────────────
-- cam_trueup_runs：一次 true-up 批次（按 settlement_period 归集，记录实际总发生额）。
CREATE TABLE IF NOT EXISTS cam_trueup_runs (
    id VARCHAR(64) PRIMARY KEY,
    settlement_period VARCHAR(32) NOT NULL,              -- 被 true-up 的归属期（如 2026 年度 / 2026-H1）
    actual_total_amount_cent BIGINT NOT NULL,           -- 该归属期实际发生总额（分，权威实际值）
    prepaid_total_amount_cent BIGINT NOT NULL,          -- 该归属期已预收合计（分，来自 cam_allocations 归集）
    status VARCHAR(32) NOT NULL DEFAULT 'completed',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_cam_trueup_actual CHECK (actual_total_amount_cent >= 0),
    CONSTRAINT chk_cam_trueup_prepaid CHECK (prepaid_total_amount_cent >= 0),
    CONSTRAINT chk_cam_trueup_status CHECK (status IN ('completed', 'void'))
);

CREATE INDEX IF NOT EXISTS idx_cam_trueup_runs_period
    ON cam_trueup_runs (settlement_period, created_at DESC);

-- cam_trueup_lines：批次内逐户差额（实际应摊 - 已预收）。
--   diff_amount_cent > 0：少收 → 向商户补收应收（ar_ap direction=receivable）。
--   diff_amount_cent < 0：多收 → 向商户退还应付（ar_ap direction=payable）。
--   diff_amount_cent = 0：不生成台账项（无差额）。
CREATE TABLE IF NOT EXISTS cam_trueup_lines (
    id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL REFERENCES cam_trueup_runs(id),
    merchant_id VARCHAR(64) NOT NULL,
    weight_bps INTEGER NOT NULL,                         -- 本户在 true-up 中所占权重（基点）
    actual_allocated_amount_cent BIGINT NOT NULL,       -- 实际应摊（actual_total × weight_bps / 10000，末位归差额）
    prepaid_amount_cent BIGINT NOT NULL,                -- 该户该归属期已预收合计
    diff_amount_cent BIGINT NOT NULL,                   -- = actual_allocated - prepaid（带符号，多退少补）
    ar_ap_item_id VARCHAR(64) REFERENCES ar_ap_items(id),  -- 差额联动生成的调整台账（diff=0 时为空）
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_cam_trueup_line_weight CHECK (weight_bps >= 0 AND weight_bps <= 10000),
    CONSTRAINT chk_cam_trueup_line_actual CHECK (actual_allocated_amount_cent >= 0),
    CONSTRAINT chk_cam_trueup_line_prepaid CHECK (prepaid_amount_cent >= 0),
    CONSTRAINT uq_cam_trueup_lines_run_merchant UNIQUE (run_id, merchant_id)
);

CREATE INDEX IF NOT EXISTS idx_cam_trueup_lines_merchant
    ON cam_trueup_lines (merchant_id, created_at DESC);

-- ── 3) 退款方向（refund_direction）统一维度 ─────────────────────────────────────
-- original 原路退回 / cash 现金退 / third_party 第三方代退（如渠道方）/ mixed 混合退。
ALTER TABLE finance_refunds
    ADD COLUMN IF NOT EXISTS refund_direction VARCHAR(32);

-- 历史行回填：默认 original（原路退回为最常见、最安全的默认语义）。
UPDATE finance_refunds
   SET refund_direction = 'original'
 WHERE refund_direction IS NULL;

ALTER TABLE finance_refunds
    ALTER COLUMN refund_direction SET DEFAULT 'original';

ALTER TABLE finance_refunds
    ALTER COLUMN refund_direction SET NOT NULL;

ALTER TABLE finance_refunds
    DROP CONSTRAINT IF EXISTS chk_finance_refunds_direction;
ALTER TABLE finance_refunds
    ADD CONSTRAINT chk_finance_refunds_direction
    CHECK (refund_direction IN ('original', 'cash', 'third_party', 'mixed'));

CREATE INDEX IF NOT EXISTS idx_finance_refunds_direction
    ON finance_refunds (refund_direction, created_at DESC);

-- ── 4) seed 权限（参照 V110 finance 权限 INSERT 模式）────────────────────────────
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('finance.cam.trueup', 'Run annual CAM true-up settlement', 'finance', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('finance.cam.trueup')
ON CONFLICT (role_id, permission_code) DO NOTHING;
