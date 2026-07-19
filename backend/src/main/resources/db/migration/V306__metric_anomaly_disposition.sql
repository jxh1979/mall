-- 建设V(BI 二期遗留):指标异常处置流转。
-- 现状:异常已落表 analytics_metric_anomalies(稳定键 ano_<metricKey>_<经营日>,插入 ON CONFLICT DO NOTHING),
-- 检测重跑不覆盖既有行。因此处置状态直接叠加在检测行上,不另建处置表——异常键即行主键,重检测天然保留处置结果。

-- 1. 处置生命周期字段(处置人/说明/时间/升级出的告警引用)。
ALTER TABLE analytics_metric_anomalies
    ADD COLUMN IF NOT EXISTS disposition_note TEXT,
    ADD COLUMN IF NOT EXISTS disposed_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS disposed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS escalated_alert_id VARCHAR(64);

-- 2. 旧状态 'ignored'(V017 遗留枚举,当前无写入路径)语义并入 'false_positive' 误报,便于统一状态机。
UPDATE analytics_metric_anomalies SET status = 'false_positive' WHERE status = 'ignored';

-- 3. 状态机改版:open(未处置)/acknowledged(已阅)/resolved(已处置)/false_positive(误报)/escalated(已转告警)。
ALTER TABLE analytics_metric_anomalies DROP CONSTRAINT IF EXISTS chk_analytics_metric_anomalies_status;
ALTER TABLE analytics_metric_anomalies ADD CONSTRAINT chk_analytics_metric_anomalies_status
    CHECK (status IN ('open', 'acknowledged', 'resolved', 'false_positive', 'escalated'));

-- 4. 处置权限(高危,但不需审批——运营处置为日常动作,审批会阻断闭环)。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('analytics.metric_anomaly.manage', '处置指标异常', 'analytics', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

-- 5. 授予现有具备风险告警处置权的角色(与 analytics.risk_alert.manage 同一批人)。
INSERT INTO role_permissions (role_id, permission_code)
SELECT DISTINCT role_id, 'analytics.metric_anomaly.manage'
  FROM role_permissions
 WHERE permission_code = 'analytics.risk_alert.manage'
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- 6. 生产受限运行时权限(铁律:新列一律再 GRANT complex_app 兜底,防列级权限漂移)。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON analytics_metric_anomalies TO complex_app';
    END IF;
END $$;
