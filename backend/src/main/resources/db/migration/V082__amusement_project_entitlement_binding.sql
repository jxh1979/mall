-- V082 第二阶段（游乐现场核销 P0 断点修复）：项目↔权益类型绑定配置表。
-- 背景：AmusementService.createVerification 此前不校验/不消费票务权益，仅扣场次容量。
-- 本表配置「哪些票务权益类型可用于该游乐项目」，核销时据此判定「绑定本项目」（设计 §6.4 步骤 2/4、§12.1 权益适用项目）。
-- 回退口径（向后兼容）：若某项目在本表无任何绑定配置，则核销退化为「仅校验 entitlement_type 为游乐类」放行（见 AmusementService），
-- 避免误伤既有测试与尚未配运营绑定的项目。TODO：运营为各游乐项目配置可用权益类型绑定后，回退分支自动收紧。
CREATE TABLE IF NOT EXISTS amusement_project_entitlements (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects(id) ON DELETE CASCADE,
    entitlement_type VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_amusement_project_entitlements UNIQUE (project_id, entitlement_type)
);

CREATE INDEX IF NOT EXISTS idx_amusement_project_entitlements_project
    ON amusement_project_entitlements (project_id);
