-- V083 游乐三维度容量：点位/设备维度配置表 + 三维度容量快照表（设计 §6.4 / §7.14b / §7.14c）。
-- 背景：V014 仅有 amusement_sessions.capacity/used_capacity 单维度容量（原子扣减为真），
-- 缺点位/设备维度配置（capacity_config）与各维度容量快照（capacity_snapshot）。
-- 本迁移补两表，核销时按「项目安全上限 / 场次 / 点位设备」三维度取最小可放行余量判定；
-- 仅校验已配置的受控维度，未配置维度跳过（向后兼容：无 capacity_config 时退化为场次单维）。

-- §7.14b 点位/设备并发容量配置（容量三维度之第三维）。
-- scope_ref 物化为 location_point_id / device_id 两列（与现有 amusement 表 device_id 口径一致）；
-- 至少配置其一。同一 (project_id, location_point_id, device_id, effective_session_pattern) 唯一。
CREATE TABLE IF NOT EXISTS amusement_capacity_config (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects(id) ON DELETE CASCADE,
    location_point_id VARCHAR(128),
    device_id VARCHAR(128),
    max_concurrent INTEGER NOT NULL,
    effective_session_pattern VARCHAR(64) NOT NULL DEFAULT '',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_amusement_capacity_config_max CHECK (max_concurrent >= 0),
    CONSTRAINT chk_amusement_capacity_config_scope
        CHECK (location_point_id IS NOT NULL OR device_id IS NOT NULL),
    CONSTRAINT uq_amusement_capacity_config_key
        UNIQUE (project_id, location_point_id, device_id, effective_session_pattern)
);

CREATE INDEX IF NOT EXISTS idx_amusement_capacity_config_project
    ON amusement_capacity_config (project_id, enabled);

-- §7.14c 三维度容量快照（物化各维度实时在场人数）。
-- dimension：project（汇总/安全上限告警，旁路）、session（场次维，沿用 amusement_sessions.used_capacity，本表仅冗余可空）、
-- point_device（点位/设备维，现场放行受控维度）。
-- 维度键以可空列承载：session_id / location_point_id / device_id；COALESCE 空串入唯一键，保证 upsert 命中。
CREATE TABLE IF NOT EXISTS amusement_capacity_snapshot (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects(id) ON DELETE CASCADE,
    session_id VARCHAR(64) REFERENCES amusement_sessions(id) ON DELETE CASCADE,
    location_point_id VARCHAR(128),
    device_id VARCHAR(128),
    dimension VARCHAR(16) NOT NULL,
    capacity_limit INTEGER NOT NULL,
    occupied_count INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_amusement_capacity_snapshot_dimension
        CHECK (dimension IN ('project', 'session', 'point_device')),
    CONSTRAINT chk_amusement_capacity_snapshot_counts
        CHECK (capacity_limit >= 0 AND occupied_count >= 0)
);

-- 维度键唯一：以 COALESCE 把可空列收敛为空串参与唯一约束（点位维 session_id 空、场次维 point/device 空）。
CREATE UNIQUE INDEX IF NOT EXISTS uk_amusement_capacity_snapshot_key
    ON amusement_capacity_snapshot (
        project_id,
        dimension,
        COALESCE(session_id, ''),
        COALESCE(location_point_id, ''),
        COALESCE(device_id, '')
    );

CREATE INDEX IF NOT EXISTS idx_amusement_capacity_snapshot_session
    ON amusement_capacity_snapshot (session_id);
