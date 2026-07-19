-- V303 持股分红扣除项三开关（设计稿 §5 / PRD §9 留档项落地）。
-- 三个可选扣除，全部方案级可配、默认关闭=不改 V299 现有口径（默认全关时逐分等于现状）：
--   1) 亏损结转 loss_carryforward：可分配额 = GREATEST(净利 − 往期未弥补亏损累计, 0)。
--      累计不另建可变台账——往期未弥补亏损由「已审批/已发放的正式计算单」自身的
--      loss_carryforward_after_cent 快照逐期结转（approved/paid 单即账本，voided/draft 不计，
--      天然幂等 + voided 自动回滚），本期入账 in/applied/after 三值冻结在单上可追溯。
--   2) 欠款抵扣 arrears_offset：主理人应得（税后 principal_net）先抵扣该商户对平台未清应收
--      （ar_ap_items direction=receivable, unpaid>0），抵扣额冻结在 arrears_offset_cent，
--      审批通过时按冻结额对该商户应收逐项结算（dividend_offset 收款），抵扣后余额才挂应付发放。
--   3) 发放保护线 payout_floor：按比例(bps)或固定额留存风险准备金不发放，
--      实发 payout_cent = principal_net − arrears_offset − reserve_withheld。
-- 三扣除只削减「发放额」，不破坏 V299 净利/劈分/税恒等；可分配额定义随亏损结转开关分支（下方 CHECK 重写）。

-- ── 1. 方案配置：三开关 + 保护线参数 ─────────────────────────────────────────
ALTER TABLE dividend_plans
    ADD COLUMN IF NOT EXISTS loss_carryforward_enabled boolean NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS arrears_offset_enabled boolean NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS payout_floor_enabled boolean NOT NULL DEFAULT FALSE,
    -- 保护线：留存 = min(可发额, max(可发额×bps/10000, 固定额))；默认 0/0 即留 0（等于关）。
    ADD COLUMN IF NOT EXISTS payout_floor_bps int NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS payout_floor_fixed_cent bigint NOT NULL DEFAULT 0;

ALTER TABLE dividend_plans DROP CONSTRAINT IF EXISTS chk_dp_payout_floor_bps;
ALTER TABLE dividend_plans ADD CONSTRAINT chk_dp_payout_floor_bps
    CHECK (payout_floor_bps BETWEEN 0 AND 10000);
ALTER TABLE dividend_plans DROP CONSTRAINT IF EXISTS chk_dp_payout_floor_fixed;
ALTER TABLE dividend_plans ADD CONSTRAINT chk_dp_payout_floor_fixed
    CHECK (payout_floor_fixed_cent >= 0);

-- ── 2. 计算单快照：三扣除入账留痕列 ──────────────────────────────────────────
-- loss_carryforward_enabled 快照本期是否启用（CHECK 依它区分「启用的首个亏损期(in=0,after=|亏损|)」
-- 与「未启用(after=0)」，否则两者 in/applied 同为 0 无法判别）。
ALTER TABLE dividend_statements
    ADD COLUMN IF NOT EXISTS loss_carryforward_enabled boolean NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS loss_carryforward_in_cent bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS loss_carryforward_applied_cent bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS loss_carryforward_after_cent bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS arrears_offset_cent bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS reserve_withheld_cent bigint NOT NULL DEFAULT 0,
    -- 实发额（挂应付发放的口径，取代 principal_net 直发）。存量单回填 = principal_net（三扣除均 0）。
    ADD COLUMN IF NOT EXISTS payout_cent bigint NOT NULL DEFAULT 0;

-- 存量单回填：三扣除均 0 时实发 = 税后应得（口径与 V299 现状一致）。
UPDATE dividend_statements SET payout_cent = principal_net_cent WHERE payout_cent = 0;
-- 亏损结转累计恒结转（用户 2026-07-19 拍板：亏损一直累到被盈利填平，开关只控本期扣不扣）：after 恒 = in − applied + max(−净利,0)。
-- 存量单 in=applied=0 → 回填 after = GREATEST(−净利,0)（亏损期即本期亏损额，盈利期 0），以满足下方无条件 after CHECK。
UPDATE dividend_statements SET loss_carryforward_after_cent = GREATEST(-net_profit_cent, 0);

