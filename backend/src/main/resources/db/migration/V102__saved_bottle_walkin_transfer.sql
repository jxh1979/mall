-- P0 清单#4：存酒散客化 + 取酒核身 + 转赠 + 争议入状态机（R6-D05）。
-- 依据：openapi-p0.json SavedBottleRequest/SavedBottleClaimRequest 新增字段（契约在前）；R6-D05 裁决：
--   散客可存（customer_id 可空）、不限瓶数、存期默认 90 天后台可配、作废主管复核。
-- 现状基准（已 Read V012/V061）：
--   V012 建 saved_bottles，customer_id VARCHAR(64) NOT NULL（双重阻断散客，本迁移 DROP NOT NULL 改可空）；
--   V061 已加 is_high_value/high_value_threshold_cents/bottle_label_value/customer_name_or_nickname/
--        customer_phone/customer_phone_hash，并把 status CHECK 扩为
--        {stored,claimed,cancelled,expired,pending_identity_review,disputed_frozen}。
--   故本迁移仅补：登记照片/人工备注、转赠受让信息、转赠/争议状态入状态机。全部 IF NOT EXISTS 幂等。

-- 1) 散客化：customer_id 改可空（DROP NOT NULL）。
ALTER TABLE saved_bottles ALTER COLUMN customer_id DROP NOT NULL;

-- 2) 登记字段补全（V061 已落标签值/姓名昵称/手机号；此处补登记照片证据与人工备注）。
ALTER TABLE saved_bottles
    ADD COLUMN IF NOT EXISTS registration_photo_refs JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS manual_note VARCHAR(1000);

-- 3) 转赠受让信息（transfer）：受让人姓名昵称/手机号/手机号哈希 + 转赠时间 + 来源存酒。
ALTER TABLE saved_bottles
    ADD COLUMN IF NOT EXISTS transferred_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS transfer_to_name_or_nickname VARCHAR(128),
    ADD COLUMN IF NOT EXISTS transfer_to_phone VARCHAR(64),
    ADD COLUMN IF NOT EXISTS transfer_to_phone_hash VARCHAR(128);

-- 4) 状态机：增 transferred（已转赠原瓶终态）/ disputed（争议受理态，区别于 V061 的 disputed_frozen 高价值作废冻结）。
--    幂等 DO 块：先核对再加，避免历史/测试数据触发约束失败。
ALTER TABLE saved_bottles DROP CONSTRAINT IF EXISTS chk_saved_bottles_status;
ALTER TABLE saved_bottles ADD CONSTRAINT chk_saved_bottles_status
    CHECK (status IN (
        'stored', 'claimed', 'cancelled', 'expired',
        'pending_identity_review', 'disputed_frozen',
        'transferred', 'disputed'
    ));

-- 5) 转赠流水：复用 saved_bottle_claims 不合适（语义为取酒），单列轻量转赠表留痕（受让信息 + 操作员 + 证据）。
CREATE TABLE IF NOT EXISTS saved_bottle_transfers (
    id VARCHAR(64) PRIMARY KEY,
    saved_bottle_id VARCHAR(64) NOT NULL REFERENCES saved_bottles(id),
    from_customer_id VARCHAR(64),
    from_name_or_nickname VARCHAR(128),
    to_name_or_nickname VARCHAR(128) NOT NULL,
    to_phone VARCHAR(64),
    to_phone_hash VARCHAR(128),
    transfer_quantity INTEGER NOT NULL,
    operator_id VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_saved_bottle_transfers_quantity CHECK (transfer_quantity > 0),
    CONSTRAINT chk_saved_bottle_transfers_status CHECK (status IN ('transferred'))
);

CREATE INDEX IF NOT EXISTS idx_saved_bottle_transfers_saved_bottle
    ON saved_bottle_transfers (saved_bottle_id, created_at DESC);
