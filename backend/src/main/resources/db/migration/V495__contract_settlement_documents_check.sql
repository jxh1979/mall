-- 文件版本：V0.01
-- HP-C02-R1 退租结算「齐单核对」事实。依据：BSM L1352／ENUM L252「退租对应期间的全部相关单据收齐并核对完成后，才允许退租结算最后审批通过；
-- 缺单时保持待处理」；裁决 Q16A「缺单保持待处理。不是齐单前连审批也不得发起」。核对事实挂在结算单本身（与 ENUM L252 诸态同一张单），空＝尚未核对。
ALTER TABLE contract_settlements
    ADD COLUMN IF NOT EXISTS documents_checked_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS documents_checked_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS documents_check_note VARCHAR(500);
