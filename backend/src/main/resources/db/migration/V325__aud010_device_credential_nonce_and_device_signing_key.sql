-- AUD-010 enforce 前置加固（2026-07-20 用户拍板本批做完）：
-- (1) 设备凭证验签重放防护——nonce 落库（device_id+nonce 唯一），验签通过后检查+写入，重复即拒。
--     TTL=容忍窗（±300s），created_at 供定期清理（本迁移不建定时任务，清理由运维/后续 job）。
-- (2) kiosk/tablet 设备级签名公钥——给设备本身一把密钥，不借员工 Keystore 密钥。
--     staff_work_phone 仍用 staff_device_credentials.device_public_key。独立表，避免改 TerminalDeviceRecord。

CREATE TABLE IF NOT EXISTS terminal_device_nonces (
    device_id  VARCHAR(128) NOT NULL,
    nonce      VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (device_id, nonce)
);
CREATE INDEX IF NOT EXISTS idx_terminal_device_nonces_created_at
    ON terminal_device_nonces (created_at);
GRANT SELECT, INSERT, DELETE ON terminal_device_nonces TO complex_app;

-- 设备级签名公钥（X.509 SubjectPublicKeyInfo，base64）。仅 kiosk/tablet 用；staff_work_phone 走员工凭证。
-- status: active/revoked。缺行=未登记签名密钥（enforce 下该设备走软态：grace 放行、enforce 拒）。
CREATE TABLE IF NOT EXISTS terminal_device_signing_keys (
    device_id  VARCHAR(128) NOT NULL PRIMARY KEY,
    public_key TEXT         NOT NULL,
    status     VARCHAR(32)  NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
GRANT SELECT, INSERT, UPDATE, DELETE ON terminal_device_signing_keys TO complex_app;
