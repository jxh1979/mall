-- 文件版本：V0.01
-- V579：第十八批 K 段第 95 条（95-R1 开台客人来源、95-R2 来源与销售人员的事后改派）。
-- 依据：业务地图第 1021 行「开台时系统自动判定并记录客人来源：……经平台自有渠道订台或购票到店的，自动记平台自有渠道；由有销售归属的订台开台的，
--   自动记营销带客并带出该销售人员；以上都不命中的直接到店开台默认记自来客——此时现场可人工补选营销带客并选定销售人员，不补选即维持自来客。……
--   自来客由该来源记录定义，销售提成判定引用此记录。来源与销售人员的事后改派须店长复核并留痕，只能在该账单结清前办理。」
--   第十八批 K 段裁决 21（开台记来源只看有没有订台、订台从哪里来；开台时不对客人的门票）、裁决 22（四类来源与判定规则固定；
--   按项目可配的只是回流渠道下接了哪些平台，随第三方团购接入即清单第 47 条一起做，本段不做配置）。
-- 铁律 8：不为存量数据回填；新列默认自来客。

-- 1) 开台客人来源（回流渠道随清单第 47 条第三方团购接入再加）。
ALTER TABLE bar_table_sessions ADD COLUMN IF NOT EXISTS guest_source VARCHAR(32) NOT NULL DEFAULT 'walk_in';
ALTER TABLE bar_table_sessions DROP CONSTRAINT IF EXISTS chk_bar_table_sessions_guest_source;
ALTER TABLE bar_table_sessions ADD CONSTRAINT chk_bar_table_sessions_guest_source
    CHECK (guest_source IN ('platform_own', 'marketing', 'walk_in'));
COMMENT ON COLUMN bar_table_sessions.guest_source IS '客人来源：platform_own 平台自有渠道（客人在 App／小程序自助订台后开台）、marketing 营销带客（有销售归属，与 sales_staff_id 成对）、walk_in 自来客（直接到店未补选销售）；回流渠道待第三方团购接入（清单第 47 条）';

-- 2) 店长改派开台客人来源与销售人员（范式同 V559；由运营分配给店长角色）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('bar.table_session.attribution_reassign', '店长改派开台客人来源与销售人员', 'bar', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'bar.table_session.attribution_reassign')
ON CONFLICT (role_id, permission_code) DO NOTHING;
