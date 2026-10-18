-- 文件版本：V0.01
-- 酒吧服务责任、收费来源、审批规则、分配原件及真实退款分项。
CREATE TABLE bar_order_service_sources (
    order_id varchar(64) PRIMARY KEY REFERENCES bar_orders(id),
    session_id varchar(64) NOT NULL REFERENCES bar_table_sessions(id),
    table_id varchar(64) NOT NULL REFERENCES bar_tables(id),
    project_id varchar(64) NOT NULL REFERENCES projects(id),
    merchant_id varchar(64) NOT NULL REFERENCES merchants(id),
    business_date date NOT NULL,
    recorded_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_bar_order_service_session ON bar_order_service_sources(session_id);
CREATE TABLE bar_service_responsibilities (
    id varchar(64) PRIMARY KEY,
    session_id varchar(64) NOT NULL REFERENCES bar_table_sessions(id),
    table_id varchar(64) NOT NULL REFERENCES bar_tables(id),
    project_id varchar(64) NOT NULL REFERENCES projects(id),
    merchant_id varchar(64) NOT NULL REFERENCES merchants(id),
    user_id varchar(64) NOT NULL REFERENCES access_users(id),
    payer_company_id varchar(64) NOT NULL,
    started_at timestamptz NOT NULL,
    ended_at timestamptz,
    source_id varchar(128) NOT NULL UNIQUE,
    recorded_by varchar(64) NOT NULL,
    reason text NOT NULL,
    CHECK (ended_at IS NULL OR ended_at > started_at)
);
CREATE UNIQUE INDEX uq_bar_service_current ON bar_service_responsibilities(session_id) WHERE ended_at IS NULL;
CREATE INDEX idx_bar_service_person ON bar_service_responsibilities(user_id,started_at);
CREATE TABLE bar_payroll_rules (
    id varchar(64) PRIMARY KEY,
    rule_kind varchar(20) NOT NULL CHECK(rule_kind IN ('charge','allocation')),
    project_id varchar(64) NOT NULL REFERENCES projects(id),
    merchant_id varchar(64) REFERENCES merchants(id),
    effective_from date NOT NULL,
    version varchar(20) NOT NULL,
    rule_json jsonb NOT NULL,
    reason text NOT NULL,
    status varchar(24) NOT NULL CHECK(status IN ('draft','pending_review','approved','rejected')),
    merchant_confirmed_by varchar(64), merchant_confirmed_at timestamptz,
    platform_confirmed_by varchar(64), platform_confirmed_at timestamptz,
    platform_company_id varchar(64) NOT NULL,
    review_id varchar(64),
    evidence_attachment_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_by varchar(64) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    decided_at timestamptz,
    CHECK((rule_kind='charge' AND merchant_id IS NULL) OR (rule_kind='allocation' AND merchant_id IS NOT NULL))
);
CREATE UNIQUE INDEX uq_bar_payroll_rule_effective ON bar_payroll_rules(rule_kind,project_id,COALESCE(merchant_id,''),effective_from) WHERE status='approved';
CREATE TABLE bar_payroll_bill_components (
    id varchar(64) PRIMARY KEY,
    source_bill_id varchar(64) NOT NULL REFERENCES bar_bills(id),
    session_id varchar(64) NOT NULL REFERENCES bar_table_sessions(id),
    source_table_id varchar(64) NOT NULL REFERENCES bar_tables(id),
    project_id varchar(64) NOT NULL REFERENCES projects(id),
    merchant_id varchar(64) NOT NULL REFERENCES merchants(id),
    business_date date NOT NULL,
    component_type varchar(20) NOT NULL CHECK(component_type IN ('tip','service_fee')),
    amount_cent bigint NOT NULL CHECK(amount_cent>0),
    original_base_cent bigint NOT NULL CHECK(original_base_cent>0),
    charge_rule_id varchar(64) REFERENCES bar_payroll_rules(id),
    source_event_id varchar(128) NOT NULL UNIQUE,
    source_snapshot jsonb NOT NULL,
    collection_bill_id varchar(64) NOT NULL REFERENCES bar_bills(id),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_bar_payroll_components_month ON bar_payroll_bill_components(project_id,merchant_id,business_date);
CREATE TABLE bar_payroll_collection_links (
    source_bill_id varchar(64) NOT NULL REFERENCES bar_bills(id),
    collection_bill_id varchar(64) NOT NULL REFERENCES bar_bills(id),
    active boolean NOT NULL DEFAULT true,
    PRIMARY KEY(source_bill_id,collection_bill_id)
);
CREATE TABLE bar_payroll_runs (
    id varchar(64) PRIMARY KEY,
    project_id varchar(64) NOT NULL REFERENCES projects(id),
    merchant_id varchar(64) NOT NULL REFERENCES merchants(id),
    month varchar(7) NOT NULL,
    source_fingerprint varchar(64) NOT NULL,
    source_snapshot jsonb NOT NULL,
    collected_tip_cent bigint NOT NULL,
    collected_service_fee_cent bigint NOT NULL,
    payable_tip_cent bigint NOT NULL,
    payable_service_fee_cent bigint NOT NULL,
    withheld_service_fee_cent bigint NOT NULL,
    rounding_residual_cent bigint NOT NULL,
    settlement_entity_id varchar(64) NOT NULL,
    status varchar(16) NOT NULL CHECK(status='confirmed'),
    created_by varchar(64) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(project_id,merchant_id,month,source_fingerprint)
);
CREATE TABLE bar_payroll_allocations (
    id varchar(64) PRIMARY KEY,
    run_id varchar(64) NOT NULL REFERENCES bar_payroll_runs(id),
    user_id varchar(64) NOT NULL REFERENCES access_users(id),
    month varchar(7) NOT NULL,
    component_type varchar(20) NOT NULL CHECK(component_type IN ('tip','service_fee')),
    amount_cent bigint NOT NULL,
    merchant_id varchar(64) NOT NULL REFERENCES merchants(id),
    project_id varchar(64) NOT NULL REFERENCES projects(id),
    payer_company_id varchar(64) NOT NULL,
    source_bill_ids jsonb NOT NULL,
    rule_version_id varchar(64) NOT NULL,
    status varchar(16) NOT NULL CHECK(status='confirmed'),
    UNIQUE(run_id,user_id,component_type,payer_company_id,rule_version_id)
);
INSERT INTO permissions(permission_code,permission_name,domain,risk_level,requires_approval,status)
VALUES ('bar.payroll.view','查看酒吧服务收入分配','bar','high',true,'active'),
       ('bar.payroll.manage','办理酒吧服务收入分配及规则','bar','high',true,'active'),
       ('bar.payroll.approve','审批酒吧服务收入分配规则','bar','high',true,'active')
ON CONFLICT(permission_code) DO NOTHING;

CREATE INDEX idx_bar_payroll_component_collection ON bar_payroll_bill_components(collection_bill_id);
CREATE TABLE bar_payroll_refund_lines (
    id varchar(64) PRIMARY KEY,
    adjustment_id varchar(64) NOT NULL REFERENCES bar_bill_adjustments(id),
    collection_bill_id varchar(64) NOT NULL REFERENCES bar_bills(id),
    source_bill_id varchar(64) NOT NULL REFERENCES bar_bills(id),
    component_type varchar(20) NOT NULL CHECK(component_type IN ('goods','service_fee','tip')),
    amount_cent bigint NOT NULL CHECK(amount_cent>0),
    ar_ap_item_id varchar(64) NOT NULL REFERENCES ar_ap_items(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(adjustment_id,source_bill_id,component_type)
);
CREATE INDEX idx_bar_payroll_refund_source ON bar_payroll_refund_lines(source_bill_id,component_type);
