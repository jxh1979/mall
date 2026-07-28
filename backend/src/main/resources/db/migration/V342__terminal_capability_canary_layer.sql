-- V342 终端开关「灰度层」（R-10，2026-07-22 用户裁决 · 业务 HTML「终端开关」节已改口径）。
--
-- 裁决两条：
--   1) 多级开关求值改「取最严」：任一层关闭即最终关闭（代码侧改 resolveEffectiveSwitch，无 DDL）。
--   2) 新增灰度层，作为【独立的最细粒度级别】接入求值链（白名单试点）。
--
-- 灰度语义（按「取最严」母口径推导，见 PRD §5.5 重裁）：
--   灰度是【只能收窄、不能放宽】的最细一档 —— 它不能把上层已关的能力重新打开。
--   · 该 (端, 能力) 没有 active 灰度行 → 灰度层弃权，行为与今天完全一致（本迁移零行为变化）。
--   · 有 active 灰度行 → 试点白名单内的对象照常放行；【白名单外一律关闭】。
--   即灰度 = 「在所有上层都开的前提下再收窄到试点对象」，与取最严自洽。
--
-- 两张表：
--   terminal_capability_canary_rollouts  灰度试点（每个 端×能力 一条，active 才参与求值）
--   terminal_capability_canary_targets   试点白名单条目（按既有 scope 维度指定试点对象）
--
-- 权限：复用 terminal.switch.view / terminal.switch.manage（灰度就是终端开关的最细一档，
--       不另立权限点，避免开关权限被切碎后出现「能改灰度但不能改开关」的越权面）。
-- 删除：白名单条目遵守【删除=软删除】铁律 —— archived_at/archive_reason/archived_by 三列，
--       求值与列表只认未存档条目，可恢复、可审计。

CREATE TABLE IF NOT EXISTS terminal_capability_canary_rollouts (
    id              VARCHAR(64)  PRIMARY KEY,
    terminal_type   VARCHAR(64)  NOT NULL,
    capability_code VARCHAR(128) NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT FALSE,
    reason          TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_tccr_terminal_capability UNIQUE (terminal_type, capability_code),
    -- 取值集与 V068 chk_tcs_terminal_type 逐字一致（含历史 'tablet' 与跨端通配 '*'）。
    CONSTRAINT chk_tccr_terminal_type CHECK (terminal_type IN (
        'customer_app', 'customer_miniapp', 'web_admin', 'mobile_admin',
        'staff_tablet', 'staff_work_phone', 'staff_personal_app',
        'merchant_web', 'merchant_app', 'kiosk', 'tablet', '*',
        -- V197 十端权威码（D5）：与 terminal_capability_switches 现存数据一致。
        'app', 'miniapp', 'web', 'staff_private_phone', 'merchant_portal'
    ))
);

CREATE INDEX IF NOT EXISTS idx_tccr_lookup
    ON terminal_capability_canary_rollouts (capability_code, terminal_type, active);

CREATE TABLE IF NOT EXISTS terminal_capability_canary_targets (
    id             VARCHAR(64)  PRIMARY KEY,
    rollout_id     VARCHAR(64)  NOT NULL
        REFERENCES terminal_capability_canary_rollouts (id) ON DELETE CASCADE,
    target_type    VARCHAR(64)  NOT NULL,
    target_id      VARCHAR(64)  NOT NULL,
    reason         TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    archived_at    TIMESTAMPTZ,
    archive_reason TEXT,
    archived_by    VARCHAR(64),
    CONSTRAINT uq_tcct_target UNIQUE (rollout_id, target_type, target_id),
    -- 试点对象只能落在既有 scope 维度上；不含 'global'（把 global 列入白名单等于全量放开，
    -- 灰度就失去意义），也不含 'canary'（灰度层自身不可再嵌套灰度）。
    CONSTRAINT chk_tcct_target_type CHECK (target_type IN (
        'project', 'merchant', 'business_system', 'module', 'version', 'device'
    ))
);

CREATE INDEX IF NOT EXISTS idx_tcct_rollout
    ON terminal_capability_canary_targets (rollout_id, archived_at);

-- 新表一律 GRANT 给受限运行时角色 complex_app（V296/V298/V338 教训：漏授权 = 生产静默 permission denied）。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON terminal_capability_canary_rollouts TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON terminal_capability_canary_targets TO complex_app';
    ELSE
        RAISE NOTICE 'skip GRANT to complex_app (restricted runtime role absent in this environment)';
    END IF;
END $$;

-- 不 seed 任何 rollout：无 active 灰度行 = 灰度层弃权 = 与本迁移前行为完全一致（上线零行为变化）。
