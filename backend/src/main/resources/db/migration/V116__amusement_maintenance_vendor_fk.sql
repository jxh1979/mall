-- V116: 游乐检修单位外键收口（审计 ④ P2 / amuse-3 / 设计 §6.6.1）
-- 背景：amusement_maintenance_record.maintenance_vendor_id 原为软引用（V081 仅软引用、无外键），
--   Service 层此前只做非空校验，未校验供应商真实存在性，存在悬空引用风险。
-- 主表确认：检修单位复用供应链供应商主数据 inventory_suppliers（V012），
--   主键 id VARCHAR(64) 与 maintenance_vendor_id VARCHAR(64) 类型完全匹配，可安全补外键。
-- 类别口径：V081 注释所写 supplier_category ∈ {maintenance, service} 为文档口径；
--   主表实际列名为 supplier_type（无 CHECK 枚举收口，默认 'general'），与文档口径不一致，
--   故 DB 层仅做存在性外键约束，不强卡 supplier_type 取值；类别合适性由 Service 层
--   （存在 + status=active）近似把关，避免误伤存量数据（详见 AmusementMaintenanceService.requireExistingVendor）。
-- 兼容策略：使用 NOT VALID 仅对新增/更新行强制外键，不校验存量行，规避历史脏数据导致迁移失败；
--   待存量回填/清洗完成后，可执行 VALIDATE CONSTRAINT 收紧。
ALTER TABLE amusement_maintenance_record
    ADD CONSTRAINT fk_amusement_maint_vendor
        FOREIGN KEY (maintenance_vendor_id)
        REFERENCES inventory_suppliers (id)
        NOT VALID;
