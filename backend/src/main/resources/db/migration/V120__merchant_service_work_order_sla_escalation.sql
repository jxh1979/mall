-- ④ P2：物业工单 SLA 自动超时升级。
-- 审计指出 V022 仅有静态 due_at，无 sla_breached 标记，也无升级留痕动作类型。
-- 本迁移补齐 SLA 升级所需最小字段与动作类型，状态机/状态枚举保持不变（升级不改 status，仅打标 + 留痕）。

-- 1) 工单表补 SLA 升级标记字段（向后兼容：默认未违约、未升级）。
ALTER TABLE merchant_service_work_orders
    ADD COLUMN IF NOT EXISTS sla_breached BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE merchant_service_work_orders
    ADD COLUMN IF NOT EXISTS escalated_at TIMESTAMPTZ;

-- 2) 扫描“due_at 已过、未完成、尚未升级”的工单需要高效索引。
--    仅索引未关闭/未取消且未升级的行（partial index），定时任务命中后将 escalated_at 置非空而自动移出索引。
CREATE INDEX IF NOT EXISTS idx_merchant_service_work_orders_sla_due
    ON merchant_service_work_orders (due_at)
 WHERE due_at IS NOT NULL
   AND escalated_at IS NULL
   AND status NOT IN ('resolved', 'closed', 'cancelled');

-- 3) 动作流水增加 'escalated' 动作类型，用于 SLA 自动升级留痕（既有约束不含该值）。
ALTER TABLE merchant_service_work_order_actions
    DROP CONSTRAINT IF EXISTS chk_merchant_service_work_order_action_type;

ALTER TABLE merchant_service_work_order_actions
    ADD CONSTRAINT chk_merchant_service_work_order_action_type
    CHECK (action_type IN ('created', 'assigned', 'commented', 'status_changed', 'closed', 'escalated'));
