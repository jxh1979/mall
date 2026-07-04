-- V222 结算重构批1地基:settlement_transactions 按核销逐笔结算流水表 + 双写观察对账表。
--   背景:现状核销与结算解耦——核销仅更新 issued_entitlements.used_times + 发积分/赠饮事件、不记账;
--         analytics 按 issued_entitlements.allocated_revenue_cent(出票即分摊快照)聚合商户 net。
--   批1:新建 settlement_transactions,核销时双写(transaction_type=verification_settled),
--         捕捉已核销/未核销金额三段;保留旧 allocated_revenue_cent 字段与查询不动;
--         双写观察期用 settlement_reconciliation_status 按日比对新旧口径差异(未核销部分=预期差异来源);
--         未核销权益到期由后续 SettlementExpiryJob 归项目(transaction_type=unverified_expired,beneficiary_type=project)。
--   口径:结算重构 PRD v1.0 §5.2(按核销结算)/§11(迁移双写观察)。verified+unverified=allocated。
--   本迁移仅建两张新表,不改任何现有表/逻辑,可独立回滚。

CREATE TABLE IF NOT EXISTS settlement_transactions (
    id                     VARCHAR(64)  PRIMARY KEY,
    transaction_no         VARCHAR(128) NOT NULL,
    transaction_type       VARCHAR(32)  NOT NULL,   -- verification_settled(核销结算) / unverified_expired(过期未核销归项目)
    entitlement_id         VARCHAR(64)  NOT NULL,   -- 溯源 issued_entitlements.id
    order_id               VARCHAR(64),             -- 可空(direct 出票无订单)
    ticket_type_id         VARCHAR(64),
    verification_id        VARCHAR(64),             -- 核销记录 id(未核销/过期归项目时为 NULL)
    entitlement_type       VARCHAR(64)  NOT NULL,
    verified_count         INT          NOT NULL DEFAULT 0,   -- 已核销次数
    total_times            INT          NOT NULL,             -- 权益总次数
    num_people             INT          NOT NULL DEFAULT 1,
    allocated_revenue_cent BIGINT       NOT NULL,             -- 权益总分配收入(issued_entitlements 快照)
    verified_amount_cent   BIGINT       NOT NULL DEFAULT 0,   -- 已核销部分金额
    unverified_amount_cent BIGINT       NOT NULL DEFAULT 0,   -- 未核销部分金额(verified+unverified=allocated)
    beneficiary_type       VARCHAR(16)  NOT NULL,   -- merchant(核销归商户) / project(未核销归项目公司)
    merchant_id            VARCHAR(64),             -- beneficiary_type=merchant 时
    project_id             VARCHAR(64),             -- 项目维度(综合体)
    settlement_status      VARCHAR(32)  NOT NULL DEFAULT 'draft',  -- draft / confirmed / posted / reversed
    ar_ap_item_id          VARCHAR(64),             -- settlement_status=posted 时关联 ar_ap_items.id
    entitlement_valid_to   TIMESTAMPTZ  NOT NULL,   -- 权益有效期截止(过期判定)
    confirmed_at           TIMESTAMPTZ,
    posted_at              TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_st_type        CHECK (transaction_type IN ('verification_settled','unverified_expired')),
    CONSTRAINT chk_st_beneficiary CHECK (beneficiary_type IN ('merchant','project')),
    CONSTRAINT chk_st_status      CHECK (settlement_status IN ('draft','confirmed','posted','reversed')),
    CONSTRAINT chk_st_amount      CHECK (verified_amount_cent >= 0 AND unverified_amount_cent >= 0 AND allocated_revenue_cent >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_st_transaction_no    ON settlement_transactions (transaction_no);
CREATE INDEX        IF NOT EXISTS idx_st_merchant_created  ON settlement_transactions (merchant_id, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_st_entitlement       ON settlement_transactions (entitlement_id);
CREATE INDEX        IF NOT EXISTS idx_st_status_validto    ON settlement_transactions (settlement_status, entitlement_valid_to);
-- 同一权益的"核销结算"交易唯一(幂等):核销双写时按 entitlement 累积更新,而非重复插入。
CREATE UNIQUE INDEX IF NOT EXISTS uq_st_entitlement_verified
    ON settlement_transactions (entitlement_id)
 WHERE transaction_type = 'verification_settled';

-- 双写观察期:旧口径(allocated 快照)vs 新口径(settlement 逐笔)按日对账,未核销部分为预期差异来源。
CREATE TABLE IF NOT EXISTS settlement_reconciliation_status (
    reconciliation_date        DATE         PRIMARY KEY,
    old_ticket_allocated_cent  BIGINT       NOT NULL DEFAULT 0,   -- 旧:issued_entitlements.allocated 当日聚合
    new_settlement_cent        BIGINT       NOT NULL DEFAULT 0,   -- 新:settlement_transactions 当日聚合
    variance_cent              BIGINT       NOT NULL DEFAULT 0,   -- 差异(未核销部分是预期差异)
    reconciled                 BOOLEAN      NOT NULL DEFAULT FALSE,
    reconciled_by              VARCHAR(64),
    reconciled_at              TIMESTAMPTZ,
    created_at                 TIMESTAMPTZ  NOT NULL DEFAULT now()
);
