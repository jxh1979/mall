-- R3 修复 P0-1：全过程留痕三层模型（REQ-34/37）。
-- 背景：R3 确认 business_process_instances/business_process_step_records/service_responsibility_records
-- 三层模型从未建表（仅事件模型 audit_events/evidence_events 承载基础留痕）。本迁移建表，提供服务责任链与过程时间线的承载。
-- process_type/business_object_type 在设计稿 9 值基础上补 R1-SPEC-06 的 bar_table_commission/saved_bottle_lifecycle 与 commission/saved_bottle。
-- 隔离：instances 自带 merchant_id/project_id（direct）；responsibility 加 merchant_id（direct）；step 经 process_instance_id 继承（inherited）。scope 值只由 token 注入。

CREATE TABLE IF NOT EXISTS business_process_instances (
    id VARCHAR(64) PRIMARY KEY,
    process_type VARCHAR(48) NOT NULL,
    business_object_type VARCHAR(48) NOT NULL,
    business_object_id VARCHAR(128) NOT NULL,
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    venue_id VARCHAR(64),
    area_id VARCHAR(64),
    location_point_id VARCHAR(64),
    started_event_id VARCHAR(64),
    current_step_code VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'running',
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT chk_bpi_process_type CHECK (process_type IN (
        'order_acquisition','ticket_verification','table_service','bar_order_fulfillment',
        'inventory_delivery','amusement_operation','exception_handling','finance_adjustment',
        'contract_admission','bar_table_commission','saved_bottle_lifecycle')),
    CONSTRAINT chk_bpi_object_type CHECK (business_object_type IN (
        'order','ticket','entitlement','table','inventory_item','robot_task','exception','bill',
        'contract','commission','saved_bottle')),
    CONSTRAINT chk_bpi_status CHECK (status IN ('running','completed','abnormal','cancelled'))
);
CREATE INDEX IF NOT EXISTS idx_bpi_merchant ON business_process_instances(merchant_id) WHERE merchant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_bpi_object ON business_process_instances(business_object_type, business_object_id);

CREATE TABLE IF NOT EXISTS business_process_step_records (
    id VARCHAR(64) PRIMARY KEY,
    process_instance_id VARCHAR(64) NOT NULL REFERENCES business_process_instances(id) ON DELETE CASCADE,
    step_code VARCHAR(64) NOT NULL,
    step_name VARCHAR(128),
    actor_type VARCHAR(32) NOT NULL,
    actor_id VARCHAR(64),
    terminal_type VARCHAR(32),
    device_id VARCHAR(64),
    location_point_id VARCHAR(64),
    event_id VARCHAR(64),
    auto_captured BOOLEAN NOT NULL DEFAULT FALSE,
    capture_source VARCHAR(32) NOT NULL,
    requires_manual_reason BOOLEAN NOT NULL DEFAULT FALSE,
    result VARCHAR(32) NOT NULL DEFAULT 'success',
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_bpsr_actor_type CHECK (actor_type IN ('staff','customer','device','robot','system','third_party')),
    CONSTRAINT chk_bpsr_capture_source CHECK (capture_source IN (
        'login_context','device_binding','qr_scan','nfc_scan','payment_callback','robot_callback',
        'wecom_archive','third_party_webhook','mdm_event','system_rule','manual_input')),
    CONSTRAINT chk_bpsr_result CHECK (result IN ('success','failed','abnormal','skipped'))
);
CREATE INDEX IF NOT EXISTS idx_bpsr_instance ON business_process_step_records(process_instance_id);

CREATE TABLE IF NOT EXISTS service_responsibility_records (
    id VARCHAR(64) PRIMARY KEY,
    order_id VARCHAR(64),
    table_id VARCHAR(64),
    customer_id VARCHAR(64),
    exception_id VARCHAR(64),
    primary_service_staff_id VARCHAR(64),
    assist_staff_ids JSONB NOT NULL DEFAULT '[]',
    sales_staff_id VARCHAR(64),
    first_handler_staff_id VARCHAR(64),
    refunded_by VARCHAR(64),
    waived_by VARCHAR(64),
    approved_by VARCHAR(64),
    assigned_by VARCHAR(64),
    shift_assignment_id VARCHAR(64),
    assigned_event_id VARCHAR(64),
    merchant_id VARCHAR(64),
    service_start_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    service_end_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    CONSTRAINT chk_srr_status CHECK (status IN ('active','changed','completed','cancelled'))
);
CREATE INDEX IF NOT EXISTS idx_srr_merchant ON service_responsibility_records(merchant_id) WHERE merchant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_srr_order ON service_responsibility_records(order_id) WHERE order_id IS NOT NULL;
