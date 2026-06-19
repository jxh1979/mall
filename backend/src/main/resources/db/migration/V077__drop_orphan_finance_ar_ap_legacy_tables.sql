-- TODO#4 finance 孤儿表清理（R5/T7 收口）
-- finance_ar_ap_items / finance_ar_ap_payments（V002 legacy，UUID PK，business_entity_id/amount_cents 旧 schema）
-- 经核验：无任何 Java/测试/seed 引用、无其它迁移引用；真实运行期 AR/AP 台账为 ar_ap_items(V008)。
-- T7(V069) 已把 locked_day_close_id / ar_ap_adjustments 落到真实表 ar_ap_items，本表彻底无用。
-- 先删子表（finance_ar_ap_payments 通过 ar_ap_item_id FK 引用父表），再删父表。
DROP TABLE IF EXISTS finance_ar_ap_payments;
DROP TABLE IF EXISTS finance_ar_ap_items;
