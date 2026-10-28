-- 文件版本：V0.01
-- 第十八批 C 段第 62 条 商户处罚记录与单次扣分上下限。
-- BSM 第 1838 行「商户违反运营规则的，由城市项目公司的运营人员填写处罚记录，处罚即扣减健康分，扣分规则由后台配置（业务参数，按项目可配，
-- 须同时配置上下限）……具体计分与优惠规则待统一的商场运营规则出台后另定」；第十八批裁决 8「只配单次扣分上下限（按项目可配），
-- 运营登记处罚时自己填扣分」。每条处罚记录的扣分即对健康分的一次扣减；总分、计分周期与优惠映射另定，本迁移不做。
-- HTML 未给出厂值：不预置出厂行，项目配置上下限前不能登记处罚（409）。下限至少 1（处罚即扣减）。

CREATE TABLE IF NOT EXISTS merchant_penalty_point_bounds (
    project_id VARCHAR(64) PRIMARY KEY REFERENCES projects(id),
    min_penalty_points INTEGER NOT NULL,
    max_penalty_points INTEGER NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_merchant_penalty_point_bounds CHECK (min_penalty_points >= 1 AND min_penalty_points <= max_penalty_points)
);

CREATE TABLE IF NOT EXISTS merchant_penalty_records (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    merchant_id VARCHAR(64) NOT NULL REFERENCES merchants(id),
    penalty_points INTEGER NOT NULL,
    reason TEXT NOT NULL,
    recorded_by VARCHAR(128) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_merchant_penalty_records_points CHECK (penalty_points >= 1),
    CONSTRAINT chk_merchant_penalty_records_reason CHECK (btrim(reason) <> '')
);
CREATE INDEX IF NOT EXISTS idx_merchant_penalty_records_scope
    ON merchant_penalty_records (project_id, merchant_id, recorded_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('merchant.penalty.record', '填写并查看商户处罚记录', 'platform', 'medium', FALSE, 'active'),
       ('merchant.penalty.bounds.manage', '配置本项目单次扣分上下限', 'platform', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'merchant.penalty.record'),
       ('role_platform_admin', 'merchant.penalty.bounds.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON merchant_penalty_point_bounds, merchant_penalty_records TO complex_app';
    END IF;
END $$;
