-- 文件版本：V0.01
-- FS-008/009/010。正式迁移号由 root 串行分配；无历史事实回填。
ALTER TABLE ar_ap_items DROP CONSTRAINT chk_ar_ap_due_date_contract_event;
ALTER TABLE ar_ap_items ADD CONSTRAINT chk_ar_ap_due_date_contract_event
 CHECK(due_date IS NOT NULL OR source_type IN('dealer_order','purchase_inbound','purchase_consignment'));
ALTER TABLE supply_contracts
 ADD COLUMN settlement_terms jsonb,
 ADD COLUMN terms_evidence_ids jsonb NOT NULL DEFAULT '[]',
 ADD COLUMN requested_by varchar(64) REFERENCES access_users(id),
 ADD COLUMN review_id varchar(80) REFERENCES supervisor_review_decisions(review_id),
 ADD COLUMN approved_at timestamptz;
ALTER TABLE inventory_purchase_requests
 ADD COLUMN project_id varchar(64) REFERENCES projects(id),
 ADD COLUMN requested_by varchar(64) REFERENCES access_users(id),
 ADD COLUMN contract_snapshot jsonb,
 ADD COLUMN review_id varchar(80) REFERENCES supervisor_review_decisions(review_id),
 ADD COLUMN accepted_quantity integer NOT NULL DEFAULT 0 CHECK(accepted_quantity>=0),
 ADD COLUMN terminated_quantity integer NOT NULL DEFAULT 0 CHECK(terminated_quantity>=0),
 ADD CONSTRAINT purchase_fulfilled_limit CHECK(accepted_quantity+terminated_quantity<=quantity);
