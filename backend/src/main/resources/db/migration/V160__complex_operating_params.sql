-- V160 综合体经营参数（T4.2 坪效）：承载「可经营面积」等综合体级主数据——这是未来综合体主数据的第一颗种子。
--   坪效 = 净收入 / operating_area_sqm，由 DashboardAggregationJob 派生 metric revenue.per_sqm；面积未配置则该指标诚实空缺。
--   纯新建表（不撞存量数据）+ 一条经营参数管理权限种子（读复用 analytics.dashboard.view，写需新权限）。

CREATE TABLE IF NOT EXISTS complex_operating_params (
    project_id VARCHAR(64) PRIMARY KEY,         -- 综合体（项目）维度；单综合体默认 project_1，与 ETL segment_id 对齐
    operating_area_sqm NUMERIC(18,2),           -- 可经营面积(㎡)；可空=未配置（坪效该日诚实空缺，不伪造）
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_complex_operating_params_area_pos CHECK (operating_area_sqm IS NULL OR operating_area_sqm > 0),
    CONSTRAINT chk_complex_operating_params_status CHECK (status IN ('active', 'inactive'))
);

-- 经营参数管理权限（高敏：决定坪效口径分母）；查看复用 analytics.dashboard.view，故仅种 manage。仿 V158/V154 范式。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('analytics.operating_params.manage', 'Maintain complex operating parameters (operating area for revenue-per-sqm)', 'analytics', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('analytics.operating_params.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
