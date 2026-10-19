-- 文件版本：V0.01
-- HP-C06 押金退还等待期（按项目可配）。依据：BSM L1352／ENUM L252「押金的退还不在审批通过时立即执行，而是以退租结算算账完成为准执行支付，
-- 且不早于等待期届满（等待期自最后一个审批完成之日起算，业务参数，按项目可配，出厂默认 30 日，须同时配置上下限）」；2026-10-18 操作员裁决第 7 条。
-- 范式同 V447（'__global__' 出厂回落行）与 V497（上下限留空，首次配置必须同时填写；不自拟上下限出厂数值）：项目无自有行即用出厂 30 日。
CREATE TABLE contract_deposit_refund_wait_policy (
    project_id VARCHAR(64) PRIMARY KEY,               -- 综合体项目；'__global__' 出厂回落行
    wait_days INTEGER NOT NULL,                       -- 等待期（日），自最后一个审批完成之日起算
    min_days INTEGER,
    max_days INTEGER,
    updated_by VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_contract_deposit_refund_wait_policy_days CHECK (wait_days >= 0),
    CONSTRAINT chk_contract_deposit_refund_wait_policy_bounds CHECK (
        (min_days IS NULL AND max_days IS NULL)
        OR (min_days IS NOT NULL AND max_days IS NOT NULL
            AND min_days >= 0 AND min_days <= wait_days AND wait_days <= max_days))
);
INSERT INTO contract_deposit_refund_wait_policy (project_id, wait_days) VALUES ('__global__', 30);

-- 配置权限挂平台管理员（服务层另按项目范围校验：项目账号只能改本项目）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('contract.deposit_refund_wait.manage', '配置押金退还等待期（按项目）', 'platform', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'contract.deposit_refund_wait.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
