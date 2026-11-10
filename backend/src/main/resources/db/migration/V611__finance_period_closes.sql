-- 文件版本：V0.01
-- V611（第十八批 Z 段第 188 条 188-R1）：财务封账，按综合体＋月份。
-- 依据：BSM:1803「封账由财务部门执行，条件是账目已算清楚、经确认无误、四流一致。」；
--   PGM:664「跨主体与平台层面的财务工作——三方对账、分账复核、封账、开票、付款执行、守恒校验不通过时的追查——由财务中心负责。」；
--   第十八批裁决 41「财务中心按综合体＋月份在平台办理；只放行该月每天五类数据都已最终、日结都已锁定的月份；四流一致由财务核对后勾选确认留痕；
--   封账不能撤销，不另加拦截」。
-- 一综合体一月一行（period_month 取月首）；记录存在即财务已勾选确认四流一致。不提供撤销，不加禁删禁改触发器。研发阶段无存量（UD-07）。
CREATE TABLE finance_period_closes (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    period_month DATE NOT NULL,
    closed_by VARCHAR(128) NOT NULL,
    closed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_finance_period_closes_project_month UNIQUE (project_id, period_month)
);
COMMENT ON TABLE finance_period_closes IS '财务封账（第十八批裁决 41）：按综合体＋月份，财务核对四流一致后勾选确认';

-- 新权限码：财务域、高风险（同 finance.daily_close.lock，V008、V170）；授给哪个模板由平台管理员配置
-- （PGM:664「平台管理员配置各岗位权限模板」），本迁移不分配。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('finance.period_close.execute', '财务封账（按综合体＋月份）', 'finance', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

-- 封账入口 @GuardedAction(capability = finance.period_close.execute) 的跨端全局显式开启行（照 V607；平台治理地图第 742 行缺行即关）。
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_optin_finance.period_close.execute', '*', 'finance.period_close.execute', 'global', 'global', TRUE,
     '已发布在用能力显式开启（平台治理地图第 742 行：缺行默认关闭，须显式打开）', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;
