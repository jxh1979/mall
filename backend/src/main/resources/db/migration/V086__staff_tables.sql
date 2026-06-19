-- R5 T1：员工三表（员工档案 / 员工私人手机设备凭证 / 员工通知）。
-- 解锁 T9（Android 设备凭证）。裁决⑨：员工三表 + Android 端凭证本期 P0；MDM 自有侧字段暂缓。
-- 关键边界：本表 staff_device_credentials（员工私人手机 Keystore 公钥 / 不可逆指纹）与
--           既有 terminal_device_credentials（V011，kiosk 一体机随机 token）是两套完全不同实体，
--           互不复用、互不串值、无外键关联。
-- 工程约定：id VARCHAR(64) PK；TIMESTAMPTZ DEFAULT now()；CHECK 命名 chk_<表>_<字段>、唯一 uq_<表>_<列>；
--           隔离列 project_id VARCHAR(64) NOT NULL、merchant_id VARCHAR(64)（NULL=平台员工）；
--           个人信息只存不可逆哈希（PIPL，compliance §4）。

-- 1) 员工档案：注册 → 审批状态机
CREATE TABLE IF NOT EXISTS staff_profiles (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,                 -- 综合体归属（隔离）
    merchant_id VARCHAR(64),                         -- NULL=平台员工；非空=商户员工
    user_id VARCHAR(64),                             -- 审批通过后关联 access_users.id（生成角色）
    name VARCHAR(128) NOT NULL,
    phone_hash VARCHAR(64),                          -- 手机号不可逆哈希（PIPL）
    id_doc_hash VARCHAR(64),                         -- 证件不可逆哈希（可选）
    staff_level_id VARCHAR(64) REFERENCES staff_levels(id),  -- 职级（V055）
    registration_status VARCHAR(32) NOT NULL DEFAULT 'submitted',
    approved_by VARCHAR(64),
    approved_at TIMESTAMPTZ,
    reject_reason VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_staff_profiles_registration_status
        CHECK (registration_status IN ('submitted','approved','rejected','suspended')),
    CONSTRAINT chk_staff_profiles_status
        CHECK (status IN ('active','inactive'))
);
CREATE INDEX IF NOT EXISTS idx_staff_profiles_scope ON staff_profiles(project_id, merchant_id);

-- 2) 员工私人手机设备凭证（Android Keystore 公钥 / 不可逆设备指纹）—— 区别于 kiosk 的 terminal_device_credentials
CREATE TABLE IF NOT EXISTS staff_device_credentials (
    id VARCHAR(64) PRIMARY KEY,
    staff_profile_id VARCHAR(64) NOT NULL REFERENCES staff_profiles(id) ON DELETE CASCADE,
    device_public_key TEXT NOT NULL,                 -- Android Keystore 设备公钥
    device_fingerprint_hash VARCHAR(64) NOT NULL,    -- 不可逆设备指纹哈希（不读手机本机 NFC 唯一标识，D8）
    bind_status VARCHAR(32) NOT NULL DEFAULT 'pending_review',
    supersedes_credential_id VARCHAR(64) REFERENCES staff_device_credentials(id),  -- 换机链：指向被替换旧凭证
    integrity_status VARCHAR(32) NOT NULL DEFAULT 'trusted',
    revoked_reason VARCHAR(255),
    bound_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_staff_device_credentials_bind_status
        CHECK (bind_status IN ('pending_review','active','superseded','revoked')),
    CONSTRAINT chk_staff_device_credentials_integrity_status
        CHECK (integrity_status IN ('trusted','root_detected','debug_detected','abnormal'))
);
-- 每个员工至多一条 active 凭证（换机先 superseded 旧的、再 active 新的）
CREATE UNIQUE INDEX IF NOT EXISTS uq_staff_device_credentials_one_active
    ON staff_device_credentials(staff_profile_id) WHERE bind_status = 'active';
-- 待审 / 生效中的指纹全局唯一，避免同设备并行多挂
CREATE UNIQUE INDEX IF NOT EXISTS uq_staff_device_credentials_fingerprint
    ON staff_device_credentials(device_fingerprint_hash) WHERE bind_status IN ('pending_review','active');

-- 3) 员工通知 / 合规同意凭据（必须有写入方，避免"建而无写入方"）
CREATE TABLE IF NOT EXISTS employee_notices (
    id VARCHAR(64) PRIMARY KEY,
    staff_profile_id VARCHAR(64) NOT NULL REFERENCES staff_profiles(id) ON DELETE CASCADE,
    notice_type VARCHAR(48) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    consent_required BOOLEAN NOT NULL DEFAULT false,
    consent_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    consented_at TIMESTAMPTZ,
    created_by VARCHAR(64) NOT NULL,                 -- 写入方（系统/审批人/合规模块）
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_employee_notices_notice_type
        CHECK (notice_type IN ('approval_result','status_change','compliance_consent','system_notice')),
    CONSTRAINT chk_employee_notices_consent_status
        CHECK (consent_status IN ('pending','acknowledged','consented','declined'))
);
CREATE INDEX IF NOT EXISTS idx_employee_notices_staff ON employee_notices(staff_profile_id);

-- 权限点种子（D3，dot 风格；纳入最小颗粒度预置）
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('staff.profile.submit', 'Submit staff registration', 'access', 'low', FALSE, 'active'),
    ('staff.profile.review', 'Review staff registration', 'access', 'high', TRUE, 'active'),
    ('staff.profile.view', 'View staff profiles', 'access', 'medium', FALSE, 'active'),
    ('staff.device.bind', 'Bind / rebind staff device credential', 'access', 'medium', FALSE, 'active'),
    ('staff.device.review', 'Review staff device rebind', 'access', 'high', TRUE, 'active'),
    ('staff.device.revoke', 'Revoke staff device credential', 'access', 'high', TRUE, 'active'),
    ('staff.notice.view', 'View / acknowledge staff notices', 'access', 'low', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
