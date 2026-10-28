-- 文件版本：V0.01
-- 第十八批 E 段第 20 条 20-R1 商户加经营项目申请。
-- PGM 第 659 行「一个商户要在别的项目经营的，由能管理商户档案的人在商户档案里加经营项目，要经过审批才生效。」
-- 审批走统一审批模型（PGM 第 528 行，发起人沿直接上级到其指定的最终审批人；第 529 行申请人不得审批自己的申请）。
-- 批准才写入 merchant_projects（V441，结构不动）；本表只记申请与结论。

CREATE TABLE IF NOT EXISTS merchant_project_requests (
    id VARCHAR(64) PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL REFERENCES merchants(id),
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    reason VARCHAR(500) NOT NULL,
    requested_by VARCHAR(64) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    review_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending_approval',
    decided_by VARCHAR(64),
    decided_at TIMESTAMPTZ,
    CONSTRAINT chk_merchant_project_requests_status CHECK (status IN ('pending_approval', 'approved', 'rejected')),
    CONSTRAINT chk_merchant_project_requests_reason CHECK (btrim(reason) <> '')
);
-- 同一商户同一项目同时只能有一张待审批的申请。
CREATE UNIQUE INDEX IF NOT EXISTS uq_merchant_project_requests_pending
    ON merchant_project_requests (merchant_id, project_id)
 WHERE status = 'pending_approval';
CREATE INDEX IF NOT EXISTS idx_merchant_project_requests_review ON merchant_project_requests (review_id);

COMMENT ON TABLE merchant_project_requests IS '商户加经营项目申请（PGM 第 659 行，须审批才生效）；批准写入 merchant_projects';
COMMENT ON COLUMN merchant_project_requests.review_id IS '统一审批复核单编号（动作类型 merchant.project.add）';
COMMENT ON COLUMN merchant_project_requests.decided_by IS '作出最终结论的审批人';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON merchant_project_requests TO complex_app';
    END IF;
END $$;