CREATE INDEX idx_purchase_project ON inventory_purchase_requests(project_id,merchant_id,created_at);
CREATE TABLE purchase_receipts (
 id varchar(80) PRIMARY KEY, purchase_id varchar(64) NOT NULL REFERENCES inventory_purchase_requests(id),
 project_id varchar(64) NOT NULL REFERENCES projects(id), merchant_id varchar(64) REFERENCES merchants(id), supplier_id varchar(64) NOT NULL REFERENCES inventory_suppliers(id),
 item_id varchar(64) NOT NULL REFERENCES inventory_items(id), warehouse_id varchar(64) NOT NULL REFERENCES warehouses(id),
 location_id varchar(80) NOT NULL, batch_id varchar(80) REFERENCES inventory_stock_batches(id),
 arrived_quantity integer NOT NULL CHECK(arrived_quantity>0), accepted_quantity integer NOT NULL CHECK(accepted_quantity>=0),
 rejected_quantity integer NOT NULL CHECK(rejected_quantity>=0), unit_cost_cent bigint NOT NULL CHECK(unit_cost_cent>=0),
 received_at timestamptz NOT NULL, received_by varchar(64) NOT NULL REFERENCES access_users(id), evidence_attachment_ids jsonb NOT NULL,
 movement_id varchar(80) REFERENCES inventory_stock_movements(id), payable_id varchar(80) REFERENCES ar_ap_items(id), created_at timestamptz NOT NULL DEFAULT now(),
 CHECK(arrived_quantity=accepted_quantity+rejected_quantity)
);
CREATE INDEX idx_purchase_receipts_source ON purchase_receipts(purchase_id,received_at);
CREATE TABLE purchase_quantity_resolutions (
 id varchar(80) PRIMARY KEY, purchase_id varchar(64) NOT NULL REFERENCES inventory_purchase_requests(id),
 resolution_type varchar(24) NOT NULL CHECK(resolution_type IN('replenishment','terminate')),
 quantity integer NOT NULL CHECK(quantity>0), expected_arrival_date date, reason text NOT NULL,
 review_id varchar(80) REFERENCES supervisor_review_decisions(review_id), status varchar(24) NOT NULL CHECK(status IN('recorded','pending_approval','approved','rejected')),
 evidence_attachment_ids jsonb NOT NULL, requested_by varchar(64) NOT NULL REFERENCES access_users(id), requested_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE purchase_consignment_sales (
 id varchar(80) PRIMARY KEY, receipt_id varchar(80) NOT NULL REFERENCES purchase_receipts(id),
 quantity integer NOT NULL CHECK(quantity>0), sale_basis text NOT NULL, source_reference text NOT NULL,
 sold_at timestamptz NOT NULL, evidence_attachment_ids jsonb NOT NULL,
 status varchar(24) NOT NULL CHECK(status IN('pending_confirmation','confirmed','rejected')),
 confirmed_by varchar(64) REFERENCES access_users(id), confirmed_at timestamptz,
 payable_id varchar(80) REFERENCES ar_ap_items(id), requested_by varchar(64) NOT NULL REFERENCES access_users(id), requested_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(receipt_id,source_reference)
);
CREATE TABLE purchase_prepayment_allocations (
 id varchar(80) PRIMARY KEY, purchase_id varchar(64) NOT NULL REFERENCES inventory_purchase_requests(id),
 source_payment_id varchar(80) NOT NULL REFERENCES ar_ap_payments(id), target_item_id varchar(80) NOT NULL REFERENCES ar_ap_items(id),
 amount_cent bigint NOT NULL CHECK(amount_cent>0), allocated_by varchar(64) NOT NULL REFERENCES access_users(id), allocated_at timestamptz NOT NULL DEFAULT now(),
 evidence_attachment_ids jsonb NOT NULL
);
CREATE INDEX idx_purchase_prepaid_source ON purchase_prepayment_allocations(source_payment_id);
CREATE TABLE purchase_returns (
 id varchar(80) PRIMARY KEY, receipt_id varchar(80) NOT NULL REFERENCES purchase_receipts(id),
 quantity integer NOT NULL CHECK(quantity>0), return_unit_price_cent bigint NOT NULL CHECK(return_unit_price_cent>=0),
 confirmed_amount_cent bigint NOT NULL CHECK(confirmed_amount_cent>=0), responsibility_terms text NOT NULL,
 supplier_refund_due_date date,
 consignment_sale_id varchar(80) REFERENCES purchase_consignment_sales(id),
 reason text NOT NULL, evidence_attachment_ids jsonb NOT NULL, review_id varchar(80) REFERENCES supervisor_review_decisions(review_id),
 status varchar(24) NOT NULL CHECK(status IN('pending_approval','approved','rejected','handed_over')),
 requested_by varchar(64) NOT NULL REFERENCES access_users(id), requested_at timestamptz NOT NULL DEFAULT now(),
 handed_over_by varchar(64) REFERENCES access_users(id), handed_over_at timestamptz, handover_evidence_ids jsonb NOT NULL DEFAULT '[]',
 movement_id varchar(80) REFERENCES inventory_stock_movements(id), actual_cost_cent bigint, cost_difference_cent bigint,
 accounting_status varchar(24) NOT NULL DEFAULT 'pending_review' CHECK(accounting_status IN('pending_review','posted')),
 finance_reviewed_by varchar(64) REFERENCES access_users(id), finance_reviewed_at timestamptz, finance_evidence_ids jsonb NOT NULL DEFAULT '[]',
 payable_reduction_cent bigint NOT NULL DEFAULT 0 CHECK(payable_reduction_cent>=0),
 supplier_refund_item_id varchar(80) REFERENCES ar_ap_items(id)
);
CREATE INDEX idx_purchase_returns_receipt ON purchase_returns(receipt_id,status);
CREATE TABLE purchase_payable_adjustments (
 id varchar(80) PRIMARY KEY, return_id varchar(80) NOT NULL REFERENCES purchase_returns(id),
 original_item_id varchar(80) NOT NULL REFERENCES ar_ap_items(id), amount_cent bigint NOT NULL CHECK(amount_cent>0),
 reversal_item_id varchar(80) NOT NULL REFERENCES ar_ap_items(id), replacement_item_id varchar(80) REFERENCES ar_ap_items(id),
 created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(return_id,original_item_id)
);
