-- 文件版本：V0.01
-- V503：FS-011 门店要货（供货调拨）九态、转换留痕与合格签收。
-- 依据：BSM L1212「门店要货依次经过门店提交、供货主体审核、仓库拣配、出库、配送、门店签收和差异处理；每次转换记录操作者和时间。
--   签收分别记录实收、合格接收、拒收和短少数量；合格接收部分转移货权、计入门店库存，并按已锁定内部结算价形成货款。未出库数量可经审批取消」；
--   BSM L1213 / ENUM L169 九态；台账 06｜Q09；操作员裁决 2026-10-19 第 2 条（供货主体不同意退回草稿，门店改后可重新提交，不想要了再自行取消）。

-- 1) 供货调拨状态扩为九态（草稿、待供货主体审核、待拣配、待出库、配送中、待签收、差异处理中、已完成、已取消）；借调沿用原码。
ALTER TABLE inventory_transfer_orders ALTER COLUMN status TYPE varchar(32);
ALTER TABLE inventory_transfer_orders DROP CONSTRAINT inv_transfer_status_check;
ALTER TABLE inventory_transfer_orders ADD CONSTRAINT inv_transfer_status_check CHECK (status IN (
    'created', 'shipped', 'received', 'settled', 'returned', 'cancelled',
    'draft', 'pending_supplier_review', 'pending_picking', 'pending_outbound', 'in_delivery', 'pending_receipt',
    'discrepancy', 'completed'));

-- 2) 已提交、未出库的取消须经审批：挂起中的审批单。
ALTER TABLE inventory_transfer_orders ADD COLUMN cancel_review_id varchar(64) REFERENCES supervisor_review_decisions(review_id);

-- 3) 每次状态转换一行：从哪个状态到哪个状态、操作者、时间（只追加）。
CREATE TABLE inventory_transfer_order_events (
    id                varchar(64)  PRIMARY KEY,
    transfer_order_id varchar(64)  NOT NULL REFERENCES inventory_transfer_orders(id),
    from_status       varchar(32),
    to_status         varchar(32)  NOT NULL,
    operator_id       varchar(128) NOT NULL,
    note              varchar(500),
    operated_at       timestamptz  NOT NULL
);
CREATE INDEX idx_transfer_order_events_order ON inventory_transfer_order_events(transfer_order_id, operated_at);

-- 4) 签收按行登记实收、合格接收、拒收、短少；合格部分的入库流水与货款应付回指（同一次签收共用 receipt_no 与一笔应付）。
CREATE TABLE inventory_transfer_receipts (
    id                   varchar(128) PRIMARY KEY,
    receipt_no           varchar(128) NOT NULL,
    transfer_order_id    varchar(64)  NOT NULL REFERENCES inventory_transfer_orders(id),
    line_id              varchar(64)  NOT NULL REFERENCES inventory_transfer_lines(id),
    arrived_quantity     integer      NOT NULL,
    accepted_quantity    integer      NOT NULL,
    rejected_quantity    integer      NOT NULL,
    short_quantity       integer      NOT NULL,
    accepted_trace_codes jsonb        NOT NULL DEFAULT '[]',
    warehouse_id         varchar(64)  NOT NULL REFERENCES warehouses(id),
    location_id          varchar(64)  NOT NULL,
    movement_id          varchar(64)  REFERENCES inventory_stock_movements(id),
    payable_item_id      varchar(128) REFERENCES ar_ap_items(id),
    received_by          varchar(128) NOT NULL,
    received_at          timestamptz  NOT NULL,
    CONSTRAINT chk_transfer_receipt_quantities CHECK (arrived_quantity >= 0 AND accepted_quantity >= 0 AND rejected_quantity >= 0
        AND short_quantity >= 0 AND arrived_quantity = accepted_quantity + rejected_quantity),
    CONSTRAINT uq_transfer_receipt_line UNIQUE (receipt_no, line_id)
);
CREATE INDEX idx_transfer_receipts_order ON inventory_transfer_receipts(transfer_order_id);

-- 5) 受限运行时角色：留痕与签收只追加；草稿改单替换明细需删旧明细。
GRANT SELECT, INSERT ON inventory_transfer_order_events TO complex_app;
GRANT SELECT, INSERT ON inventory_transfer_receipts TO complex_app;
GRANT DELETE ON inventory_transfer_lines TO complex_app;
