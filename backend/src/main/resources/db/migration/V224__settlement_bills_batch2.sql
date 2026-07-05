-- V224 结算重构批2-2:月度分账结算单(单头 settlement_bills + 各酒吧明细 settlement_items)。
--   口径:结算重构 PRD v1.0 §5.0 通票分账全流程,月结(用户 2026-07-04)。
--   月度分账作业(批2-3)按当月 settlement_transactions 汇总 → 算池/留存/各酒吧分配 → 写本两表 → 过账 ar_ap_items。
--   本迁移仅建两表,不改现有表/逻辑,可独立回滚。

-- ① 月度结算单头(每 项目公司 × 结算月 一张,§5.0 步骤①-⑥)
CREATE TABLE IF NOT EXISTS settlement_bills (
    id                  VARCHAR(64) PRIMARY KEY,
    project_id          VARCHAR(64),
    period_start        DATE        NOT NULL,       -- 结算月起(含)
    period_end          DATE        NOT NULL,       -- 结算月止(含)
    ticket_revenue_cent BIGINT      NOT NULL DEFAULT 0,  -- ① 当月通票总收入(settlement_transactions verified 聚合)
    channel_fee_cent    BIGINT      NOT NULL DEFAULT 0,  -- ② 渠道费(最先扣)
    giveaway_cost_cent  BIGINT      NOT NULL DEFAULT 0,  -- ③ 赠酒成本(核销杯数×单价)
    available_pool_cent BIGINT      NOT NULL DEFAULT 0,  -- ④ 可分配池 = 收入 − 渠道费 − 赠酒成本
    retention_bps       INT         NOT NULL DEFAULT 0,  -- ⑤ 留存比例快照(万分比)
    retention_cent      BIGINT      NOT NULL DEFAULT 0,  -- ⑤ 项目公司留存额 = 池 × 留存%
    spv_pool_cent       BIGINT      NOT NULL DEFAULT 0,  -- ⑥ 各酒吧总池 = 池 − 留存
    distributed_cent    BIGINT      NOT NULL DEFAULT 0,  -- ⑦ 各酒吧实际分配合计(须 ≤ spv_pool_cent,除非特批)
    distribution_method VARCHAR(24),                     -- 本期分配方式快照(by_venue_per_guest/by_rank_tier/by_fixed_amount)
    special_approval_ref VARCHAR(128),                   -- 超总池特批引用(§5.0:仅平台负责人特批短期激励≤3月)
    status              VARCHAR(24) NOT NULL DEFAULT 'draft',  -- draft / confirmed / settled / reversed
    confirmed_at        TIMESTAMPTZ,
    settled_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_sb_status CHECK (status IN ('draft','confirmed','settled','reversed')),
    CONSTRAINT chk_sb_amount CHECK (ticket_revenue_cent >= 0 AND channel_fee_cent >= 0
                                AND giveaway_cost_cent >= 0 AND available_pool_cent >= 0
                                AND retention_cent >= 0 AND spv_pool_cent >= 0 AND distributed_cent >= 0),
    CONSTRAINT chk_sb_period CHECK (period_end >= period_start)
);
-- 同 项目×结算月 唯一(月度作业幂等 upsert 落点)
CREATE UNIQUE INDEX IF NOT EXISTS uq_sb_project_period
    ON settlement_bills (COALESCE(project_id, '__DEFAULT__'), period_start, period_end);
CREATE INDEX IF NOT EXISTS idx_sb_status_period ON settlement_bills (status, period_start);

-- ② 各酒吧分账明细(每 结算单 × 酒吧 一行,§5.0 步骤⑦)
CREATE TABLE IF NOT EXISTS settlement_items (
    id                    VARCHAR(64) PRIMARY KEY,
    bill_id               VARCHAR(64) NOT NULL REFERENCES settlement_bills(id),
    merchant_id           VARCHAR(64) NOT NULL,          -- 酒吧商户(经 merchants.business_entity_id 溯源 SPV)
    verified_count        INT         NOT NULL DEFAULT 0,  -- 当月该酒吧通票核销数
    guest_count           INT         NOT NULL DEFAULT 0,  -- 当月该酒吧客数(by_venue_per_guest 用)
    rank_no               INT,                            -- 当月核销排名(by_rank_tier 用)
    distribution_method   VARCHAR(24) NOT NULL,           -- 分配方式
    calculated_amount_cent BIGINT     NOT NULL DEFAULT 0,  -- 按方式算得分账额
    final_amount_cent     BIGINT      NOT NULL DEFAULT 0,  -- 特批调整后(默认=calculated)
    ar_ap_item_id         VARCHAR(64),                    -- 过账关联 ar_ap_items.id(posted 时)
    status                VARCHAR(24) NOT NULL DEFAULT 'draft',  -- draft / posted / reversed
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_si_status CHECK (status IN ('draft','posted','reversed')),
    CONSTRAINT chk_si_amount CHECK (calculated_amount_cent >= 0 AND final_amount_cent >= 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_si_bill_merchant ON settlement_items (bill_id, merchant_id);
CREATE INDEX IF NOT EXISTS idx_si_merchant ON settlement_items (merchant_id);
