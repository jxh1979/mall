-- 文件版本：V0.01
-- 候选 V473；正式编号、创建和落库由根代理负责。
-- 新职责只随新批准排班生效，不推测历史夜班负责人，不回填旧班。
ALTER TABLE staff_shift_schedules
    ADD COLUMN night_duty_project_id VARCHAR(64) REFERENCES projects(id),
    ADD CONSTRAINT chk_staff_night_duty_project
        CHECK (night_duty_project_id IS NULL OR
            (project_id IS NOT NULL AND night_duty_project_id = project_id));

CREATE UNIQUE INDEX uq_staff_night_duty_project_date
    ON staff_shift_schedules(night_duty_project_id, work_date)
    WHERE night_duty_project_id IS NOT NULL;

CREATE INDEX idx_staff_night_duty_period
    ON staff_shift_schedules(night_duty_project_id, start_at, end_at)
    WHERE night_duty_project_id IS NOT NULL;

