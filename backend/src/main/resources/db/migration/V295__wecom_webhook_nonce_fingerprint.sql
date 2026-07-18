-- AUD-021（P2）：企微 webhook nonce 绑定报文指纹。
-- 此前 nonce 先于业务事务独立落库：首次业务 DB 失败或响应丢失后，同一合法报文重试先被 401 REPLAY，
-- 瞬时故障可能永久漏同步该会话。现把报文体摘要(与验签 body_digest 同源)与 nonce 同行登记：
-- 同 nonce+同指纹=合法重投递（放行给业务幂等层精确重放/安全接管）；同 nonce+异指纹=真重放拒绝。
-- 存量行指纹为空串：其重投递会被判 REPLAY，随 TTL(≈5分钟)清理自然消失，无业务影响。
ALTER TABLE wecom_webhook_nonces ADD COLUMN request_fingerprint TEXT NOT NULL DEFAULT '';
