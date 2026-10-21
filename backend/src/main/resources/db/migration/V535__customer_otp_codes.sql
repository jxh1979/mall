-- 文件版本：V0.01
-- ROOT-05-R3：平台自有的客人短信验证码签发、留存与核验（真实下发仍经短信供应商，平台不自研短信通道）。
-- 依据：BSM 第 1563 行「验证码下发与送达结果、失败原因（用于新人判定、注册登录和关键操作验证）」；第 1564 行「不自研短信通道。」；
--   第 886 行「登录可支持手机号短信验证码……」；第 1073 行取酒核验短信验证码；第 1171 行「转让须经出让人手机短信验证码确认。」
-- 不存明文手机号与验证码：phone_index＝手机号盲索引，code_hash＝盲索引（id + ':' + 验证码）。不回填存量数据。
CREATE TABLE customer_otp_codes (
    id VARCHAR(64) PRIMARY KEY,
    phone_index VARCHAR(128) NOT NULL,
    scene VARCHAR(64) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    consumed_at TIMESTAMPTZ,
    invalidated_at TIMESTAMPTZ,
    send_status VARCHAR(32) NOT NULL,
    provider_ref VARCHAR(128),
    send_reason VARCHAR(256),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_customer_otp_send_status CHECK (send_status IN ('sent', 'failed', 'throttled'))
);
CREATE INDEX idx_customer_otp_codes_lookup ON customer_otp_codes (phone_index, scene, created_at DESC);
