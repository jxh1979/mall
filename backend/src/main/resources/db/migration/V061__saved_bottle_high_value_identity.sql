-- R3 修复 A1（P1）：客人存酒高价值判定 / 取酒核身 / 手机号检索（REQ-30/32）。
-- 背景：R2/R3 确认 saved_bottles 缺 is_high_value/标签值/姓名昵称/手机号字段，取酒不做核身比对、无复核状态、无作废链。
-- 手机号：本迁移落 customer_phone（值列，加密至 rest 为后续基础设施项）+ customer_phone_hash（SHA-256，用于精确检索，解决"脱敏 vs 按手机号查询"冲突）。

ALTER TABLE saved_bottles
    ADD COLUMN IF NOT EXISTS is_high_value BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS high_value_threshold_cents BIGINT,
    ADD COLUMN IF NOT EXISTS bottle_label_value VARCHAR(128),
    ADD COLUMN IF NOT EXISTS customer_name_or_nickname VARCHAR(128),
    ADD COLUMN IF NOT EXISTS customer_phone VARCHAR(64),
    ADD COLUMN IF NOT EXISTS customer_phone_hash VARCHAR(128);

-- 状态机增 pending_identity_review（取酒核身不一致）/ disputed_frozen（争议冻结）；作废沿用 cancelled
ALTER TABLE saved_bottles DROP CONSTRAINT IF EXISTS chk_saved_bottles_status;
ALTER TABLE saved_bottles ADD CONSTRAINT chk_saved_bottles_status
    CHECK (status IN ('stored', 'claimed', 'cancelled', 'expired', 'pending_identity_review', 'disputed_frozen'));

ALTER TABLE saved_bottles
    ADD CONSTRAINT chk_saved_bottles_high_value_threshold
    CHECK (high_value_threshold_cents IS NULL OR high_value_threshold_cents >= 0);

CREATE INDEX IF NOT EXISTS idx_saved_bottles_phone_hash
    ON saved_bottles(customer_phone_hash) WHERE customer_phone_hash IS NOT NULL;