-- 可分配额只扣「本期结转弥补额 applied」（关闭期 applied=0 不扣；存量单 applied=0 → GREATEST(净利,0) 不变）。
ALTER TABLE dividend_statements DROP CONSTRAINT IF EXISTS chk_ds_distributable;
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_distributable
    CHECK (distributable_cent = GREATEST(net_profit_cent - loss_carryforward_applied_cent, 0));

-- 亏损结转链恒等（DB 层硬保证结转不丢/不造）：
--   applied = 启用? min(max(净利,0), 期初累计亏损) : 0（开关只控本期扣减，关闭期不扣）
--   after   = 恒 期初累计亏损 − applied + max(−净利,0)（累计恒结转，关闭期不清零）
ALTER TABLE dividend_statements DROP CONSTRAINT IF EXISTS chk_ds_lcf_nonneg;
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_lcf_nonneg
    CHECK (loss_carryforward_in_cent >= 0 AND loss_carryforward_applied_cent >= 0
           AND loss_carryforward_after_cent >= 0);
ALTER TABLE dividend_statements DROP CONSTRAINT IF EXISTS chk_ds_lcf_applied;
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_lcf_applied
    CHECK (loss_carryforward_applied_cent = CASE WHEN loss_carryforward_enabled
        THEN LEAST(GREATEST(net_profit_cent, 0), loss_carryforward_in_cent) ELSE 0 END);
ALTER TABLE dividend_statements DROP CONSTRAINT IF EXISTS chk_ds_lcf_after;
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_lcf_after
    CHECK (loss_carryforward_after_cent
        = loss_carryforward_in_cent - loss_carryforward_applied_cent + GREATEST(-net_profit_cent, 0));

-- 发放额恒等（税后应得 − 欠款抵扣 − 留存 = 实发，均非负；抵扣不超税后应得）。
ALTER TABLE dividend_statements DROP CONSTRAINT IF EXISTS chk_ds_arrears_range;
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_arrears_range
    CHECK (arrears_offset_cent >= 0 AND arrears_offset_cent <= principal_net_cent);
ALTER TABLE dividend_statements DROP CONSTRAINT IF EXISTS chk_ds_reserve_nonneg;
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_reserve_nonneg
    CHECK (reserve_withheld_cent >= 0);
ALTER TABLE dividend_statements DROP CONSTRAINT IF EXISTS chk_ds_payout_identity;
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_payout_identity
    CHECK (payout_cent = principal_net_cent - arrears_offset_cent - reserve_withheld_cent);
ALTER TABLE dividend_statements DROP CONSTRAINT IF EXISTS chk_ds_payout_nonneg;
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_payout_nonneg
    CHECK (payout_cent >= 0);

-- ── 3. 算式明细行：三扣除留痕行类型 ──────────────────────────────────────────
-- append-only（V140 触发器兜底），新增三类留痕行类型。
ALTER TABLE dividend_statement_lines DROP CONSTRAINT IF EXISTS chk_dsl_line_type;
ALTER TABLE dividend_statement_lines ADD CONSTRAINT chk_dsl_line_type CHECK (line_type IN
    ('revenue_bar', 'revenue_bar_refund', 'revenue_ticket', 'revenue_ticket_refund',
     'cost_inventory', 'expense_rent_allocation', 'expense_energy', 'expense_property_fee',
     'expense_labor', 'expense_amortization', 'expense_other',
     'net_profit', 'loss_carryforward', 'platform_share', 'principal_gross', 'tax_withheld',
     'principal_net', 'arrears_offset', 'reserve_withheld', 'payout'));

-- ── 4. 生产受限运行时权限（V298/V299 教训：新列一律再 GRANT complex_app 兜底）──
-- 新增列由既有表级 GRANT 覆盖，此处幂等重授属铁律兜底（防列级权限漂移）。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_plans TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_statements TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT ON dividend_statement_lines TO complex_app';
        EXECUTE 'REVOKE UPDATE, DELETE ON dividend_statement_lines FROM complex_app';
    END IF;
END $$;
