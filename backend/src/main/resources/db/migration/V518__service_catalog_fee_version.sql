-- 文件版本：V0.01
-- V518：TK-23-R3 服务管理费变更草案/版本化（第十批）。
-- 依据：ENUM L291 / BSM L1127「配置主体与审批、出厂默认值、变更版本化与指定生效日、
--   单据下单时锁定费率版本……一律比照销售提成的对应规则执行」。
-- 铁律 8：不为存量数据回填；存量目录项 fee_version 取默认 1（现行值即 V1）。

-- 1) 服务单：下单锁定的管理费版本。
ALTER TABLE service_orders ADD COLUMN IF NOT EXISTS snap_fee_version INT;

-- 2) 服务目录：现行管理费版本号 + 待转正草案（拟改模式/数值、指定生效日、起草人、复核人）。
ALTER TABLE service_catalog_items ADD COLUMN IF NOT EXISTS fee_version INT NOT NULL DEFAULT 1;
ALTER TABLE service_catalog_items ADD COLUMN IF NOT EXISTS next_mgmt_fee_mode VARCHAR(16);
ALTER TABLE service_catalog_items ADD COLUMN IF NOT EXISTS next_mgmt_fee_value BIGINT;
ALTER TABLE service_catalog_items ADD COLUMN IF NOT EXISTS next_effective_from DATE;
ALTER TABLE service_catalog_items ADD COLUMN IF NOT EXISTS next_requested_by VARCHAR(64);
ALTER TABLE service_catalog_items ADD COLUMN IF NOT EXISTS next_requested_at TIMESTAMPTZ;
ALTER TABLE service_catalog_items ADD COLUMN IF NOT EXISTS next_approved_by VARCHAR(64);
ALTER TABLE service_catalog_items ADD CONSTRAINT chk_svc_next_mgmt_mode
    CHECK (next_mgmt_fee_mode IS NULL OR next_mgmt_fee_mode IN ('none', 'percent', 'fixed'));
