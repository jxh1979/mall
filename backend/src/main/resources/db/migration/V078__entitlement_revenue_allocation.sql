-- 待确认项#1：游乐通票收入按业态拆分（财务确认：酒吧+游乐组合票按打折比例拆分计入酒吧/游乐，法律双实体；
-- 也支持手动输入各分项标准单价）。在出票时按各权益分项单价比例把订单实付拆到每张权益，落地以备
-- 业态归属聚合与法律双实体对账，且不重复计入净收入（见下"硬约束"）。
--
-- 两列均可空/向后兼容：
--   * ticket_type_entitlements.component_price_cent —— 该票种该权益（binding 级）的分项标准单价/手动单价。
--     联合票（酒吧分项 + 游乐分项）按各分项单价比例拆分订单实付；全空时回退按数量均摊或全归票务（见服务层注释）。
--   * issued_entitlements.allocated_revenue_cent —— 出票时算得的该权益分配收入（订单实付按比例拆到本权益）。
--     按 entitlement_type 映射业态（bar/amusement/ticket）+ merchant_id（V075）做业态/商户聚合的基础。
--
-- 【净收入不重复硬约束】订单实付全额已计入 T7 official_ticket / T12 revenue.ticket（售票确认收入），
--   本字段仅作"售票收入的业态拆分展示"与法律双实体对账依据：
--   - T7 六源与 revenue.net 口径保持不变（net = 自营售票实付 + 酒吧实收 − 退款，bar 仅取 bar_bill_settlements 现场消费实收）。
--   - T12 另设展示口径 revenue.ticket_bar_share / revenue.ticket_amusement_share（取本字段按业态/商户汇总），
--     不并入 net；revenue.ticket 仍为完整售票收入。二者口径明确区分，避免重复计净收入。

ALTER TABLE ticket_type_entitlements
    ADD COLUMN IF NOT EXISTS component_price_cent BIGINT;

ALTER TABLE issued_entitlements
    ADD COLUMN IF NOT EXISTS allocated_revenue_cent BIGINT;
