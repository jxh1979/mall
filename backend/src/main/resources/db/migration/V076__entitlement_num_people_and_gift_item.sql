-- R5 遗留 TODO 数据来源补齐（合并一个迁移）：
--   TODO#1 多人票核销人数来源：票种该权益绑定（binding 级）配置每份核销人数；
--   TODO#2 赠饮→吧台 SKU 映射来源：权益模板（template 级）指向吧台商品/库存 item。
-- 两列均给默认/可空，向后兼容既有数据。

-- TODO#1：ticket_type_entitlements.num_people——该票种该权益每份核销人数。
-- 联合票/多人票 binding 配 >1；默认 1（单人票）。发放时写入 issued_entitlements.num_people，
-- 核销 verified_people 随之生效（接 R5 T5）。
ALTER TABLE ticket_type_entitlements
    ADD COLUMN IF NOT EXISTS num_people INTEGER NOT NULL DEFAULT 1;

-- TODO#2：entitlement_templates.bar_item_ref——gift_drink 模板指向的吧台商品/库存 item。
-- 可空：未配置时赠饮核销跳过库存扣减并在 redemption 标注（接 R5 T15）。
ALTER TABLE entitlement_templates
    ADD COLUMN IF NOT EXISTS bar_item_ref VARCHAR(64);
