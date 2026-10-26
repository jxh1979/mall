-- 文件版本：V0.01
-- V548（第十六批 G3：待开发清单第 26、27、28 条）：分红周期补半年与年度、默认半年；存酒到期提醒节点按项目可配；
-- 存酒人工例外取酒消费历史条件落出厂行。不回填存量数据。

-- ① 分红周期。依据 BSM L1777「分红周期一律取月及月的整数倍粒度（如月、季、半年、年），不使用周粒度；分红按所配周期一期一分配，
--    默认周期为半年」。取值补 semiannual（半年）、annual（年度）；新建方案不填周期时为半年。已有方案的 quarterly / monthly 照旧有效。
ALTER TABLE dividend_plans DROP CONSTRAINT chk_dp_period_type;
ALTER TABLE dividend_plans
    ADD CONSTRAINT chk_dp_period_type CHECK (period_type IN ('monthly', 'quarterly', 'semiannual', 'annual'));
ALTER TABLE dividend_plans ALTER COLUMN period_type SET DEFAULT 'semiannual';

-- ② 存酒到期提醒节点。依据 BSM L1072「到期前按后台配置的提醒节点各发一次短信提醒（业务参数，按项目可配，出厂默认到期前第 14 天、
--    第 7 天、第 3 天、第 1 天）；提醒节点与存酒有效期同一粒度配置，超出本次存酒实际有效期的节点自动跳过」。
--    scope_id = global（出厂行）| project:<项目 id>，缺行回落 global；reminder_days 为「到期前第 N 天」的逗号清单（按天，同存酒有效期粒度）。
--    原单一提醒窗 notification_windows(inventory.saved_bottle) 由本表取代，删除该行，后台不再留不生效的参数。
CREATE TABLE IF NOT EXISTS saved_bottle_reminder_policy (
    scope_id VARCHAR(128) PRIMARY KEY,
    reminder_days VARCHAR(200) NOT NULL,
    updated_by VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_saved_bottle_reminder_policy_scope CHECK (scope_id = 'global' OR scope_id LIKE 'project:_%'),
    CONSTRAINT chk_saved_bottle_reminder_policy_days CHECK (reminder_days ~ '^[1-9][0-9]*(,[1-9][0-9]*)*$')
);
INSERT INTO saved_bottle_reminder_policy (scope_id, reminder_days)
VALUES ('global', '14,7,3,1')
ON CONFLICT (scope_id) DO NOTHING;
DELETE FROM notification_windows WHERE window_key = 'inventory.saved_bottle';

-- 配置入口 PUT /saved-bottles/reminder-policy（范式同 V513 bar.saved_bottle.renewal_policy.manage）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('bar.saved_bottle.reminder_policy.manage', '配置存酒到期提醒节点（按项目）', 'bar', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'bar.saved_bottle.reminder_policy.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- ③ 人工例外取酒消费历史条件出厂行。依据 BSM L1073 ②「出厂默认：近 1 个月内至少 2 笔、近 3 个月内至少 3 笔、或历史累计至少 5 笔
--    有效卡座消费订单……须同时配置上下限」。范式同 V497 / V543：落 global 出厂行，上下限留空，首次改参时与条件值同填
--    （配置入口已要求五项同填）；未单独配置的项目回落出厂行。
ALTER TABLE saved_bottle_manual_pickup_policy
    ALTER COLUMN min_orders DROP NOT NULL,
    ALTER COLUMN max_orders DROP NOT NULL,
    DROP CONSTRAINT chk_saved_bottle_manual_pickup_policy_bounds;
ALTER TABLE saved_bottle_manual_pickup_policy
    ADD CONSTRAINT chk_saved_bottle_manual_pickup_policy_bounds CHECK (
        (min_orders IS NULL AND max_orders IS NULL
            AND orders_within_1m >= 1 AND orders_within_3m >= 1 AND orders_total >= 1)
        OR (min_orders >= 1 AND min_orders <= max_orders
            AND orders_within_1m BETWEEN min_orders AND max_orders
            AND orders_within_3m BETWEEN min_orders AND max_orders
            AND orders_total BETWEEN min_orders AND max_orders));
INSERT INTO saved_bottle_manual_pickup_policy (project_id, orders_within_1m, orders_within_3m, orders_total)
VALUES ('global', 2, 3, 5)
ON CONFLICT (project_id) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON saved_bottle_reminder_policy TO complex_app';
    END IF;
END $$;
