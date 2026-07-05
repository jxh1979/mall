-- 阶段1（2026-07-05 用户拍板）游乐核销走正路：通票核销分流进游乐正路后，通票侧 ticket_verifications
-- 补一列 amusement_verification_id 作为「两侧写 + 交叉引用」（D3），指向 amusement_verifications.id，
-- 便于游乐核销与通票核销双向对账。仅游乐类权益走正路核销时写入；非游乐核销与历史记录为 NULL（向后兼容）。
ALTER TABLE ticket_verifications
    ADD COLUMN IF NOT EXISTS amusement_verification_id VARCHAR(64);

-- 交叉引用反查索引（游乐核销 id → 通票核销记录），部分索引仅覆盖已分流记录。
CREATE INDEX IF NOT EXISTS idx_ticket_verifications_amusement_ref
    ON ticket_verifications (amusement_verification_id)
    WHERE amusement_verification_id IS NOT NULL;
