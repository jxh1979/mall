-- V153 赠菜/赠酒提成扣减（F-TXN-05）：bar_order_items 增 is_gift 赠送标记。
-- 背景：吧台销售随单赠送的菜品/酒（如开瓶赠果盘）对客 amount_cent=0、不计消费额（账单 subtotal），
--   但其成本应按 V152 商品 internal_settlement_price_cent × 数量 从该销售的提成基数中扣除——
--   即销售人员不就赠送项成本计提成（业主 2026-06-22 确认口径）。此前账单/提成表与 bar_order_items
--   均无赠送标记，calculateCommission 无法识别赠送项（开发文档 §6.9A.2 标 BLOCKED）。本列承载赠送标记，
--   解锁 F-TXN-05 的「赠品按内部价从提成扣除」。亦为 §6.8 点单明细表逻辑设计中既登记的 is_gift 列之物理落地。
-- 口径：布尔；NOT NULL DEFAULT FALSE（既有明细均为正常售卖项，向后兼容），与 V091 voided/return_to_stock 同范式。
ALTER TABLE bar_order_items
    ADD COLUMN IF NOT EXISTS is_gift BOOLEAN NOT NULL DEFAULT FALSE;
