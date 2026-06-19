-- V123: 游乐检修单位外键收紧 NOT VALID → VALIDATED（延期项③ / amuse-3 / 设计 §6.6.1）
-- 背景：V116 给 amusement_maintenance_record.maintenance_vendor_id 加了外键
--   fk_amusement_maint_vendor → inventory_suppliers(id)，但使用 NOT VALID（仅对新增/更新行强制，
--   不校验存量行）。本迁移在清洗存量悬空引用后执行 VALIDATE CONSTRAINT，将约束收紧为已校验。
--
-- 列可空性确认（V081 第 12 行）：maintenance_vendor_id VARCHAR(64) NOT NULL —— 该列「不可空」。
--   因此任务建议的「悬空引用置 NULL」方案不可行（会违反 NOT NULL 约束）。
--   且业务上检修单必须有检修单位（NOT NULL 即体现该业务规则），不应删除整条检修记录（审计留痕需保留）。
--
-- 清洗策略（占位供应商回挂，安全、不删记录、不破坏 NOT NULL）：
--   1) 幂等创建一个保留占位供应商 '__legacy_orphan_vendor__'（status='archived'，
--      使其不会被业务侧 requireExistingVendor 选中——Service 仅放行 status=active，详见
--      AmusementMaintenanceService.requireExistingVendor）。
--   2) 将所有 maintenance_vendor_id 非空但在 inventory_suppliers 无对应 id 的存量悬空行，
--      改挂到该占位供应商，使其满足外键存在性，从而 VALIDATE 不会因存量脏数据失败。
--   仅当确实存在悬空行时才落地占位供应商的实际引用（占位行始终保证存在以维持幂等与可重跑）。
--
-- 幂等：占位供应商 ON CONFLICT DO NOTHING；UPDATE 用 NOT EXISTS 守卫（重跑时悬空集为空，无副作用）；
--   VALIDATE 前用 information_schema 守卫，约束不存在或已校验时跳过（IF EXISTS 语义）。

-- 步骤 1：幂等创建保留占位供应商（archived，业务侧不可选）。
INSERT INTO inventory_suppliers (id, supplier_name, supplier_type, status, created_at, updated_at)
VALUES (
    '__legacy_orphan_vendor__',
    '[历史悬空占位] 检修单位主数据缺失',
    'general',
    'archived',
    now(),
    now()
)
ON CONFLICT (id) DO NOTHING;

-- 步骤 2：清洗存量悬空引用——改挂到占位供应商（不置 NULL，因列 NOT NULL；不删记录，保留审计留痕）。
UPDATE amusement_maintenance_record amr
   SET maintenance_vendor_id = '__legacy_orphan_vendor__'
 WHERE amr.maintenance_vendor_id IS NOT NULL
   AND amr.maintenance_vendor_id <> '__legacy_orphan_vendor__'
   AND NOT EXISTS (
        SELECT 1
          FROM inventory_suppliers s
         WHERE s.id = amr.maintenance_vendor_id
   );

-- 步骤 3：收紧外键——VALIDATE 此前 V116 创建的 NOT VALID 约束 fk_amusement_maint_vendor。
--   守卫：仅当约束存在且尚未校验（is_validated=false）时执行，保证幂等可重跑。
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM pg_constraint c
          JOIN pg_class t ON t.oid = c.conrelid
         WHERE c.conname = 'fk_amusement_maint_vendor'
           AND t.relname = 'amusement_maintenance_record'
           AND c.contype = 'f'
           AND c.convalidated = false
    ) THEN
        ALTER TABLE amusement_maintenance_record
            VALIDATE CONSTRAINT fk_amusement_maint_vendor;
    END IF;
END $$;
