-- P0 清单#2 积分入账引擎：customer_points_ledger 扩展为可幂等、可分状态的入账明细。
--
-- 背景（R6-D06）：支付成功入 pending、履约（核销/完成）转 confirmed、退款按已入账比例 deduct。
-- 既有列（V025 建表 + V089 增 expire_at/remaining_points/expired）：
--   id, customer_id, points_delta, balance_after, source_type, source_id, description,
--   expire_at, remaining_points, expired, created_at
--
-- 本迁移新增（全部可空 / 带默认，向后兼容既有 earn/expire/check_in/transfer 流水）：
--   change_type     变动类型：earn / deduct（兼容既有 expire / check_in / transfer_* 等沿用 source_type 含义）
--   status          入账状态：pending（支付待确认）/ confirmed（履约已入账）/ reversed（退款冲减后置位）
--   source_doc_type 业务单据类型：ticket_order / ticket_refund / ...（幂等键组成）
--   source_doc_id   业务单据 id（幂等键组成）

ALTER TABLE customer_points_ledger
    ADD COLUMN IF NOT EXISTS change_type     VARCHAR(32),
    ADD COLUMN IF NOT EXISTS status          VARCHAR(32) NOT NULL DEFAULT 'confirmed',
    ADD COLUMN IF NOT EXISTS source_doc_type VARCHAR(64),
    ADD COLUMN IF NOT EXISTS source_doc_id   VARCHAR(120);

-- 历史行回填 change_type=source_type（保持既有语义可读，不影响幂等：source_doc_* 为空不入唯一索引）。
UPDATE customer_points_ledger
   SET change_type = source_type
 WHERE change_type IS NULL;

-- 幂等唯一索引：同一业务单据 + 同一变动类型至多一条（重放不重复入账）。
-- 仅对带 source_doc 的入账引擎流水生效（partial index），既有无单据流水（expire/check_in/transfer）不受约束。
CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_points_ledger_source_doc
    ON customer_points_ledger (source_doc_type, source_doc_id, change_type)
    WHERE source_doc_type IS NOT NULL AND source_doc_id IS NOT NULL;

-- 履约/退款按单据查 pending/confirmed 流水（confirm 转 confirmed、deduct 按已 confirmed 比例冲减）。
CREATE INDEX IF NOT EXISTS idx_customer_points_ledger_source_doc_status
    ON customer_points_ledger (source_doc_type, source_doc_id, status);
