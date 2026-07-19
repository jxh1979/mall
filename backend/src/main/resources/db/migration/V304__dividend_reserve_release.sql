-- V304 分红发放保护线留存·释放机制（建设 R；建设 O 的 V303 留存待释放应付的释放侧闭环）。
-- 口径（用户 2026-07-19 拍板）：发放保护线留存 = 暂扣待释放返还——留存是欠主理人的、以后要还，非平台白拿。
--   V303 审批时把留存记为待释放应付（ar_ap_items direction=payable, source_type='dividend_reserve_withheld',
--   source_id=计算单 id）。本迁移补释放侧：按方案释放规则（到期/手动）把该待释放应付「释放」为可发放，
--   使主理人能真正拿到这笔钱。
--
-- 释放动作口径：re-tag 既有待释放应付的 source_type（dividend_reserve_withheld → dividend_reserve_released），
--   金额不变（=原留存额，不多不少），全额一次释放；不新挂第二笔应付（否则同一笔钱双记 = 平台欠 2 倍）。
--   released 应付走既有应收应付付款流程真正发放给主理人。未释放（withheld）应付被支付守卫拦截（app 层
--   settleArApItem：source_type=dividend_reserve_withheld 直接拒付，须先释放）——「待释放」是真锁不是标签。
--   幂等：同一计算单的留存只释放一次（uq_dividend_reserve_releases_statement）+ re-tag 前态守卫（仅 withheld 可翻）。

-- ── 1. 方案配置：释放规则（释放时点，可配；默认手动=不自动释放，最安全，人工决策每一笔） ──────────
-- 释放时点 reserve_release_mode：
--   manual        手动释放（默认）：仅后台手动触发，不自动释放。
--   next_period   下期返还：正式单期间结束后（进入下一期）即到期，自动作业释放。
--   fixed_delay   固定延后 N 期：期间结束后再过 N 个周期（季度方案 N×3 月 / 月度方案 N 月）到期。
--   annual        年度末清算：计算单期间所在自然年 12-31 之后到期。
-- reserve_release_delay_periods：fixed_delay 的 N（其余模式忽略）；>=1。
ALTER TABLE dividend_plans
    ADD COLUMN IF NOT EXISTS reserve_release_mode varchar(32) NOT NULL DEFAULT 'manual',
    ADD COLUMN IF NOT EXISTS reserve_release_delay_periods int NOT NULL DEFAULT 1;

ALTER TABLE dividend_plans DROP CONSTRAINT IF EXISTS chk_dp_reserve_release_mode;
ALTER TABLE dividend_plans ADD CONSTRAINT chk_dp_reserve_release_mode
    CHECK (reserve_release_mode IN ('manual', 'next_period', 'fixed_delay', 'annual'));
ALTER TABLE dividend_plans DROP CONSTRAINT IF EXISTS chk_dp_reserve_release_delay;
ALTER TABLE dividend_plans ADD CONSTRAINT chk_dp_reserve_release_delay
    CHECK (reserve_release_delay_periods >= 1);

-- ── 2. 释放台账：每张计算单的留存释放留痕（一单一释放=幂等；追溯释放时间/触发方式/关联原计算单/金额）──
CREATE TABLE IF NOT EXISTS dividend_reserve_releases (
    id varchar(64) PRIMARY KEY,
    statement_id varchar(64) NOT NULL,          -- 原计算单 id（留存来源）
    merchant_id varchar(64) NOT NULL,
    principal_user_id varchar(64) NOT NULL,     -- 收款主理人（=计算单主理人）
    reserve_cent bigint NOT NULL,               -- 释放额 = 原留存额（不多不少）
    ar_ap_item_id varchar(128) NOT NULL,        -- 被释放（re-tag 为 released）的应付项 id
    trigger_type varchar(32) NOT NULL,          -- manual（手动）/ scheduled（定时作业）
    released_by varchar(64) NOT NULL,           -- 操作人（手动=后台账号 / 定时=系统操作人）
    released_at timestamptz NOT NULL,
    note text,
    CONSTRAINT chk_drr_reserve_positive CHECK (reserve_cent > 0),
    CONSTRAINT chk_drr_trigger CHECK (trigger_type IN ('manual', 'scheduled')),
    -- 幂等硬保证：同一计算单的留存只允许释放一次（重复释放被主键/唯一索引拦下）。
    CONSTRAINT uq_dividend_reserve_releases_statement UNIQUE (statement_id)
);

CREATE INDEX IF NOT EXISTS ix_dividend_reserve_releases_merchant
    ON dividend_reserve_releases (merchant_id, released_at DESC);

-- ── 3. 生产受限运行时权限（V296/V297/V298/V299/V303 教训：新表/新列一律再 GRANT complex_app 兜底）──
-- 释放台账 append-only（仅 INSERT/SELECT，无 UPDATE/DELETE——释放不可改，留痕永久）。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT ON dividend_reserve_releases TO complex_app';
        -- 新列由既有 dividend_plans 表级 GRANT 覆盖，此处幂等重授属铁律兜底（防列级权限漂移）。
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_plans TO complex_app';
    END IF;
END $$;
