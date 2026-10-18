-- 文件版本：V0.01
-- FS-004：逐次交付、批准差异处理与统一隔离库存。
ALTER TABLE dealer_orders ADD COLUMN delivery_adjustment_cent BIGINT NOT NULL DEFAULT 0 CHECK (delivery_adjustment_cent >= 0);
ALTER TABLE dealer_orders ADD COLUMN delivery_completed_at TIMESTAMPTZ;

CREATE TABLE dealer_delivery_batches (
 id VARCHAR(128) PRIMARY KEY, order_id VARCHAR(128) NOT NULL REFERENCES dealer_orders(id),
 resolution_id VARCHAR(128) UNIQUE, shipped_by VARCHAR(128) NOT NULL, shipped_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE dealer_delivery_batch_lines (
 batch_id VARCHAR(128) NOT NULL REFERENCES dealer_delivery_batches(id), order_line_id VARCHAR(128) NOT NULL REFERENCES dealer_order_lines(id),
 quantity INTEGER NOT NULL CHECK (quantity > 0), PRIMARY KEY(batch_id, order_line_id)
);
CREATE TABLE dealer_delivery_receipts (
 id VARCHAR(128) PRIMARY KEY, order_id VARCHAR(128) NOT NULL REFERENCES dealer_orders(id),
 batch_id VARCHAR(128) NOT NULL REFERENCES dealer_delivery_batches(id), confirmed_at TIMESTAMPTZ NOT NULL,
 buyer_contact TEXT NOT NULL, evidence_attachment_ids JSONB NOT NULL CHECK(jsonb_array_length(evidence_attachment_ids)>0),
 note TEXT, created_by VARCHAR(128) NOT NULL, created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE dealer_delivery_receipt_lines (
 id VARCHAR(128) PRIMARY KEY, receipt_id VARCHAR(128) NOT NULL REFERENCES dealer_delivery_receipts(id),
 order_line_id VARCHAR(128) NOT NULL REFERENCES dealer_order_lines(id),
 accepted_quantity INTEGER NOT NULL CHECK(accepted_quantity>=0), rejected_quantity INTEGER NOT NULL CHECK(rejected_quantity>=0),
 damaged_quantity INTEGER NOT NULL CHECK(damaged_quantity>=0), short_quantity INTEGER NOT NULL CHECK(short_quantity>=0),
 CHECK(accepted_quantity+rejected_quantity+damaged_quantity+short_quantity>0), UNIQUE(receipt_id,order_line_id)
);
CREATE TABLE dealer_delivery_discrepancies (
 id VARCHAR(128) PRIMARY KEY, order_id VARCHAR(128) NOT NULL REFERENCES dealer_orders(id),
 receipt_line_id VARCHAR(128) NOT NULL REFERENCES dealer_delivery_receipt_lines(id),
 order_line_id VARCHAR(128) NOT NULL REFERENCES dealer_order_lines(id),
 discrepancy_type VARCHAR(32) NOT NULL CHECK(discrepancy_type IN ('rejected','damaged','short')),
 quantity INTEGER NOT NULL CHECK(quantity>0), created_at TIMESTAMPTZ NOT NULL,
 UNIQUE(receipt_line_id,discrepancy_type)
);
CREATE TABLE dealer_delivery_resolutions (
 id VARCHAR(128) PRIMARY KEY, discrepancy_id VARCHAR(128) NOT NULL REFERENCES dealer_delivery_discrepancies(id),
 quantity INTEGER NOT NULL CHECK(quantity>0), resolution_type VARCHAR(32) NOT NULL CHECK(resolution_type IN ('replacement','price_adjustment')),
 physical_route VARCHAR(32) NOT NULL CHECK(physical_route IN ('return_resalable','return_quarantine','loss_confirmed','accepted_as_is')),
 adjustment_cent BIGINT NOT NULL CHECK(adjustment_cent>=0),
 CHECK(resolution_type<>'replacement' OR adjustment_cent=0),
 responsibility_party TEXT NOT NULL, agreement_basis TEXT NOT NULL,
 evidence_attachment_ids JSONB NOT NULL CHECK(jsonb_array_length(evidence_attachment_ids)>0),
 review_id VARCHAR(128), status VARCHAR(32) NOT NULL CHECK(status IN ('pending_approval','approved','rejected','completed')),
 requested_by VARCHAR(128) NOT NULL, requested_at TIMESTAMPTZ NOT NULL,
 physical_completed_at TIMESTAMPTZ, physical_evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
 physical_completed_by VARCHAR(128), adjustment_applied_at TIMESTAMPTZ,
 completed_at TIMESTAMPTZ, updated_at TIMESTAMPTZ NOT NULL
);
ALTER TABLE dealer_delivery_batches ADD CONSTRAINT fk_delivery_batch_resolution FOREIGN KEY(resolution_id) REFERENCES dealer_delivery_resolutions(id);
CREATE UNIQUE INDEX uq_delivery_resolution_review ON dealer_delivery_resolutions(review_id) WHERE review_id IS NOT NULL;
CREATE INDEX idx_delivery_receipts_order ON dealer_delivery_receipts(order_id,created_at,id);
CREATE INDEX idx_delivery_discrepancies_order ON dealer_delivery_discrepancies(order_id,id);
CREATE INDEX idx_delivery_resolutions_discrepancy ON dealer_delivery_resolutions(discrepancy_id,id);
ALTER TABLE dealer_order_refunds ADD COLUMN delivery_resolution_id VARCHAR(128) REFERENCES dealer_delivery_resolutions(id);
ALTER TABLE dealer_order_refunds DROP CONSTRAINT dealer_order_refunds_review_id_key;
CREATE UNIQUE INDEX uq_dealer_refund_single_review ON dealer_order_refunds(review_id) WHERE delivery_resolution_id IS NULL;
CREATE INDEX idx_dealer_refund_delivery_resolution ON dealer_order_refunds(delivery_resolution_id) WHERE delivery_resolution_id IS NOT NULL;

-- 隔离是统一物理库存的组成部分，不参与 available/reserved；lot 是其来源/处置明细，不另计一遍总库存。
ALTER TABLE inventory_stock_balances ADD COLUMN quarantined_quantity INTEGER NOT NULL DEFAULT 0 CHECK(quarantined_quantity>=0);
ALTER TABLE inventory_stock_balances ADD CONSTRAINT chk_inventory_quarantine_within_quantity CHECK(quarantined_quantity<=quantity);
ALTER TABLE inventory_stock_balances ADD CONSTRAINT chk_inventory_quarantine_not_saleable
 CHECK(quarantined_quantity=0 OR quantity::BIGINT>=available_quantity::BIGINT+reserved_quantity::BIGINT+quarantined_quantity::BIGINT);
CREATE TABLE inventory_quarantine_lots (
 id VARCHAR(128) PRIMARY KEY, stock_balance_id VARCHAR(128) NOT NULL REFERENCES inventory_stock_balances(id),
 item_id VARCHAR(128) NOT NULL REFERENCES inventory_items(id), warehouse_id VARCHAR(128) NOT NULL REFERENCES warehouses(id),
 project_id VARCHAR(128) NOT NULL, source_type VARCHAR(64) NOT NULL, source_id VARCHAR(128) NOT NULL,
 quantity INTEGER NOT NULL CHECK(quantity>0), remaining_quantity INTEGER NOT NULL CHECK(remaining_quantity>=0 AND remaining_quantity<=quantity),
 evidence_attachment_ids JSONB NOT NULL CHECK(jsonb_array_length(evidence_attachment_ids)>0),
 received_by VARCHAR(128) NOT NULL, received_at TIMESTAMPTZ NOT NULL, UNIQUE(source_type,source_id)
);
CREATE TABLE inventory_quarantine_dispositions (
 id VARCHAR(128) PRIMARY KEY, lot_id VARCHAR(128) NOT NULL REFERENCES inventory_quarantine_lots(id),
 quantity INTEGER NOT NULL CHECK(quantity>0), disposition_type VARCHAR(32) NOT NULL CHECK(disposition_type IN ('release_resalable','scrap')),
 reason TEXT NOT NULL, evidence_attachment_ids JSONB NOT NULL CHECK(jsonb_array_length(evidence_attachment_ids)>0),
 review_id VARCHAR(128), status VARCHAR(32) NOT NULL CHECK(status IN ('pending_approval','approved','rejected','completed')),
 requested_by VARCHAR(128) NOT NULL, requested_at TIMESTAMPTZ NOT NULL,
 completed_by VARCHAR(128), completed_at TIMESTAMPTZ, completion_evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb
);
CREATE INDEX idx_quarantine_lots_warehouse ON inventory_quarantine_lots(warehouse_id,item_id,id);
CREATE UNIQUE INDEX uq_quarantine_disposition_review ON inventory_quarantine_dispositions(review_id) WHERE review_id IS NOT NULL;

-- 审批策略须由 root 按当前 refund/库存调整策略的真实结构补齐：
-- dealer.delivery_resolution / dealer_delivery_resolution：双方确认后的差异方案审批，quantity/price/责任/实物路径在批准前锁定。
-- inventory.quarantine_disposition / inventory_quarantine_disposition：使用统一审批体系，批准不等于实物处置。
-- 不复制历史回填，不自动导入旧 signed 订单的虚拟收货数量。

