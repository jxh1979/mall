-- 文件版本：V0.01
-- V515：FS-019 不可消费物品借用只转保管责任不转所有权、归还须借出方实际扫码按数量登记、无法归还双方确认与赔偿分别收尾（第九批）。
-- 依据：BSM L1201「「借」只转移保管责任、不转移所有权（仅适用于不可消费物品……）——借出方始终是这批货的所有权人」
--   「不可消费物品（设备、器具等）的借用必须约定归还时间，借用期间保管人变更为借入方并在溯源码上登记」
--   「溯源码同时记录所有权主体、当前保管主体、借出主体、借入主体、应归还数量和期限，并记录三个时间：借出时间、每一次保管人变更的时间、
--    实际归还时间。这三个时间都由已有的扫码动作自动生成，不设手工填写」「实际归还时间取借出方收货扫码的时刻」
--   「不可消费借用物无法归还时，关联原借用记录，由借出所有权方与借入保管方确认原因、数量和实物处置，并经主管及财务复核；赔偿……形成借入方对借出方的应付。
--    双方确认的实物处置实际完成后，结束对应数量的原物归还事项；未付赔偿款单独跟进直到实际清偿。借入方单方报废不构成归还」；
--   ENUM L166 同款；台账 §25、DEC-R3B-2026-08-04-04（三个时间）、DEC-R7-14（可消费物品不存在保留所有权的借）、2026-10-17 批准 13｜Q18。

-- 1) 借用（lending+borrow）必须约定归还时间。
ALTER TABLE inventory_transfer_orders ADD COLUMN return_due_at timestamptz;
ALTER TABLE inventory_transfer_orders ADD CONSTRAINT chk_transfer_borrow_return_due
    CHECK (lending_mode <> 'borrow' OR return_due_at IS NOT NULL);

-- 2) 溯源码：所有权（owner_*）之外记当前保管主体与在借单。借用中 lending_order_id 指向借用单，归还或实物处置完成后置空；
--    custodian_* 为空＝保管人即所有权人。
ALTER TABLE inventory_trace_codes ADD COLUMN custodian_type varchar(20);
ALTER TABLE inventory_trace_codes ADD COLUMN custodian_id varchar(64);
ALTER TABLE inventory_trace_codes ADD COLUMN lending_order_id varchar(64);

-- 3) 归属变更台账加变更种类：ownership 所有权变更（买断、供货签收）；custody 保管人变更（借出方交货扫码＝借出时间、借入方收货扫码、
--    借出方收货扫码＝实际归还时间、实物处置完成）。时间都取扫码动作的时刻，只追加不改。
ALTER TABLE inventory_trace_ownership_history ADD COLUMN change_kind varchar(16) NOT NULL DEFAULT 'ownership';
ALTER TABLE inventory_trace_ownership_history ADD CONSTRAINT chk_trace_history_change_kind CHECK (change_kind IN ('ownership', 'custody'));

-- 4) 物料档案：只有不可消费物品（设备、器具等）才能借用；可消费物品的借用实质是买断。
ALTER TABLE inventory_items ADD COLUMN non_consumable boolean NOT NULL DEFAULT FALSE;

-- 5) 归还登记：按行、按数量、按码，借出方收货扫码时登记；received_at 即实际归还时间。
CREATE TABLE inventory_transfer_returns (
    id                varchar(128) PRIMARY KEY,
    return_no         varchar(128) NOT NULL,
    transfer_order_id varchar(64)  NOT NULL REFERENCES inventory_transfer_orders(id),
    line_id           varchar(64)  NOT NULL REFERENCES inventory_transfer_lines(id),
    returned_quantity integer      NOT NULL CHECK (returned_quantity > 0),
    trace_codes       jsonb        NOT NULL DEFAULT '[]',
    received_by       varchar(128) NOT NULL,
    received_at       timestamptz  NOT NULL,
    CONSTRAINT uq_transfer_return_line UNIQUE (return_no, line_id)
);
CREATE INDEX idx_transfer_returns_order ON inventory_transfer_returns(transfer_order_id);

-- 6) 无法归还（损坏/丢失）处置复用差异处置表：登记（待对方确认）→ 对方确认并挂主管复核（待主管复核）→ 主管通过（待财务复核）→
--    财务通过形成借入方对借出方应付 → 借出方登记实物处置完成（结束对应数量的归还事项）。
ALTER TABLE inventory_transfer_dispositions DROP CONSTRAINT inventory_transfer_dispositions_disposition_type_check;
ALTER TABLE inventory_transfer_dispositions ADD CONSTRAINT inventory_transfer_dispositions_disposition_type_check
    CHECK (disposition_type IN ('return_accepted', 'loss', 'reship', 'payment_adjustment', 'unreturnable'));
ALTER TABLE inventory_transfer_dispositions DROP CONSTRAINT inventory_transfer_dispositions_status_check;
ALTER TABLE inventory_transfer_dispositions ADD CONSTRAINT inventory_transfer_dispositions_status_check
    CHECK (status IN ('pending_counterparty', 'pending_supervisor', 'pending_review', 'approved', 'rejected', 'disposal_completed'));
ALTER TABLE inventory_transfer_dispositions DROP CONSTRAINT chk_transfer_disposition_reviewed;
ALTER TABLE inventory_transfer_dispositions ADD CONSTRAINT chk_transfer_disposition_reviewed
    CHECK ((status IN ('pending_counterparty', 'pending_supervisor', 'pending_review')) = (reviewed_at IS NULL));
ALTER TABLE inventory_transfer_dispositions ADD COLUMN confirmed_by varchar(128);
ALTER TABLE inventory_transfer_dispositions ADD COLUMN confirmed_at timestamptz;
ALTER TABLE inventory_transfer_dispositions ADD COLUMN supervisor_review_id varchar(64) REFERENCES supervisor_review_decisions(review_id);
ALTER TABLE inventory_transfer_dispositions ADD COLUMN disposal_note varchar(500);
ALTER TABLE inventory_transfer_dispositions ADD COLUMN disposal_completed_by varchar(128);
ALTER TABLE inventory_transfer_dispositions ADD COLUMN disposal_completed_at timestamptz;

-- 7) 受限运行时角色：归还登记只追加。
GRANT SELECT, INSERT ON inventory_transfer_returns TO complex_app;
