-- V111 terminal_capability_switches.terminal_type 收口对齐《统一口径基准》D5 / PRD §6.3 「10 端权威码」。
-- 背景（P1 #17 证据②）：V068 的 chk_tcs_terminal_type 取值集与 D5 权威码命名漂移：
--   customer_app / customer_miniapp / web_admin / staff_personal_app / merchant_web 为非权威别名，
--   且缺 D5 的 staff_private_phone / merchant_portal，CHECK 内多了现状别名 'tablet'（D5 权威码恰含 tablet，保留）。
-- D5 10 端唯一权威码（PRD §6.3 表体）：
--   app / miniapp / web / tablet / staff_work_phone / staff_private_phone / merchant_portal / merchant_app / kiosk / mobile_admin
--   （T2 终端开关额外保留 '*' 跨端通配，非 D5 终端，仅作开关作用域语义）。
--
-- 收口策略（先 UPDATE 既有数据、再换 CHECK，避免在历史漂移行上 ADD CONSTRAINT 失败）：
--   1) 把 terminal_capability_switches 既有行的漂移别名 UPDATE 为 D5 权威码（幂等：仅命中漂移值）。
--   2) DROP 旧 chk_tcs_terminal_type，ADD 新 CHECK = D5 10 端权威码 ∪ {'*'}。
-- 影响面：现状全部 seed（V010/V071/V088/V093/V096/V097/V098）terminal_type 均为 '*'，UPDATE 不命中；
--   仅收敛运行时经 API（CapabilitySwitchRequest）可能写入的漂移别名。scope_type CHECK（chk_tcs_scope_type）不动。
--   设备表 terminal_devices 无 terminal_type CHECK，且其取值（staff_work_phone/tablet/kiosk）已是 D5 权威码，不在本迁移范围。

-- ── 1) 漂移别名 → D5 权威码（UPDATE 先行） ──────────────────────────────────────
UPDATE terminal_capability_switches SET terminal_type = 'app'                WHERE terminal_type = 'customer_app';
UPDATE terminal_capability_switches SET terminal_type = 'miniapp'            WHERE terminal_type = 'customer_miniapp';
UPDATE terminal_capability_switches SET terminal_type = 'web'                WHERE terminal_type = 'web_admin';
UPDATE terminal_capability_switches SET terminal_type = 'staff_private_phone' WHERE terminal_type = 'staff_personal_app';
UPDATE terminal_capability_switches SET terminal_type = 'merchant_portal'    WHERE terminal_type = 'merchant_web';
-- 注：'staff_tablet' 旧 CHECK 曾允许；D5 权威码为 'tablet'，若历史有 'staff_tablet' 漂移行一并收敛。
UPDATE terminal_capability_switches SET terminal_type = 'tablet'             WHERE terminal_type = 'staff_tablet';

-- ── 2) 换 CHECK：D5 10 端权威码 ∪ {'*'}（DROP 旧、ADD 新；幂等 DO 块） ──────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_tcs_terminal_type') THEN
        ALTER TABLE terminal_capability_switches DROP CONSTRAINT chk_tcs_terminal_type;
    END IF;

    ALTER TABLE terminal_capability_switches
        ADD CONSTRAINT chk_tcs_terminal_type CHECK (terminal_type IN (
            'app', 'miniapp', 'web', 'tablet',
            'staff_work_phone', 'staff_private_phone',
            'merchant_portal', 'merchant_app',
            'kiosk', 'mobile_admin', '*'
        ));
END $$;
