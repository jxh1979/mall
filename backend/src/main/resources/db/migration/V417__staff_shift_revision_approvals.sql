-- 文件版本：V0.01
-- 草稿和每次修改均追加保存；运行排班是批准后的当前视图。
CREATE TABLE staff_shift_revisions (
    id VARCHAR(64) PRIMARY KEY,
    shift_id VARCHAR(64) NOT NULL,
    review_id VARCHAR(64) NOT NULL,
    before_value JSONB,
    after_value JSONB NOT NULL,
    initiated_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    applied_at TIMESTAMPTZ,
    UNIQUE(review_id, shift_id)
);
CREATE INDEX idx_staff_shift_revision ON staff_shift_revisions(shift_id, created_at);
