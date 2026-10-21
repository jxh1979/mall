-- 文件版本：V0.01
-- HP-P01（BSM「停车场：入出场、计费缴费、消费抵免和月租车位」L1425–L1452；决策台账停车 P1～P8）第一组：
-- 停车业务参数、第三方停车系统回写与下发、客人端缴费、商户月租车位、员工车辆、人工放行。
-- 消费抵免（资格、成本分摊、退款重核）与停车看板指标另组落地，不在本迁移。

-- R1 停车业务参数（BSM L1434～L1437「业务参数，按项目可配，须同时配置上下限；出厂默认值实施前补定」）：
-- 七项各带上下限，全部必填、值落在闭区间内；不插任何出厂行，项目未配置时停车业务一律拒绝、不回落默认值。
CREATE TABLE parking_policies (
    project_id VARCHAR(64) PRIMARY KEY REFERENCES projects(id),
    free_minutes INTEGER NOT NULL, free_minutes_min INTEGER NOT NULL, free_minutes_max INTEGER NOT NULL,
    hourly_rate_cent BIGINT NOT NULL, hourly_rate_cent_min BIGINT NOT NULL, hourly_rate_cent_max BIGINT NOT NULL,
    daily_cap_cent BIGINT NOT NULL, daily_cap_cent_min BIGINT NOT NULL, daily_cap_cent_max BIGINT NOT NULL,
    exit_grace_minutes INTEGER NOT NULL, exit_grace_minutes_min INTEGER NOT NULL, exit_grace_minutes_max INTEGER NOT NULL,
    credit_threshold_cent BIGINT NOT NULL, credit_threshold_cent_min BIGINT NOT NULL, credit_threshold_cent_max BIGINT NOT NULL,
    credit_minutes INTEGER NOT NULL, credit_minutes_min INTEGER NOT NULL, credit_minutes_max INTEGER NOT NULL,
    -- 员工车辆优惠：按万分比减免，10000＝免费。
    staff_discount_bp INTEGER NOT NULL, staff_discount_bp_min INTEGER NOT NULL, staff_discount_bp_max INTEGER NOT NULL,
    updated_by VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_pp_free CHECK (0 <= free_minutes_min AND free_minutes_min <= free_minutes AND free_minutes <= free_minutes_max),
    CONSTRAINT chk_pp_rate CHECK (0 <= hourly_rate_cent_min AND hourly_rate_cent_min <= hourly_rate_cent AND hourly_rate_cent <= hourly_rate_cent_max),
    CONSTRAINT chk_pp_cap CHECK (0 <= daily_cap_cent_min AND daily_cap_cent_min <= daily_cap_cent AND daily_cap_cent <= daily_cap_cent_max),
    CONSTRAINT chk_pp_grace CHECK (0 <= exit_grace_minutes_min AND exit_grace_minutes_min <= exit_grace_minutes AND exit_grace_minutes <= exit_grace_minutes_max),
    CONSTRAINT chk_pp_threshold CHECK (0 <= credit_threshold_cent_min AND credit_threshold_cent_min <= credit_threshold_cent AND credit_threshold_cent <= credit_threshold_cent_max),
    CONSTRAINT chk_pp_credit CHECK (0 <= credit_minutes_min AND credit_minutes_min <= credit_minutes AND credit_minutes <= credit_minutes_max),
    CONSTRAINT chk_pp_staff CHECK (0 <= staff_discount_bp_min AND staff_discount_bp_min <= staff_discount_bp
        AND staff_discount_bp <= staff_discount_bp_max AND staff_discount_bp_max <= 10000)
);

