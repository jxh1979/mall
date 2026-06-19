-- P0 清单#6 游乐超容落事件（文档 amusement §6.4 / §7.18 + R6-D10）。
-- 背景：amusement_incidents（V024）无 incident_type 列（CHECK 仅 severity/status）；
-- 核销超容仅抛 SESSION_CAPACITY_EXHAUSTED 拒绝放行，不落 incident、不发 over_capacity 事件。
-- 本迁移：
--   1) 给 amusement_incidents 补 incident_type 列（枚举含 accident/fault/complaint/risk/over_capacity）+ CHECK；
--      存量行回填 'accident'（既有人工上报事件默认归为安全事故类，与原语义一致）。
--   2) 补「超容处置策略」最小配置表 amusement_over_capacity_policy：
--      复用既有容量口径（V083 安全容量=硬上限），新增 operating_capacity（运营容量=软上限）
--      与 disposal_mode（处置模式后台可切换：block_hard 默认硬阻断+软告警 / warn_only 仅告警+留痕不阻断）。
--      未配置该表的项目退化为「仅硬上限阻断、无软上限告警」，与既有行为一致（向后兼容）。

ALTER TABLE amusement_incidents
    ADD COLUMN IF NOT EXISTS incident_type VARCHAR(32) NOT NULL DEFAULT 'accident';

-- 存量与默认归为 accident；新枚举含 over_capacity（超容事件）。
ALTER TABLE amusement_incidents
    DROP CONSTRAINT IF EXISTS chk_amusement_incidents_type;
ALTER TABLE amusement_incidents
    ADD CONSTRAINT chk_amusement_incidents_type
        CHECK (incident_type IN ('accident', 'fault', 'complaint', 'risk', 'over_capacity'));

CREATE INDEX IF NOT EXISTS idx_amusement_incidents_type
    ON amusement_incidents (project_id, incident_type, occurred_at DESC);

-- 超容处置策略（项目维一行）。operating_capacity 软上限：核销在场达到该值即「超容」；
-- disposal_mode 决定超容后的处置：block_hard=硬阻断（达安全容量阻断 + 达运营容量告警放行，均落 incident+事件）；
-- warn_only=仅告警留痕不阻断（达任一上限均不阻断，但必落 incident+事件）。
CREATE TABLE IF NOT EXISTS amusement_over_capacity_policy (
    project_id VARCHAR(64) PRIMARY KEY REFERENCES amusement_projects(id) ON DELETE CASCADE,
    operating_capacity INTEGER NOT NULL DEFAULT 0,
    disposal_mode VARCHAR(16) NOT NULL DEFAULT 'block_hard',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_amusement_over_capacity_policy_operating CHECK (operating_capacity >= 0),
    CONSTRAINT chk_amusement_over_capacity_policy_mode
        CHECK (disposal_mode IN ('block_hard', 'warn_only'))
);
