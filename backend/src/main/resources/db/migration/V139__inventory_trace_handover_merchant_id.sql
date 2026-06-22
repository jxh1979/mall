-- 2e 溯源归属：inventory_trace_handovers 加 merchant_id（直接归属列）。
-- 背景：交接三方法 scanTraceSegment/reportRobotSegment/reviewException 无归属校验，且 handover 表无 merchant_id，
-- 可携他人 handoverId/exceptionId 跨租户扫码推进/机器人上报/复核异常，篡改交接责任链结论。
-- 加 merchant_id 后三方法按 currentMerchantScope 比对 header.merchant_id（CROSS_MERCHANT_FORBIDDEN）。
ALTER TABLE inventory_trace_handovers
    ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_inventory_trace_handovers_merchant
    ON inventory_trace_handovers (merchant_id);
