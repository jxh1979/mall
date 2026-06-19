-- P0 清单#2 审计接线：audit_events 增分类 / 主体类型两列，供积分入账等事件按类目与主体检索。
--
-- 既有列（V002 建表）：id, occurred_at, event_name, event_result, actor_type, actor_id,
--   auth_context, terminal, permission_code, business_type, business_id, request_id,
--   idempotency_key, metadata, merchant_id（V045 另加隔离列，与此无关）。
--
-- 本迁移新增（均可空，向后兼容：不加 NOT NULL，既有写入路径 recordSuccess/recordFailure 不受影响）：
--   event_category 事件类目：loyalty / order / refund / ...（聚合检索维度）
--   subject_type   事件主体类型：customer_points / customer_points_transfer / ...（与 business_id 配合定位主体）

ALTER TABLE audit_events
    ADD COLUMN IF NOT EXISTS event_category VARCHAR(64),
    ADD COLUMN IF NOT EXISTS subject_type   VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_audit_events_category_occurred_at
    ON audit_events (event_category, occurred_at DESC)
    WHERE event_category IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_events_subject
    ON audit_events (subject_type, business_id)
    WHERE subject_type IS NOT NULL;
