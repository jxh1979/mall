-- V450 CG-12 涉及员工的人力操作须经 HR 会签＋直属领导向上三级复核（BSM:1478 / BSM:1487 / ENUM:283 / PRD:757、779）：
-- 1) 新审批档位 hr_three_levels_up（主线＝该员工直属上级链前三级、平行线＝人力审核线 hr_review）；
-- 2) HR 会签人须经角色持有的权限码 staff.hr.review（技术决定：HTML 只写「HR」，现有权限表无 HR 岗位码）；
-- 3) 员工状态变更、奖惩入账改为「复核全部通过后写入」的过渡记录表；
-- 4) 员工对已生效扣罚的申诉（同链裁定；撤销＝冲回原调整，原记录保留）。
-- 不回填存量（铁律 8）。

-- 审批档位闭集扩展（V057:19、V068:54）
ALTER TABLE supervisor_review_decisions DROP CONSTRAINT IF EXISTS chk_supervisor_review_approval_level;
ALTER TABLE supervisor_review_decisions
    ADD CONSTRAINT chk_supervisor_review_approval_level
    CHECK (approval_level IS NULL OR approval_level IN
        ('one_level_up', 'two_levels_up', 'top_authority', 'specific_role', 'hr_three_levels_up'));
ALTER TABLE supervisor_review_policies DROP CONSTRAINT IF EXISTS chk_srp_level;
ALTER TABLE supervisor_review_policies
    ADD CONSTRAINT chk_srp_level
    CHECK (approval_level IN ('one_level_up', 'two_levels_up', 'top_authority', 'specific_role', 'hr_three_levels_up'));

-- HR 会签权限码（PGM:658「HR 部门定每个人的层级/职位」：HR 岗位由角色装配持有本码）
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('staff.hr.review', '人力会签复核（涉及员工的人力操作须人力部门会签）', 'staff', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'staff.hr.review')
ON CONFLICT DO NOTHING;

-- 员工状态变更：复核全部通过后写入 staff_profiles.employment_status
CREATE TABLE IF NOT EXISTS staff_employment_status_revisions (
    review_id     VARCHAR(64)  PRIMARY KEY REFERENCES supervisor_review_decisions(review_id),
    profile_id    VARCHAR(64)  NOT NULL REFERENCES staff_profiles(id),
    user_id       VARCHAR(64)  NOT NULL,
    before_status VARCHAR(32)  NOT NULL,
    after_status  VARCHAR(32)  NOT NULL,
    reason        VARCHAR(1000) NOT NULL,
    initiated_by  VARCHAR(64)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    applied_at    TIMESTAMPTZ,
    CONSTRAINT chk_staff_emp_rev_after CHECK (after_status IN ('pending_entry', 'employed', 'trial', 'suspended', 'departed'))
);
CREATE INDEX IF NOT EXISTS idx_staff_emp_rev_profile ON staff_employment_status_revisions (profile_id, applied_at);

-- 奖惩申请：复核全部通过后才写入 staff_payroll_adjustments（id 预分配即最终奖惩记录 id）
CREATE TABLE IF NOT EXISTS staff_payroll_adjustment_requests (
    id           VARCHAR(64)  PRIMARY KEY,
    review_id    VARCHAR(64)  NOT NULL UNIQUE REFERENCES supervisor_review_decisions(review_id),
    user_id      VARCHAR(64)  NOT NULL,
    month        VARCHAR(7)   NOT NULL,
    adj_type     VARCHAR(16)  NOT NULL CHECK (adj_type IN ('bonus', 'penalty')),
    amount_cent  BIGINT       NOT NULL CHECK (amount_cent > 0),
    reason       VARCHAR(255) NOT NULL,
    initiated_by VARCHAR(64)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    applied_at   TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_staff_adj_req_user ON staff_payroll_adjustment_requests (user_id, month);

-- 申诉撤销＝冲回原调整：新增冲回类型 penalty_reversal（负向计入处罚合计），原记录保留并以 reversal_of 关联
ALTER TABLE staff_payroll_adjustments DROP CONSTRAINT IF EXISTS chk_adj_type;
ALTER TABLE staff_payroll_adjustments
    ADD CONSTRAINT chk_adj_type CHECK (adj_type IN ('bonus', 'penalty', 'penalty_reversal'));
ALTER TABLE staff_payroll_adjustments
    ADD COLUMN IF NOT EXISTS reversal_of VARCHAR(64) REFERENCES staff_payroll_adjustments(id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_staff_adj_reversal_of ON staff_payroll_adjustments (reversal_of) WHERE reversal_of IS NOT NULL;

-- 员工申诉（同链裁定；过程与结论留痕）
CREATE TABLE IF NOT EXISTS staff_payroll_adjustment_appeals (
    id                     VARCHAR(64)  PRIMARY KEY,
    adjustment_id          VARCHAR(64)  NOT NULL REFERENCES staff_payroll_adjustments(id),
    user_id                VARCHAR(64)  NOT NULL,
    reason                 VARCHAR(1000) NOT NULL,
    review_id              VARCHAR(64)  NOT NULL UNIQUE REFERENCES supervisor_review_decisions(review_id),
    status                 VARCHAR(16)  NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'upheld', 'revoked')),
    reversal_adjustment_id VARCHAR(64)  REFERENCES staff_payroll_adjustments(id),
    initiated_by           VARCHAR(64)  NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    decided_at             TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_staff_adj_appeal_user ON staff_payroll_adjustment_appeals (user_id, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_staff_adj_appeal_pending ON staff_payroll_adjustment_appeals (adjustment_id) WHERE status = 'pending';
