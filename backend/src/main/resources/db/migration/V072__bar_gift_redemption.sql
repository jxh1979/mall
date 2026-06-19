-- R5 T15 赠饮核销→0元点单→扣吧台库存（事件驱动）
-- 链路：ticketing 核销赠饮权益(entitlement_type='gift_drink') 成功 → 发 Spring 事件 ticket.gift_verified
--   → bar 域 BarGiftConsumer 监听 → 生成 0 元 bar_order(order_type='gift') + 出品 ticket → deliver 调
--   atomicDecreaseStock(redeem_consume) → 回填 inventory_movement_id 到出品事件与本桥表。
-- 修 R5/R4-G1 DRIFT：events 表无库存关联列；门票赠饮→bar 库存链路未实现。

-- ── 1) 出品事件关联库存流水（修 R4-G1 DRIFT：events 无 inventory_movement_id 关联）────────
ALTER TABLE bar_production_ticket_events
    ADD COLUMN IF NOT EXISTS inventory_movement_id VARCHAR(64);

-- ── 2) bar_orders.order_type：现状无该列（V013 建表未含 order_type，亦无 order_type CHECK）──
-- 新增 order_type 列，默认 'standard'（既有单视为普通单），CHECK 取值集 = {standard, gift}。
-- 0 元赠饮单 order_type='gift'：不计商户收入（成本归平台）、但进账单/留痕。
ALTER TABLE bar_orders
    ADD COLUMN IF NOT EXISTS order_type VARCHAR(32) NOT NULL DEFAULT 'standard';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_bar_orders_order_type'
    ) THEN
        ALTER TABLE bar_orders
            ADD CONSTRAINT chk_bar_orders_order_type CHECK (order_type IN ('standard', 'gift'));
    END IF;
END $$;

-- ── 3) 赠饮兑换桥表（幂等：一次核销至多一单；冲正联动）──────────────────────────
CREATE TABLE IF NOT EXISTS bar_gift_redemptions (
    id VARCHAR(64) PRIMARY KEY,
    entitlement_id VARCHAR(64) NOT NULL,
    verification_id VARCHAR(64) NOT NULL,
    bar_order_id VARCHAR(64) NOT NULL REFERENCES bar_orders(id),
    bar_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64),
    item_ref VARCHAR(64),
    inventory_movement_id VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'created',
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_bar_gift_redemption_verification UNIQUE (verification_id),
    CONSTRAINT chk_bar_gift_redemption_status CHECK (status IN ('created', 'delivered', 'reversed'))
);

CREATE INDEX IF NOT EXISTS idx_bar_gift_redemptions_entitlement
    ON bar_gift_redemptions (entitlement_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_bar_gift_redemptions_merchant
    ON bar_gift_redemptions (merchant_id, created_at DESC);
