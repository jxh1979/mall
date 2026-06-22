-- P2 修复（用户裁定：ticket_orders 加 merchant_id 行级隔离，堵订单列表潜伏越权）。
-- 背景：ticket_orders 无 merchant_id 列，listTicketOrders 返全平台；ticket.order.read 一旦下放商户角色即跨租户泄露全部订单。
-- 语义：merchant_id = 下单受理/销售商户（哪个商户的终端/渠道售出）；平台下单为 NULL（平台官方票务口径，向后兼容既有行）。
-- 列表读取：平台 scope 看全部（含 NULL）；商户 scope 仅看 merchant_id = 自身（不含 NULL 平台单）。
-- domain-ownership：本列使 ticket_orders 由 single_complex_default 升级为 direct(merchant_id) scope（CSV 同步更新）。
ALTER TABLE ticket_orders
    ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_ticket_orders_merchant_created
    ON ticket_orders (merchant_id, created_at DESC);
