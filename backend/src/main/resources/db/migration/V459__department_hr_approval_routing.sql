-- 文件版本：V0.01
-- 人事会签负责人按员工所属部门配置解析，不由业务申请人随单指定。
ALTER TABLE approval_lines
    ADD COLUMN hr_final_approver_id VARCHAR(64) REFERENCES access_users(id) ON DELETE SET NULL;
