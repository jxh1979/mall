-- M087 审批链引擎（组织层级逐级节点链）——数据模型。
-- M087-A-01 / F-M087-01（PGM-1.7.1「系统按组织层级自动补全从发起人到该最终审批人之间的全部直接上级节点，逐级审批」）：
--   org_hierarchy —— 每名员工的直接上级（人→人的汇报线，不是行政隶属树）。
-- M087-A-03 / F-M087-03（PGM-1.7.1「每个部门或业务线须预先设定其平行审批的最低起点层级」）：
--   approval_lines —— 平行审批线（如法务、财务）的最低起点层级（level_rank）与起点人。
-- M087-A-02 / F-M087-02（PGM-1.7.1「逐级审批」「任一节点驳回则整单退回起草人」）：
--   supervisor_review_nodes —— 复核单的有序审批节点；line_code=main 为主线，其余为平行线；
--   approver_kind：user=组织层级上的直接上级；master=USB 最高权限主会话；role=按角色解析（specific_role 策略）。

CREATE TABLE IF NOT EXISTS org_hierarchy (
    user_id     VARCHAR(64)  PRIMARY KEY REFERENCES access_users(id) ON DELETE CASCADE,
    superior_id VARCHAR(64)  REFERENCES access_users(id) ON DELETE SET NULL,
    line_code   VARCHAR(64),
    updated_by  VARCHAR(64),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_org_hierarchy_not_self CHECK (superior_id IS NULL OR superior_id <> user_id)
);

CREATE INDEX IF NOT EXISTS idx_org_hierarchy_superior
    ON org_hierarchy (superior_id)
 WHERE superior_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS approval_lines (
    line_code      VARCHAR(64)  PRIMARY KEY,
    line_name      VARCHAR(100) NOT NULL,
    min_start_rank INT          NOT NULL DEFAULT 1 CHECK (min_start_rank BETWEEN 1 AND 30),
    entry_user_id  VARCHAR(64)  REFERENCES access_users(id) ON DELETE SET NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'active',
    updated_by     VARCHAR(64),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS supervisor_review_nodes (
    node_id          VARCHAR(64)  PRIMARY KEY,
    review_id        VARCHAR(64)  NOT NULL REFERENCES supervisor_review_decisions(review_id),
    line_code        VARCHAR(64)  NOT NULL DEFAULT 'main',
    seq              INT          NOT NULL,
    approver_kind    VARCHAR(16)  NOT NULL CHECK (approver_kind IN ('user', 'master', 'role')),
    approver_id      VARCHAR(64),
    status           VARCHAR(20)  NOT NULL DEFAULT 'pending'
                     CHECK (status IN ('pending', 'approved', 'rejected', 'cancelled')),
    decided_by       VARCHAR(64),
    decided_at       TIMESTAMPTZ,
    forwarded_to     VARCHAR(64),
    forwarded_by     VARCHAR(64),
    escalation_count INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_supervisor_review_nodes_seq UNIQUE (review_id, line_code, seq)
);

CREATE INDEX IF NOT EXISTS idx_supervisor_review_nodes_pending
    ON supervisor_review_nodes (review_id)
 WHERE status = 'pending';
