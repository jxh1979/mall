-- 文件版本：V0.07
-- M006-006：授权申请的审批事实快照；不回填既有申请，缺快照须重新申请。
ALTER TABLE access_grant_requests ADD COLUMN authorization_fingerprint VARCHAR(64);

-- M006-016：复用主管审批策略表；保留已有初始值与正整数取值域，上下限可在后台修改。
ALTER TABLE supervisor_review_policies ADD COLUMN parameters JSONB NOT NULL DEFAULT '{}';
INSERT INTO supervisor_review_policies
    (policy_code, action_type, object_type, trigger_mode, outcome, approval_level, required_level, parameters)
VALUES ('access_grant', 'access.grant_request', 'access_grant_request', 'permission_insufficient',
        'block', 'one_level_up', 1, '{"windowDays":{"value":30,"min":1,"max":2147483647},"positionThreshold":{"value":3,"min":1,"max":2147483647},"userThreshold":{"value":3,"min":1,"max":2147483647},"riskReviews":{"low":{"approvalLevel":"one_level_up","requiredLevel":1},"medium":{"approvalLevel":"one_level_up","requiredLevel":1},"high":{"approvalLevel":"two_levels_up","requiredLevel":2},"critical":{"approvalLevel":"top_authority","requiredLevel":3}}}');

-- M006-026：沿用存档、主管复核和软删除口径，记录可恢复。
ALTER TABLE position_role_templates
    ADD COLUMN archived_at TIMESTAMPTZ,
    ADD COLUMN archived_by VARCHAR(64),
    ADD COLUMN deleted_at TIMESTAMPTZ,
    ADD COLUMN deleted_by VARCHAR(64),
    ADD COLUMN delete_review_id VARCHAR(128),
    ADD COLUMN delete_reason TEXT;
INSERT INTO supervisor_review_policies
    (policy_code, action_type, object_type, trigger_mode, outcome, approval_level, required_level)
VALUES ('position_role_template_delete', 'access.position_role_template.delete',
        'position_role_template', 'always', 'block', 'top_authority', 3);

-- M006-009/026：岗位派生角色/应用及配置来源；换岗只替换这些来源，独立授权继续有效。
ALTER TABLE user_position_assignments
    ADD COLUMN assigned_role_id VARCHAR(64) REFERENCES roles(id),
    ADD COLUMN assigned_position_template_id VARCHAR(64) REFERENCES position_role_templates(id),
    ADD COLUMN position_application_ids VARCHAR(64)[] NOT NULL DEFAULT '{}';

-- M006-009：旧入口仅岗位应用使用实际操作员ID；独立套用固定写access-api。
-- 只接续成功岗位事件能证明的来源，不按时间接近或相同权限猜测来源。
UPDATE user_position_assignments AS position
   SET assigned_role_id = (
           SELECT CASE WHEN EXISTS (
                      SELECT 1 FROM user_roles legacy WHERE legacy.user_id = position.user_id
                        AND legacy.role_id = event.metadata->>'role_id')
                  THEN event.metadata->>'role_id' END
             FROM audit_events event
            WHERE event.event_name = 'access.user_position.assigned' AND event.event_result = 'success'
              AND event.business_type = 'user_position_assignment' AND event.business_id = position.user_id
            ORDER BY event.occurred_at DESC, event.id DESC LIMIT 1),
       position_application_ids = ARRAY(
           SELECT application.id FROM permission_template_applications application
            WHERE application.target_type = 'user' AND application.target_id = position.user_id
              AND application.applied_by IS NOT NULL AND application.applied_by <> 'access-api'
              AND application.status = 'active'
              AND EXISTS (SELECT 1 FROM audit_events event
                           WHERE event.event_name = 'access.user_position.assigned'
                             AND event.event_result = 'success' AND event.business_type = 'user_position_assignment'
                             AND event.business_id = position.user_id
                             AND event.metadata->>'template_id' = application.template_id));

-- 角色表没有来源列；成功岗位事件识别派生角色，独立显式授予及既有注册/引导角色保留。
DELETE FROM user_roles AS legacy
 WHERE EXISTS (SELECT 1 FROM user_position_assignments position WHERE position.user_id = legacy.user_id)
   AND EXISTS (SELECT 1 FROM audit_events event
                WHERE event.event_name = 'access.user_position.assigned' AND event.event_result = 'success'
                  AND event.business_type = 'user_position_assignment' AND event.business_id = legacy.user_id
                  AND event.metadata->>'role_id' = legacy.role_id)
   AND COALESCE((SELECT event.event_name FROM audit_events event
                  WHERE event.event_name IN ('access.role.assigned', 'access.role.unassigned')
                    AND event.event_result = 'success' AND event.business_type = 'user_role'
                    AND event.business_id = legacy.user_id || ':' || legacy.role_id
                  ORDER BY event.occurred_at DESC, event.id DESC LIMIT 1), '') <> 'access.role.assigned'
   AND NOT EXISTS (
       SELECT 1 FROM access_users account WHERE account.id = legacy.user_id
         AND ((account.account_type = 'platform_admin' AND legacy.role_id = 'role_platform_admin')
           OR (account.partner_org_id IS NOT NULL AND legacy.role_id IN
               ('role_outsourced_security_cleaning', 'role_outsourced_construction', 'role_outsourced_facility'))
           OR (account.account_type = 'staff' AND legacy.role_id = 'role_staff_self')));
