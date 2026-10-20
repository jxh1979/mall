-- 文件版本：V0.01
-- FS-025 存酒转存（延期）（第九批）。依据 ENUM L120 / BSM L1072「转存（延期）：每份存酒的转存次数不得超过后台配置的上限
-- （业务参数，按项目可配，出厂默认 2 次，须同时配置上下限），只能在当前到期日前办理；每次从当前到期日起顺延该酒吧当前配置的
-- 期限（业务参数，按酒吧可配，出厂默认 30 天，须同时配置上下限）；每次转存固化当时的配置版本、顺延天数、新到期日与累计转存次数」
-- 「转存须客人本人短信验证码确认（与转让同款）；员工代办转存的另须当班营销主管复核。转存留痕在配置版本、顺延天数、新到期日、
-- 累计转存次数四项之外，加记经办人与客人确认方式。转存次数达到上限后不再受理转存……不设例外通道」。

-- 1) 转存业务参数（范式同 V413 staff_policy_settings / V497）：出厂行 global（上限 2 次 / 顺延 30 天），上下限留空、首次配置须同时填写；
--    max_renewals 按项目（project:<id>）、extension_days 按酒吧（bar:<id>）配置，缺行回落 global。version 供转存流水固化「当时的配置版本」。
CREATE TABLE IF NOT EXISTS saved_bottle_renewal_policy (
    policy_key VARCHAR(32) NOT NULL,
    scope_id VARCHAR(128) NOT NULL,
    numeric_value INTEGER NOT NULL,
    min_value INTEGER,
    max_value INTEGER,
    version INTEGER NOT NULL DEFAULT 1,
    updated_by VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (policy_key, scope_id),
    CONSTRAINT chk_saved_bottle_renewal_policy_key CHECK (
        (policy_key = 'max_renewals' AND numeric_value >= 0 AND (scope_id = 'global' OR scope_id LIKE 'project:%'))
        OR (policy_key = 'extension_days' AND numeric_value >= 1 AND (scope_id = 'global' OR scope_id LIKE 'bar:%'))),
    CONSTRAINT chk_saved_bottle_renewal_policy_bounds CHECK (
        (min_value IS NULL AND max_value IS NULL)
        OR (min_value IS NOT NULL AND max_value IS NOT NULL AND min_value <= numeric_value AND numeric_value <= max_value))
);
INSERT INTO saved_bottle_renewal_policy (policy_key, scope_id, numeric_value)
VALUES ('max_renewals', 'global', 2), ('extension_days', 'global', 30)
ON CONFLICT (policy_key, scope_id) DO NOTHING;

-- 2) 转存流水：客人本人办理即 effective；员工代办先 pending_review，营销主管（非经办人）复核：通过 effective 并顺延到期日，
--    驳回 rejected（到期日不动、不计入累计转存次数，驳回原因 review_reason）。
CREATE TABLE IF NOT EXISTS saved_bottle_renewals (
    id VARCHAR(64) PRIMARY KEY,
    saved_bottle_id VARCHAR(64) NOT NULL REFERENCES saved_bottles(id),
    merchant_id VARCHAR(64),
    project_id VARCHAR(64),
    customer_id VARCHAR(64),
    phone_hash VARCHAR(128) NOT NULL,
    extension_days INTEGER NOT NULL CHECK (extension_days >= 1),
    previous_expire_at TIMESTAMPTZ NOT NULL,
    new_expire_at TIMESTAMPTZ NOT NULL,
    renewal_count INTEGER NOT NULL CHECK (renewal_count >= 1),
    max_renewals INTEGER NOT NULL,
    limit_config_version VARCHAR(160) NOT NULL,
    days_config_version VARCHAR(160) NOT NULL,
    operator_id VARCHAR(128) NOT NULL,
    operation_source VARCHAR(32) NOT NULL,
    confirmation_method VARCHAR(16) NOT NULL,
    reviewed_by VARCHAR(128),
    reviewed_at TIMESTAMPTZ,
    review_reason VARCHAR(500),
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_saved_bottle_renewals_status CHECK (status IN ('pending_review', 'effective', 'rejected')),
    CONSTRAINT chk_saved_bottle_renewals_reject_reason CHECK (status <> 'rejected' OR review_reason IS NOT NULL),
    CONSTRAINT chk_saved_bottle_renewals_confirmation CHECK (confirmation_method IN ('sms')),
    CONSTRAINT chk_saved_bottle_renewals_reviewer CHECK (reviewed_by IS NULL OR reviewed_by <> operator_id)
);
CREATE INDEX IF NOT EXISTS idx_saved_bottle_renewals_bottle
    ON saved_bottle_renewals (saved_bottle_id, created_at);

-- 3) 员工代办转存的营销主管复核权限码；转存业务参数配置权限码（ENUM L120「按项目可配」「按酒吧可配」「须同时配置上下限」，
--    配置入口 PUT /saved-bottles/renewal-policy，范式同 V497 contract.deposit_months.manage）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('bar.saved_bottle.renewal.review', '营销主管复核员工代办转存', 'bar', 'high', TRUE, 'active'),
    ('bar.saved_bottle.renewal_policy.manage', '配置存酒转存次数上限与顺延期限', 'bar', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES
    ('role_platform_admin', 'bar.saved_bottle.renewal.review'),
    ('role_platform_admin', 'bar.saved_bottle.renewal_policy.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON saved_bottle_renewal_policy TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON saved_bottle_renewals TO complex_app';
    END IF;
END $$;
