-- V220 ticket_refunds 补 merchant_id（受理退款的销售商户），打通商户级退款分量直连管道。
--   背景:ticket_orders 早有 merchant_id(V135),退款单 ticket_refunds 缺列,此前商户级退款分量靠
--        JOIN issued_entitlements 按 allocated 占比反推分摊(基数为 0 的订单保守漏归)。补列后可直接 GROUP BY。
--   口径:退款受理商户 = 原销售订单 merchant_id(退款不改变归属商户);平台直营单 merchant_id 为 NULL。
ALTER TABLE ticket_refunds ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);

-- 历史回填:按 target_id 关联原订单取 merchant_id(仅回填 NULL 行,幂等)。
UPDATE ticket_refunds r
   SET merchant_id = o.merchant_id
  FROM ticket_orders o
 WHERE r.target_id = o.id
   AND r.merchant_id IS NULL;

-- 商户级退款分量聚合走此索引(created_at 落经营日窗口 + merchant_id 分组)。
CREATE INDEX IF NOT EXISTS idx_ticket_refunds_merchant
    ON ticket_refunds (merchant_id, created_at);
