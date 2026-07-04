-- V219 提醒窗天数可配置化(治理铁律:业务策略参数禁止硬编码)。
--   原:三处提醒窗写死常量——
--     contract.renewal(续约提醒)=30天  ContractRenewalNotificationJob.RENEWAL_NOTICE_DAYS
--     inventory.saved_bottle(存酒临期)=7天 SavedBottleNotificationJob.EXPIRING_NOTICE_DAYS
--     leasing.lead_follow_up(招商跟进)=3天 LeasingService.FOLLOW_UP_REMINDER_DAYS
--   改:落键值配置表 notification_windows,各处经共享 NotificationWindowConfig 读(缺配置兜底默认,行为不变)。
--   运营调窗口改配置不改代码。跨域键值表(仿 operating_params 键值范式)。

CREATE TABLE IF NOT EXISTS notification_windows (
    window_key  VARCHAR(64) PRIMARY KEY,   -- 域.用途,如 contract.renewal / inventory.saved_bottle / leasing.lead_follow_up
    days        INT         NOT NULL,       -- 提醒提前量(天),>0
    description VARCHAR(200),
    updated_by  VARCHAR(64),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_notif_window_days CHECK (days > 0)
);
-- 种子=原硬编码值(行为不变)。
INSERT INTO notification_windows (window_key, days, description) VALUES
    ('contract.renewal',          30, '商户合同续约提醒提前天数'),
    ('inventory.saved_bottle',     7, '存酒临期提醒提前天数'),
    ('leasing.lead_follow_up',     3, '招商线索未跟进提醒天数')
ON CONFLICT (window_key) DO NOTHING;

-- 维护权限(中敏:仅影响提醒时机,非资金口径)。复用现有平台管理员;查看不单设。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('platform.notification_window.manage', 'Maintain notification reminder windows (days)', 'platform', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'platform.notification_window.manage'
ON CONFLICT (role_id, permission_code) DO NOTHING;
