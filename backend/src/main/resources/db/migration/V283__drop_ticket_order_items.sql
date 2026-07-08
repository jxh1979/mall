-- 废除票务订单明细行空壳表(2026-07-08 用户拍板)。
-- 全代码库零引用(仅 V009 建表);票务订单直接展开成一张张权益凭证(issued_entitlements),不走"订单明细行"。
DROP TABLE IF EXISTS ticket_order_items;
