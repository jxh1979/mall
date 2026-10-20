-- 文件版本：V0.01
-- V519：TK-24 订台定金不抵扣消费、达标各笔原路退回（第十批）。
-- 依据：BSM L889「定金不抵扣消费、不计入最低消费累计；客人在该台位的消费达到该台位适用的最低消费后，定金原路退回；
--   到店未达最低消费、爽约或超时取消的，按既有规则没收」；BSM L1062「每笔定金保留原订台单、原付款人、付款渠道和金额」
--   「达标时，各笔定金分别原路退回」；ENUM L103 订台定金状态「达标已原路退回」；裁决 DEC-R6-05、Q27。
-- 铁律 8：不为存量数据回填；历史 deposit_settlement='deducted' 只读保留，新流程不再写入。

-- 1) 定金付款渠道（支付时写入，退回时「原路」依据）与定金处置时点。
ALTER TABLE bar_reservations ADD COLUMN IF NOT EXISTS deposit_channel VARCHAR(32);
ALTER TABLE bar_reservations ADD COLUMN IF NOT EXISTS deposit_settled_at TIMESTAMPTZ;
COMMENT ON COLUMN bar_reservations.deposit_channel IS '定金付款渠道（payDeposit 写入；达标退回按此原路）';
COMMENT ON COLUMN bar_reservations.deposit_settled_at IS '定金处置（达标退回/退还/没收）时点';

-- 2) 处置闭集扩 refunded_on_threshold（达标已原路退回）；列宽由 16 扩到 32 容纳新值。
ALTER TABLE bar_reservations ALTER COLUMN deposit_settlement TYPE VARCHAR(32);
ALTER TABLE bar_reservations DROP CONSTRAINT IF EXISTS chk_bar_reservations_deposit_settle;
ALTER TABLE bar_reservations ADD CONSTRAINT chk_bar_reservations_deposit_settle CHECK (
    deposit_settlement IS NULL
    OR deposit_settlement IN ('deducted', 'refunded', 'forfeited', 'refunded_on_threshold')
);
