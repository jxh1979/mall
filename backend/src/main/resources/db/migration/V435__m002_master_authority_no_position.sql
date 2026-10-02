-- M002-F19（依据：操作员 2026-09-25 裁决原句「最高权限账号不能被定岗」、治理地图第 703 行「最高权限账号不设岗位、不套用岗位模板：给员工定岗、
-- 换岗的操作不能指向最高权限账号」；以下存量清理为技术推导）：
-- 一次性清掉最高权限账号（usr_master_authority）名下的存量岗位与岗位高危挂起——待审的岗位高危释放、定岗与范围变更申请置 rejected
-- 并关闭其主管复核单（decided_by 同为 system:V435），挂起行删除、岗位派生的模板套用撤销、岗位行删除；
-- 员工显式拒绝（origin=explicit）与其他授权来源不动。
-- 幂等：条件只命中 pending / pending_review / position_hold / active 的行，重复执行不再改动任何行。
UPDATE supervisor_review_nodes
   SET status = 'cancelled', decided_by = 'system:V435', decided_at = now()
 WHERE status = 'pending'
   AND review_id IN (SELECT review_id FROM access_grant_requests
                      WHERE target_user_id = 'usr_master_authority' AND request_type IN ('position_high_risk_release', 'user_position', 'user_scope_change')
                        AND status = 'pending' AND review_id IS NOT NULL);

UPDATE supervisor_review_decisions
   SET review_status = 'rejected', decision = 'rejected', evaluated_at = now()
 WHERE review_status = 'pending_review'
   AND review_id IN (SELECT review_id FROM access_grant_requests
                      WHERE target_user_id = 'usr_master_authority' AND request_type IN ('position_high_risk_release', 'user_position', 'user_scope_change')
                        AND status = 'pending' AND review_id IS NOT NULL);

UPDATE access_grant_requests
   SET status = 'rejected', decided_by = 'system:V435', decided_at = now(), updated_at = now()
 WHERE target_user_id = 'usr_master_authority'
   AND request_type IN ('position_high_risk_release', 'user_position', 'user_scope_change')
   AND status = 'pending';

DELETE FROM user_permission_overrides
 WHERE user_id = 'usr_master_authority'
   AND origin = 'position_hold';

UPDATE permission_template_applications
   SET status = 'revoked', revoked_by = 'system:V435', revoked_at = now()
 WHERE target_type = 'user'
   AND target_id = 'usr_master_authority'
   AND status = 'active'
   AND id IN (SELECT unnest(position_application_ids) FROM user_position_assignments WHERE user_id = 'usr_master_authority');

DELETE FROM user_position_assignments
 WHERE user_id = 'usr_master_authority';
