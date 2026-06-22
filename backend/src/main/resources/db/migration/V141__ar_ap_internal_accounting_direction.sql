-- F2 财务统一台账：ar_ap_items 放开 direction 允许 internal_accounting（内部核算）。
-- 背景：finance-ar-ap-ledger 设计 §4.1 早已列 direction=internal_accounting（平台自营酒吧/游乐内部经营收入归集），
--   但 V008 建表 CHECK 仅允许 receivable/payable，导致内部核算归集无法落库（设计与迁移漂移）。
-- 用途：票务/游乐/储值/会员收入确认（finance 支付意图 succeeded）时按 internal_accounting 生成内部核算台账，
--   供对账/内部核算下钻；按用户裁定 C1，internal_accounting 不并入 revenue.net（net 仍取业务表）。
ALTER TABLE ar_ap_items DROP CONSTRAINT IF EXISTS chk_ar_ap_items_direction;
ALTER TABLE ar_ap_items ADD CONSTRAINT chk_ar_ap_items_direction
    CHECK (direction IN ('receivable', 'payable', 'internal_accounting'));
