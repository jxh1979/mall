-- V454 CG-11d 独立经销商 Web 门户：受控第十一端 dealer_portal（ENUM:233；BSM:1217-1218、1246；ENUM:181）。
-- 隔离账号（account_type=dealer + dealer_portal_accounts 归属）、隔离权限（role_dealer_portal 只含 dealer_portal.* 不含任何内部 dealer.*）、
-- 统一终端开关（tcs_term_dealer_portal 总开关行 + CHECK 闭集加 dealer_portal）；停用撤权由 DealerPortalAccountService 立即终止会话。
-- 不为存量数据做兼容（铁律 8）。

-- 1) 终端闭集：十端 → 十一端（terminal_capability_switches 与灰度层 CHECK 同步）
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_tcs_terminal_type') THEN
        ALTER TABLE terminal_capability_switches DROP CONSTRAINT chk_tcs_terminal_type;
    END IF;
    ALTER TABLE terminal_capability_switches
        ADD CONSTRAINT chk_tcs_terminal_type CHECK (terminal_type IN (
            'app', 'miniapp', 'web', 'tablet',
            'staff_work_phone', 'staff_private_phone',
            'merchant_portal', 'merchant_app',
            'kiosk', 'mobile_admin', 'dealer_portal', '*'
        ));
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_tccr_terminal_type') THEN
        ALTER TABLE terminal_capability_canary_rollouts DROP CONSTRAINT chk_tccr_terminal_type;
    END IF;
    ALTER TABLE terminal_capability_canary_rollouts
        ADD CONSTRAINT chk_tccr_terminal_type CHECK (terminal_type IN (
            'customer_app', 'customer_miniapp', 'web_admin', 'mobile_admin',
            'staff_tablet', 'staff_work_phone', 'staff_personal_app',
            'merchant_web', 'merchant_app', 'kiosk', 'tablet', '*',
            'app', 'miniapp', 'web', 'staff_private_phone', 'merchant_portal', 'dealer_portal'
        ));
END $$;

-- 2) 第十一端总开关行（默认开，关闭后逐请求 TERMINAL_DISABLED + terminal.access_rejected 留痕，与其余十端同一闸）
INSERT INTO terminal_capability_switches (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES ('tcs_term_dealer_portal', 'dealer_portal', 'terminal.enabled', 'global', 'global', TRUE, '独立经销商门户总开关', now(), now())
ON CONFLICT (id) DO NOTHING;

-- 3) 门户账号归属（access_users.account_type = 'dealer' 的账号 → 所属经销商）
CREATE TABLE IF NOT EXISTS dealer_portal_accounts (
    user_id     VARCHAR(64) PRIMARY KEY REFERENCES access_users(id),
    dealer_id   VARCHAR(64) NOT NULL REFERENCES dealers(id),
    status      VARCHAR(16) NOT NULL DEFAULT 'active',
    created_by  VARCHAR(64),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at  TIMESTAMPTZ,
    CONSTRAINT chk_dealer_portal_accounts_status CHECK (status IN ('active', 'revoked'))
);
CREATE INDEX IF NOT EXISTS idx_dealer_portal_accounts_dealer ON dealer_portal_accounts (dealer_id, status);

-- 4) 门户角色与权限（与内部 dealer.* 完全隔离；账号管理权归内部后台）
INSERT INTO roles (id, role_name, description, data_scope, status, created_at)
VALUES ('role_dealer_portal', '经销商门户账号', '独立经销商门户：只见本企业资料、合同、询价、订单与退货，不含任何内部后台权限', 'dealer', 'active', now())
ON CONFLICT (id) DO UPDATE SET role_name = EXCLUDED.role_name, description = EXCLUDED.description, data_scope = EXCLUDED.data_scope, status = EXCLUDED.status;

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('dealer_portal.profile.view',    '经销门户：查看本企业资料与合同', 'dealer', 'low', FALSE, 'active'),
    ('dealer_portal.profile.submit',  '经销门户：提交注册资料送审', 'dealer', 'medium', FALSE, 'active'),
    ('dealer_portal.stock.view',      '经销门户：查看库存外显（有货、紧张、缺货、预计交期）', 'dealer', 'low', FALSE, 'active'),
    ('dealer_portal.inquiry.manage',  '经销门户：未定价商品询价', 'dealer', 'medium', FALSE, 'active'),
    ('dealer_portal.order.manage',    '经销门户：下单、提交与签收', 'dealer', 'high', FALSE, 'active'),
    ('dealer_portal.order.pay',       '经销门户：登记对公转账或平台线上支付到账', 'dealer', 'high', FALSE, 'active'),
    ('dealer_portal.return.request',  '经销门户：退货申请', 'dealer', 'medium', FALSE, 'active'),
    ('dealer.portal_account.manage',  '经销商门户账号开通与查看', 'dealer', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET permission_name = EXCLUDED.permission_name, domain = EXCLUDED.domain, risk_level = EXCLUDED.risk_level, status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
VALUES
    ('role_dealer_portal', 'dealer_portal.profile.view'),
    ('role_dealer_portal', 'dealer_portal.profile.submit'),
    ('role_dealer_portal', 'dealer_portal.stock.view'),
    ('role_dealer_portal', 'dealer_portal.inquiry.manage'),
    ('role_dealer_portal', 'dealer_portal.order.manage'),
    ('role_dealer_portal', 'dealer_portal.order.pay'),
    ('role_dealer_portal', 'dealer_portal.return.request'),
    ('role_platform_admin', 'dealer.portal_account.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
