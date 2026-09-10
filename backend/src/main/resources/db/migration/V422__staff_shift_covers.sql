-- 文件版本：V0.01
-- U06-004（BSM L1443「临时顶班由领班直接安排并在系统记录，不受审批流程限制」）：已开始班次不能改写，顶班只追加记录。
CREATE TABLE staff_shift_covers (
    id VARCHAR(64) PRIMARY KEY,
    shift_id VARCHAR(64) NOT NULL REFERENCES staff_shift_schedules(id),
    original_user_id VARCHAR(64) NOT NULL,
    cover_user_id VARCHAR(64) NOT NULL,
    arranged_by VARCHAR(64) NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_staff_shift_cover_distinct CHECK (cover_user_id <> original_user_id)
);
CREATE INDEX idx_staff_shift_covers_shift ON staff_shift_covers(shift_id);
