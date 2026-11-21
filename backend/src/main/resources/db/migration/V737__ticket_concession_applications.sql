-- 文件版本：V0.01
-- V737：第十八批 BV 段第 264 条 264-R1（商户让利由商户在系统里申请并在线确认）。
-- 依据：BSM:1033、ENUM:53「只有商户主动申请并书面确认让利的权益才记为商户让利、不结算」；第十八批裁决 78「商户用自己的账号提交让利申请
--   （哪张票、哪项权益）并在线确认，运营配票种时只能从已确认的申请里选商户让利型。」
-- 提交即在线确认，表里每一行都是已确认的申请；运营选用时写入票种权益绑定编号（不设外键：已上架跨域组合通票的新绑定先只在草稿版里）。
-- 研发阶段无存量（UD-07），不回填。

CREATE TABLE IF NOT EXISTS ticket_concession_applications (
    id VARCHAR(64) PRIMARY KEY,
    ticket_type_id VARCHAR(64) NOT NULL REFERENCES ticket_types(id) ON DELETE CASCADE,
    merchant_id VARCHAR(64) NOT NULL REFERENCES merchants(id),
    entitlement_content VARCHAR(200) NOT NULL,
    confirmed_by VARCHAR(128) NOT NULL,
    confirmed_at TIMESTAMPTZ NOT NULL,
    binding_id VARCHAR(64),
    bound_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_ticket_concession_applications_ticket_type ON ticket_concession_applications (ticket_type_id);
CREATE INDEX IF NOT EXISTS idx_ticket_concession_applications_merchant ON ticket_concession_applications (merchant_id);
COMMENT ON TABLE ticket_concession_applications IS '商户让利申请（商户在商户专区提交并在线确认；运营配票种标商户让利型时只能从中选）';
COMMENT ON COLUMN ticket_concession_applications.ticket_type_id IS '申请让利的票种';
COMMENT ON COLUMN ticket_concession_applications.merchant_id IS '提交并确认让利的商户';
COMMENT ON COLUMN ticket_concession_applications.entitlement_content IS '让利的是哪项权益（商户写明）';
COMMENT ON COLUMN ticket_concession_applications.confirmed_by IS '提交并在线确认的商户账号';
COMMENT ON COLUMN ticket_concession_applications.confirmed_at IS '在线确认时间';
COMMENT ON COLUMN ticket_concession_applications.binding_id IS '运营选用时写入的票种权益绑定编号（空＝尚未选用）';
COMMENT ON COLUMN ticket_concession_applications.bound_at IS '运营选用时间';

-- 商户专区申请并确认本商户让利的权限码（商户角色由后台配置，仓库无商户角色种子）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('merchant.portal.concession.apply', '商户专区：申请并确认本商户让利', 'merchant_portal', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'merchant.portal.concession.apply')
ON CONFLICT (role_id, permission_code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON ticket_concession_applications TO complex_app';
    END IF;
END $$;
