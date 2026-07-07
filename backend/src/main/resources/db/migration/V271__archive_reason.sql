-- V271 存档理由:给存档删除范式的所有实体补 archive_reason 列。
-- 用户拍板(2026-07-07,业务HTML 数据删除章节):"存档及删除需要填写存档及删除理由。"
-- 删除理由已有 delete_reason 列(V255 起);存档轴此前只有 archived_at/archived_by 无理由列,本迁移补齐。
-- 存档时写入非空理由(服务层 requireText 强校验),取消存档时清空。全列可空/向后兼容/可独立回滚。

ALTER TABLE entitlement_templates          ADD COLUMN IF NOT EXISTS archive_reason TEXT;
ALTER TABLE ticket_types                   ADD COLUMN IF NOT EXISTS archive_reason TEXT;
ALTER TABLE ticket_price_rules             ADD COLUMN IF NOT EXISTS archive_reason TEXT;
ALTER TABLE merchants                      ADD COLUMN IF NOT EXISTS archive_reason TEXT;
ALTER TABLE bar_product_categories         ADD COLUMN IF NOT EXISTS archive_reason TEXT;
ALTER TABLE bar_products                   ADD COLUMN IF NOT EXISTS archive_reason TEXT;
ALTER TABLE bar_combos                     ADD COLUMN IF NOT EXISTS archive_reason TEXT;
ALTER TABLE merchant_service_work_orders   ADD COLUMN IF NOT EXISTS archive_reason TEXT;
ALTER TABLE bar_reservations               ADD COLUMN IF NOT EXISTS archive_reason TEXT;
ALTER TABLE staff_profiles                 ADD COLUMN IF NOT EXISTS archive_reason TEXT;
ALTER TABLE facility_equipment             ADD COLUMN IF NOT EXISTS archive_reason TEXT;
