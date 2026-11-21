-- 文件版本：V0.01
-- V729：第十八批 BL 段第 388 条 388-R1（空间补用途与负责人两列，供后台新建与修改空间记录）。
-- 依据：PGM:470「记录空间面积、用途、状态、负责人和所属商户。」；面积、状态、所属商户已有列，本迁移补用途与负责人。
-- 用途记自由文本（三份 HTML 无用途枚举）；负责人记员工账号编号（access_users.id）。可空、不回填（研发阶段无存量，UD-07）。
ALTER TABLE spaces ADD COLUMN purpose VARCHAR(200);
ALTER TABLE spaces ADD COLUMN owner_user_id VARCHAR(64);
COMMENT ON COLUMN spaces.purpose IS '用途';
COMMENT ON COLUMN spaces.owner_user_id IS '负责人员工账号编号';
