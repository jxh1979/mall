-- 文件版本：V0.01
-- M010-006：发起人用于注册审批职责分离，后续更新不覆盖。
ALTER TABLE staff_profiles ADD COLUMN submitted_by VARCHAR(128);
