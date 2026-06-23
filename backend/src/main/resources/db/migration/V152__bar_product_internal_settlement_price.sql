-- V152 内部结算价（F-TXN-05 + 通票赠饮平台↔酒吧结算解锁）：bar_products 增 internal_settlement_price_cent。
-- 背景：bar_products 仅有对客售价 price_cent；通票免费酒水「平台↔酒吧结算」与吧台赠送菜品「按内部价从销售提成扣除」
--   （F-TXN-05）均因系统无内部价/成本价存储而搁置。本列为每个吧台商品（酒水/菜品）录入「内部结算价」——
--   作为 ① 平台向酒吧结算通票免费酒水 与 ② 赠送菜品扣减销售提成 的统一价基。运营在商品管理页录入。
-- 口径：单位分；**可空**（未录入=该商品不参与结算/提成扣减，向后兼容既有商品）；非空时 >=0。
ALTER TABLE bar_products
    ADD COLUMN IF NOT EXISTS internal_settlement_price_cent BIGINT;

ALTER TABLE bar_products
    DROP CONSTRAINT IF EXISTS chk_bar_products_internal_price;
ALTER TABLE bar_products
    ADD CONSTRAINT chk_bar_products_internal_price
    CHECK (internal_settlement_price_cent IS NULL OR internal_settlement_price_cent >= 0);