-- R2 第三方停车系统接入（BSM L1428／L1433／L1444）：开放平台端点与项目一一绑定（随停车系统开通由运维登记，同开放平台端点本身）。
CREATE TABLE parking_vendor_bindings (
    endpoint_key VARCHAR(128) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- 停车系统作为供应商适配器类型（字段映射沿用适配器 field_mapping，开关沿用适配器与端点的 webhook_enabled）。
ALTER TABLE integration_provider_adapters DROP CONSTRAINT IF EXISTS chk_integration_provider_adapters_type;
ALTER TABLE integration_provider_adapters ADD CONSTRAINT chk_integration_provider_adapters_type
    CHECK (adapter_type IN ('payment', 'ticket_channel', 'face_age', 'wecom', 'robot', 'video', 'device', 'retail', 'catering', 'parking'));

-- 入出场记录（停车系统回传）：有牌按车牌、无牌按入口领取的临时码。
CREATE TABLE parking_sessions (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    vendor_session_ref VARCHAR(128) NOT NULL,
    plate_no VARCHAR(16),
    temp_code VARCHAR(64),
    vehicle_kind VARCHAR(16) NOT NULL DEFAULT 'temporary',
    entered_at TIMESTAMPTZ NOT NULL,
    exited_at TIMESTAMPTZ,
    vendor_fee_cent BIGINT,
    vendor_paid_cent BIGINT,
    status VARCHAR(16) NOT NULL DEFAULT 'in_lot',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_parking_sessions_vendor_ref UNIQUE (project_id, vendor_session_ref),
    CONSTRAINT chk_ps_vehicle CHECK (num_nonnulls(plate_no, temp_code) >= 1),
    CONSTRAINT chk_ps_kind CHECK (vehicle_kind IN ('temporary', 'monthly', 'staff')),
    CONSTRAINT chk_ps_status CHECK (status IN ('in_lot', 'exited')),
    CONSTRAINT chk_ps_amounts CHECK (COALESCE(vendor_fee_cent, 0) >= 0 AND COALESCE(vendor_paid_cent, 0) >= 0)
);
CREATE INDEX idx_parking_sessions_plate ON parking_sessions (project_id, plate_no) WHERE status = 'in_lot';
CREATE INDEX idx_parking_sessions_temp ON parking_sessions (project_id, temp_code) WHERE status = 'in_lot';

-- 停车系统回传的缴费结果（与平台收取的缴费对账）。
CREATE TABLE parking_vendor_payments (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    vendor_payment_ref VARCHAR(128) NOT NULL,
    session_id VARCHAR(64) REFERENCES parking_sessions(id),
    amount_cent BIGINT NOT NULL,
    channel VARCHAR(32),
    paid_at TIMESTAMPTZ NOT NULL,
    platform_payment_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_parking_vendor_payments_ref UNIQUE (project_id, vendor_payment_ref),
    CONSTRAINT chk_pvp_amount CHECK (amount_cent > 0)
);

-- 车位占用快照（停车系统回传）。
CREATE TABLE parking_occupancy_snapshots (
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    reported_at TIMESTAMPTZ NOT NULL,
    total_spaces INTEGER NOT NULL,
    occupied_total INTEGER NOT NULL,
    occupied_monthly INTEGER NOT NULL DEFAULT 0,
    occupied_staff INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (project_id, reported_at),
    CONSTRAINT chk_pos_counts CHECK (total_spaces > 0 AND occupied_total >= 0 AND occupied_monthly >= 0 AND occupied_staff >= 0)
);

-- R3 客人端缴费（BSM L1435「出口扫码支付，或在官方 App/小程序提前缴费后在离场宽限时限内离场……不做车牌绑定无感支付」；
-- L1445「停车费收入归城市项目公司」）：收款主体固定为本项目唯一有效的城市项目公司。
CREATE TABLE parking_payments (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    session_id VARCHAR(64) NOT NULL REFERENCES parking_sessions(id),
    customer_id VARCHAR(64) NOT NULL REFERENCES customers(id),
    amount_cent BIGINT NOT NULL,
    payment_intent_id VARCHAR(80) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    paid_at TIMESTAMPTZ,
    grace_until TIMESTAMPTZ,
    receiver_entity_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_ppay_amount CHECK (amount_cent > 0),
    CONSTRAINT chk_ppay_status CHECK (status IN ('pending', 'paid')),
    CONSTRAINT chk_ppay_paid CHECK ((status = 'paid') = (paid_at IS NOT NULL AND grace_until IS NOT NULL))
);
CREATE INDEX idx_parking_payments_session ON parking_payments (session_id, status);

-- R7 商户月租车位（BSM L1437「商户月租车位按合同约定、按月出账，账单类型为「车位月租」」；ENUM「车位月租」）。
ALTER TABLE contract_rent_bills DROP CONSTRAINT IF EXISTS chk_crb_fee_type;
ALTER TABLE contract_rent_bills ADD CONSTRAINT chk_crb_fee_type
    CHECK (fee_type IN ('rent', 'property_fee', 'energy_fee', 'parking_monthly'));

-- 月租车牌（在本平台后台登记并下发给停车系统）。
CREATE TABLE parking_monthly_vehicles (
    id VARCHAR(64) PRIMARY KEY,
    contract_id VARCHAR(64) NOT NULL REFERENCES contracts(id),
    plate_no VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    registered_by VARCHAR(64) NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_by VARCHAR(64),
    revoked_at TIMESTAMPTZ,
    CONSTRAINT chk_pmv_status CHECK (status IN ('active', 'revoked'))
);
CREATE UNIQUE INDEX uq_parking_monthly_vehicles_active ON parking_monthly_vehicles (contract_id, plate_no) WHERE status = 'active';

-- R8 员工车辆（BSM L1437「员工车辆由所属主体登记车牌后，按后台配置免费或折扣」）：商户员工记所属商户。
CREATE TABLE parking_staff_vehicles (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    user_id VARCHAR(64) NOT NULL REFERENCES access_users(id),
    owner_merchant_id VARCHAR(64),
    plate_no VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    registered_by VARCHAR(64) NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_by VARCHAR(64),
    revoked_at TIMESTAMPTZ,
    CONSTRAINT chk_psv_status CHECK (status IN ('active', 'revoked'))
);
CREATE UNIQUE INDEX uq_parking_staff_vehicles_active ON parking_staff_vehicles (project_id, plate_no) WHERE status = 'active';

-- R9 人工放行（BSM L1438「人工放行必须记录原因、操作人、车牌和时间，进留痕与异常；免费放行和减免须经物业主管审批」）。
CREATE TABLE parking_manual_releases (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    session_id VARCHAR(64) REFERENCES parking_sessions(id),
    plate_no VARCHAR(16) NOT NULL,
    release_reason VARCHAR(32) NOT NULL,
    reason TEXT NOT NULL,
    operator_id VARCHAR(64) NOT NULL,
    operator_shift_id VARCHAR(64) NOT NULL,
    released_at TIMESTAMPTZ NOT NULL,
    waive_type VARCHAR(16) NOT NULL DEFAULT 'none',
    reduction_cent BIGINT,
    review_id VARCHAR(64),
    status VARCHAR(24) NOT NULL,
    risk_alert_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_pmr_reason CHECK (release_reason IN ('recognition_failed', 'gate_fault', 'fee_dispute')),
    CONSTRAINT chk_pmr_waive CHECK (waive_type IN ('none', 'free', 'reduction')),
    CONSTRAINT chk_pmr_reduction CHECK ((waive_type = 'reduction') = (reduction_cent IS NOT NULL)
        AND COALESCE(reduction_cent, 1) > 0),
    CONSTRAINT chk_pmr_status CHECK (status IN ('released', 'pending_approval', 'approved', 'rejected')),
    CONSTRAINT chk_pmr_review CHECK ((waive_type = 'none') = (review_id IS NULL))
);
CREATE INDEX idx_parking_manual_releases_plate ON parking_manual_releases (project_id, plate_no, released_at);

-- 超长滞留、逃费、人工放行镜像统一异常队列（BSM L1438／L1448「进异常队列由物业跟进」）。
ALTER TABLE risk_alerts DROP CONSTRAINT IF EXISTS chk_risk_alerts_source_domain;
ALTER TABLE risk_alerts ADD CONSTRAINT chk_risk_alerts_source_domain CHECK (source_domain IN (
    'ticketing', 'bar', 'amusement', 'inventory', 'finance',
    'wecom', 'device', 'thirdparty', 'analytics', 'platform', 'parking'
));

-- 停车缴费回写与平台收款对不上的，进第三方对账差异（BSM L1445「进入收入归属与对账」）。
ALTER TABLE integration_reconciliation_batches DROP CONSTRAINT IF EXISTS chk_integration_recon_batches_business;
ALTER TABLE integration_reconciliation_batches
    ADD CONSTRAINT chk_integration_recon_batches_business CHECK (business_type IN ('catering', 'retail', 'ticket', 'parking'));
ALTER TABLE integration_reconciliation_differences DROP CONSTRAINT IF EXISTS chk_integration_recon_diffs_business;
ALTER TABLE integration_reconciliation_differences
    ADD CONSTRAINT chk_integration_recon_diffs_business CHECK (business_type IN ('catering', 'retail', 'ticket', 'parking'));

-- 权限（挂平台管理员；服务层另按项目范围校验）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status) VALUES
    ('parking.config.manage', '配置停车业务参数（按项目）', 'parking', 'high', TRUE, 'active'),
    ('parking.ops.view', '查看停车场在场、出场与缴费记录', 'parking', 'medium', FALSE, 'active'),
    ('parking.vehicle.manage', '登记月租车牌与员工车辆', 'parking', 'medium', FALSE, 'active'),
    ('parking.release.record', '登记停车人工放行', 'parking', 'medium', FALSE, 'active'),
    ('parking.release.approve', '审批停车免费放行与减免', 'parking', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code FROM permissions
 WHERE permission_code IN ('parking.config.manage', 'parking.ops.view', 'parking.vehicle.manage',
                           'parking.release.record', 'parking.release.approve')
ON CONFLICT (role_id, permission_code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON parking_policies, parking_vendor_bindings, parking_sessions,
            parking_vendor_payments, parking_occupancy_snapshots, parking_payments, parking_monthly_vehicles,
            parking_staff_vehicles, parking_manual_releases TO complex_app';
    END IF;
END $$;
