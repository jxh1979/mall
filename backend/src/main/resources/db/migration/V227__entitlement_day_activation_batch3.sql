-- V227 结算重构批3-3(游乐「当天无限」PRD §6.3):consumption_window=unlimited_in_day 的按自然日激活。
--   口径(2026-07-04 用户拍板):每次核销激活「一个整天无限」——每自然日首刷即消耗一次(扣 total_times、落当天结算价);
--         同一权益当天再刷卡/扫码判「已激活」放行、不重复扣次不重复计费;跨自然日=新的一天再消耗一次(从剩余次数再扣)。
--   单人跨日作废靠 total_times=1(次日 used 已尽→ENTITLEMENT_USED_UP);多次跨日保留靠 total_times=N(天天各扣一次)。
--   本迁移:建 entitlement_day_activations(某权益某自然日仅一条=当天无限判别+并发防重扣)。
--   consumption_window 字段 V226 已加,此处不重复。仅建表,不改现有逻辑,可独立回滚。

-- 某权益某自然日激活留痕:唯一键(entitlement_id,usage_date)=同权益同自然日仅一条。
--   当天首刷 INSERT ON CONFLICT DO NOTHING 真插(rows>0)→ 调用方 fall-through 正常消耗一次;
--   当天再刷已有记录/并发抢输(rows=0)→ 放行不消耗。跨自然日=新 usage_date=新的一天再消耗。
CREATE TABLE IF NOT EXISTS entitlement_day_activations (
    id                VARCHAR(64)  PRIMARY KEY,
    entitlement_id    VARCHAR(64)  NOT NULL,
    usage_date        DATE         NOT NULL,
    first_activated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_eda_ent_date UNIQUE (entitlement_id, usage_date)
);
CREATE INDEX IF NOT EXISTS idx_eda_entitlement ON entitlement_day_activations (entitlement_id);
