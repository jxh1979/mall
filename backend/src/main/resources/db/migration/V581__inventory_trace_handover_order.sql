-- 文件版本：V0.01
-- 第十八批 K 段第 94 条 94-R1 交接链表头记酒吧订单。BSM:1197「出库须同时核验订单和物品码」；
-- BSM:1620「酒水或物品从库房出库、订单核验、机器人取货、后厨接收、服务员送达客人。」
-- 建链时服务层核验该订单含此溯源码登记物品的按瓶待出品明细，order_id 由服务层保证必填。

ALTER TABLE inventory_trace_handovers
    ADD COLUMN IF NOT EXISTS order_id VARCHAR(64) REFERENCES bar_orders(id);

COMMENT ON COLUMN inventory_trace_handovers.order_id IS '出库核验的酒吧订单（第十八批 K 段第 94 条 94-R1）';
