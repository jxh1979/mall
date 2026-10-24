-- 文件版本：V0.01
-- ROOT-03 B 段券与展示组（B-voucher-content）。

-- 1) R32＝clients-R41 券包权益核销动态码。BSM 第 585 行「客人出示的门票核销码、权益核销码、入场码等——都不是一张固定的图，而是每隔一段时间自动更新的
--    动态码……截图、转发出去的旧码过了有效期就作废，验不过……权益转赠一经完成，原持有人手里的码立即全部作废」。范式同 V512 动态取酒码：只存 SHA-256，
--    取新码先撤旧码，窗口取全场统一刷新间隔（ticket_qr_refresh_config）。
CREATE TABLE IF NOT EXISTS voucher_redeem_codes (
    id VARCHAR(64) PRIMARY KEY,
    voucher_instance_id VARCHAR(64) NOT NULL REFERENCES voucher_instances(id),
    customer_id VARCHAR(64) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    refresh_interval_seconds INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL,
    consumed_redemption_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_voucher_redeem_codes_status CHECK (status IN ('active', 'consumed', 'revoked'))
);
CREATE INDEX IF NOT EXISTS idx_voucher_redeem_codes_instance_status ON voucher_redeem_codes (voucher_instance_id, status);
CREATE INDEX IF NOT EXISTS idx_voucher_redeem_codes_hash ON voucher_redeem_codes (code_hash);

-- 2) R31 商户级储值卡退出公示。ENUM 第 86 行「公告须在客人手机应用、小程序和该商户的商场店面三处同时公示，公示期不少于后台配置的天数（业务参数，
--    全平台统一可配，须同时配置上下限）」；第十三批裁决第 2 条「商户在商户门户自行发公告，平台只校验公示天数，不影响退租流程」。
--    公示天数 HTML 未给出厂值：不预置，平台配置（值与上下限同配）前商户发公告 409。
CREATE TABLE IF NOT EXISTS merchant_card_exit_notice_policy (
    scope_id VARCHAR(16) PRIMARY KEY,
    publish_days INTEGER NOT NULL,
    min_days INTEGER NOT NULL,
    max_days INTEGER NOT NULL,
    updated_by VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_mcen_policy_scope CHECK (scope_id = 'global'),
    CONSTRAINT chk_mcen_policy_bounds CHECK (min_days >= 1 AND min_days <= publish_days AND publish_days <= max_days)
);
CREATE TABLE IF NOT EXISTS merchant_card_exit_notices (
    id VARCHAR(64) PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL REFERENCES merchants(id),
    project_id VARCHAR(64),
    use_until DATE NOT NULL,
    refundable BOOLEAN NOT NULL,
    refund_ratio_bps INTEGER NOT NULL,
    refund_method VARCHAR(500) NOT NULL,
    refund_contact VARCHAR(200) NOT NULL,
    publish_from TIMESTAMPTZ NOT NULL,
    publish_until DATE NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_mcen_ratio CHECK (refund_ratio_bps BETWEEN 0 AND 10000)
);
CREATE INDEX IF NOT EXISTS idx_mcen_merchant ON merchant_card_exit_notices (merchant_id, publish_until);
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('merchant.portal.card_exit_notice.publish', '商户专区发布本商户储值卡退出公告', 'merchant_portal', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'merchant.portal.card_exit_notice.publish')
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- 3) clients-R10 未核销票到期提醒。BSM 第 1596 行「业务参数，按项目可配，出厂默认到期前二十四小时，须同时配置上下限」。
--    范式同 V499：'__global__' 出厂回落行（24 小时、上下限留空），首次配置必须同时填写上下限。
CREATE TABLE IF NOT EXISTS ticket_expiry_reminder_policies (
    project_id VARCHAR(64) PRIMARY KEY,
    remind_hours INTEGER NOT NULL,
    min_hours INTEGER,
    max_hours INTEGER,
    updated_by VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_ticket_expiry_reminder_hours CHECK (remind_hours >= 1),
    CONSTRAINT chk_ticket_expiry_reminder_bounds CHECK (
        (min_hours IS NULL AND max_hours IS NULL)
        OR (min_hours IS NOT NULL AND max_hours IS NOT NULL
            AND min_hours >= 1 AND min_hours <= remind_hours AND remind_hours <= max_hours))
);
INSERT INTO ticket_expiry_reminder_policies (project_id, remind_hours) VALUES ('__global__', 24)
ON CONFLICT (project_id) DO NOTHING;

-- 4) clients-R9 客人端首页综合体介绍与活动公告。BSM 第 857 行「查看综合体介绍、营业时间、活动公告、票务入口」；第十三批裁决第 5 条「运营编辑后走审批，
--    批准才对客人展示」：发布动作走现有准入审批（一级上级），批准后同一内容再提交才写入本表，客人端只读本表。
CREATE TABLE IF NOT EXISTS customer_home_contents (
    project_id VARCHAR(64) PRIMARY KEY REFERENCES projects(id),
    intro TEXT NOT NULL,
    announcement TEXT NOT NULL,
    published_by VARCHAR(128) NOT NULL,
    published_at TIMESTAMPTZ NOT NULL
);
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('customer.home_content.manage', '编辑并提交发布客人端首页综合体介绍与活动公告', 'customer', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'customer.home_content.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
INSERT INTO supervisor_review_policies
    (policy_code, action_type, object_type, trigger_mode, outcome, approval_level, required_level)
VALUES ('customer_home_content_publish', 'customer.home_content.publish', 'customer_home_content', 'always', 'block', 'one_level_up', 1)
ON CONFLICT (policy_code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON voucher_redeem_codes TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON merchant_card_exit_notice_policy TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON merchant_card_exit_notices TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON ticket_expiry_reminder_policies TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON customer_home_contents TO complex_app';
    END IF;
END $$;
