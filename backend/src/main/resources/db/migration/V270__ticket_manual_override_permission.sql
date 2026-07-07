-- C1/REQ-05 人工放行（主管）高风险权限种子。
-- 服务方法 TicketingService.manualOverrideVerification 早已存在（留痕 status=manual_overridden，
-- 服务层 enforceMerchantScope 租户守卫 + @GuardedAction ticketing.special_release 准入闸），
-- 本次补 HTTP 端点 POST /api/v1/ticketing/verifications/manual-override，端点层按主管权限码 verification.manual_override 控制。
-- 权限码/风险/审批以权威文档 p0-operation-permission-matrix-v0.1.md 为准：
--   verification.manual_override / 高风险 / requires_approval=TRUE（现场主管、总控授权人，须原因+证据）。
-- 列结构参照 V067 ticket.verify.reverse：permission_code/permission_name/domain/risk_level/requires_approval/status。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('verification.manual_override', '人工放行票务核销', 'ticketing', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

-- 基线（V261）：新权限迁移必须同步授 role_platform_admin（平台管理员=全部权限）。
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'verification.manual_override')
ON CONFLICT (role_id, permission_code) DO NOTHING;
