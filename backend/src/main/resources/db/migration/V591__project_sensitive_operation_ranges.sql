-- 文件版本：V0.01
-- 第 123 条 123-R1 各项目敏感操作授权范围（按项目在后台配置）。依据：BSM:1630「核销、收款、退款、放行等敏感操作，操作设备定位落在该项目综合体
-- 授权范围之外时一律拒绝并留痕……授权范围按项目在后台配置（业务参数，按项目可配，须同时配置上下限）。」；决策台账 DEC-R8-97-23。
-- 范围＝圆心（GPS 原始坐标 WGS-84，十进制度）＋授权半径（米），半径须落在同时配置的上下限内。HTML 未给出厂值：不插出厂回落行，
-- 半径与上下限首次配置就必须同时填写（同 V499、V497 范式，不自拟上下限出厂数值）；项目没有行＝未配置，配好之前不按位置拦截（第十八批裁决 32）。
CREATE TABLE project_sensitive_operation_ranges (
    project_id VARCHAR(64) PRIMARY KEY REFERENCES projects(id),
    center_latitude NUMERIC(10,7) NOT NULL,
    center_longitude NUMERIC(10,7) NOT NULL,
    radius_meters INTEGER NOT NULL,
    min_radius_meters INTEGER NOT NULL,
    max_radius_meters INTEGER NOT NULL,
    updated_by VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_project_sensitive_operation_ranges_latitude CHECK (center_latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_project_sensitive_operation_ranges_longitude CHECK (center_longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_project_sensitive_operation_ranges_radius CHECK (
        min_radius_meters > 0 AND min_radius_meters <= radius_meters AND radius_meters <= max_radius_meters)
);

-- 配置权限挂平台管理员（服务层另按项目范围校验：项目账号只能改本权限可触及的项目）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('platform.sensitive_operation_range.manage', '配置敏感操作授权范围（按项目）', 'platform', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'platform.sensitive_operation_range.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
