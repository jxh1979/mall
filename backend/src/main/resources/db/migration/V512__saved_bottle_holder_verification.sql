-- 文件版本：V0.01
-- FS-016 存酒持有人核验（第九批）。依据：BSM L1073「取酒……必须核验当前客户的短信验证码，或由已登录的官方手机应用、小程序出示
-- 动态取酒码……取酒核验、对应标签、取用前后剩余量、操作员工和时间必须留痕。人工例外取酒……须同时满足四项」；
-- BSM L1198「转让须经出让人手机短信验证码确认，验证成功即完成客户权益人变更……完整记录转让前后客户、时间与操作来源」；
-- BSM L585「权益转赠一经完成，原持有人手里的码立即全部作废」「刷新间隔由后台统一设置」；BSM L867「我的存酒……动态取酒码」。

-- 1) 转让流水补「操作来源 / 转让前持有人手机哈希 / 受让人客户 id」（BSM L1198 转让前后客户、时间与操作来源）。
ALTER TABLE saved_bottle_transfers
    ADD COLUMN IF NOT EXISTS operation_source VARCHAR(32),
    ADD COLUMN IF NOT EXISTS from_phone_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS to_customer_id VARCHAR(64);

-- 2) 取酒流水补「取用前后剩余量 / 核验方式 / 被核验手机哈希」（BSM L1073 留痕四项）。
ALTER TABLE saved_bottle_claims
    ADD COLUMN IF NOT EXISTS remaining_before INTEGER,
    ADD COLUMN IF NOT EXISTS remaining_after INTEGER,
    ADD COLUMN IF NOT EXISTS verification_method VARCHAR(16),
    ADD COLUMN IF NOT EXISTS verified_phone_hash VARCHAR(128);
ALTER TABLE saved_bottle_claims DROP CONSTRAINT IF EXISTS chk_saved_bottle_claims_verification_method;
ALTER TABLE saved_bottle_claims ADD CONSTRAINT chk_saved_bottle_claims_verification_method
    CHECK (verification_method IS NULL OR verification_method IN ('sms', 'pickup_code', 'manual_exception'));

-- 3) 官方动态取酒码：只存 SHA-256 哈希；一瓶同时只一枚 active；窗口取全场统一刷新间隔（ticket_qr_refresh_config）。
CREATE TABLE IF NOT EXISTS saved_bottle_pickup_codes (
    id VARCHAR(64) PRIMARY KEY,
    saved_bottle_id VARCHAR(64) NOT NULL REFERENCES saved_bottles(id),
    customer_id VARCHAR(64) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    refresh_interval_seconds INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL,
    consumed_claim_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_saved_bottle_pickup_codes_status CHECK (status IN ('active', 'consumed', 'revoked'))
);
CREATE INDEX IF NOT EXISTS idx_saved_bottle_pickup_codes_bottle_status
    ON saved_bottle_pickup_codes (saved_bottle_id, status);

-- 4) 人工例外取酒申请（BSM L1073 四项：店长发起→另一主管复核→营销确认→本人到场放酒；消费历史条件按项目配置）。
CREATE TABLE IF NOT EXISTS saved_bottle_manual_pickups (
    id VARCHAR(64) PRIMARY KEY,
    saved_bottle_id VARCHAR(64) NOT NULL REFERENCES saved_bottles(id),
    merchant_id VARCHAR(64),
    project_id VARCHAR(64),
    customer_id VARCHAR(64),
    phone_hash VARCHAR(128) NOT NULL,
    reason VARCHAR(500),
    history_condition VARCHAR(32) NOT NULL,
    initiated_by VARCHAR(128) NOT NULL,
    initiated_at TIMESTAMPTZ NOT NULL,
    reviewed_by VARCHAR(128),
    reviewed_at TIMESTAMPTZ,
    marketing_confirmed_by VARCHAR(128),
    marketing_confirm_basis VARCHAR(32),
    marketing_confirmed_at TIMESTAMPTZ,
    dispensed_by VARCHAR(128),
    dispensed_at TIMESTAMPTZ,
    claim_id VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_saved_bottle_manual_pickups_status
        CHECK (status IN ('initiated', 'reviewed', 'marketing_confirmed', 'dispensed')),
    CONSTRAINT chk_saved_bottle_manual_pickups_condition
        CHECK (history_condition IN ('orders_within_1m', 'orders_within_3m', 'orders_total')),
    CONSTRAINT chk_saved_bottle_manual_pickups_basis
        CHECK (marketing_confirm_basis IS NULL OR marketing_confirm_basis IN ('attributed_marketer', 'marketer_supervisor', 'on_duty_marketing_supervisor'))
);
CREATE INDEX IF NOT EXISTS idx_saved_bottle_manual_pickups_bottle
    ON saved_bottle_manual_pickups (saved_bottle_id, created_at DESC);

-- 5) 人工例外消费历史条件（安全阈值，按项目可配；BSM L1073「出厂默认：近 1 个月内至少 2 笔、近 3 个月内至少 3 笔、
--    或历史累计至少 5 笔有效卡座消费订单……须同时配置上下限」）。范式同 V497/V413：出厂行留空须配置，未配置时人工例外一律拒绝。
CREATE TABLE IF NOT EXISTS saved_bottle_manual_pickup_policy (
    project_id VARCHAR(64) PRIMARY KEY,
    orders_within_1m INTEGER NOT NULL,
    orders_within_3m INTEGER NOT NULL,
    orders_total INTEGER NOT NULL,
    min_orders INTEGER NOT NULL,
    max_orders INTEGER NOT NULL,
    updated_by VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_saved_bottle_manual_pickup_policy_bounds CHECK (
        min_orders >= 1 AND min_orders <= max_orders
        AND orders_within_1m BETWEEN min_orders AND max_orders
        AND orders_within_3m BETWEEN min_orders AND max_orders
        AND orders_total BETWEEN min_orders AND max_orders)
);

-- 6) 人工例外三步各一权限码（店长发起 / 另一主管复核 / 营销确认）＋消费历史条件配置权限码（BSM L1073「按项目可配，调整须最高权限审批并留痕」：
--    配置入口 PUT /saved-bottles/manual-pickup-policy，服务层另要求最高权限本人办理）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('bar.saved_bottle.manual_pickup.initiate', '店长发起人工例外取酒', 'bar', 'high', TRUE, 'active'),
    ('bar.saved_bottle.manual_pickup.review', '主管复核人工例外取酒', 'bar', 'high', TRUE, 'active'),
    ('bar.saved_bottle.manual_pickup.confirm_marketing', '营销确认人工例外取酒客户身份', 'bar', 'high', TRUE, 'active'),
    ('bar.saved_bottle.manual_pickup_policy.manage', '配置人工例外取酒消费历史条件（按项目）', 'bar', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES
    ('role_platform_admin', 'bar.saved_bottle.manual_pickup.initiate'),
    ('role_platform_admin', 'bar.saved_bottle.manual_pickup.review'),
    ('role_platform_admin', 'bar.saved_bottle.manual_pickup.confirm_marketing'),
    ('role_platform_admin', 'bar.saved_bottle.manual_pickup_policy.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON saved_bottle_pickup_codes TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON saved_bottle_manual_pickups TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON saved_bottle_manual_pickup_policy TO complex_app';
    END IF;
END $$;
