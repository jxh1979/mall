-- AUD 复核 P1-2：企微 webhook nonce 防重放持久化。
-- 验签通过后原子登记 nonce（INSERT ON CONFLICT DO NOTHING），重复 nonce 判重放拒绝。
-- seen_at 供 TTL 清理（超窗 nonce 亦被 verify 的 timestamp 窗口二次拦截）。
CREATE TABLE wecom_webhook_nonces (
    nonce   TEXT PRIMARY KEY,
    seen_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_wecom_webhook_nonces_seen_at ON wecom_webhook_nonces (seen_at);
