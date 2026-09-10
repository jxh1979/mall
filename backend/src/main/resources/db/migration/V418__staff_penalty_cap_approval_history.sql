-- 文件版本：V0.01
CREATE TABLE staff_penalty_cap_revisions (
    review_id VARCHAR(64) PRIMARY KEY,
    before_value INTEGER NOT NULL,
    numeric_value INTEGER NOT NULL,
    min_value INTEGER NOT NULL,
    max_value INTEGER NOT NULL,
    initiated_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    applied_at TIMESTAMPTZ,
    CHECK (0<=min_value AND min_value<=numeric_value AND numeric_value<=max_value AND max_value<=10000)
);
INSERT INTO approval_lines(line_code,line_name,min_start_rank,entry_user_id,status)
VALUES ('hr_review','人力审核线',1,NULL,'active') ON CONFLICT(line_code) DO NOTHING;
-- 已月结单据锁定扣除上限，重新月结不读取后来修改的上限。
ALTER TABLE staff_payroll_statements ADD COLUMN penalty_cap_bps INTEGER NOT NULL DEFAULT 2000
    CHECK (penalty_cap_bps BETWEEN 0 AND 10000);
