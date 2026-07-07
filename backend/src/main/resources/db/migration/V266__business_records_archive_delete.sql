-- V264 数据存档与审批后删除范式铺到 4 个业务记录实体（数据删除 Tier-B，照 V259 商户 / V260 票种 / V262 酒吧目录范式）。
-- 用户拍板(2026-07-06)："所有数据都应有存档或删除的能力,可要求审批,不能录入后就不可变。"
-- 本迁移覆盖：
--   1) merchant_service_work_orders（商户服务工单）
--   2) bar_reservations（卡座预订）
--   3) staff_profiles（员工档案）
--   4) facility_equipment（设施设备）
--
-- 存档轴：archived_at 非空=已存档；列表默认 WHERE archived_at IS NULL 过滤（软下线可恢复，照 V255 先例）。
-- 删除轴：delete_status none->pending（申请）->approved（执行前瞬时态，随即物理 DELETE）；
--   申请须先存档、审批禁自审、无活引用方可删。
-- 全列可空/带默认，向后兼容，可独立回滚。
-- 设计详见 docs/data-archive-delete-pattern-design-v0.1.md。

-- ============ 1) merchant_service_work_orders（商户服务工单） ============
ALTER TABLE merchant_service_work_orders
    ADD COLUMN IF NOT EXISTS archived_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS archived_by         VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_status       VARCHAR(16) NOT NULL DEFAULT 'none',
    ADD COLUMN IF NOT EXISTS delete_requested_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delete_reason       TEXT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_mswo_delete_status') THEN
        ALTER TABLE merchant_service_work_orders
            ADD CONSTRAINT chk_mswo_delete_status
            CHECK (delete_status IN ('none','pending','approved'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_merchant_service_work_orders_active_list
    ON merchant_service_work_orders (created_at DESC, id)
 WHERE archived_at IS NULL AND delete_status <> 'approved';

-- ============ 2) bar_reservations（卡座预订） ============
ALTER TABLE bar_reservations
    ADD COLUMN IF NOT EXISTS archived_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS archived_by         VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_status       VARCHAR(16) NOT NULL DEFAULT 'none',
    ADD COLUMN IF NOT EXISTS delete_requested_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delete_reason       TEXT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_bar_reservation_delete_status') THEN
        ALTER TABLE bar_reservations
            ADD CONSTRAINT chk_bar_reservation_delete_status
            CHECK (delete_status IN ('none','pending','approved'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_bar_reservations_active_list
    ON bar_reservations (created_at DESC, id)
 WHERE archived_at IS NULL AND delete_status <> 'approved';

-- ============ 3) staff_profiles（员工档案） ============
ALTER TABLE staff_profiles
    ADD COLUMN IF NOT EXISTS archived_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS archived_by         VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_status       VARCHAR(16) NOT NULL DEFAULT 'none',
    ADD COLUMN IF NOT EXISTS delete_requested_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delete_reason       TEXT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_staff_profile_delete_status') THEN
        ALTER TABLE staff_profiles
            ADD CONSTRAINT chk_staff_profile_delete_status
            CHECK (delete_status IN ('none','pending','approved'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_staff_profiles_active_list
    ON staff_profiles (created_at DESC, id)
 WHERE archived_at IS NULL AND delete_status <> 'approved';

-- ============ 4) facility_equipment（设施设备） ============
ALTER TABLE facility_equipment
    ADD COLUMN IF NOT EXISTS archived_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS archived_by         VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_status       VARCHAR(16) NOT NULL DEFAULT 'none',
    ADD COLUMN IF NOT EXISTS delete_requested_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delete_reason       TEXT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_facility_equipment_delete_status') THEN
        ALTER TABLE facility_equipment
            ADD CONSTRAINT chk_facility_equipment_delete_status
            CHECK (delete_status IN ('none','pending','approved'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_facility_equipment_active_list
    ON facility_equipment (created_at DESC, id)
 WHERE archived_at IS NULL AND delete_status <> 'approved';

-- ============ 审批权限码（4 个）============
-- 与既有 *.manage 职责分离：发起删除权≠审批删除权（照 supervisor_review 原则）。critical + requires_approval。
-- domain 沿用各实体既有权限所在域：merchant_service / bar / access / platform。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('merchant.service_work_order.approve', '审批商户服务工单删除', 'merchant_service', 'critical', TRUE, 'active'),
    ('bar.reservation.approve', '审批卡座预订删除', 'bar', 'critical', TRUE, 'active'),
    ('staff.profile.approve', '审批员工档案删除', 'access', 'critical', TRUE, 'active'),
    ('facility.equipment.approve', '审批设施设备删除', 'platform', 'critical', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

-- V261 已把「当时」全部 active 权限授 role_platform_admin，但本迁移新插的 4 个权限晚于 V261，
-- 不在其覆盖范围内，须按 V261 确立的基线自行补授（新权限迁移必须同步授 role_platform_admin）。
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN (
        'merchant.service_work_order.approve',
        'bar.reservation.approve',
        'staff.profile.approve',
        'facility.equipment.approve'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
