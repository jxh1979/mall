-- V448 CG-03 在玩人数的系统自动清零（BSM「出场自动扣减」「在玩人数不允许人工调整」）：
--   「按场清场的场次型项目，散场时由系统按场次结束自动清空；非场次型项目，营业日结束时由系统自动清零在玩人数」
--   「计数只能由入场核销、出场自动扣减以及系统按场次结束的自动清空、按营业日结束的自动清零产生」
-- 本迁移只加系统清零留痕表；清零动作由调度作业（系统主体）执行，不开任何人工入口。
CREATE TABLE IF NOT EXISTS amusement_concurrent_reset_log (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects (id) ON DELETE CASCADE,
    scope VARCHAR(32) NOT NULL,                 -- business_day_end（营业日结束自动清零）| session_end（场次结束自动清空）
    business_date DATE NOT NULL,                -- 所属营业日（取 amusement 域日切配置）
    session_id VARCHAR(64),                     -- session_end：被清空的场次
    cleared_rows INTEGER NOT NULL DEFAULT 0,    -- 清零的快照行数（项目维 / 点位设备维 / 场次维）
    cleared_people INTEGER NOT NULL DEFAULT 0,  -- 清零前的在玩人数合计
    boundary_at TIMESTAMPTZ NOT NULL,           -- 营业日结束时刻 / 场次结束时刻
    reset_at TIMESTAMPTZ NOT NULL,              -- 系统执行时刻
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_amusement_concurrent_reset_log_scope CHECK (scope IN ('business_day_end', 'session_end'))
);
-- 每项目每营业日只清零一次；每场次只清空一次（重复调度不重复记）。
CREATE UNIQUE INDEX IF NOT EXISTS uq_amusement_concurrent_reset_log_day
    ON amusement_concurrent_reset_log (project_id, business_date) WHERE scope = 'business_day_end';
CREATE UNIQUE INDEX IF NOT EXISTS uq_amusement_concurrent_reset_log_session
    ON amusement_concurrent_reset_log (session_id) WHERE scope = 'session_end';
CREATE INDEX IF NOT EXISTS idx_amusement_concurrent_reset_log_project
    ON amusement_concurrent_reset_log (project_id, reset_at DESC);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON amusement_concurrent_reset_log TO complex_app;
    END IF;
END $$;
