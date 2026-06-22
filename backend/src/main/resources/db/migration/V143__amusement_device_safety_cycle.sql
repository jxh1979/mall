-- V143 游乐设备安全检查周期子系统（B0 · F-OPS-04 + F-AMUSE-INSPECT-CYCLE，裁决 #6）。
-- 背景与断链：
--   ① 巡检（amusement_safety_inspections）是项目级（仅 project_id），核销准入 evaluateOpenEligibility
--      取"历史最近一次 PASS"且无任何有效期判定 → 项目一辈子 PASS 过一次即永久放行（人身安全 fail-open）。
--   ② 开发设计 §6.6.1「系统按项目、设备、点位和检修周期生成定期检修任务」完全未实现：
--      无设备主数据、无"每设备检查周期"配置、无到期任务调度。
--   ③ 系统无设备主数据表，device_id 散落 6 表为无约束自由字符串，三套设备标识（终端/MDM/游乐）互不关联。
-- 本迁移建立"设备主数据 + 每设备检查周期 + 巡检设备级化 + 到期任务工单"数据层（裁决 #6：本期做设备级完整周期模型）。
-- 口径与兼容：
--   - 存量 6 表的 device_id 维持软关联（字符串 device_code 语义），本期不强迁 FK，避免破坏存量与测试。
--   - 新链路（巡检有效期/任务工单）用 amusement_devices.id 强引用；与旧 device_id 字符串经 device_code 桥接。
--   - 全部 CREATE TABLE/ADD COLUMN IF NOT EXISTS，存量 NULL 向后兼容。

-- 1) 设备主数据表（承载 device_code、状态、每设备安全检查周期、最近 PASS 巡检时间）。
CREATE TABLE IF NOT EXISTS amusement_devices (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects(id) ON DELETE CASCADE,
    device_code VARCHAR(128) NOT NULL,            -- 桥接旧 device_id 字符串
    name VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    safety_check_interval_hours INTEGER,          -- 每设备检查周期（空=回退项目级缺省）
    location_point_id VARCHAR(64),
    last_pass_inspection_at TIMESTAMPTZ,          -- 最近一次 PASS 巡检时间（冗余，加速核销校验与到期计算）
    merchant_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_amusement_devices_status
        CHECK (status IN ('active', 'inactive', 'maintenance', 'retired')),
    CONSTRAINT chk_amusement_devices_interval
        CHECK (safety_check_interval_hours IS NULL OR safety_check_interval_hours > 0),
    CONSTRAINT uq_amusement_devices_code UNIQUE (project_id, device_code)
);
CREATE INDEX IF NOT EXISTS idx_amusement_devices_project
    ON amusement_devices (project_id, status);

-- 2) 项目级缺省检查周期（设备未单独配置时回退）。缺省 24 小时（保守值，可后台按项目/设备覆盖）。
ALTER TABLE amusement_projects
    ADD COLUMN IF NOT EXISTS safety_check_interval_hours INTEGER NOT NULL DEFAULT 24;
ALTER TABLE amusement_projects
    DROP CONSTRAINT IF EXISTS chk_amusement_projects_safety_interval;
ALTER TABLE amusement_projects
    ADD CONSTRAINT chk_amusement_projects_safety_interval
        CHECK (safety_check_interval_hours > 0);

-- 3) 巡检记录设备级化：device_id = 设备业务码 device_code 字符串（与核销 deviceId / amusement_capacity_config.device_id 同口径），可空=项目级巡检。
ALTER TABLE amusement_safety_inspections
    ADD COLUMN IF NOT EXISTS device_id VARCHAR(128);
CREATE INDEX IF NOT EXISTS idx_amusement_safety_inspections_device
    ON amusement_safety_inspections (project_id, device_id, inspected_at DESC);

-- 4) 到期检查任务工单表（§6.6.1：按周期预生成；由 AmusementInspectionTaskJob 薄 @Scheduled 包装生成，默认关）。
CREATE TABLE IF NOT EXISTS amusement_inspection_tasks (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects(id) ON DELETE CASCADE,
    device_id VARCHAR(64) REFERENCES amusement_devices(id) ON DELETE CASCADE,  -- 空=项目级任务
    due_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    generated_at TIMESTAMPTZ NOT NULL,
    completed_inspection_id VARCHAR(64),
    completed_at TIMESTAMPTZ,
    merchant_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_amusement_inspection_tasks_status
        CHECK (status IN ('pending', 'completed', 'overdue', 'cancelled'))
);
-- 防重：同设备（device_id 空时按项目）同一到期点只生成一张待办，COALESCE 收敛空设备维度。
CREATE UNIQUE INDEX IF NOT EXISTS uk_amusement_inspection_tasks_due
    ON amusement_inspection_tasks (project_id, COALESCE(device_id, ''), due_at);
CREATE INDEX IF NOT EXISTS idx_amusement_inspection_tasks_status
    ON amusement_inspection_tasks (status, due_at);
