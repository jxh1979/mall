-- 文件版本：V0.01
-- V504：FS-011 门店要货差异处理——退回验收、损失、补发、货款调整的登记与财务复核。
-- 依据：BSM L1212「已出库数量须实际退回验收，或经复核完成损失处理，不能仅点取消就回库。供货、配送保管和收货方按交接证据确认差异，
--   财务复核金额后办理补发、退回、货款调整或责任损失，不重复收款。全部数量处理完且库存、款项记录齐全后完成要货单，付款是否结清另行显示」；台账 06｜Q09。
-- 规则：登记须附交接证据，复核通过才生效；退回验收与损失给拒收、短少的件一个结果（同一件只能有一个结果），全部件有结果才完成；
--   收货方担责的损失按锁定单价追加应付（payable_item_id），供货主体或配送保管方担责不形成应付；
--   补发通过后另开一张关联的要货单（reship_order_id）；货款调整按 V344 原行不动、追加冲正行与重述行（reversal_item_id / payable_item_id）。
CREATE TABLE inventory_transfer_dispositions (
    id                      varchar(64)  PRIMARY KEY,
    transfer_order_id       varchar(64)  NOT NULL REFERENCES inventory_transfer_orders(id),
    line_id                 varchar(64)  REFERENCES inventory_transfer_lines(id),
    disposition_type        varchar(32)  NOT NULL CHECK (disposition_type IN ('return_accepted', 'loss', 'reship', 'payment_adjustment')),
    quantity                integer      CHECK (quantity > 0),
    responsible_party       varchar(32)  CHECK (responsible_party IN ('supplier', 'carrier', 'receiver')),
    amount_cent             bigint       CHECK (amount_cent > 0),
    trace_codes             jsonb        NOT NULL DEFAULT '[]',
    reason                  varchar(500) NOT NULL,
    evidence_attachment_ids jsonb        NOT NULL,
    status                  varchar(32)  NOT NULL CHECK (status IN ('pending_review', 'approved', 'rejected')),
    requested_by            varchar(128) NOT NULL,
    requested_at            timestamptz  NOT NULL,
    reviewed_by             varchar(128),
    reviewed_at             timestamptz,
    review_reason           varchar(500),
    payable_item_id         varchar(128) REFERENCES ar_ap_items(id),
    reversal_item_id        varchar(128) REFERENCES ar_ap_items(id),
    reship_order_id         varchar(64)  REFERENCES inventory_transfer_orders(id),
    CONSTRAINT chk_transfer_disposition_shape CHECK (
        (disposition_type = 'payment_adjustment' AND line_id IS NULL AND quantity IS NULL AND amount_cent IS NOT NULL)
        OR (disposition_type <> 'payment_adjustment' AND line_id IS NOT NULL AND quantity IS NOT NULL)),
    CONSTRAINT chk_transfer_disposition_responsible CHECK ((disposition_type = 'loss') = (responsible_party IS NOT NULL)),
    CONSTRAINT chk_transfer_disposition_reviewed CHECK ((status = 'pending_review') = (reviewed_at IS NULL))
);
CREATE INDEX idx_transfer_dispositions_order ON inventory_transfer_dispositions(transfer_order_id, status);

GRANT SELECT, INSERT, UPDATE ON inventory_transfer_dispositions TO complex_app;
