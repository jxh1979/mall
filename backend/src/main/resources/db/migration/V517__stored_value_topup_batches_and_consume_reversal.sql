-- 文件版本：V0.01
-- V517：TK-19 储值充值批次与消费分摊、TK-20 消费退款回原卡（第十批）。
-- 依据：ENUM「储值卡类型与收款主体」：「通用卡的每笔充值分别保留当前剩余本金、支付通路和支付成功时固化费率，同一通路不同费率的
--   充值分批保存，不得用新费率覆盖旧批次。每次消费需扣本金时，按消费前各批次剩余本金占总剩余本金的比例分摊本次本金扣减，
--   并逐批按其原固化费率计算对应通路费；本金已耗尽的批次不参与」「通用卡消费退款……该本金按原消费各充值批次已扣且尚未回冲本金
--   的比例恢复到原批次，保留原支付通路和原固化费率……原消费、退款恢复及调整记录分别保留」
--   「综合体通用卡和商户级卡的消费退款均退回原消费卡余额，固定先回冲……赠送金……才回冲……本金」；裁决 Q34/Q35/Q35A、DEC-P1A-04。
-- 铁律 8：不为存量数据回填；存量充值无批次、存量消费无分摊。

-- 1) 通用卡充值批次：id=充值流水 id；费率为该笔充值支付成功时固化的通路费率（万分比），后续改价不覆盖。
CREATE TABLE IF NOT EXISTS stored_value_topup_batches (
    id                      VARCHAR(64) PRIMARY KEY REFERENCES customer_stored_value_transactions(id),
    account_id              VARCHAR(64) NOT NULL REFERENCES customer_stored_value_accounts(id) ON DELETE CASCADE,
    customer_id             VARCHAR(64) NOT NULL,
    project_id              VARCHAR(64),
    payment_channel         VARCHAR(64) NOT NULL,
    fee_bps                 INTEGER NOT NULL DEFAULT 0,
    principal_cent          BIGINT NOT NULL,
    remaining_principal_cent BIGINT NOT NULL,
    paid_at                 TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_svtb_fee_bps CHECK (fee_bps >= 0 AND fee_bps <= 10000),
    CONSTRAINT chk_svtb_remaining CHECK (remaining_principal_cent >= 0 AND remaining_principal_cent <= principal_cent)
);
CREATE INDEX IF NOT EXISTS idx_svtb_account_open ON stored_value_topup_batches (account_id, paid_at, id)
    WHERE remaining_principal_cent > 0;
COMMENT ON TABLE stored_value_topup_batches IS '通用卡充值批次：剩余本金、支付通路、支付成功时固化的通路费率（ENUM「储值卡类型与收款主体」）';

-- 2) 消费本金按批次分摊（原消费记录）：每批扣减本金与按原固化费率算出的通路费；restored_principal_cent 累计退款恢复，不超原扣。
CREATE TABLE IF NOT EXISTS stored_value_consume_allocations (
    id                      VARCHAR(64) PRIMARY KEY,
    consume_txn_id          VARCHAR(64) NOT NULL REFERENCES customer_stored_value_transactions(id),
    batch_id                VARCHAR(64) NOT NULL REFERENCES stored_value_topup_batches(id),
    principal_cent          BIGINT NOT NULL,
    restored_principal_cent BIGINT NOT NULL DEFAULT 0,
    fee_bps                 INTEGER NOT NULL,
    fee_cent                BIGINT NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_svca_principal CHECK (principal_cent > 0),
    CONSTRAINT chk_svca_restored CHECK (restored_principal_cent >= 0 AND restored_principal_cent <= principal_cent)
);
CREATE INDEX IF NOT EXISTS idx_svca_consume ON stored_value_consume_allocations (consume_txn_id);

-- 3) 退款恢复与通路费调整记录（与原消费分摊分别保留）：fee_adjust_cent 为按原费率冲减的通路费（负数）。
CREATE TABLE IF NOT EXISTS stored_value_consume_restorations (
    id                      VARCHAR(64) PRIMARY KEY,
    reversal_txn_id         VARCHAR(64) NOT NULL REFERENCES customer_stored_value_transactions(id),
    allocation_id           VARCHAR(64) NOT NULL REFERENCES stored_value_consume_allocations(id),
    principal_cent          BIGINT NOT NULL,
    fee_bps                 INTEGER NOT NULL,
    fee_adjust_cent         BIGINT NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_svcr_principal CHECK (principal_cent > 0)
);
CREATE INDEX IF NOT EXISTS idx_svcr_reversal ON stored_value_consume_restorations (reversal_txn_id);

-- 4) 消费退款回原卡流水类型 consume_reversal（两段正向加回），original_txn_id 回指原消费流水。
ALTER TABLE customer_stored_value_transactions
    ADD COLUMN IF NOT EXISTS original_txn_id VARCHAR(64);
ALTER TABLE customer_stored_value_transactions DROP CONSTRAINT IF EXISTS chk_csvt_type;
ALTER TABLE customer_stored_value_transactions
    ADD CONSTRAINT chk_csvt_type CHECK (txn_type IN ('topup','consume','refund','adjust','consume_reversal'));
CREATE INDEX IF NOT EXISTS idx_csvt_original_txn ON customer_stored_value_transactions (original_txn_id)
    WHERE original_txn_id IS NOT NULL;
COMMENT ON COLUMN customer_stored_value_transactions.original_txn_id IS 'consume_reversal 回指的原消费流水 id';

-- 5) 酒吧退款分项（V489）：回冲到储值卡的退款不挂应付，ar_ap_item_id 允许为空（非储值渠道部分仍引用应付行）。
ALTER TABLE bar_payroll_refund_lines ALTER COLUMN ar_ap_item_id DROP NOT NULL;
