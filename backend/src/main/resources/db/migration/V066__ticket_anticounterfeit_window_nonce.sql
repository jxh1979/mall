-- R5 T4 二维码折中防伪（裁决①：P0=哈希 + 核销端强制 30/60s 刷新窗口校验 + nonce 防重放，截图窗口内失效）。
-- ticket_credentials：补刷新窗口锚点 window_started_at；预留 HMAC 签名位 signature_value/signature_version（P1，本期不写签名逻辑）。
-- ticket_verifications：补 nonce 防重放 + 事后核对维度 staff_id/merchant_id/window_valid + 签名校验占位 signature_valid（本期恒 NULL）。
ALTER TABLE ticket_credentials
    ADD COLUMN IF NOT EXISTS window_started_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS signature_value VARCHAR(256),
    ADD COLUMN IF NOT EXISTS signature_version VARCHAR(16);

ALTER TABLE ticket_verifications
    ADD COLUMN IF NOT EXISTS nonce VARCHAR(128),
    ADD COLUMN IF NOT EXISTS staff_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS window_valid BOOLEAN,
    ADD COLUMN IF NOT EXISTS signature_valid BOOLEAN;

-- 防重放：同一权益的同一 nonce 只能成功核销一次（重放第二次撞唯一键 → 拒绝）。
CREATE UNIQUE INDEX IF NOT EXISTS uq_ticket_verifications_nonce
    ON ticket_verifications (entitlement_id, nonce) WHERE status = 'success';
