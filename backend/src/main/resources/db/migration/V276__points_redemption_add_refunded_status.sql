-- 积分虚拟券 P1(2026-07-08):券核销领取时若实物缺货,退回积分并标记 refunded。
-- 原 status_check 仅允许 pending_pickup/picked_up/cancelled,新增 refunded 终态。
ALTER TABLE customer_points_redemptions DROP CONSTRAINT IF EXISTS customer_points_redemptions_status_check;
ALTER TABLE customer_points_redemptions ADD CONSTRAINT customer_points_redemptions_status_check
    CHECK (status IN ('pending_pickup', 'picked_up', 'refunded', 'cancelled'));
