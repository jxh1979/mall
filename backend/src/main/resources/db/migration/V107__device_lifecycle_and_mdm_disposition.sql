-- P0 清单#9：设备生命周期状态机 + MDM 门控接线 + 违规处置闭环。
-- 背景（与 android §12.3 设备生命周期 / mdm 计划一致）：
--   * V007 terminal_devices.status 无 CHECK；代码状态机仅 bound/returned/unbound，
--     缺维修(送修)/挂失/找回/报废/封禁 生命周期态。
--   * V088 mdm_policy_violations.handled 无端点可置真（纯外部 POST 上报、无服务端违规检测、无处置闭环）。
-- 本迁移：
--   1) terminal_devices.status 补 CHECK 枚举 + 生命周期态（沿用既有 bound/returned/unbound 命名基础，
--      增 repair/lost/found/scrapped/blocked）。
--   2) management_status 补 lost/retired/blocked（增量于 V088 已有 unmanaged/enrolled/compliant/violation）。
--   3) mdm_policy_violations 补处置字段 blocked_at / disposition（CHECK 枚举）。
--   4) seed 设备生命周期/违规处置相关权限点。
--
-- 命名映射说明（android 文档 §12.3 vs 既有代码命名，以既有代码命名为准）：
--   文档 in_use     → 代码 bound      （已绑定在用）
--   文档 checked_out→ 代码 returned   （归还/退库；归还后未解绑）
--   文档 retired    → 代码 scrapped   （设备生命周期报废；management_status 另有 retired 表示"纳管退役"，语义不同，见下）
--   新增 repair / lost / found / blocked 为既有代码命名基础上的增量生命周期态。

-- ── 1) terminal_devices.status CHECK 枚举（含新增生命周期态） ──────────────────
-- 既有数据仅含 bound/returned/unbound/disabled（测试出现 disabled），全部纳入白名单避免回填失败。
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_terminal_devices_status'
    ) THEN
        ALTER TABLE terminal_devices
            ADD CONSTRAINT chk_terminal_devices_status CHECK (
                status IN (
                    'bound',      -- 已绑定在用（文档 in_use）
                    'returned',   -- 已归还/退库（文档 checked_out）
                    'unbound',    -- 已解绑
                    'disabled',   -- 已停用（既有兼容值）
                    'repair',     -- 送修中
                    'lost',       -- 挂失
                    'found',      -- 找回（挂失后恢复的中间态，可再绑定）
                    'scrapped',   -- 报废（终态，文档 retired）
                    'blocked'     -- 封禁（终态，违规处置联动）
                )
            );
    END IF;
END $$;

-- ── 2) management_status 补 lost/retired/blocked（增量重建 CHECK） ─────────────
-- V088 原 CHECK: unmanaged/enrolled/compliant/violation；此处补 lost(设备挂失)/retired(纳管退役)/blocked(封禁)。
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_terminal_devices_management_status'
    ) THEN
        ALTER TABLE terminal_devices DROP CONSTRAINT chk_terminal_devices_management_status;
    END IF;
    ALTER TABLE terminal_devices
        ADD CONSTRAINT chk_terminal_devices_management_status CHECK (
            management_status IN (
                'unmanaged', 'enrolled', 'compliant', 'violation',
                'lost', 'retired', 'blocked'
            )
        );
END $$;

-- ── 3) mdm_policy_violations 处置字段 ────────────────────────────────────────
ALTER TABLE mdm_policy_violations
    ADD COLUMN IF NOT EXISTS blocked_at  TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS disposition VARCHAR(32);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_mdm_policy_violations_disposition'
    ) THEN
        ALTER TABLE mdm_policy_violations
            ADD CONSTRAINT chk_mdm_policy_violations_disposition CHECK (
                disposition IS NULL OR disposition IN (
                    'acknowledged',    -- 仅确认/记录，不联动封禁
                    'device_blocked',  -- 处置=封禁设备（联动 management_status=blocked + 撤销凭证）
                    'dismissed'        -- 误报驳回
                )
            );
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_mdm_policy_violations_disposition
    ON mdm_policy_violations (disposition, occurred_at DESC);

-- ── 4) 权限 seed（设备生命周期管理 + 违规处置） ───────────────────────────────
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('terminal.device.lifecycle', 'Manage terminal device lifecycle (repair/lost/found/scrap)', 'terminal', 'high', TRUE, 'active'),
    ('mdm.violation.handle',      'Handle/dispose MDM device policy violations',                'terminal', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('terminal.device.lifecycle', 'mdm.violation.handle')
ON CONFLICT (role_id, permission_code) DO NOTHING;
